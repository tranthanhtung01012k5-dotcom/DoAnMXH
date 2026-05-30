package com.example.doanmxh;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.doanmxh.Log_Res.LoginActivity;
import com.example.doanmxh.Message.MessageFragment;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private ImageButton btnMessage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check login
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // PHẢI setContentView trước
        setContentView(R.layout.activity_main);

        // Sau đó mới findViewById
//        btnMessage = findViewById(R.id.btnDirectMessage);
//
//        btnMessage.setOnClickListener(v -> {
//            Intent intent =
//                    new Intent(MainActivity.this, MessageFragment.class);
//
//            startActivity(intent);
//        });

        BottomNavigationView bottomNav =
                findViewById(R.id.bottomNav);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {

            NavController navController =
                    navHostFragment.getNavController();

            NavigationUI.setupWithNavController(
                    bottomNav,
                    navController
            );

            bottomNav.setOnItemSelectedListener(item -> {

                int selectedId = item.getItemId();

                for (int i = 0; i < bottomNav.getMenu().size(); i++) {

                    int id =
                            bottomNav.getMenu().getItem(i).getItemId();

                    View itemView = bottomNav.findViewById(id);

                    if (itemView != null) {

                        if (id == selectedId) {

                            itemView.animate()
                                    .scaleX(1.20f)
                                    .scaleY(1.20f)
                                    .setDuration(150)
                                    .start();

                        } else {

                            itemView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(150)
                                    .start();
                        }
                    }
                }

                navController.navigate(selectedId);

                return true;
            });
        }
    }
    // ── Trong MainActivity ────────────────────────────────────────────────
    @Override
    protected void onStart() {
        super.onStart();
        updateOnlineStatus(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateOnlineStatus(false);
    }
    private void updateOnlineStatus(boolean trangThaiHoatDong) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("nguoi_dung")
                .document(user.getUid())
                .update("trang_thai_hoat_dong", trangThaiHoatDong);
        if (trangThaiHoatDong == false)
        {
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
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null || result.getContents() == null) return;

        String scannedUrl = result.getContents();

        if (scannedUrl.startsWith("https://DoAnMXH.net/@")) {
            String scannedUsername =
                    scannedUrl.replace("https://DoAnMXH.net/@", "");

            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung")
                    .whereEqualTo("ten_dang_nhap", scannedUsername)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            String uid = query.getDocuments().get(0).getId();
                            Intent intent =
                                    new Intent(this, UserProfileActivity.class);
                            intent.putExtra("user_uid", uid);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this,
                                    "Không tìm thấy người dùng",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(scannedUrl)));
        }
    }
}