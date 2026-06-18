package com.example.doanmxh.Log_Res;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class VerifyRegisterOtpActivity extends AppCompatActivity {

    EditText edtOtp;
    Button btnVerify;
    TextView btnResendOtp, txtTitle, txtDescription;

    FirebaseFirestore db;

    String uid;
    String email;

    // EmailJS
    private static final String EMAILJS_SERVICE_ID   = "service_09rmxf2";
    private static final String EMAILJS_USER_ID      = "q5i3vyErg69Xfxaza";
    private static final String EMAILJS_TEMPLATE_OTP = "template_wcxozzh";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_register_otp);

        initView();

        db    = FirebaseFirestore.getInstance();
        uid   = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        btnVerify.setOnClickListener(v -> verifyOtp());
        btnResendOtp.setOnClickListener(v -> resendOtp());

        // ── Tự động gửi OTP ngay khi vào màn hình ────────────────────────────
        sendOtp();
    }

    private void initView() {
        edtOtp       = findViewById(R.id.edtOtp);
        btnVerify    = findViewById(R.id.btnVerify);
        btnResendOtp = findViewById(R.id.btnResendOtp);
        txtTitle       = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
    }

    // ================= GỬI OTP (tạo mới hoặc gửi lại) =================

    /**
     * Tạo OTP mới, lưu vào Firestore bằng set() + merge để không crash
     * dù document chưa tồn tại, sau đó gửi email.
     */
    private void sendOtp() {
        String newOtp = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        long expire   = System.currentTimeMillis() + 5 * 60 * 1000; // 5 phút

        Map<String, Object> data = new HashMap<>();
        data.put("otp_code",    newOtp);
        data.put("expire_time", expire);
        data.put("da_su_dung",  false);
        data.put("uid",         uid);

        // set() + merge → tạo mới nếu chưa có, cập nhật nếu đã có → không crash
        db.collection("otp_register")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    sendEmailJs(email, newOtp);
                    Log.d("OTP", "Đã lưu OTP và gửi email tới: " + email);
                })
                .addOnFailureListener(e -> {
                    toast("Lỗi tạo OTP: " + e.getMessage());
                    Log.e("OTP", "Lỗi lưu OTP: " + e.getMessage());
                });
    }

    // ================= RESEND OTP =================

    private void resendOtp() {
        toast("Đang gửi lại OTP...");
        sendOtp(); // dùng lại sendOtp() cho nhất quán
    }

    // ================= VERIFY OTP =================

    private void verifyOtp() {
        String inputOtp = edtOtp.getText().toString().trim();

        if (inputOtp.length() != 6) {
            edtOtp.setError("OTP 6 số");
            return;
        }

        btnVerify.setEnabled(false);

        db.collection("otp_register")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    btnVerify.setEnabled(true);

                    if (!doc.exists()) {
                        toast("Không tìm thấy OTP");
                        return;
                    }

                    String savedOtp = doc.getString("otp_code");
                    long   expire   = doc.getLong("expire_time");
                    boolean used    = Boolean.TRUE.equals(doc.getBoolean("da_su_dung"));

                    if (used) {
                        toast("OTP đã được dùng");
                        return;
                    }

                    if (System.currentTimeMillis() > expire) {
                        toast("OTP đã hết hạn, vui lòng gửi lại");
                        return;
                    }

                    if (!inputOtp.equals(savedOtp)) {
                        edtOtp.setError("OTP sai");
                        return;
                    }

                    // Đánh dấu đã dùng
                    db.collection("otp_register")
                            .document(uid)
                            .update("da_su_dung", true);

                    saveUser();
                })
                .addOnFailureListener(e -> {
                    btnVerify.setEnabled(true);
                    toast("Lỗi: " + e.getMessage());
                });
    }

    // ================= SAVE USER =================

    private void saveUser() {
        db.collection("nguoi_dung")
                .document(uid)
                .update("verified", true)
                .addOnSuccessListener(unused -> {
                    toast("Đăng ký thành công!");
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> toast("Lỗi cập nhật user: " + e.getMessage()));
    }

    // ================= EMAILJS =================

    private void sendEmailJs(String toEmail, String otp) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("origin", "http://localhost");
                conn.setDoOutput(true);

                JSONObject params = new JSONObject();
                params.put("to_email", toEmail);
                params.put("otp",      otp);

                JSONObject body = new JSONObject();
                body.put("service_id",      EMAILJS_SERVICE_ID);
                body.put("template_id",     EMAILJS_TEMPLATE_OTP);
                body.put("user_id",         EMAILJS_USER_ID);
                body.put("template_params", params);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                Log.d("EmailJS", "Response code: " + code);

                runOnUiThread(() -> {
                    if (code == 200) {
                        toast("OTP đã gửi tới " + toEmail);
                    } else {
                        toast("EmailJS lỗi: " + code);
                    }
                });

            } catch (Exception e) {
                Log.e("EmailJS", "Exception: " + e.getMessage());
                runOnUiThread(() -> toast("Lỗi gửi email: " + e.getMessage()));
            }
        }).start();
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}