package com.example.doanmxh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.doanmxh.Log_Res.LoginActivity;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends BaseActivity {
    private static final String PREF_SETTINGS = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState); // BaseActivity tự lo ẩn status bar

        ProcessLifecycleOwner.get().getLifecycle()
                .addObserver(new DefaultLifecycleObserver() {
                    @Override
                    public void onStart(@NonNull LifecycleOwner owner) {
                        updateOnlineStatus(true);
                    }

                    @Override
                    public void onStop(@NonNull LifecycleOwner owner) {
                        updateOnlineStatus(false);
                    }
                });

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            bottomNav.setOnItemSelectedListener(item -> {
                int selectedId = item.getItemId();

                for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                    int id = bottomNav.getMenu().getItem(i).getItemId();
                    View itemView = bottomNav.findViewById(id);

                    if (itemView != null) {
                        if (id == selectedId) {
                            itemView.animate().scaleX(1.20f).scaleY(1.20f).setDuration(150).start();
                        } else {
                            itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                        }
                    }
                }

                navController.navigate(selectedId);
                return true;
            });
        }
    }

    private void updateOnlineStatus(boolean trangThaiHoatDong) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("nguoi_dung")
                .document(user.getUid())
                .update("trang_thai_hoat_dong", trangThaiHoatDong);

        if (!trangThaiHoatDong) {
            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung")
                    .document(user.getUid())
                    .update("lan_cuoi_hoat_dong", FieldValue.serverTimestamp());
        }
    }

    public void startQrScanner() {
        new IntentIntegrator(this)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setPrompt("Quét mã QR profile")
                .setCameraId(0)
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(false)
                .setOrientationLocked(true)
                .initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null || result.getContents() == null) return;

        String scannedUrl = result.getContents();

        if (scannedUrl.startsWith("https://DoAnMXH.net/@")) {
            String scannedUsername = scannedUrl.replace("https://DoAnMXH.net/@", "");

            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung")
                    .whereEqualTo("ten_dang_nhap", scannedUsername)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            String uid = query.getDocuments().get(0).getId();
                            Intent intent = new Intent(this, UserProfileActivity.class);
                            intent.putExtra("user_uid", uid);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(scannedUrl)));
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        android.util.Log.d("THEME", "darkMode = " + darkMode);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}