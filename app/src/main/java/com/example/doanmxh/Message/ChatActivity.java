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
    private ImageView pointOnline, btnBack, btnCall, btnSearch, btnAttach, btnSend, btnTimNhanh;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private boolean isPinnedExpanded = false;

    private RecyclerView rvPinnedMessages;
    private TextView txtPinnedCount;
    private ImageView btnExpandPinned;
    private LinearLayout layoutPinnedContainer;
    private final List<Map<String, Object>> pinnedMessages = new ArrayList<>();

    private List<Integer> searchResultIndexes = new ArrayList<>();
    private int currentSearchIndex = -1;
    private final SimpleDateFormat searchTimeFormat =
            new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
    private PinnedMessageAdapter pinnedAdapter;
    private static final int PERMISSION_AUDIO_CODE = 102;

    private Context content;
    private ImageView btnUnpin;

    // ── Reply bar views ───
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

    // ── Reply state ──
    private ChatMessage replyingMessage = null;

    // ── Camera / Gallery ─────────────────────────────────
    private Uri cameraImageUri;
    private Uri cameraVideoUri;
    private ImageRepository imageRepository;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // ════════════════════════════════════════════════════
    //  LAUNCHERS
    // ════════════════════════════════════════════════════

    /** Chọn ảnh + video từ thư viện (image/* và video/*) */
    private final ActivityResultLauncher<Intent> pickMediaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            List<Uri> imageUris = new ArrayList<>();
                            List<Uri> videoUris = new ArrayList<>();
                            Intent data = result.getData();

                            List<Uri> allUris = new ArrayList<>();
                            if (data.getClipData() != null) {
                                ClipData clip = data.getClipData();
                                for (int i = 0; i < clip.getItemCount(); i++)
                                    allUris.add(clip.getItemAt(i).getUri());
                            } else if (data.getData() != null) {
                                allUris.add(data.getData());
                            }

                            // Phân loại ảnh / video dựa vào MIME type
                            for (Uri uri : allUris) {
                                String mime = getContentResolver().getType(uri);
                                if (mime != null && mime.startsWith("video/")) {
                                    videoUris.add(uri);
                                } else {
                                    imageUris.add(uri);
                                }
                            }

                            if (!imageUris.isEmpty()) uploadImagesSequentially(imageUris, 0);
                            if (!videoUris.isEmpty()) uploadVideosSequentially(videoUris, 0);
                        }
                    });

    /** Chụp ảnh bằng camera */
    private final ActivityResultLauncher<Uri> cameraPhotoLauncher =
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

    /** Quay video bằng camera */
    private final ActivityResultLauncher<Uri> cameraVideoLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CaptureVideo(),
                    result -> {
                        if (Boolean.TRUE.equals(result) && cameraVideoUri != null) {
                            uploadVideosSequentially(
                                    new ArrayList<>(List.of(cameraVideoUri)), 0);
                        } else {
                            Toast.makeText(this, "Quay video thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });

    /** Chọn file âm thanh */
    private final ActivityResultLauncher<Intent> pickAudioLauncher =
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
                            if (!uris.isEmpty()) uploadAudiosSequentially(uris, 0);
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
        conversationId  = myUid.compareTo(targetUid) < 0
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

        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
            intent.putExtra("user_uid", targetUid);
            Log.d("target_uid", targetUid);
            startActivity(intent);
        });
        txtChatName.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
            intent.putExtra("user_uid", targetUid);
            Log.d("target_uid", targetUid);
            startActivity(intent);
        });
        btnTimNhanh.setOnClickListener(v -> sendTimNhanh());
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> checkPermissionAndShowSheet());
        btnSearch.setOnClickListener(v -> openSearchSheet());
        btnSend.setOnClickListener(v -> sendMessage());
        btnExpandPinned.setOnClickListener(v -> togglePinnedList());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener  != null) messageListener.remove();
        if (userInfoListener != null) userInfoListener.remove();
        if (chatAdapter      != null) chatAdapter.releaseAudio();
    }

    // ════════════════════════════════════════════════════
    //  BIND VIEWS
    // ════════════════════════════════════════════════════
    private void bindViews() {
        imgAvatar             = findViewById(R.id.imgAvatar);
        txtChatName           = findViewById(R.id.txtChatName);
        txtOnlineStatus       = findViewById(R.id.txtOnlineStatus);
        pointOnline           = findViewById(R.id.point_active);
        btnBack               = findViewById(R.id.btnBack);
        btnSearch             = findViewById(R.id.btnSearch);
        btnTimNhanh           = findViewById(R.id.btnTimNhanh);
        btnAttach             = findViewById(R.id.btnAttach);
        btnSend               = findViewById(R.id.btnSend);
        etMessage             = findViewById(R.id.etMessage);
        rvMessages            = findViewById(R.id.rvMessages);
        rvPinnedMessages      = findViewById(R.id.rvPinnedMessages);
        txtPinnedCount        = findViewById(R.id.txtPinnedCount);
        btnExpandPinned       = findViewById(R.id.btnExpandPinned);
        layoutPinnedContainer = findViewById(R.id.layoutPinnedContainer);
        layoutReply           = findViewById(R.id.layoutReply);
        txtReplyName          = findViewById(R.id.txtReplyName);
        txtReplyContent       = findViewById(R.id.txtReplyContent);
        btnCloseReply         = findViewById(R.id.btnCloseReply);
    }

    // ════════════════════════════════════════════════════
    //  RECYCLERVIEW
    // ════════════════════════════════════════════════════
    private void setupPinnedRecycler() {
        pinnedAdapter = new PinnedMessageAdapter(
                pinnedMessages,
                pin -> {
                    String messageId = (String) pin.get("message_id");
                    Log.d("PIN", "messageId = " + messageId);
                    if (messageId != null) {
                        scrollToMessage(messageId);
                        chatAdapter.highlightMessage(messageId);
                    }
                });
        rvPinnedMessages.setLayoutManager(new LinearLayoutManager(this));
        rvPinnedMessages.setAdapter(pinnedAdapter);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, messageList, myUid, conversationId, msg -> {
            replyingMessage = msg;
            showReplyBar(msg);
        }, this::scrollToMessage);

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
                LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
                if (lm != null) lm.scrollToPositionWithOffset(i, 100);
                break;
            }
        }
    }

    private void setupReplyBar() {
        btnCloseReply.setOnClickListener(v -> clearReply());
    }

    private void sendTimNhanh() {
        String content = "❤\uFE0F";
        etMessage.setText("");

        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", content);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "tim");

        if (replyingMessage == null) {
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").add(message)
                    .addOnSuccessListener(ref -> updateLastMessage(content, ref.getId(), "tim"));
            return;
        }

        String replyId = replyingMessage.getMessageId();
        db.collection("cuoc_tro_chuyen").document(conversationId)
                .collection("tin_nhan").document(replyId).get()
                .addOnSuccessListener(replyDoc -> {
                    String replySenderUid = replyDoc.getString("nguoi_gui_id");
                    db.collection("nguoi_dung").document(replySenderUid).get()
                            .addOnSuccessListener(userDoc -> {
                                String senderName = userDoc.getString("ten_dang_nhap");
                                message.put("reply_to_id", replyId);
                                message.put("reply_to_content",
                                        replyingMessage.getNoiDung() != null
                                                ? replyingMessage.getNoiDung() : "[Hình ảnh]");
                                message.put("reply_to_sender_name",
                                        senderName != null ? senderName : "");
                                clearReply();
                                db.collection("cuoc_tro_chuyen").document(conversationId)
                                        .collection("tin_nhan").add(message)
                                        .addOnSuccessListener(ref ->
                                                updateLastMessage(content, ref.getId(), "tim"));
                            });
                });
    }

    private void showReplyBar(ChatMessage msg) {
        db.collection("nguoi_dung").document(msg.getNguoiGuiId()).get()
                .addOnSuccessListener(v -> txtReplyName.setText(v.getString("ten_dang_nhap")));
        txtReplyContent.setText(
                (msg.getNoiDung() != null && !msg.getNoiDung().isEmpty())
                        ? msg.getNoiDung() : "[Hình ảnh]");
        layoutReply.setVisibility(View.VISIBLE);
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
                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("HH:mm dd/MM/yy", Locale.getDefault());
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

                    boolean hasNewMessage = false;

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

                                int prevLastIndex = messageList.size() - 1;
                                messageList.add(msg);
                                hasNewMessage = true;
                                if (prevLastIndex >= 0)
                                    chatAdapter.notifyItemChanged(prevLastIndex);
                            }

                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String id = dc.getDocument().getId();
                            for (int i = 0; i < messageList.size(); i++) {
                                if (id.equals(messageList.get(i).getMessageId())) {
                                    ChatMessage updated =
                                            dc.getDocument().toObject(ChatMessage.class);
                                    if (updated != null) {
                                        updated.setMessageId(id);
                                        updated.setNoiDung(
                                                dc.getDocument().getString("noi_dung"));
                                        updated.setNguoiGuiId(
                                                dc.getDocument().getString("nguoi_gui_id"));
                                        updated.setDaDoc(Boolean.TRUE.equals(
                                                dc.getDocument().getBoolean("da_doc")));
                                        messageList.set(i, updated);
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
                .orderBy("thoi_gian_ghim", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    pinnedMessages.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) pinnedMessages.add(data);
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
        if (pinnedAdapter != null) pinnedAdapter.notifyDataSetChanged();
        rvPinnedMessages.setVisibility(isPinnedExpanded ? View.VISIBLE : View.GONE);
    }

    private void togglePinnedList() {
        isPinnedExpanded = !isPinnedExpanded;
        rvPinnedMessages.setVisibility(isPinnedExpanded ? View.VISIBLE : View.GONE);
        btnExpandPinned.setRotation(isPinnedExpanded ? 180f : 0f);
    }

    // ════════════════════════════════════════════════════
    //  GỬI TIN NHẮN TEXT
    // ════════════════════════════════════════════════════
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

        if (replyingMessage == null) {
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage(content, ref.getId(), "text"));
            return;
        }

        String replyId = replyingMessage.getMessageId();
        db.collection("cuoc_tro_chuyen").document(conversationId)
                .collection("tin_nhan").document(replyId).get()
                .addOnSuccessListener(replyDoc -> {
                    String replySenderUid = replyDoc.getString("nguoi_gui_id");
                    db.collection("nguoi_dung").document(replySenderUid).get()
                            .addOnSuccessListener(userDoc -> {
                                String senderName = userDoc.getString("ten_dang_nhap");
                                message.put("reply_to_id", replyId);
                                message.put("reply_to_content",
                                        replyingMessage.getNoiDung() != null
                                                ? replyingMessage.getNoiDung() : "[Hình ảnh]");
                                message.put("reply_to_sender_name",
                                        senderName != null ? senderName : "");
                                clearReply();
                                db.collection("cuoc_tro_chuyen").document(conversationId)
                                        .collection("tin_nhan").add(message)
                                        .addOnSuccessListener(ref ->
                                                updateLastMessage(content, ref.getId(), "text"));
                            });
                });
    }

    // ════════════════════════════════════════════════════
    //  GỬI TIN NHẮN ẢNH
    // ════════════════════════════════════════════════════
    private void sendImageMessage(String imageUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", imageUrl);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "image");

        if (replyingMessage == null) {
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage("[Hình ảnh]", ref.getId(), "image"));
            return;
        }

        String replyId   = replyingMessage.getMessageId();
        String senderUid = replyingMessage.getNguoiGuiId();
        message.put("reply_to_id", replyId);
        message.put("reply_to_content",
                replyingMessage.getNoiDung() != null
                        ? replyingMessage.getNoiDung() : "[Hình ảnh]");

        db.collection("nguoi_dung").document(senderUid).get()
                .addOnSuccessListener(doc -> {
                    message.put("reply_to_sender_name",
                            doc.getString("ten_dang_nhap") != null
                                    ? doc.getString("ten_dang_nhap") : "");
                    clearReply();
                    db.collection("cuoc_tro_chuyen").document(conversationId)
                            .collection("tin_nhan").add(message)
                            .addOnSuccessListener(ref ->
                                    updateLastMessage("[Hình ảnh]", ref.getId(), "image"));
                });
    }

    // ════════════════════════════════════════════════════
    //  GỬI TIN NHẮN VIDEO
    // ════════════════════════════════════════════════════
    private void sendVideoMessage(String videoUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", videoUrl);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "video");

        if (replyingMessage == null) {
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage("[Video]", ref.getId(), "video"));
            return;
        }

        String replyId   = replyingMessage.getMessageId();
        String senderUid = replyingMessage.getNguoiGuiId();
        message.put("reply_to_id", replyId);
        message.put("reply_to_content",
                replyingMessage.getNoiDung() != null
                        ? replyingMessage.getNoiDung() : "[Video]");

        db.collection("nguoi_dung").document(senderUid).get()
                .addOnSuccessListener(doc -> {
                    message.put("reply_to_sender_name",
                            doc.getString("ten_dang_nhap") != null
                                    ? doc.getString("ten_dang_nhap") : "");
                    clearReply();
                    db.collection("cuoc_tro_chuyen").document(conversationId)
                            .collection("tin_nhan").add(message)
                            .addOnSuccessListener(ref ->
                                    updateLastMessage("[Video]", ref.getId(), "video"));
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
    //  BOTTOM SHEET CHỌN MEDIA / AUDIO
    // ════════════════════════════════════════════════════
    private void checkPermissionAndShowSheet() {
        String readPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, readPermission)
                != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(readPermission);

        // Xin thêm READ_MEDIA_VIDEO trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_VIDEO);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(android.Manifest.permission.CAMERA);

        if (permissionsNeeded.isEmpty()) {
            showMediaPickerSheet();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void showMediaPickerSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_image_picker, null);
        dialog.setContentView(sheetView);

        // Chụp ảnh hoặc quay video từ camera
        sheetView.findViewById(R.id.layoutCamera).setOnClickListener(v -> {
            dialog.dismiss();
            showCameraOptions();
        });

        // Chọn ảnh + video từ thư viện
        sheetView.findViewById(R.id.layoutGallery).setOnClickListener(v -> {
            dialog.dismiss();
            openMediaGallery();
        });

        // Ghi âm / chọn file audio
        sheetView.findViewById(R.id.layoutAudio).setOnClickListener(v -> {
            dialog.dismiss();
            showAudioOptions();
        });

        sheetView.findViewById(R.id.layoutCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ════════════════════════════════════════════════════
    //  CAMERA: chọn chụp ảnh hay quay video
    // ════════════════════════════════════════════════════
    private void showCameraOptions() {
        String[] options = {"Chụp ảnh", "Quay video"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn chế độ camera")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCameraPhoto();
                    else            openCameraVideo();
                }).show();
    }

    private void openCameraPhoto() {
        File file = new File(getCacheDir(),
                "photo_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
        cameraPhotoLauncher.launch(cameraImageUri);
    }

    private void openCameraVideo() {
        File file = new File(getCacheDir(),
                "video_" + System.currentTimeMillis() + ".mp4");
        cameraVideoUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
        cameraVideoLauncher.launch(cameraVideoUri);
    }

    // ════════════════════════════════════════════════════
    //  THƯ VIỆN: mở cả ảnh lẫn video
    // ════════════════════════════════════════════════════
    private void openMediaGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Cho phép chọn cả ảnh và video
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickMediaLauncher.launch(intent);
    }

    // ════════════════════════════════════════════════════
    //  UPLOAD ẢNH (tuần tự)
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
    //  UPLOAD VIDEO (tuần tự)
    // ════════════════════════════════════════════════════
    private void uploadVideosSequentially(List<Uri> uris, int index) {
        if (index >= uris.size()) {
            btnAttach.setEnabled(true);
            return;
        }
        btnAttach.setEnabled(false);

        imageRepository.uploadVideo(this, uris.get(index),
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        sendVideoMessage(url);
                        uploadVideosSequentially(uris, index + 1);
                    }
                    @Override
                    public void onError() {
                        Toast.makeText(ChatActivity.this,
                                "Upload video " + (index + 1) + " thất bại",
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
    //  TÌM KIẾM TIN NHẮN
    // ════════════════════════════════════════════════════
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
                    msg.getNoiDung().toLowerCase(Locale.ROOT).contains(lowerKey))
                searchResultIndexes.add(i);
        }

        if (searchResultIndexes.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
        goToSearchResult(0);
    }

    private void goToSearchResult(int index) {
        if (searchResultIndexes.isEmpty()) return;
        if (index < 0 || index >= searchResultIndexes.size()) return;
        currentSearchIndex = index;
        int position = searchResultIndexes.get(index);
        rvMessages.scrollToPosition(position);
        chatAdapter.highlightMessage(messageList.get(position).getMessageId());
    }

    private void nextSearchResult() {
        if (searchResultIndexes.isEmpty()) return;
        int next = currentSearchIndex + 1;
        if (next >= searchResultIndexes.size()) next = 0;
        goToSearchResult(next);
    }

    private void previousSearchResult() {
        if (searchResultIndexes.isEmpty()) return;
        int prev = currentSearchIndex - 1;
        if (prev < 0) prev = searchResultIndexes.size() - 1;
        goToSearchResult(prev);
    }

    private void openSearchSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_search_message, null);

        EditText edtSearch = view.findViewById(R.id.edtSearch);
        RecyclerView rv    = view.findViewById(R.id.rvSearchResult);

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
                        Date ts = msg.getThoiGian();
                        results.add(new SearchItem(msg.getMessageId(), msg.getNoiDung(), ts, i));
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void scrollToPosition(int position) {
        LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
        if (lm != null) lm.scrollToPositionWithOffset(position, 100);
    }

    // ════════════════════════════════════════════════════
    //  TOGGLE SEND / TIM NHANH
    // ════════════════════════════════════════════════════
    private void setupInputBehavior() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnTimNhanh.setVisibility(hasText ? View.GONE  : View.VISIBLE);
                btnSend.setVisibility(hasText    ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ════════════════════════════════════════════════════
    //  GỬI AUDIO
    // ════════════════════════════════════════════════════
    private void showAudioOptions() {
        String[] options = {"Tự ghi âm", "Chọn file âm thanh"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn nguồn âm thanh")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this,
                                android.Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                                    PERMISSION_AUDIO_CODE);
                            return;
                        }
                        openAudioRecorder();
                    } else {
                        openAudioPicker();
                    }
                }).show();
    }

    private void openAudioRecorder() {
        com.example.doanmxh.CreatePage.AudioRecordBottomSheet bottomSheet =
                new com.example.doanmxh.CreatePage.AudioRecordBottomSheet(audioUri -> {
                    if (audioUri != null)
                        uploadAudiosSequentially(new ArrayList<>(List.of(audioUri)), 0);
                });
        bottomSheet.show(getSupportFragmentManager(), "AudioRecordBottomSheet");
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickAudioLauncher.launch(intent);
    }

    private void uploadAudiosSequentially(List<Uri> uris, int index) {
        if (index >= uris.size()) return;
        btnAttach.setEnabled(false);

        imageRepository.uploadAudio(this, uris.get(index),
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        sendAudioMessage(url);
                        uploadAudiosSequentially(uris, index + 1);
                        if (index + 1 >= uris.size()) btnAttach.setEnabled(true);
                    }
                    @Override
                    public void onError() {
                        Toast.makeText(ChatActivity.this,
                                "Upload audio " + (index + 1) + " thất bại",
                                Toast.LENGTH_SHORT).show();
                        btnAttach.setEnabled(true);
                    }
                });
    }

    private void sendAudioMessage(String audioUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", audioUrl);
        message.put("thoi_gian", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("da_doc", false);
        message.put("loai", "audio");

        if (replyingMessage == null) {
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").add(message)
                    .addOnSuccessListener(ref ->
                            updateLastMessage("[Âm thanh]", ref.getId(), "audio"));
            return;
        }

        String replyId = replyingMessage.getMessageId();
        message.put("reply_to_id", replyId);
        message.put("reply_to_content",
                replyingMessage.getNoiDung() != null
                        ? replyingMessage.getNoiDung() : "[Âm thanh]");

        db.collection("nguoi_dung").document(replyingMessage.getNguoiGuiId()).get()
                .addOnSuccessListener(doc -> {
                    message.put("reply_to_sender_name",
                            doc.getString("ten_dang_nhap") != null
                                    ? doc.getString("ten_dang_nhap") : "");
                    clearReply();
                    db.collection("cuoc_tro_chuyen").document(conversationId)
                            .collection("tin_nhan").add(message)
                            .addOnSuccessListener(ref ->
                                    updateLastMessage("[Âm thanh]", ref.getId(), "audio"));
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
            showMediaPickerSheet();
        } else if (requestCode == PERMISSION_AUDIO_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openAudioRecorder();
        } else {
            Toast.makeText(this, "Cần cấp quyền để thực hiện chức năng này",
                    Toast.LENGTH_SHORT).show();
        }
    }
}