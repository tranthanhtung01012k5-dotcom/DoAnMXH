package com.example.doanmxh.Message;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
//    private boolean isPinned = false;
    // Truyền đầy đủ thông tin từ ChatUser
    public static BottomMessageMore newInstance(
            String uid,
            String username,
            String lastMessage,
            String avatarUrl
//            boolean isPinned
    ) {
        BottomMessageMore fragment = new BottomMessageMore();
        Bundle args = new Bundle();

        args.putString(ARG_UID_URL, uid);
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_LAST_MSG, lastMessage);
        args.putString(ARG_AVATAR_URL, avatarUrl);
//        args.putBoolean("isPinned", isPinned);

        fragment.setArguments(args);
        return fragment;
    }
    public interface OnActionDoneListener {
        void onActionDone();
    }

    private OnActionDoneListener actionDoneListener;

    public void setOnActionDoneListener(OnActionDoneListener listener) {
        Log.d("BOTTOM_TEST",
                "SET LISTENER CALLED " + System.identityHashCode(this));

        this.actionDoneListener = listener;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            lastMessage = getArguments().getString(ARG_LAST_MSG);
            avatarUrl = getArguments().getString(ARG_AVATAR_URL);
            targetUid = getArguments().getString(ARG_UID_URL);

//            isPinned = getArguments().getBoolean("isPinned", false);
        }
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle);
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        Log.d("BOTTOM_TEST",
                "ON CREATE = " + System.identityHashCode(this));
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
//        TextView btnGhim = view.findViewById(R.id.btnGhim);

        checkPinnedStatus(targetUid, btnGhim);

        btnDanhDau.setOnClickListener(v -> {
            saveMessage(targetUid);
            dismiss();
        });
        btnTat.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tắt thông báo", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        btnGhim.setOnClickListener(v -> {

            String myUid = auth.getCurrentUser().getUid();

            String convId = myUid.compareTo(targetUid) < 0
                    ? myUid + "_" + targetUid
                    : targetUid + "_" + myUid;

            db.collection("nguoi_dung")
                    .document(myUid)
                    .collection("tin_nhan_ghim")
                    .document(convId)
                    .get()
                    .addOnSuccessListener(doc -> {

                        if (doc.exists()) {
                            unpinConversation(targetUid, btnGhim);
                        } else {
                            pinConversation(targetUid, btnGhim);
                        }
                    });
        });
        btnXoa.setOnClickListener(v -> {
            deleteConversationDocument(targetUid);
            deleteConversation(targetUid);
            dismiss();
        });

    }
    private void updatePinButton(LinearLayout btnGhim, boolean isPinned) {
        TextView tv = btnGhim.findViewById(R.id.tvGhim);
        tv.setText(isPinned ? "Bỏ ghim" : "Ghim");
    }
    private void unpinConversation(String targetUid, LinearLayout btnGhim) {

        String myUid = auth.getCurrentUser().getUid();

        String convId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("tin_nhan_ghim")
                .document(convId)
                .delete()
                .addOnSuccessListener(unused -> {

                    checkPinnedStatus(targetUid, btnGhim);

                    showToast("Đã bỏ ghim");

                    if (actionDoneListener != null) {
                        actionDoneListener.onActionDone();
                    }
                });
        dismiss();
    }
    private void saveMessage(String targetUid) {
        String myUid = auth.getCurrentUser().getUid();

        Log.d("BOTTOM_TEST",
                "SAVE INSTANCE = " + System.identityHashCode(this));

        Log.d("BOTTOM_TEST",
                "LISTENER = " + actionDoneListener);

        String convId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        Map<String, Object> data = new HashMap<>();
        data.put("tin_nhan_id", convId);
        data.put("ngay_luu", com.google.firebase.Timestamp.now());

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("cuoc_tro_luu_tru")
                .document(convId)
                .set(data)
                .addOnSuccessListener(unused -> {
//                    Log.d("BOTTOM_TEST", "SAVE SUCCESS");
//
//                    Log.d("BOTTOM_TEST",
//                            "SUCCESS INSTANCE = " + System.identityHashCode(this));
//
//                    Log.d("BOTTOM_TEST",
//                            "SUCCESS LISTENER = " + actionDoneListener);

                    if (actionDoneListener != null) {
                        Log.d("BOTTOM_TEST", "CALL ACTION DONE");
                        actionDoneListener.onActionDone();
                    } else {
                        Log.d("BOTTOM_TEST", "LISTENER NULL");
                    }
//                    Toast.makeText(getContext(),"Lưu tin nhắn thành công",Toast.LENGTH_SHORT).show();
//
                });
    }

    private void deleteConversation(String targetUid) {
        String myUid = auth.getCurrentUser().getUid();
        String convId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        db.collection("cuoc_tro_chuyen")
                .document(convId)
                .collection("tin_nhan")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        deleteConversationDocument(convId);
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> deleteConversationDocument(convId))
                            .addOnFailureListener(err -> showToast("Lỗi xóa tin nhắn: " + err.getMessage()));
                })
                .addOnFailureListener(err -> showToast("Lỗi: " + err.getMessage()));
    }
    private void pinConversation(String targetUid, LinearLayout btnGhim) {

        String myUid = auth.getCurrentUser().getUid();

        String convId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        Map<String, Object> data = new HashMap<>();
        data.put("id_doan_chat", convId);
        data.put("tin_nhan_cuoi", lastMessage);
        data.put("thoi_gian_ghim", com.google.firebase.Timestamp.now());

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("tin_nhan_ghim")
                .document(convId)
                .set(data)
                .addOnSuccessListener(unused -> {

                    checkPinnedStatus(targetUid, btnGhim);

                    showToast("Đã ghim đoạn chat");

                    if (actionDoneListener != null) {
                        actionDoneListener.onActionDone();
                    }
                    dismiss();
                });
    }
    private void deleteConversationDocument(String convId) {
        db.collection("cuoc_tro_chuyen")
                .document(convId)
                .delete()
                .addOnSuccessListener(a -> showToast("Đã xóa cuộc trò chuyện"))
                .addOnFailureListener(err -> showToast("Lỗi: " + err.getMessage()));
    }
    // Thêm method này vào class
    private void checkPinnedStatus(String targetUid, LinearLayout btnGhim) {
        String myUid = auth.getCurrentUser().getUid();

        String convId = myUid.compareTo(targetUid) < 0
                ? myUid + "_" + targetUid
                : targetUid + "_" + myUid;

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("tin_nhan_ghim")
                .document(convId)
                .get()
                .addOnSuccessListener(doc -> {
                    boolean pinned = doc.exists();
                    updatePinButton(btnGhim, pinned);
                });
    }
    private void showToast(String msg) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (getContext() != null)
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }
}