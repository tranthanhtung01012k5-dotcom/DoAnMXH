package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
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

        String uid = getIntent().getStringExtra("user_uid");

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

}