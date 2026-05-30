package com.example.doanmxh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

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
        enableImmersiveMode();
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
                                updateOnlineStatus(true);
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
    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
    private void updateOnlineStatus(boolean online) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("nguoi_dung")
                .document(user.getUid())
                .update("trang_thai_hoat_dong", online);
    }
}