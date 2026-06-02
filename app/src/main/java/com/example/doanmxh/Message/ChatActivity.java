package com.example.doanmxh.Message;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.CreatePage.ImageRepository;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────
    private ShapeableImageView imgAvatar;
    private ListenerRegistration userInfoListener;
    private TextView txtChatName, txtOnlineStatus;
    private ImageView pointOnline, btnBack, btnCall, btnMore, btnAttach, btnMic, btnSend;
    private EditText etMessage;
    private RecyclerView rvMessages;

    // ── Reply bar views (thuộc Activity, nằm phía trên input) ───
    private LinearLayout layoutReply;
    private TextView txtReplyName, txtReplyContent;
    private ImageView btnCloseReply;

    // ── Firebase ─────────────────────────────────────────
    private FirebaseFirestore db;
    private ListenerRegistration messageListener;

    // ── Data ─────────────────────────────────────────────
    private String myUid, targetUid, conversationId;
    private List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    // ── Reply state — field cấp class để dùng được trong mọi method ──
    private ChatMessage replyingMessage = null;

    // ── Camera / Gallery ─────────────────────────────────
    private Uri cameraImageUri;
    private ImageRepository imageRepository;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // ════════════════════════════════════════════════════
    //  LAUNCHERS
    // ════════════════════════════════════════════════════
    private final ActivityResultLauncher<Intent> pickImagesLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            List<Uri> uris = new ArrayList<>();
                            Intent data = result.getData();
                            if (data.getClipData() != null) {
                                ClipData clip = data.getClipData();
                                for (int i = 0; i < clip.getItemCount(); i++)
                                    uris.add(clip.getItemAt(i).getUri());
                            } else if (data.getData() != null) {
                                uris.add(data.getData());
                            }
                            if (!uris.isEmpty()) uploadImagesSequentially(uris, 0);
                        }
                    });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    result -> {
                        if (Boolean.TRUE.equals(result) && cameraImageUri != null) {
                            uploadImagesSequentially(
                                    new ArrayList<>(List.of(cameraImageUri)), 0);
                        } else {
                            Toast.makeText(this, "Chụp ảnh thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });

    // ════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        targetUid = getIntent().getStringExtra("target_uid");
        if (targetUid == null || targetUid.isEmpty()) { finish(); return; }

        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (myUid == null) { finish(); return; }

        db              = FirebaseFirestore.getInstance();
        imageRepository = new ImageRepository();

        conversationId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        bindViews();
        setupRecyclerView();
        setupReplyBar();
        loadTargetUserInfo();
        listenMessages();
        setupInputBehavior();
        imgAvatar.setOnClickListener( v -> {
            Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
            intent.putExtra("user_uid", targetUid);
            Log.d("target_uid", targetUid);
            startActivity(intent);
        });
        txtChatName.setOnClickListener( v -> {
            Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
            intent.putExtra("user_uid", targetUid);
            Log.d("target_uid", targetUid);
            startActivity(intent);
        });
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> checkPermissionAndShowSheet());
//        btnCall.setOnClickListener(v -> { /* TODO */ });
        btnMore.setOnClickListener(v -> { /* TODO */ });
        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener  != null) messageListener.remove();
        if (userInfoListener != null) userInfoListener.remove();
    }

    // ════════════════════════════════════════════════════
    //  BIND VIEWS
    // ════════════════════════════════════════════════════
    private void bindViews() {
        imgAvatar       = findViewById(R.id.imgAvatar);
        txtChatName     = findViewById(R.id.txtChatName);
        txtOnlineStatus = findViewById(R.id.txtOnlineStatus);
        pointOnline     = findViewById(R.id.point_active);
        btnBack         = findViewById(R.id.btnBack);
//        btnCall         = findViewById(R.id.btnCall);
        btnMore    = findViewById(R.id.btnMore);
        btnAttach       = findViewById(R.id.btnAttach);
        btnMic          = findViewById(R.id.btnMic);
        btnSend         = findViewById(R.id.btnSend);
        etMessage       = findViewById(R.id.etMessage);
        rvMessages      = findViewById(R.id.rvMessages);

        // Reply bar — những view này nằm trong activity_chat.xml, phía trên input row
        layoutReply     = findViewById(R.id.layoutReply);
        txtReplyName    = findViewById(R.id.txtReplyName);
        txtReplyContent = findViewById(R.id.txtReplyContent);
        btnCloseReply   = findViewById(R.id.btnCloseReply);
    }

    // ════════════════════════════════════════════════════
    //  RECYCLERVIEW
    // ════════════════════════════════════════════════════
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList, myUid, conversationId, msg -> {
            // Callback từ Adapter khi user nhấn "Trả lời"
            replyingMessage = msg;
            showReplyBar(msg);
        },this::scrollToMessage
        );

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        rvMessages.setAdapter(chatAdapter);
    }

    // ════════════════════════════════════════════════════
    //  REPLY BAR
    // ════════════════════════════════════════════════════
    private void scrollToMessage(String messageId) {

        for (int i = 0; i < messageList.size(); i++) {

            ChatMessage msg = messageList.get(i);

            if (messageId.equals(msg.getMessageId())) {

                LinearLayoutManager lm =
                        (LinearLayoutManager) rvMessages.getLayoutManager();

                if (lm != null) {
                    lm.scrollToPositionWithOffset(i, 100);
                }

                break;
            }
        }
    }
    private void setupReplyBar() {
        // Nút X đóng reply bar
        btnCloseReply.setOnClickListener(v -> clearReply());
    }

    private void showReplyBar(ChatMessage msg) {
        db.collection("nguoi_dung").document(msg.getNguoiGuiId())
                        .get()
                                .addOnSuccessListener(v -> {
                                    txtReplyName.setText(v.getString("ten_dang_nhap"));
                                });
        txtReplyContent.setText(
                (msg.getNoiDung() != null && !msg.getNoiDung().isEmpty())
                        ? msg.getNoiDung() : "[Hình ảnh]");
        layoutReply.setVisibility(View.VISIBLE);
        // Focus vào ô nhập để bàn phím hiện lên
        etMessage.requestFocus();
    }

    private void clearReply() {
        replyingMessage = null;
        layoutReply.setVisibility(View.GONE);
        txtReplyName.setText("");
        txtReplyContent.setText("");
    }

    // ════════════════════════════════════════════════════
    //  LOAD THÔNG TIN NGƯỜI DÙNG ĐỐI PHƯƠNG (realtime)
    // ════════════════════════════════════════════════════
    private void loadTargetUserInfo() {
        userInfoListener = db.collection("nguoi_dung").document(targetUid)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    txtChatName.setText(doc.getString("ho_va_ten"));

                    String avatar = doc.getString("anh_dai_dien");
                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(this).load(avatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .circleCrop().into(imgAvatar);
                    }

                    Boolean isOnline = doc.getBoolean("trang_thai_hoat_dong");
                    if (Boolean.TRUE.equals(isOnline)) {
                        txtOnlineStatus.setText("Đang hoạt động");
                        Glide.with(this).load(R.drawable.bg_online_dot).into(pointOnline);
                    } else {
                        Timestamp lastSeen = doc.getTimestamp("lan_cuoi_hoat_dong");
                        if (lastSeen != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat(
                                    "HH:mm dd/MM/yy", Locale.getDefault());
                            txtOnlineStatus.setText(
                                    "Hoạt động lần cuối: " + sdf.format(lastSeen.toDate()));
                        } else {
                            txtOnlineStatus.setText("Không hoạt động");
                        }
                        Glide.with(this).load(R.drawable.bg_offline_dot).into(pointOnline);
                    }
                });
    }

    // ════════════════════════════════════════════════════
    //  LẮNG NGHE TIN NHẮN REALTIME
    // ════════════════════════════════════════════════════
    private void listenMessages() {
        messageListener = db.collection("cuoc_tro_chuyen")
                .document(conversationId)
                .collection("tin_nhan")
                .orderBy("thoi_gian", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                            if (msg != null) {
                                msg.setMessageId(dc.getDocument().getId());

                                // Thêm log này
                                Log.d("MSG_RAW", "loai=" + dc.getDocument().getString("loai")
                                        + " post_id=" + dc.getDocument().getString("post_id")
                                        + " obj_loai=" + msg.getLoai()
                                        + " obj_postId=" + msg.getPostId());
                                if (msg.getNoiDung() == null)
                                    msg.setNoiDung(dc.getDocument().getString("noi_dung"));
                                if (msg.getNguoiGuiId() == null)
                                    msg.setNguoiGuiId(dc.getDocument().getString("nguoi_gui_id"));
                                messageList.add(msg);
                            }
                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String id = dc.getDocument().getId();
                            for (int i = 0; i < messageList.size(); i++) {
                                if (id.equals(messageList.get(i).getMessageId())) {
                                    ChatMessage updated = dc.getDocument().toObject(ChatMessage.class);
                                    if (updated != null) {
                                        updated.setMessageId(id);
                                        updated.setNoiDung(dc.getDocument().getString("noi_dung"));
                                        updated.setNguoiGuiId(dc.getDocument().getString("nguoi_gui_id"));
                                        messageList.set(i, updated);
                                        if (i == messageList.size() - 1)
                                            db.collection("cuoc_tro_chuyen").document(conversationId)
                                                    .update("tin_nhan_cuoi", updated.getNoiDung());
                                    }
                                    break;
                                }
                            }
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            String id = dc.getDocument().getId();
                            for (int i = 0; i < messageList.size(); i++) {
                                if (id.equals(messageList.get(i).getMessageId())) {
                                    if (i == messageList.size() - 1)
                                        db.collection("cuoc_tro_chuyen").document(conversationId)
                                                .update("tin_nhan_cuoi", "Tin nhắn đã bị xóa");
                                    messageList.remove(i);
                                    break;
                                }
                            }
                        }
                    }

                    chatAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty())
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    markMessagesAsRead();
                });
    }

    // ════════════════════════════════════════════════════
    //  GỬI TIN NHẮN TEXT
    // ════════════════════════════════════════════════════
