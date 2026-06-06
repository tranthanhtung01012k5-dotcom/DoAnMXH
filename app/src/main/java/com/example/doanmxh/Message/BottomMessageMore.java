package com.example.doanmxh.Message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BottomMessageMore extends BottomSheetDialogFragment {

    private static final String ARG_USERNAME    = "username";
    private static final String ARG_LAST_MSG    = "lastMessage";
    private static final String ARG_AVATAR_URL  = "avatarUrl";
    private static final String ARG_UID_URL  = "uid";
    private FirebaseFirestore db;
    private String conversationId;
    private FirebaseAuth auth;
    private ChatMessage msg;
    private String username, lastMessage, avatarUrl,targetUid;

    // Truyền đầy đủ thông tin từ ChatUser
    public static BottomMessageMore newInstance(String uid, String username, String lastMessage, String avatarUrl) {
        BottomMessageMore fragment = new BottomMessageMore();
        Bundle args = new Bundle();
        args.putString(ARG_UID_URL,   uid);
        args.putString(ARG_USERNAME,   username);
        args.putString(ARG_LAST_MSG,   lastMessage);
        args.putString(ARG_AVATAR_URL, avatarUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username    = getArguments().getString(ARG_USERNAME);
            lastMessage = getArguments().getString(ARG_LAST_MSG);
            avatarUrl   = getArguments().getString(ARG_AVATAR_URL);
            targetUid   = getArguments().getString(ARG_UID_URL); // ← THÊM
        }
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle);
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_message_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Bind header ---
        ImageView headerAvatar      = view.findViewById(R.id.headerAvatar);
        TextView  headerUsername    = view.findViewById(R.id.headerUsername);
        TextView  headerLastMessage = view.findViewById(R.id.headerLastMessage);

        headerUsername.setText(username);
        headerLastMessage.setText(lastMessage);
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .circleCrop()
                .into(headerAvatar);

        // --- Bind actions ---
        LinearLayout btnDanhDau = view.findViewById(R.id.btnDanhDauChuaDoc);
        LinearLayout btnTat     = view.findViewById(R.id.btnTat);
        LinearLayout btnGhim    = view.findViewById(R.id.btnGhim);
        LinearLayout btnXoa     = view.findViewById(R.id.btnXoa);

        btnDanhDau.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đánh dấu là chưa đọc", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        btnTat.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tắt thông báo", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        btnGhim.setOnClickListener(v -> {
//pinMessage(msg);
dismiss();
        });
        btnXoa.setOnClickListener(v -> {
            deleteConversationDocument(targetUid);
            deleteConversation(targetUid);
            dismiss();
        });

    }

    private void deleteConversation(String targetUid) {
        String myUid = auth.getCurrentUser().getUid();

        String convId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        // Bước 1: Xóa tất cả tin nhắn trong subcollection trước
        db.collection("cuoc_tro_chuyen")
                .document(convId)
                .collection("tin_nhan")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Không có tin nhắn → xóa thẳng document
                        deleteConversationDocument(convId);
                        return;
                    }

                    // Dùng batch để xóa tất cả tin nhắn 1 lần
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                // Bước 2: Sau khi xóa xong tin nhắn → xóa document cha
                                deleteConversationDocument(convId);
                            })
                            .addOnFailureListener(err ->
                                    Toast.makeText(getContext(), "Lỗi xóa tin nhắn: " + err.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(err ->
                        Toast.makeText(getContext(), "Lỗi: " + err.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void deleteConversationDocument(String convId) {
        db.collection("cuoc_tro_chuyen")
                .document(convId)
                .delete()
                .addOnSuccessListener(a ->
                        Toast.makeText(getContext(), "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(err ->
                        Toast.makeText(getContext(), "Lỗi: " + err.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}