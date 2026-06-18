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

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RegisterActivity extends BaseActivity {
    // Cấu hình EmailJS (Giống như trong ForgotPassActivity)
    private static final String EMAILJS_SERVICE_ID = "service_09rmxf2";
    private static final String EMAILJS_USER_ID    = "q5i3vyErg69Xfxaza";
    private static final String EMAILJS_TEMPLATE_OTP = "template_wcxozzh"; // Đảm bảo template này chấp nhận biến {{otp}}

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
        setupLoginLink();
    }

    private void registerUser() {
        String hoVaTen = edtFullname.getText().toString().trim();
        String tenDangNhap = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String matKhau = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(hoVaTen) || TextUtils.isEmpty(tenDangNhap) || TextUtils.isEmpty(email) || TextUtils.isEmpty(matKhau)) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra tên người dùng trùng
        db.collection("nguoi_dung").whereEqualTo("ten_dang_nhap", tenDangNhap).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Tên người dùng đã tồn tại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createAccount(hoVaTen, tenDangNhap, email, matKhau);
                });
    }

    private void createAccount(String hoVaTen, String tenDangNhap, String email, String matKhau) {
        auth.createUserWithEmailAndPassword(email, matKhau).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = task.getResult().getUser().getUid();

                // 1. Lưu thông tin người dùng vào Firestore với verified = false
                Map<String, Object> nguoiDung = new HashMap<>();
                nguoiDung.put("ho_va_ten",   hoVaTen);
                nguoiDung.put("ten_dang_nhap", tenDangNhap);
                nguoiDung.put("email",       email);
                nguoiDung.put("mat_khau",       matKhau);
                nguoiDung.put("anh_dai_dien", "");
                nguoiDung.put("tieu_su",     "");
                nguoiDung.put("ngay_tao",    new Date());
                nguoiDung.put("verified",    false);
                nguoiDung.put("private",     false);
                nguoiDung.put("lan_hoat_dong_cuoi",new Date());
                nguoiDung.put("ngay_cap_nhat_mat_khau",new Date());
                nguoiDung.put("uid",         uid);
                nguoiDung.put("so_nguoi_theo_doi",       0);
                nguoiDung.put("trang_thai_hoat_dong",  false);
                nguoiDung.put("so_nguoi_dang_theo_doi",  0);
                nguoiDung.put("link_lien_ket",  "https://DoAnMXH.net/@" + tenDangNhap);

                db.collection("nguoi_dung").document(uid).set(nguoiDung).addOnSuccessListener(unused -> {
                    // 2. Tạo và gửi OTP
                    sendOtpProcess(uid, email);
                });
            } else {
                Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Trong RegisterActivity.java, tại hàm sendOtpProcess

    private void sendOtpProcess(String uid, String email) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        long expireTime = System.currentTimeMillis() + 5 * 60 * 1000;

        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp_code", otp);
        otpData.put("expire_time", expireTime);
        otpData.put("da_su_dung", false); // Thêm trạng thái này cho khớp với VerifyRegisterOtpActivity

        // Lưu vào collection "otp_register" thay vì "otp_verify" để khớp với Activity mới
        db.collection("otp_register").document(uid).set(otpData).addOnSuccessListener(unused -> {
            // Gửi qua EmailJS
            sendEmailJs(email, otp);

            Toast.makeText(this, "Vui lòng nhập mã OTP đã gửi về email", Toast.LENGTH_LONG).show();

            // Mở màn hình VerifyRegisterOtpActivity
            Intent intent = new Intent(RegisterActivity.this, VerifyRegisterOtpActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("email", email);
            startActivity(intent);
            finish(); // Đóng Activity đăng ký
        });
    }

    private void sendEmailJs(String email, String otp) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("origin", "http://localhost");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("service_id", EMAILJS_SERVICE_ID);
                body.put("template_id", EMAILJS_TEMPLATE_OTP);
                body.put("user_id", EMAILJS_USER_ID);
                JSONObject params = new JSONObject();
                params.put("to_email", email);
                params.put("otp", otp);
                body.put("template_params", params);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void setupLoginLink() {
        txtLogin = findViewById(R.id.txtLogin);
        String text = "Đã có tài khoản? Đăng nhập";
        SpannableString ss = new SpannableString(text);
        ClickableSpan cs = new ClickableSpan() {
            @Override public void onClick(@NonNull View v) { startActivity(new Intent(RegisterActivity.this, LoginActivity.class)); }
            @Override public void updateDrawState(@NonNull TextPaint ds) { ds.setUnderlineText(false); ds.setFakeBoldText(true); }
        };
        ss.setSpan(cs, text.indexOf("Đăng nhập"), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        txtLogin.setText(ss);
        txtLogin.setMovementMethod(LinkMovementMethod.getInstance());
        txtLogin.setHighlightColor(Color.TRANSPARENT);
    }
}