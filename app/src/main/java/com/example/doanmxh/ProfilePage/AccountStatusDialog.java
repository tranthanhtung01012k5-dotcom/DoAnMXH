package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountStatusDialog {
    private final Fragment host;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AccountStatusDialog(Fragment host, FirebaseAuth auth, FirebaseFirestore db) {
        this.host = host;
        this.auth = auth;
        this.db = db;
    }

    public void show() {
        if (host.getContext() == null || auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail() != null
                ? auth.getCurrentUser().getEmail()
                : "Chưa có email";

        db.collection("nguoi_dung")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("ho_va_ten");
                    String username = documentSnapshot.getString("ten_dang_nhap");
                    boolean isPrivate = Boolean.TRUE.equals(documentSnapshot.getBoolean("private"));
                    boolean verified = Boolean.TRUE.equals(documentSnapshot.getBoolean("verified"));
                    Long followers = documentSnapshot.getLong("so_nguoi_dang_theo_doi");

                    showContent(
                            safeText(name, "Chưa cập nhật"),
                            safeText(username, "user"),
                            email,
                            uid,
                            isPrivate,
                            verified,
                            followers != null ? followers : 0
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(host.getContext(), "Không thể tải trạng thái tài khoản", Toast.LENGTH_SHORT).show());
    }

    private void showContent(
            String name,
            String username,
            String email,
            String uid,
            boolean isPrivate,
            boolean verified,
            long followers
    ) {
        Context context = host.requireContext();
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_account_status);

        TextView txtVisibility = dialog.findViewById(R.id.txtAccountVisibility);
        TextView txtName = dialog.findViewById(R.id.txtStatusName);
        TextView txtUsername = dialog.findViewById(R.id.txtStatusUsername);
        TextView txtEmail = dialog.findViewById(R.id.txtStatusEmail);
        TextView txtVerified = dialog.findViewById(R.id.txtStatusVerified);
        TextView txtFollowers = dialog.findViewById(R.id.txtStatusFollowers);
        TextView txtUid = dialog.findViewById(R.id.txtStatusUid);
        TextView btnClose = dialog.findViewById(R.id.btnCloseAccountStatus);

        txtVisibility.setText(isPrivate ? "Riêng tư" : "Công khai");
        txtName.setText(accountStatusLine("Tên", name));
        txtUsername.setText(accountStatusLine("Tên người dùng", "@" + username));
        txtEmail.setText(accountStatusLine("Email", email));
        txtVerified.setText(accountStatusLine("Xác minh", verified ? "Đã xác minh" : "Chưa xác minh"));
        txtFollowers.setText(accountStatusLine("Người theo dõi", String.valueOf(followers)));
        txtUid.setText(accountStatusLine("UID", uid));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String accountStatusLine(String label, String value) {
        return label + "\n" + value;
    }

    private String safeText(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }
}
