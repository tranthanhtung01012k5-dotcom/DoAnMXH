package com.example.doanmxh.Log_Res;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ForgotPassActivity extends BaseActivity {

    // ── Views ──────────────────────────────────────────────
    private LinearLayout layoutEmail, layoutOtp;
    private EditText     edtEmail, edtOtp;
    private Button       btnSendOtp, btnVerifyOtp;
    private ProgressBar  progressBar;
    private TextView     txtTitle, txtSubtitle;

    // ── Firebase ───────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    // ── State ──────────────────────────────────────────────
    private String currentUid   = null;
    private String currentEmail = null;

    // ── EmailJS config — điền thông tin của bạn ───────────
    private static final String EMAILJS_SERVICE_ID        = "service_09rmxf2";
    private static final String EMAILJS_USER_ID           = "q5i3vyErg69Xfxaza";
    private static final String EMAILJS_TEMPLATE_OTP      = "template_wcxozzh";
    private static final String EMAILJS_TEMPLATE_PASSWORD = "template_uahnvl8";

    // ══════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        anhXa();
        setupListeners();

        // Bắt đầu ở bước nhập email
        showStep(1);
    }

    // ── Ánh xạ view ───────────────────────────────────────
    private void anhXa() {
        layoutEmail  = findViewById(R.id.layoutEmail);
        layoutOtp    = findViewById(R.id.layoutOtp);
        edtEmail     = findViewById(R.id.edtEmail);
        edtOtp       = findViewById(R.id.edtOtp);
        btnSendOtp   = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        progressBar  = findViewById(R.id.progressBar);
        txtTitle     = findViewById(R.id.txtTitle);
        txtSubtitle  = findViewById(R.id.txtSubtitle);
    }

    // ── Listener ──────────────────────────────────────────
    private void setupListeners() {

        // Bước 1: Gửi OTP
        btnSendOtp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();

            if (email.isEmpty()) {
                edtEmail.setError("Vui lòng nhập email");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Email không hợp lệ");
                return;
            }

            checkEmailAndSendOtp(email);
        });

        // Bước 2: Xác thực OTP
        btnVerifyOtp.setOnClickListener(v -> {
            String otp = edtOtp.getText().toString().trim();

            if (otp.length() != 6) {
                edtOtp.setError("OTP gồm 6 chữ số");
                return;
            }

            verifyOtpAndResetPassword(otp);
        });
    }

    // ══════════════════════════════════════════════════════
    //  BƯỚC 1 — Kiểm tra email trong Firestore
    // ══════════════════════════════════════════════════════
    private void checkEmailAndSendOtp(String email) {
        setLoading(true);

        db.collection("nguoi_dung")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        setLoading(false);
                        edtEmail.setError("Email không tồn tại trong hệ thống");
                        return;
                    }

                    currentUid   = querySnapshot.getDocuments().get(0).getId();
                    currentEmail = email;

                    String otp        = generateOtp();
                    long   expireTime = System.currentTimeMillis() + 5 * 60 * 1000;

                    saveOtpToFirestore(currentUid, email, otp, expireTime);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi: " + e.getMessage());
                });
    }

    // ══════════════════════════════════════════════════════
    //  BƯỚC 2 — Lưu OTP lên Firestore rồi gửi mail
    // ══════════════════════════════════════════════════════
    private void saveOtpToFirestore(String uid, String email,
                                    String otp, long expireTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("otp_code",    otp);
        data.put("expire_time", expireTime);
        data.put("da_su_dung",  false);
        data.put("email",       email);

        db.collection("otp_reset")
                .document(uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    // Lưu xong → gửi OTP về mail
                    sendEmailJs(email, otp, "otp");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi lưu OTP: " + e.getMessage());
                });
    }

    // ══════════════════════════════════════════════════════
    //  BƯỚC 3 — Verify OTP
    // ══════════════════════════════════════════════════════
    private void verifyOtpAndResetPassword(String inputOtp) {
        if (currentUid == null) {
            toast("Phiên làm việc hết hạn, vui lòng thử lại");
            showStep(1);
            return;
        }

        setLoading(true);

        db.collection("otp_reset").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        setLoading(false);
                        toast("Không tìm thấy OTP, vui lòng gửi lại");
                        return;
                    }

                    String  savedOtp   = doc.getString("otp_code");
                    long    expireTime = doc.getLong("expire_time");
                    boolean daSuDung   = Boolean.TRUE.equals(
                            doc.getBoolean("da_su_dung"));
                    String  email      = doc.getString("email");

                    if (daSuDung) {
                        setLoading(false);
                        toast("OTP đã được sử dụng, vui lòng gửi lại");
                        return;
                    }
                    if (System.currentTimeMillis() > expireTime) {
                        setLoading(false);
                        toast("OTP đã hết hạn, vui lòng gửi lại");
                        return;
                    }
                    if (!inputOtp.equals(savedOtp)) {
                        setLoading(false);
                        edtOtp.setError("Mã OTP không đúng");
                        return;
                    }

                    // ✅ OTP đúng → đánh dấu đã dùng ngay
                    db.collection("otp_reset")
                            .document(currentUid)
                            .update("da_su_dung", true);

                    // Sinh mật khẩu mới → cập nhật
                    String newPassword = generatePassword();
                    updatePasswordInAuth(currentUid, email, newPassword);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi xác thực: " + e.getMessage());
                });
    }

    // ══════════════════════════════════════════════════════
    //  BƯỚC 4 — Cập nhật mật khẩu Firebase Auth + Firestore
    // ══════════════════════════════════════════════════════
    private void updatePasswordInAuth(String uid, String email,
                                      String newPassword) {
        // Lấy mật khẩu cũ trong Firestore để re-authenticate
        db.collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String oldPassword = userDoc.getString("mat_khau");
                    if (oldPassword == null) {
                        setLoading(false);
                        toast("Không thể lấy thông tin tài khoản");
                        return;
                    }

                    auth.signInWithEmailAndPassword(email, oldPassword)
                            .addOnSuccessListener(authResult -> {
                                authResult.getUser()
                                        .updatePassword(newPassword)
                                        .addOnSuccessListener(unused -> {

                                            // Cập nhật mật khẩu mới vào Firestore
                                            db.collection("nguoi_dung")
                                                    .document(uid)
                                                    .update("mat_khau", newPassword)
                                                    .addOnSuccessListener(u -> {
                                                        // Gửi mật khẩu mới về mail
                                                        sendEmailJs(email,
                                                                newPassword,
                                                                "new_password");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        setLoading(false);
                                                        toast("Lỗi cập nhật Firestore");
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            setLoading(false);
                                            toast("Lỗi đổi mật khẩu: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Xác thực thất bại: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi tải thông tin user");
                });
    }

    // ══════════════════════════════════════════════════════
    //  Gửi email qua EmailJS (dùng chung OTP + mật khẩu mới)
    // ══════════════════════════════════════════════════════
    private void sendEmailJs(String toEmail, String content, String type) {

        new Thread(() -> {

            try {

                URL url = new URL(
                        "https://api.emailjs.com/api/v1.0/email/send");

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("origin", "http://localhost");
                conn.setDoOutput(true);

                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email", toEmail);

                JSONObject body = new JSONObject();

                body.put("service_id", EMAILJS_SERVICE_ID);
                body.put("template_id",
                        type.equals("otp")
                                ? EMAILJS_TEMPLATE_OTP
                                : EMAILJS_TEMPLATE_PASSWORD);

                body.put("user_id", EMAILJS_USER_ID);

                // IMPORTANT
                if (type.equals("otp")) {
                    templateParams.put("otp", content);
                } else {
                    templateParams.put("new_password", content);
                }

                body.put("template_params", templateParams);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();

                runOnUiThread(() -> {

                    setLoading(false);

                    if (code == 200) {

                        if (type.equals("otp")) {

                            toast("OTP đã gửi về " + toEmail);
                            showStep(2);

                        } else {

                            toast("Mật khẩu mới đã gửi!");
                            auth.signOut();
                            finish();
                        }

                    } else {

                        toast("EmailJS lỗi: " + code);
                    }
                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() -> {
                    setLoading(false);
                    toast(e.getMessage());
                });
            }

        }).start();
    }

    // ══════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════

    /** Sinh OTP 6 chữ số */
    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    /** Sinh mật khẩu 10 ký tự: hoa + thường + số + ký tự đặc biệt */
    private String generatePassword() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String special = "@#$%&*!";
        String all     = upper + lower + digits + special;

        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        // Đảm bảo mỗi loại có ít nhất 1 ký tự
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        for (int i = 0; i < 6; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        // Shuffle để không bị đoán thứ tự
        List<Character> chars = new ArrayList<>();
        for (char c : sb.toString().toCharArray()) chars.add(c);
        Collections.shuffle(chars);

        StringBuilder result = new StringBuilder();
        for (char c : chars) result.append(c);
        return result.toString();
    }

    /** Chuyển bước hiển thị */
    private void showStep(int step) {
        if (step == 1) {
            layoutEmail.setVisibility(View.VISIBLE);
            layoutOtp.setVisibility(View.GONE);
            txtTitle.setText("Quên mật khẩu");
            txtSubtitle.setText("Nhập email đã đăng ký để nhận mã OTP");
        } else {
            layoutEmail.setVisibility(View.GONE);
            layoutOtp.setVisibility(View.VISIBLE);
            txtTitle.setText("Nhập mã OTP");
            txtSubtitle.setText("Mã OTP đã gửi về " + currentEmail
                    + "\nHiệu lực trong 5 phút");
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendOtp.setEnabled(!loading);
        btnVerifyOtp.setEnabled(!loading);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}