//    private void sendMessage() {
//        String content = etMessage.getText().toString().trim();
//        if (content.isEmpty()) return;
//        etMessage.setText("");
//
//        Map<String, Object> message = new HashMap<>();
//        message.put("nguoi_gui_id", myUid);
//        message.put("noi_dung", content);
//        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
//        message.put("da_doc", false);
//        message.put("loai", "text");
//
//        // Đính kèm thông tin reply nếu đang reply
//        if (replyingMessage != null) {
//
//            String replyId = replyingMessage.getMessageId();
//
//            db.collection("cuoc_tro_chuyen")
//                    .document(conversationId)
//                    .collection("tin_nhan")
//                    .document(replyId)
//                    .get()
//                    .addOnSuccessListener(replyDoc -> {
//
//                        String replySenderUid =
//                                replyDoc.getString("nguoi_gui_id");
//
//                        db.collection("nguoi_dung")
//                                .document(replySenderUid)
//                                .get()
//                                .addOnSuccessListener(userDoc -> {
//
//                                    String senderName =
//                                            userDoc.getString("ten_dang_nhap");
//
//                                    message.put("reply_to_id", replyId);
//                                    message.put("reply_to_content",
//                                            replyingMessage.getNoiDung() != null
//                                                    ? replyingMessage.getNoiDung()
//                                                    : "[Hình ảnh]");
//
//                                    message.put("reply_to_sender_name",
//                                            senderName != null
//                                                    ? senderName
//                                                    : "");
//
//                                    clearReply();
//
//                                    db.collection("cuoc_tro_chuyen")
//                                            .document(conversationId)
//                                            .collection("tin_nhan")
//                                            .add(message)
//                                            .addOnSuccessListener(ref ->
//                                                    updateLastMessage(
//                                                            content,
//                                                            ref.getId(),
//                                                            "text"));
//                                });
//                    });
//
//            return;
//        }
//    }
    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        etMessage.setText("");

        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", content);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "text");

        // ────────────────
        // CASE 1: KHÔNG REPLY
        // ────────────────
        if (replyingMessage == null) {

            db.collection("cuoc_tro_chuyen")
                    .document(conversationId)
                    .collection("tin_nhan")
                    .add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage(content, ref.getId(), "text"));

            return;
        }

        // ────────────────
        // CASE 2: CÓ REPLY
        // ────────────────
        String replyId = replyingMessage.getMessageId();

        db.collection("cuoc_tro_chuyen")
                .document(conversationId)
                .collection("tin_nhan")
                .document(replyId)
                .get()
                .addOnSuccessListener(replyDoc -> {

                    String replySenderUid = replyDoc.getString("nguoi_gui_id");

                    db.collection("nguoi_dung")
                            .document(replySenderUid)
                            .get()
                            .addOnSuccessListener(userDoc -> {

                                String senderName = userDoc.getString("ten_dang_nhap");

                                message.put("reply_to_id", replyId);
                                message.put("reply_to_content",
                                        replyingMessage.getNoiDung() != null
                                                ? replyingMessage.getNoiDung()
                                                : "[Hình ảnh]");

                                message.put("reply_to_sender_name",
                                        senderName != null ? senderName : "");

                                clearReply();

                                db.collection("cuoc_tro_chuyen")
                                        .document(conversationId)
                                        .collection("tin_nhan")
                                        .add(message)
                                        .addOnSuccessListener(ref ->
                                                updateLastMessage(content, ref.getId(), "text"));
                            });
                });
    }

    // ════════════════════════════════════════════════════
    //  GỬI TIN NHẮN ẢNH
    // ════════════════════════════════════════════════════
