package com.example.doanmxh.ProfilePage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doanmxh.CreatePage.ImageRepository;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextView btnCancel, btnDone, tvAddLink;

    private EditText edtName, edtBio;

    private ImageView ivAvatar;

    private MaterialSwitch switchPrivate;

    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    private FirebaseUser currentUser;

    private String avatarUrl = "";

    private Uri selectedImageUri;

    private final ImageRepository imageRepository =
            new ImageRepository();

    // =========================
    // PICK IMAGE
    // =========================

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            selectedImageUri =
                                    result.getData().getData();

                            // Preview ảnh
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .into(ivAvatar);

                            // Upload ảnh
                            uploadAvatarToImgbb(selectedImageUri);
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_profile);

        anhXa();

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            finish();

            return;
        }

        loadUserProfile();

        // =========================
        // CLICK AVATAR
        // =========================

        ivAvatar.setOnClickListener(v -> {

            Intent intent =
                    new Intent(Intent.ACTION_GET_CONTENT);

            intent.setType("image/*");

            pickImageLauncher.launch(intent);
        });

        // =========================
        // BUTTON
        // =========================

        btnCancel.setOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> saveProfile());

        tvAddLink.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "TODO: Thêm liên kết",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    // =========================
    // ANH XA
    // =========================

    private void anhXa() {

        btnCancel = findViewById(R.id.btn_cancel);

        btnDone = findViewById(R.id.btn_done);

        edtName = findViewById(R.id.txtName);

        edtBio = findViewById(R.id.txtTitle);

        ivAvatar = findViewById(R.id.iv_avatar);

        tvAddLink = findViewById(R.id.tv_add_link);

        switchPrivate =
                findViewById(R.id.switch_private);
    }

    // =========================
    // LOAD USER
    // =========================

    private void loadUserProfile() {

        String uid = currentUser.getUid();

        db.collection("nguoi_dung")
                .document(uid)
                .get()
                .addOnSuccessListener(this::setUserData)
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "Lỗi tải dữ liệu",
                                Toast.LENGTH_SHORT
                        ).show());
    }

    private void setUserData(DocumentSnapshot document) {

        if (!document.exists()) return;

        String name =
                document.getString("ho_va_ten");

        String bio =
                document.getString("tieu_su");

        String avatar =
                document.getString("anh_dai_dien");

        Boolean isPrivate =
                document.getBoolean("private");

        String username =
                document.getString("ten_dang_nhap");

        edtName.setText(name);

        edtBio.setText(bio);

        avatarUrl = avatar;

        if (isPrivate != null) {

            switchPrivate.setChecked(isPrivate);
        }

        // Avatar
        if (!TextUtils.isEmpty(avatar)) {

            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .into(ivAvatar);

        } else {

            ivAvatar.setImageResource(
                    R.drawable.ic_placeholder_avatar
            );
        }
    }

    // =========================
    // UPLOAD AVATAR
    // =========================

    private void uploadAvatarToImgbb(Uri uri) {

        imageRepository.uploadImage(
                this,
                uri,
                new ImageRepository.UploadCallback() {

                    @Override
                    public void onSuccess(String url) {

                        avatarUrl = url;

                        String uid =
                                currentUser.getUid();

                        db.collection("nguoi_dung")
                                .document(uid)
                                .update("anh_dai_dien", url)
                                .addOnSuccessListener(unused -> {

                                    Toast.makeText(
                                            EditProfileActivity.this,
                                            "Cập nhật avatar thành công",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                    }

                    @Override
                    public void onError() {

                        Toast.makeText(
                                EditProfileActivity.this,
                                "Upload ảnh thất bại",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // =========================
    // SAVE PROFILE
    // =========================

    private void saveProfile() {

        String name =
                edtName.getText().toString().trim();

        String bio =
                edtBio.getText().toString().trim();

        boolean isPrivate =
                switchPrivate.isChecked();

        if (name.isEmpty()) {

            edtName.setError("Vui lòng nhập tên");

            return;
        }

        String uid = currentUser.getUid();

        Map<String, Object> map =
                new HashMap<>();

        map.put("ho_va_ten", name);

        map.put("tieu_su", bio);

        map.put("anh_dai_dien", avatarUrl);

        map.put("private", isPrivate);

        db.collection("nguoi_dung")
                .document(uid)
                .update(map)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Cập nhật thành công",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "Lỗi cập nhật",
                                Toast.LENGTH_SHORT
                        ).show());
//        loadUserProfile();
    }
}