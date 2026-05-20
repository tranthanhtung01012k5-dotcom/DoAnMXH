package com.example.doanmxh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.Log_Res.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {

            FirebaseUser user =
                    FirebaseAuth.getInstance().getCurrentUser();

            // Kiểm tra ghi nhớ đăng nhập
            SharedPreferences prefs =
                    getSharedPreferences("login_pref", MODE_PRIVATE);

            boolean isRemembered =
                    prefs.getBoolean("remember_login", false);

            // Chưa đăng nhập hoặc không ghi nhớ
            if (user == null || !isRemembered) {

                FirebaseAuth.getInstance().signOut();

                goToLogin();
                return;
            }

            // Reload kiểm tra tài khoản còn tồn tại không
            user.reload().addOnCompleteListener(task -> {

                FirebaseUser reloadedUser =
                        FirebaseAuth.getInstance().getCurrentUser();

                // Tài khoản bị xóa khỏi Firebase Auth
                if (!task.isSuccessful() || reloadedUser == null) {

                    FirebaseAuth.getInstance().signOut();

                    goToLogin();
                    return;
                }

                // Kiểm tra tồn tại trong Firestore
                FirebaseFirestore.getInstance()
                        .collection("nguoi_dung")
                        .document(reloadedUser.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {

                            if (doc.exists()) {

                                // Tài khoản hợp lệ
                                goToMain();

                            } else {

                                // Bị xóa khỏi Firestore
                                FirebaseAuth.getInstance().signOut();

                                goToLogin();
                            }
                        })
                        .addOnFailureListener(e -> {

                            // Lỗi mạng vẫn cho vào app
                            goToMain();

                        });

            });

        }, 3000);
    }

    private void goToMain() {

        Intent intent =
                new Intent(SplashActivity.this, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void goToLogin() {

        Intent intent =
                new Intent(SplashActivity.this, LoginActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}