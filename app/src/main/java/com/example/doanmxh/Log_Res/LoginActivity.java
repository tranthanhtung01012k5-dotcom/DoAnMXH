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
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.MainActivity;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin;
    CheckBox checkRemember;
    TextView txtRegister;
    FirebaseAuth auth;
    FirebaseFirestore db;

    private static final String PREF_NAME = "login_pref";
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

        // ── Spannable "Đăng ký" ──
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
                ds.setColor(Color.WHITE);
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

    private void loginUser() {

        String email = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {

            Toast.makeText(
                    this,
                    "Nhập đầy đủ thông tin",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (password.length() < 6) {

            Toast.makeText(
                    this,
                    "Mật khẩu tối thiểu 6 ký tự",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        if (auth.getCurrentUser() == null) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Không lấy được user",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        auth.getCurrentUser()
                                .reload()
                                .addOnCompleteListener(reloadTask -> {

                                    boolean verified =
                                            auth.getCurrentUser()
                                                    .isEmailVerified();

                                    if (!verified) {

                                        auth.signOut();

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Email chưa được xác minh",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        // gửi lại email xác minh
                                        auth.signInWithEmailAndPassword(
                                                        email,
                                                        password
                                                )
                                                .addOnSuccessListener(authResult -> {

                                                    if (auth.getCurrentUser() != null) {

                                                        auth.getCurrentUser()
                                                                .sendEmailVerification();

                                                        Toast.makeText(
                                                                LoginActivity.this,
                                                                "Đã gửi lại email xác minh",
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    }

                                                    auth.signOut();

                                                });

                                        return;
                                    }

                                    // ✅ Đã xác minh

                                    SharedPreferences prefs =
                                            getSharedPreferences(
                                                    PREF_NAME,
                                                    MODE_PRIVATE
                                            );

                                    SharedPreferences.Editor editor =
                                            prefs.edit();

                                    editor.putBoolean(
                                            KEY_REMEMBER,
                                            checkRemember.isChecked()
                                    );

                                    editor.apply();

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Đăng nhập thành công",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    Intent intent =
                                            new Intent(
                                                    LoginActivity.this,
                                                    MainActivity.class
                                            );

                                    intent.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);

                                    finish();

                                });

                    } else {

                        Toast.makeText(
                                LoginActivity.this,
                                "Đăng nhập thất bại: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                });
    }
}