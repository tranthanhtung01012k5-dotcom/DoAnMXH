package com.example.doanmxh.Message;

import android.content.ClipData;
import android.content.Context;
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
import com.example.doanmxh.BaseActivity;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    // ── Views ────────────────────────────────────────────
    private ShapeableImageView imgAvatar;
    private ListenerRegistration userInfoListener;
    private TextView txtChatName, txtOnlineStatus;
    private ImageView pointOnline, btnBack, btnCall, btnSearch, btnAttach, btnMic, btnSend,btnTimNhanh;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private boolean isPinnedExpanded = false;

    // thêm field
    private RecyclerView rvPinnedMessages;
    private TextView txtPinnedCount;
    private ImageView btnExpandPinned;
    private LinearLayout layoutPinnedContainer;
    private final List<Map<String,Object>> pinnedMessages = new ArrayList<>();

    private List<Integer> searchResultIndexes = new ArrayList<>();
    private int currentSearchIndex = -1;
    private final SimpleDateFormat searchTimeFormat =
            new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
    private PinnedMessageAdapter pinnedAdapter;


    private Context content ;
    private ImageView btnUnpin;
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
        setupInputBehavior();
        setupPinnedRecycler();
        listenMessages();
        listenPinnedMessage();

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
        btnTimNhanh.setOnClickListener(v -> {
            sendTimNhanh();
        });
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> checkPermissionAndShowSheet());
//        btnCall.setOnClickListener(v -> { /* TODO */ });
        btnSearch.setOnClickListener(v -> openSearchSheet());
        btnSend.setOnClickListener(v -> sendMessage());
        btnExpandPinned.setOnClickListener(v -> {
            togglePinnedList();
        });
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
        btnSearch    = findViewById(R.id.btnSearch);
        btnTimNhanh = findViewById(R.id.btnTimNhanh);
        btnAttach       = findViewById(R.id.btnAttach);
        btnMic          = findViewById(R.id.btnMic);
        btnSend         = findViewById(R.id.btnSend);
        etMessage       = findViewById(R.id.etMessage);
        rvMessages      = findViewById(R.id.rvMessages);
        rvPinnedMessages = findViewById(R.id.rvPinnedMessages);
        txtPinnedCount = findViewById(R.id.txtPinnedCount);
        btnExpandPinned = findViewById(R.id.btnExpandPinned);
        layoutPinnedContainer = findViewById(R.id.layoutPinnedContainer);

        // Reply bar — những view này nằm trong activity_chat.xml, phía trên input row
        layoutReply     = findViewById(R.id.layoutReply);
        txtReplyName    = findViewById(R.id.txtReplyName);
        txtReplyContent = findViewById(R.id.txtReplyContent);
        btnCloseReply   = findViewById(R.id.btnCloseReply);
    }

    // ════════════════════════════════════════════════════
    //  RECYCLERVIEW
    // ════════════════════════════════════════════════════
    private void setupPinnedRecycler() {

        pinnedAdapter = new PinnedMessageAdapter(
                pinnedMessages,
                pin -> {

                    String messageId =
                            (String) pin.get("message_id");
                    Log.d("PIN", "messageId = " + messageId);
                    if (messageId != null) {
                        scrollToMessage(messageId);
                        chatAdapter.highlightMessage(messageId);
                    }
                });

        rvPinnedMessages.setLayoutManager(
                new LinearLayoutManager(this));

        rvPinnedMessages.setAdapter(pinnedAdapter);
    }
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this,messageList, myUid, conversationId, msg -> {
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
    private void sendTimNhanh()
    {   String content = "❤\uFE0F";

        if (content.isEmpty()) return;

        etMessage.setText("");

        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", content);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "tim");

        // ────────────────
        // CASE 1: KHÔNG REPLY
        // ────────────────
        if (replyingMessage == null) {

            db.collection("cuoc_tro_chuyen")
                    .document(conversationId)
                    .collection("tin_nhan")
                    .add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage(content, ref.getId(), "tim"));

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
                                                updateLastMessage(content, ref.getId(), "tim"));
                            });
                });
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

                    boolean hasNewMessage = false; // ✅ flag theo dõi

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                            if (msg != null) {
                                msg.setMessageId(dc.getDocument().getId());
                                msg.setDaDoc(Boolean.TRUE.equals(
                                        dc.getDocument().getBoolean("da_doc")));
                                if (msg.getNoiDung() == null)
                                    msg.setNoiDung(dc.getDocument().getString("noi_dung"));
                                if (msg.getNguoiGuiId() == null)
                                    msg.setNguoiGuiId(dc.getDocument().getString("nguoi_gui_id"));

                                int prevLastIndex = messageList.size() - 1; // ✅ lưu index cũ

                                messageList.add(msg);
                                hasNewMessage = true;

                                // ✅ notify item cũ để nó ẩn timestamp đi
                                if (prevLastIndex >= 0) {
                                    chatAdapter.notifyItemChanged(prevLastIndex);
                                }
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
                                        updated.setDaDoc(Boolean.TRUE.equals(
                                                dc.getDocument().getBoolean("da_doc")));
                                        messageList.set(i, updated);
                                        // ✅ Chỉ notify item đó, KHÔNG scroll
                                        chatAdapter.notifyItemChanged(i);
                                    }
                                    break;
                                }
                            }

                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            String id = dc.getDocument().getId();
                            for (int i = 0; i < messageList.size(); i++) {
                                if (id.equals(messageList.get(i).getMessageId())) {
                                    if (i == messageList.size() - 1)
                                        db.collection("cuoc_tro_chuyen")
                                                .document(conversationId)
                                                .update("tin_nhan_cuoi", "Tin nhắn đã bị xóa");
                                    messageList.remove(i);
                                    chatAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                        }
                    }

                    // ✅ Chỉ scroll xuống cuối khi có tin nhắn MỚI THÊM VÀO
                    if (hasNewMessage) {
                        chatAdapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }

                    markMessagesAsRead();
                });
    }

    private void listenPinnedMessage() {

        db.collection("cuoc_tro_chuyen")
                .document(conversationId)
                .collection("tin_nhan_ghim")
                .orderBy(
                        "thoi_gian_ghim",
                        Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null)
                        return;

                    pinnedMessages.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        Map<String,Object> data = doc.getData();

                        if (data != null) {
                            pinnedMessages.add(data);
                        }
                    }

                    updatePinnedUI();
                });
    }
    private void updatePinnedUI() {

        if (pinnedMessages.isEmpty()) {
            layoutPinnedContainer.setVisibility(View.GONE);
            return;
        }

        layoutPinnedContainer.setVisibility(View.VISIBLE);

        txtPinnedCount.setText(pinnedMessages.size() + " tin nhắn ghim");

        Log.d("PIN_TEST", "size = " + pinnedMessages.size());

        if (pinnedAdapter != null) {
            pinnedAdapter.notifyDataSetChanged();
        }

        // optional: giữ trạng thái hiện tại
        rvPinnedMessages.setVisibility(
                isPinnedExpanded ? View.VISIBLE : View.GONE
        );
    }
    private void togglePinnedList() {

        isPinnedExpanded = !isPinnedExpanded;

        rvPinnedMessages.setVisibility(
                isPinnedExpanded ? View.VISIBLE : View.GONE
        );

        btnExpandPinned.setRotation(
                isPinnedExpanded ? 180f : 0f
        );
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
    private void searchMessages(String keyword) {
        searchResultIndexes.clear();
        currentSearchIndex = -1;

        if (keyword == null || keyword.trim().isEmpty()) {
            chatAdapter.highlightMessage(null);
            chatAdapter.notifyDataSetChanged();
            return;
        }

        String lowerKey = keyword.toLowerCase(Locale.ROOT);

        for (int i = 0; i < messageList.size(); i++) {
            ChatMessage msg = messageList.get(i);

            if (msg.getNoiDung() != null &&
                    msg.getNoiDung().toLowerCase(Locale.ROOT).contains(lowerKey)) {

                searchResultIndexes.add(i);
            }
        }

        if (searchResultIndexes.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        // nhảy tới kết quả đầu tiên
        goToSearchResult(0);
    }

    private void goToSearchResult(int index) {

        if (searchResultIndexes.isEmpty()) return;

        if (index < 0 || index >= searchResultIndexes.size()) return;

        currentSearchIndex = index;

        int position = searchResultIndexes.get(index);

        rvMessages.scrollToPosition(position);

        chatAdapter.highlightMessage(
                messageList.get(position).getMessageId()
        );
    }
    private void nextSearchResult() {
        if (searchResultIndexes.isEmpty()) return;

        int next = currentSearchIndex + 1;

        if (next >= searchResultIndexes.size()) {
            next = 0; // vòng lại đầu
        }

        goToSearchResult(next);
    }
    private void previousSearchResult() {
        if (searchResultIndexes.isEmpty()) return;

        int prev = currentSearchIndex - 1;

        if (prev < 0) {
            prev = searchResultIndexes.size() - 1;
        }

        goToSearchResult(prev);
    }
    private void openSearchSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_search_message, null);

        EditText edtSearch = view.findViewById(R.id.edtSearch);
        RecyclerView rv = view.findViewById(R.id.rvSearchResult);

        List<SearchItem> results = new ArrayList<>();
        SearchMessageAdapter adapter = new SearchMessageAdapter(results, item -> {
            dialog.dismiss();
            scrollToPosition(item.position);
            chatAdapter.highlightMessage(item.messageId);

        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {

                String key = s.toString().toLowerCase(Locale.ROOT);
                results.clear();

                for (int i = 0; i < messageList.size(); i++) {

                    ChatMessage msg = messageList.get(i);

                    if (msg.getNoiDung() != null &&
                            msg.getNoiDung().toLowerCase(Locale.ROOT).contains(key)) {

                        Date ts =  msg.getThoiGian();

                        String timeStr = "";
                        if (ts != null) {
                            timeStr = searchTimeFormat.format(ts);
                        }

                        results.add(new SearchItem(
                                msg.getMessageId(),
                                msg.getNoiDung(),
                                ts,
                                i
                        ));
                    }
                }

                adapter.notifyDataSetChanged();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }
    private void scrollToPosition(int position) {
        LinearLayoutManager lm =
                (LinearLayoutManager) rvMessages.getLayoutManager();

        if (lm != null) {
            lm.scrollToPositionWithOffset(position, 100);
        }
    }
    // ════════════════════════════════════════════════════
    //  TOGGLE MIC / SEND
    // ════════════════════════════════════════════════════
    private void setupInputBehavior() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnTimNhanh.setVisibility(hasText ? View.GONE  : View.VISIBLE);
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