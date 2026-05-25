package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;

    private TextView txtName;
    private TextView txtUsername;
    private TextView txtBio;
    private TextView txtSoLuongTheoDoi;

    private FirebaseFirestore db;

    private String avatarUrl = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_profile);
        enableImmersiveMode();
        imgProfile = findViewById(R.id.imgProfile);

        txtName = findViewById(R.id.txtName);
        txtUsername = findViewById(R.id.txtUsername);
        txtBio = findViewById(R.id.txtTitle);
        txtSoLuongTheoDoi = findViewById(R.id.txtSoNguoiTheoDoi);

        db = FirebaseFirestore.getInstance();

        // BẮT CLICK AVATAR
        imgProfile.setClickable(true);

        imgProfile.setOnClickListener(v -> {

            Log.d("AVATAR_CLICK", "clicked");

            if (avatarUrl != null && !avatarUrl.isEmpty()) {

                showFullImage(avatarUrl);

            }

        });

        // ✅ LOG kiểm tra uid nhận được
        String uid = getIntent().getStringExtra("user_uid");
        Log.d("USER_PROFILE", "uid nhận được: " + uid);

        if (uid == null || uid.isEmpty()) {
            Log.e("USER_PROFILE", "❌ uid null → trang hiển thị trống");
            return;
        }

        if (uid != null) {
            loadUser(uid);
        }
    }

    private void loadUser(String uid) {

        db.collection("nguoi_dung")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) return;

                    String name =
                            document.getString("ho_va_ten");

                    String username =
                            document.getString("ten_dang_nhap");

                    String avatar =
                            document.getString("anh_dai_dien");

                    String bio =
                            document.getString("tieu_su");

                    Long theodoi =
                            document.getLong("so_nguoi_theo_doi");

                    avatarUrl = avatar;

                    txtName.setText(
                            name != null ? name : "No Name"
                    );

                    txtUsername.setText(
                            username != null ? username : "@user"
                    );

                    txtBio.setText(
                            bio != null ? bio : ""
                    );

                    txtSoLuongTheoDoi.setText(
                            (theodoi != null ? theodoi : 0)
                                    + " người theo dõi"
                    );

                    Glide.with(this)
                            .load(avatar)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .error(R.drawable.ic_placeholder_avatar)
                            .circleCrop()
                            .into(imgProfile);
                });
    }

    // ==============================
    // FULL SCREEN IMAGE
    // ==============================
    private void showFullImage(String imageUrl) {

        Dialog dialog = new Dialog(
                this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        );

        ImageView imageView = new ImageView(this);

        imageView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        imageView.setBackgroundColor(Color.BLACK);

        imageView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        Glide.with(this)
                .load(imageUrl)
                .into(imageView);

        dialog.setContentView(imageView);

        // Click ảnh để đóng
        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
}