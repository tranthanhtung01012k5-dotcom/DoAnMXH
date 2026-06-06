package com.example.doanmxh.Log_Res;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.Log_Res.AccountManager;
import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.MainActivity;
import com.example.doanmxh.MyApplication;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends BaseActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin;
    CheckBox checkRemember;
    TextView txtRegister;
    FirebaseAuth auth;
    FirebaseFirestore db;

    // Key lưu trạng thái "đã đăng nhập" (auto-login mỗi lần mở app)
    private static final String PREF_NAME   = "login_pref";
    private static final String KEY_REMEMBER = "remember_login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername   = findViewById(R.id.edtUsername);
        edtPassword   = findViewById(R.id.edtPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        checkRemember = findViewById(R.id.checkRemember);
        txtRegister   = findViewById(R.id.txtRegister);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());

        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPassActivity.class));
            Log.d("ForgotPassword", "Forgot password text clicked");
        });

        // ── Spannable "Đăng ký" ──────────────────────────────────────────────
        String text = "Chưa có tài khoản? Đăng ký";
        SpannableString spannableString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getResources().getColor(R.color.text_primary));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };
        spannableString.setSpan(
                clickableSpan,
                text.indexOf("Đăng ký"),
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        txtRegister.setText(spannableString);
        txtRegister.setMovementMethod(LinkMovementMethod.getInstance());
        txtRegister.setHighlightColor(Color.TRANSPARENT);
    }

    // ── Đăng nhập ─────────────────────────────────────────────────────────────

    private void loginUser() {
        String email    = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (auth.getCurrentUser() == null) {
                        Toast.makeText(this, "Không lấy được user", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    auth.getCurrentUser().reload().addOnCompleteListener(reloadTask -> {

                        boolean verified = auth.getCurrentUser().isEmailVerified();
                        if (!verified) {
                            auth.signOut();
                            Toast.makeText(LoginActivity.this,
                                    "Email chưa được xác minh", Toast.LENGTH_LONG).show();

                            // Gửi lại email xác minh
                            auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener(authResult -> {
                                        if (auth.getCurrentUser() != null) {
                                            auth.getCurrentUser().sendEmailVerification();
                                            Toast.makeText(LoginActivity.this,
                                                    "Đã gửi lại email xác minh",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        auth.signOut();
                                    });
                            return;
                        }

                        // ✅ Đã xác minh — lấy thêm displayName rồi xử lý
                        String uid         = auth.getCurrentUser().getUid();
                        String displayName = auth.getCurrentUser().getDisplayName();

                        // ── Lưu tài khoản vào danh sách nếu tick Remember ────
                        if (checkRemember.isChecked()) {
                            AccountManager.saveAccount(
                                    LoginActivity.this,
                                    new AccountManager.SavedAccount(
                                            email,
                                            password,
                                            displayName,
                                            uid
                                    )
                            );
                        }

                        // Luôn lưu cờ auto-login cho lần mở app tiếp theo
                        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_REMEMBER, true)
                                .apply();

                        // Cập nhật trạng thái online
                        db.collection("nguoi_dung")
                                .document(uid)
                                .update("trang_thai_hoat_dong", true)
                                .addOnSuccessListener(unused -> {
                                    MyApplication.setupPresence();
                                    Toast.makeText(LoginActivity.this,
                                            "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(LoginActivity.this,
                                                "Không cập nhật được trạng thái hoạt động",
                                                Toast.LENGTH_SHORT).show()
                                );
                    });
                });
    }
}