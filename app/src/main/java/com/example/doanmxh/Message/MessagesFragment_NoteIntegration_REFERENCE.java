package com.example.doanmxh.Message;


import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MessagesFragment_NoteIntegration_REFERENCE extends Fragment {

    // ════════════════════════════════════════════════════════════════════
    // 1) FIELD — thêm vào phần khai báo biến của Fragment
    // ════════════════════════════════════════════════════════════════════

    private View flMyNoteBubble;
    private TextView tvMyNote;
    private View btnAddNote;
    private ShapeableImageView imgMyAvatar;

    private FirebaseFirestore db;
    private String myUid;

    /** Ghi chú hiện tại (để truyền sang NoteEditorActivity khi bấm sửa) */
    private String currentNoteText = "";

    /** Launcher mở NoteEditorActivity, tự load lại ghi chú khi quay về */
    private final ActivityResultLauncher<Intent> noteEditorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Bất kể OK hay CANCELLED, load lại cho chắc (phòng trường hợp
                // người dùng xóa/sửa rồi back ngang)
                loadMyNote();
            });

    // ════════════════════════════════════════════════════════════════════
    // 2) ONVIEWCREATED — gọi trong onCreateView/onViewCreated, sau khi
    //    đã findViewById các view của layoutMyStory
    // ════════════════════════════════════════════════════════════════════

    private void initNoteViews(View view) {
        flMyNoteBubble = view.findViewById(R.id.flMyNoteBubble);
        tvMyNote       = view.findViewById(R.id.tvMyNote);
        btnAddNote     = view.findViewById(R.id.btnAddNote);
        imgMyAvatar    = view.findViewById(R.id.imgMyAvatar);

        db    = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        btnAddNote.setOnClickListener(v -> openNoteEditor());

        loadMyNote();
    }

    // ════════════════════════════════════════════════════════════════════
    // 3) METHOD — tải ghi chú hiện tại của bản thân và hiển thị bong bóng
    // ════════════════════════════════════════════════════════════════════

    private void loadMyNote() {
        if (myUid == null || !isAdded()) {
            if (flMyNoteBubble != null) flMyNoteBubble.setVisibility(View.GONE);
            return;
        }

        db.collection("nguoi_dung").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || !doc.exists()) return;

                    String   ghiChu   = doc.getString("ghi_chu");
                    Timestamp ngayTao = doc.getTimestamp("ngay_tao_ghi_chu");

                    boolean hopLe = !TextUtils.isEmpty(ghiChu)
                            && !NoteEditorActivity.isNoteExpired(ngayTao);

                    if (hopLe) {
                        currentNoteText = ghiChu;
                        tvMyNote.setText(ghiChu);
                        flMyNoteBubble.setVisibility(View.VISIBLE);
                    } else {
                        currentNoteText = "";
                        flMyNoteBubble.setVisibility(View.GONE);
                    }

                    // Cập nhật avatar preview nếu cần
                    String avatar = doc.getString("anh_dai_dien");
                    if (!TextUtils.isEmpty(avatar) && imgMyAvatar != null) {
                        Glide.with(this)
                                .load(avatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .into(imgMyAvatar);
                    }
                })
                .addOnFailureListener(e -> {
                    if (flMyNoteBubble != null) flMyNoteBubble.setVisibility(View.GONE);
                });
    }

    // ════════════════════════════════════════════════════════════════════
    // 4) METHOD — mở NoteEditorActivity, kèm ghi chú hiện tại (nếu có)
    //    để màn hình sửa hiển thị sẵn nội dung cũ
    // ════════════════════════════════════════════════════════════════════

    private void openNoteEditor() {
        Intent intent = new Intent(getActivity(), NoteEditorActivity.class);
        intent.putExtra(NoteEditorActivity.EXTRA_CURRENT_NOTE, currentNoteText);
        noteEditorLauncher.launch(intent);
    }

    // ════════════════════════════════════════════════════════════════════
    // 5) (TÙY CHỌN) — nếu Fragment đã có onResume(), có thể gọi thêm
    //    loadMyNote() ở đó để vừa refresh khi quay lại từ màn hình khác,
    //    vừa tự ẩn ghi chú khi hết hạn 24h dù người dùng không chỉnh sửa gì.
    // ════════════════════════════════════════════════════════════════════

    @Override
    public void onResume() {
        super.onResume();
        loadMyNote();
    }
}