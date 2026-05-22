package com.example.doanmxh.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostOptionBottomSheet extends BottomSheetDialogFragment {

    private final String postId; // ✅ Đổi int → String vì Firestore dùng String document ID
    private OnPostDeletedListener deleteListener;

    public interface OnPostDeletedListener {
        void onPostDeleted(String postId);
    }

    public PostOptionBottomSheet(String postId) {
        this.postId = postId;
    }

    public void setOnPostDeletedListener(OnPostDeletedListener listener) {
        this.deleteListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.bottom_sheet_post_options,
                container,
                false
        );

        LinearLayout layoutEdit   = view.findViewById(R.id.layoutEdit);
        LinearLayout layoutDelete = view.findViewById(R.id.layoutDelete);

        layoutEdit.setOnClickListener(v -> {
            // Mở màn hình sửa bài viết
            Intent intent = new Intent(getContext(), EditPostActivity.class);
            intent.putExtra("post_id", postId);
            startActivity(intent);
            dismiss();
        });

        layoutDelete.setOnClickListener(v -> {
            // Hiện dialog xác nhận trước khi xóa
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa bài viết")
                    .setMessage("Bài viết sẽ bị ẩn và không thể khôi phục. Bạn có chắc không?")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (dialog, which) -> deletePost())
                    .show();
        });

        return view;
    }

    private void deletePost() {
        // Kiểm tra quyền — chỉ chủ bài mới được xóa
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (myUid == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Kiểm tra bài có phải của mình không
        db.collection("bai_viet").document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(getContext(), "Bài viết không tồn tại", Toast.LENGTH_SHORT).show();
                        dismiss();
                        return;
                    }

                    String authorId = doc.getString("nguoi_dung_id");
                    if (!myUid.equals(authorId)) {
                        Toast.makeText(getContext(), "Bạn không có quyền xóa bài này", Toast.LENGTH_SHORT).show();
                        dismiss();
                        return;
                    }

                    // Soft delete — chỉ set da_xoa = true
                    db.collection("bai_viet").document(postId)
                            .update("da_xoa", true)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                                if (deleteListener != null) {
                                    deleteListener.onPostDeleted(postId);
                                }
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}