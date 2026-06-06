package com.example.doanmxh.ProfilePage;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePasswordActivity extends BaseActivity {
    private TextInputLayout layoutCurrentPassword, layoutNewPassword, layoutConfirmPassword;
    private TextInputEditText edtCurrentPassword, edtNewPassword, edtConfirmPassword;
    private MaterialButton btnChangePassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        auth = FirebaseAuth.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        layoutCurrentPassword = findViewById(R.id.layoutCurrentPassword);
        layoutNewPassword = findViewById(R.id.layoutNewPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);
        edtCurrentPassword = findViewById(R.id.edtCurrentPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnBack.setOnClickListener(v -> finish());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        clearErrors();

        String currentPassword = valueOf(edtCurrentPassword);
        String newPassword = valueOf(edtNewPassword);
        String confirmPassword = valueOf(edtConfirmPassword);

        if (!validate(currentPassword, newPassword, confirmPassword)) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        user.reauthenticate(
                        EmailAuthProvider.getCredential(
                                user.getEmail(),
                                currentPassword
                        )
                )
                .addOnSuccessListener(unused ->
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(done ->

                                        FirebaseFirestore.getInstance()
                                                .collection("nguoi_dung")
                                                .document(user.getUid())
                                                .update(
                                                        "mat_khau", newPassword,
                                                        "ngay_cap_nhat_mat_khau",
                                                        FieldValue.serverTimestamp()
                                                )
                                                .addOnSuccessListener(v -> {
                                                    setLoading(false);

                                                    Toast.makeText(
                                                            this,
                                                            "Đổi mật khẩu thành công",
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    setLoading(false);

                                                    Toast.makeText(
                                                            this,
                                                            "Đã đổi mật khẩu Firebase nhưng cập nhật dữ liệu thất bại",
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                })
                                )
                                .addOnFailureListener(e -> {
                                    setLoading(false);

                                    Toast.makeText(
                                            this,
                                            "Đổi mật khẩu thất bại: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    layoutCurrentPassword.setError("Mật khẩu hiện tại không đúng");
                });
    }

    private boolean validate(String currentPassword, String newPassword, String confirmPassword) {
        boolean valid = true;

        if (TextUtils.isEmpty(currentPassword)) {
            layoutCurrentPassword.setError("Mật khẩu hiện tại không được bỏ trống");
            valid = false;
        }


        if (TextUtils.isEmpty(newPassword)) {
            layoutNewPassword.setError("Mật khẩu mới không được bỏ trống");
            valid = false;
        } else if (newPassword.length() < 8) {
            layoutNewPassword.setError("Mật khẩu mới tối thiểu 8 ký tự");
            valid = false;
        } else if (newPassword.equals(currentPassword)) {
            layoutNewPassword.setError("Mật khẩu mới không được trùng mật khẩu cũ");
            valid = false;
        }
        if (!newPassword.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Nhập lại mật khẩu mới phải trùng khớp");
            valid = false;
        }

        return valid;
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void clearErrors() {
        layoutCurrentPassword.setError(null);
        layoutNewPassword.setError(null);
        layoutConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        btnChangePassword.setEnabled(!loading);
        btnChangePassword.setText(loading ? "Đang cập nhật..." : "Cập nhật mật khẩu");
    }
}
