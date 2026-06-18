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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostOptionBottomSheet extends BottomSheetDialogFragment {

    private final String postId;
    private OnPostDeletedListener deleteListener;

    public interface OnPostDeletedListener {
        void onPostDeleted(String postId, @Nullable String originalPostId);
    }

    public interface OnPostHiddenListener {
        void onPostHidden(String postId);
    }

    private OnPostHiddenListener onPostHiddenListener;

    public void setOnPostHiddenListener(OnPostHiddenListener listener) {
        this.onPostHiddenListener = listener;
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

        LinearLayout layoutEdit      = view.findViewById(R.id.layoutEdit);
        LinearLayout layoutDelete    = view.findViewById(R.id.layoutDelete);
        LinearLayout layoutSave      = view.findViewById(R.id.layoutSave);
        LinearLayout layoutDontCare  = view.findViewById(R.id.layoutDontCare);
        LinearLayout layoutReport    = view.findViewById(R.id.layoutReport); // ← THÊM

        // ── Chỉnh sửa ────────────────────────────────────────────────────────
        layoutEdit.setOnClickListener(v -> {
            String myUid = getCurrentUid();
            if (myUid == null) { showToast("Bạn chưa đăng nhập"); return; }

            FirebaseFirestore.getInstance()
                    .collection("bai_viet").document(postId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) { showToast("Bài viết không tồn tại"); dismiss(); return; }
                        if (!myUid.equals(doc.getString("nguoi_dung_id"))) {
                            showToast("Bạn không có quyền sửa bài viết này");
                            dismiss();
                            return;
                        }
                        Intent intent = new Intent(getContext(), EditPostActivity.class);
                        intent.putExtra("post_id", postId);
                        startActivity(intent);
                        dismiss();
                    })
                    .addOnFailureListener(e -> showToast("Không thể kiểm tra quyền chỉnh sửa"));
        });

        // ── Lưu bài ──────────────────────────────────────────────────────────
        layoutSave.setOnClickListener(v -> {
            String myUid = getCurrentUid();
            if (myUid == null) { showToast("Bạn chưa đăng nhập"); return; }

            Map<String, Object> data = new HashMap<>();
            data.put("bai_viet_id", postId);
            data.put("ngay_luu", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung").document(myUid)
                    .collection("bai_viet_luu_tru").document(postId)
                    .set(data)
                    .addOnSuccessListener(unused -> { showToast("Đã lưu bài viết"); dismiss(); })
                    .addOnFailureListener(e -> showToast("Lưu bài viết thất bại"));
        });

        // ── Hạn chế ──────────────────────────────────────────────────────────
        layoutDontCare.setOnClickListener(v -> {
            String myUid = getCurrentUid();
            if (myUid == null) return;

            Map<String, Object> map = new HashMap<>();
            map.put("id_nguoi_dung", myUid);
            map.put("ngay_chan", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("bai_viet").document(postId)
                    .collection("nguoi_dang_han_che").document(myUid)
                    .set(map)
                    .addOnSuccessListener(unused -> {
                        showToast("Đã hạn chế sự xuất hiện của bài viết");
                        if (onPostHiddenListener != null) onPostHiddenListener.onPostHidden(postId);
                        dismiss();
                    })
                    .addOnFailureListener(e -> showToast("Hạn chế bài viết thất bại"));
        });

        // ── Xóa ──────────────────────────────────────────────────────────────
        layoutDelete.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xóa bài viết")
                        .setMessage("Bài viết sẽ bị ẩn và không thể khôi phục. Bạn có chắc không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (dialog, which) -> deletePost())
                        .show()
        );

        // ── Báo cáo ──────────────────────────────────────────────────────────
        layoutReport.setOnClickListener(v -> showReportDialog());

        return view;
    }

    // ── Hiện dialog chọn lý do báo cáo ──────────────────────────────────────
    private void showReportDialog() {
        String myUid = getCurrentUid();
        if (myUid == null) { showToast("Bạn chưa đăng nhập"); return; }

        MaterialAlertDialogBuilder loadingBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Báo cáo bài viết")
                .setMessage("Đang tải lý do...")
                .setNegativeButton("Hủy", null);
        androidx.appcompat.app.AlertDialog loadingDialog = loadingBuilder.show();

        FirebaseFirestore.getInstance()
                .collection("report_reasons")
                .get()
                .addOnSuccessListener(query -> {
                    loadingDialog.dismiss();

                    if (query.isEmpty()) { showToast("Không tải được lý do báo cáo"); return; }

                    List<String> reasonTitles = new ArrayList<>();
                    List<String> reasonIds    = new ArrayList<>();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String title = doc.getString("title");
                        if (title != null && !title.isEmpty()) {
                            reasonTitles.add(title);
                            reasonIds.add(doc.getId());
                        }
                    }

                    if (reasonTitles.isEmpty()) { showToast("Không có lý do nào"); return; }

                    String[] reasons = reasonTitles.toArray(new String[0]);

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Báo cáo bài viết")
                            .setItems(reasons, (d, which) -> {
                                String selectedId    = reasonIds.get(which);
                                String selectedTitle = reasonTitles.get(which);

                                if ("other".equals(selectedId) || "nOsldyELYX5p4HhddfIs".equals(selectedId)) {
                                    // Mở dialog nhập tay
                                    showCustomReasonDialog(myUid, selectedId);
                                } else {
                                    submitReport(myUid, selectedId, selectedTitle);
                                }
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    showToast("Lỗi tải lý do: " + e.getMessage());
                });
    }

    private void showCustomReasonDialog(String myUid, String reasonId) {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Mô tả lý do báo cáo...");
        input.setMaxLines(3);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Lý do báo cáo")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String customTitle = input.getText().toString().trim();
                    if (customTitle.isEmpty()) {
                        showToast("Vui lòng nhập lý do");
                        return;
                    }
                    submitReport(myUid, reasonId, customTitle); // ← dùng id="other", title do user nhập
                })
                .show();
    }

    private void submitReport(String myUid, String reasonId, String reasonTitle) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> report = new HashMap<>();
        report.put("bai_viet_id", postId);
        report.put("nguoi_bao_cao_id", myUid);
        report.put("ly_do_id", reasonId);       // ← ID lý do
        report.put("ly_do_title", reasonTitle); // ← title lý do
        report.put("ngay_bao_cao", FieldValue.serverTimestamp());
        report.put("trang_thai", "cho_xu_ly");

        db.collection("reports").add(report);

        Map<String, Object> hidden = new HashMap<>();
        hidden.put("id_nguoi_dung", myUid);
        hidden.put("ngay_chan", FieldValue.serverTimestamp());

        db.collection("bai_viet").document(postId)
                .collection("nguoi_dang_han_che").document(myUid)
                .set(hidden)
                .addOnSuccessListener(unused -> {
                    showToast("Đã gửi báo cáo, cảm ơn bạn!");
                    if (onPostHiddenListener != null) onPostHiddenListener.onPostHidden(postId);
                    dismiss();
                })
                .addOnFailureListener(e -> showToast("Gửi báo cáo thất bại: " + e.getMessage()));
    }
    // ── Xóa bài ──────────────────────────────────────────────────────────────
    private void deletePost() {
        String myUid = getCurrentUid();
        if (myUid == null) { showToast("Vui lòng đăng nhập"); return; }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("bai_viet").document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { showToast("Bài viết không tồn tại"); dismiss(); return; }

                    if (!myUid.equals(doc.getString("nguoi_dung_id"))) {
                        showToast("Bạn không có quyền xóa bài này");
                        dismiss();
                        return;
                    }

                    Boolean isRepost = doc.getBoolean("is_repost");
                    String baiVietChaId = doc.getString("bai_viet_cha_id");
                    boolean needResetRepost = Boolean.TRUE.equals(isRepost)
                            && baiVietChaId != null && !baiVietChaId.isEmpty();

                    db.collection("bai_viet").document(postId)
                            .update("da_xoa", true)
                            .addOnSuccessListener(unused -> {
                                showToast("Đã xóa bài viết");
                                if (needResetRepost) {
                                    db.collection("bai_viet").document(baiVietChaId)
                                            .update("so_repost", FieldValue.increment(-1));
                                    if (deleteListener != null)
                                        deleteListener.onPostDeleted(postId, baiVietChaId);
                                } else {
                                    if (deleteListener != null)
                                        deleteListener.onPostDeleted(postId, null);
                                }
                                dismiss();
                            })
                            .addOnFailureListener(e -> showToast("Lỗi: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showToast("Lỗi: " + e.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    @Nullable
    private String getCurrentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void showToast(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}