package com.example.doanmxh.ProfilePage;

import android.os.Bundle;
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

        String uid =
                getIntent().getStringExtra("user_uid");

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
                    Long theodoi = document.getLong("so_nguoi_theo_doi");

                    txtName.setText(name);
                    txtUsername.setText(username);
                    txtBio.setText(bio);
                    txtSoLuongTheoDoi.setText(String.valueOf(theodoi) + " người theo dõi");
                    Glide.with(this)
                            .load(avatar)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .into(imgProfile);
                });
    }
}