//    private void sendImageMessage(String imageUrl) {
//        Map<String, Object> message = new HashMap<>();
//        message.put("nguoi_gui_id", myUid);
//        message.put("noi_dung", imageUrl);
//        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
//        message.put("da_doc", false);
//        message.put("loai", "image");
//
//        // Đính kèm thông tin reply nếu đang reply
//        if (replyingMessage != null) {
//            message.put("reply_to_id",          replyingMessage.getMessageId());
//            message.put("reply_to_content",     replyingMessage.getNoiDung() != null
//                    ? replyingMessage.getNoiDung() : "[Hình ảnh]");
//            message.put("reply_to_sender_name", replyingMessage.getTenNguoiGui() != null
//                    ? replyingMessage.getTenNguoiGui() : "");
//            clearReply();
//        }
//
//        db.collection("cuoc_tro_chuyen").document(conversationId)
//                .collection("tin_nhan").add(message)
//                .addOnSuccessListener(ref ->
//                        updateLastMessage("[Hình ảnh]", ref.getId(), "image"));
//    }
    private void sendImageMessage(String imageUrl) {

        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", imageUrl);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "image");

        // ───────────────
        // NO REPLY
        // ───────────────
        if (replyingMessage == null) {

            db.collection("cuoc_tro_chuyen")
                    .document(conversationId)
                    .collection("tin_nhan")
                    .add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage("[Hình ảnh]", ref.getId(), "image"));

            return;
        }

        String replyId = replyingMessage.getMessageId();
        String senderUid = replyingMessage.getNguoiGuiId();

        message.put("reply_to_id", replyId);
        message.put("reply_to_content",
                replyingMessage.getNoiDung() != null
                        ? replyingMessage.getNoiDung()
                        : "[Hình ảnh]");

        // ───────────────
        // LẤY NAME TỪ DATABASE
        // ───────────────
        db.collection("nguoi_dung")
                .document(senderUid)
                .get()
                .addOnSuccessListener(doc -> {

                    String senderName = doc.getString("ten_dang_nhap");

                    message.put("reply_to_sender_name",
                            senderName != null ? senderName : "");

                    clearReply();

                    db.collection("cuoc_tro_chuyen")
                            .document(conversationId)
                            .collection("tin_nhan")
                            .add(message)
                            .addOnSuccessListener(ref ->
                                    updateLastMessage("[Hình ảnh]", ref.getId(), "image"));
                });
    }

    // ════════════════════════════════════════════════════
    //  CẬP NHẬT TIN NHẮN CUỐI CÙNG
    // ════════════════════════════════════════════════════
    private void updateLastMessage(String content, String messageId, String loai) {
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("tin_nhan_cuoi",      content);
        conversation.put("thoi_gian_cuoi",
                com.google.firebase.firestore.FieldValue.serverTimestamp());
        conversation.put("nguoi_gui_cuoi_id",  myUid);
        conversation.put("loai_tin_nhan_cuoi", loai);
        conversation.put("id_tin_nhan_cuoi",   messageId);
        conversation.put("thanh_vien",
                java.util.Arrays.asList(myUid, targetUid));

        db.collection("cuoc_tro_chuyen").document(conversationId)
                .set(conversation, com.google.firebase.firestore.SetOptions.merge());
    }

    // ════════════════════════════════════════════════════
    //  CHỌN ẢNH - BOTTOM SHEET
    // ════════════════════════════════════════════════════
    private void checkPermissionAndShowSheet() {
        String readPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, readPermission)
                != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(readPermission);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(android.Manifest.permission.CAMERA);

        if (permissionsNeeded.isEmpty()) {
            showImagePickerSheet();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void showImagePickerSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_image_picker, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.layoutCamera).setOnClickListener(v -> {
            dialog.dismiss();
            openCamera();
        });
        sheetView.findViewById(R.id.layoutGallery).setOnClickListener(v -> {
            dialog.dismiss();
            openGallery();
        });
        sheetView.findViewById(R.id.layoutCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(intent);
    }

    private void openCamera() {
        File file = new File(getCacheDir(),
                "camera_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
        cameraLauncher.launch(cameraImageUri);
    }

    // ════════════════════════════════════════════════════
    //  UPLOAD ẢNH (tuần tự qua imgbb)
    // ════════════════════════════════════════════════════
    private void uploadImagesSequentially(List<Uri> uris, int index) {
        if (index >= uris.size()) {
            btnAttach.setEnabled(true);
            return;
        }
        btnAttach.setEnabled(false);

        imageRepository.uploadImage(this, uris.get(index),
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        sendImageMessage(url);
                        uploadImagesSequentially(uris, index + 1);
                    }
                    @Override
                    public void onError() {
                        Toast.makeText(ChatActivity.this,
                                "Upload ảnh " + (index + 1) + " thất bại",
                                Toast.LENGTH_SHORT).show();
                        btnAttach.setEnabled(true);
                    }
                });
    }

    // ════════════════════════════════════════════════════
    //  ĐÁNH DẤU ĐÃ ĐỌC
    // ════════════════════════════════════════════════════
    private void markMessagesAsRead() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("nguoi_dung").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    Boolean isActive = userDoc.getBoolean("trang_thai_hoat_dong");
                    if (isActive == null || !isActive) return;

                    db.collection("cuoc_tro_chuyen")
                            .document(conversationId)
                            .collection("tin_nhan")
                            .whereEqualTo("nguoi_gui_id", targetUid)
                            .whereEqualTo("da_doc", false)
                            .get()
                            .addOnSuccessListener(qs -> {
                                for (DocumentSnapshot doc : qs.getDocuments())
                                    doc.getReference().update("da_doc", true);
                            });
                });
    }

    // ════════════════════════════════════════════════════
    //  TOGGLE MIC / SEND
    // ════════════════════════════════════════════════════
    private void setupInputBehavior() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnMic.setVisibility(hasText ? View.GONE  : View.VISIBLE);
                btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ════════════════════════════════════════════════════
    //  XỬ LÝ KẾT QUẢ XIN QUYỀN
    // ════════════════════════════════════════════════════
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showImagePickerSheet();
        } else {
            Toast.makeText(this, "Cần cấp quyền để chọn ảnh",
                    Toast.LENGTH_SHORT).show();
        }
    }
}