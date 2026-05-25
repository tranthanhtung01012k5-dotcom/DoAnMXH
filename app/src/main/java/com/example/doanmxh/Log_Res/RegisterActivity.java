package com.example.doanmxh.Log_Res;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText edtFullname, edtUsername, edtEmail, edtPassword;
    Button btnRegister;
    CheckBox checkTerms;
    TextView txtLogin;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullname = findViewById(R.id.edtFullname);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail    = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        checkTerms  = findViewById(R.id.checkTerms);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());

        // ── Spannable "Đăng nhập" ──
        txtLogin = findViewById(R.id.txtLogin);
        String text = "Đã có tài khoản? Đăng nhập";
        SpannableString spannableString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.WHITE);
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };
        spannableString.setSpan(
                clickableSpan,
                text.indexOf("Đăng nhập"),
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        txtLogin.setText(spannableString);
        txtLogin.setMovementMethod(LinkMovementMethod.getInstance());
        txtLogin.setHighlightColor(Color.TRANSPARENT);
    }

    private void registerUser() {

        String hoVaTen     = edtFullname.getText().toString().trim();
        String tenDangNhap = edtUsername.getText().toString().trim();
        String email       = edtEmail.getText().toString().trim();
        String matKhau     = edtPassword.getText().toString().trim();

        // ── Validate cơ bản ──
        if (TextUtils.isEmpty(hoVaTen) || TextUtils.isEmpty(tenDangNhap)
                || TextUtils.isEmpty(email) || TextUtils.isEmpty(matKhau)) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (matKhau.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý điều khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Validate tên đăng nhập: chỉ chữ, số, dấu _ , từ 3-20 ký tự ──
        if (!tenDangNhap.matches("^[a-zA-Z0-9_]{3,20}$")) {
            Toast.makeText(this,
                    "Tên người dùng chỉ gồm chữ, số, dấu _ và từ 3-20 ký tự",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ── Kiểm tra ten_dang_nhap trùng trước khi đăng ký ──
        db.collection("nguoi_dung")
                .whereEqualTo("ten_dang_nhap", tenDangNhap)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this,
                                "Tên người dùng đã tồn tại",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ── Đăng ký Firebase Auth ──
                    auth.createUserWithEmailAndPassword(email, matKhau)
                            .addOnCompleteListener(task -> {

                                if (task.isSuccessful()) {

                                    FirebaseUser user = auth.getCurrentUser();

                                    if (user == null) {
                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Không lấy được thông tin user",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        return;
                                    }

                                    String uid = user.getUid();

                                    // ── Gửi email xác minh ──
                                    user.sendEmailVerification()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(
                                                        RegisterActivity.this,
                                                        "ĐÃ GỬI MAIL THÀNH CÔNG",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(
                                                        RegisterActivity.this,
                                                        "LỖI: " + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            })
                                            .addOnCompleteListener(verifyTask -> {

                                                if (verifyTask.isSuccessful()) {

                                                    // ── Lưu Firestore ──
                                                    Map<String, Object> nguoiDung = new HashMap<>();
                                                    nguoiDung.put("ho_va_ten",   hoVaTen);
                                                    nguoiDung.put("ten_dang_nhap", tenDangNhap);
                                                    nguoiDung.put("email",       email);
                                                    nguoiDung.put("anh_dai_dien", "");
                                                    nguoiDung.put("tieu_su",     "");
                                                    nguoiDung.put("ngay_tao",    new Date());
                                                    nguoiDung.put("verified",    false);
                                                    nguoiDung.put("private",     false);
                                                    nguoiDung.put("uid",         uid);
                                                    nguoiDung.put("so_nguoi_theo_doi",       0);
                                                    nguoiDung.put("so_nguoi_dang_theo_doi",  0);

                                                    db.collection("nguoi_dung")
                                                            .document(uid)
                                                            .set(nguoiDung)
                                                            .addOnSuccessListener(unused -> {
                                                                Toast.makeText(
                                                                        RegisterActivity.this,
                                                                        "Đã gửi email xác minh",
                                                                        Toast.LENGTH_LONG
                                                                ).show();
                                                                auth.signOut();
                                                                startActivity(new Intent(
                                                                        RegisterActivity.this,
                                                                        LoginActivity.class));
                                                                finish();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(
                                                                        RegisterActivity.this,
                                                                        "Lỗi lưu dữ liệu: " + e.getMessage(),
                                                                        Toast.LENGTH_LONG
                                                                ).show();
                                                            });

                                                } else {
                                                    Toast.makeText(
                                                            RegisterActivity.this,
                                                            "Không gửi được email xác minh",
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                }
                                            });

                                } else {
                                    Toast.makeText(
                                            RegisterActivity.this,
                                            task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "Đăng ký thất bại",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Lỗi kiểm tra tên người dùng: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}