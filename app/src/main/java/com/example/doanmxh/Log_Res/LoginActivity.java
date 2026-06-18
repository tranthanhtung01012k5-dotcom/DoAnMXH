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

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.MainActivity;
import com.example.doanmxh.MyApplication;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginActivity extends BaseActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin;
    CheckBox checkRemember;
    TextView txtRegister;
    FirebaseAuth auth;
    FirebaseFirestore db;

    private static final String PREF_NAME    = "login_pref";
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

        // ── Autofill dropdown (chỉ hiện tài khoản đã tick Remember) ──────────
        setupAutofillDropdown();

        // ── Quên mật khẩu ────────────────────────────────────────────────────
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

    // ── Autofill dropdown ─────────────────────────────────────────────────────

    private void setupAutofillDropdown() {
        // Chỉ lấy account có password (đã từng tick Remember)
        List<AccountManager.SavedAccount> list = AccountManager.getAllWithPassword(this);
        if (list.isEmpty()) return;

        edtUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showCredentialDropdown(list);
        });
    }

    private void showCredentialDropdown(List<AccountManager.SavedAccount> list) {
        String[] emails = list.stream()
                .map(a -> a.email)
                .toArray(String[]::new);

        android.widget.ListPopupWindow popup = new android.widget.ListPopupWindow(this);
        popup.setAnchorView(edtUsername);
        popup.setAdapter(new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, emails));
        popup.setWidth(edtUsername.getWidth());
        popup.setOnItemClickListener((parent, view, position, id) -> {
            edtUsername.setText(list.get(position).email);
            edtPassword.setText(list.get(position).password);
            checkRemember.setChecked(true);
            popup.dismiss();
        });
        popup.show();
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

                    String uid = auth.getCurrentUser().getUid();

                    // Kiểm tra xác thực OTP qua Firestore
                    db.collection("nguoi_dung").document(uid).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    boolean isVerified = Boolean.TRUE.equals(
                                            documentSnapshot.getBoolean("verified"));

                                    if (!isVerified) {
                                        auth.signOut();
                                        Toast.makeText(LoginActivity.this,
                                                "Tài khoản chưa xác thực OTP",
                                                Toast.LENGTH_LONG).show();

                                        Intent intent = new Intent(
                                                LoginActivity.this,
                                                VerifyRegisterOtpActivity.class);
                                        intent.putExtra("uid", uid);
                                        intent.putExtra("email",
                                                edtUsername.getText().toString().trim());
                                        Log.d("VerifyRegisterOtpActivity", "uid: " + uid + " " + "email: " + edtUsername.getText().toString().trim() + "");
                                        startActivity(intent);
                                        finish();
                                        return;
                                    }

                                    // ✅ Đã xác thực → tiếp tục đăng nhập
                                    proceedLogin(uid);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Lỗi kiểm tra xác thực",
                                            Toast.LENGTH_SHORT).show());
                });
    }

    // ── Hoàn tất đăng nhập ────────────────────────────────────────────────────

    private void proceedLogin(String uid) {
        String email    = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Luôn lưu vào AccountManager (đổi tài khoản nhanh)
        // Tick Remember → lưu kèm password (dùng cho autofill dropdown)
        // Không tick    → lưu password rỗng (chỉ đổi tài khoản nhanh)
        AccountManager.saveAccount(this, new AccountManager.SavedAccount(
                email,
                checkRemember.isChecked() ? password : "",
                null,
                uid
        ));

        // Lưu trạng thái Remember (giữ session hay không)
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_REMEMBER, checkRemember.isChecked())
                .apply();

        // Cập nhật trạng thái online
        db.collection("nguoi_dung").document(uid)
                .update("trang_thai_hoat_dong", true)
                .addOnSuccessListener(unused -> {
                    MyApplication.setupPresence();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}