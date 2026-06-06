package com.example.doanmxh.HomePage;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.CreatePage.ImageRepository;
import com.example.doanmxh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditPostActivity extends BaseActivity {

    private ShapeableImageView imgAvatar;
    private TextView txtUsername;
    private EditText edtNoiDung;
    private MaterialButton btnPost;
    private ImageButton btnBack, btnAnh, btnCamera;
    private RecyclerView rvImages;

    private FirebaseFirestore db;
    private String postId, myUid;

    // Ảnh cũ từ Firestore
    private final List<String> existingImageUrls = new ArrayList<>();
    // Ảnh mới chọn thêm (Uri local) — chỉ để upload
    private final List<Uri> newSelectedImages = new ArrayList<>();
    // Ảnh mới đã upload xong
    private final List<String> newUploadedUrls = new ArrayList<>();
    // List String duy nhất để hiển thị (ảnh cũ URL + ảnh mới Uri.toString())
    private final List<String> displayUrls = new ArrayList<>();

    private ImageAdapter imageAdapter;
    private Uri cameraImageUri;
    private final ImageRepository imageRepository = new ImageRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        postId = getIntent().getStringExtra("post_id");
        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        imgAvatar  = findViewById(R.id.imgAvatar);
        txtUsername = findViewById(R.id.txtUsername);
        edtNoiDung = findViewById(R.id.edtNoiDung);
        btnPost    = findViewById(R.id.btnPost);
        btnBack    = findViewById(R.id.btnBack);
        btnAnh     = findViewById(R.id.btnAnh);
        btnCamera  = findViewById(R.id.btnCamera);
        rvImages   = findViewById(R.id.rvImages);

        btnPost.setText("Lưu");

        // Setup adapter duy nhất
        imageAdapter = new ImageAdapter(displayUrls, position -> {
            // Xác định xóa ảnh cũ hay ảnh mới
            if (position < existingImageUrls.size()) {
                existingImageUrls.remove(position);
            } else {
                int newIndex = position - existingImageUrls.size();
                if (newIndex < newSelectedImages.size()) {
                    newSelectedImages.remove(newIndex);
                }
            }
            // displayUrls đã bị remove bên trong ImageAdapter rồi
        });
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));
        rvImages.setAdapter(imageAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnAnh.setOnClickListener(v -> openGallery());
        btnCamera.setOnClickListener(v -> openCamera());
        btnPost.setOnClickListener(v -> savePost());

        loadUserInfo();
        loadPostContent();
    }

    // =========================
    // Load user info
    // =========================

    private void loadUserInfo() {
        if (myUid == null) return;
        db.collection("nguoi_dung").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    txtUsername.setText(doc.getString("ho_va_ten"));
                    String anh = doc.getString("anh_dai_dien");
                    if (anh != null && !anh.isEmpty()) {
                        Glide.with(this).load(anh)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .circleCrop()
                                .into(imgAvatar);
                    }
                });
    }

    // =========================
    // Load nội dung bài viết
    // =========================

    private void loadPostContent() {
        if (postId == null) return;

        db.collection("bai_viet").document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    String authorId = doc.getString("nguoi_dung_id");
                    if (myUid == null || !myUid.equals(authorId)) {
                        Toast.makeText(this, "Bạn không có quyền sửa bài này", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String noiDung = doc.getString("noi_dung");
                    edtNoiDung.setText(noiDung);
                    edtNoiDung.setSelection(noiDung != null ? noiDung.length() : 0);

                    List<String> imgs = (List<String>) doc.get("danh_sach_anh");
                    if (imgs != null && !imgs.isEmpty()) {
                        existingImageUrls.addAll(imgs);
                        displayUrls.addAll(imgs);
                        imageAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không tải được bài viết", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // =========================
    // Chọn ảnh từ thư viện
    // =========================

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickImagesLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data.getClipData() != null) {
                                ClipData clipData = data.getClipData();
                                for (int i = 0; i < clipData.getItemCount(); i++) {
                                    Uri uri = clipData.getItemAt(i).getUri();
                                    newSelectedImages.add(uri);
                                    displayUrls.add(uri.toString());
                                }
                            } else if (data.getData() != null) {
                                newSelectedImages.add(data.getData());
                                displayUrls.add(data.getData().toString());
                            }
                            imageAdapter.notifyDataSetChanged();
                        }
                    });

    // =========================
    // Chụp ảnh từ camera
    // =========================

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    result -> {
                        if (result && cameraImageUri != null) {
                            newSelectedImages.add(cameraImageUri);
                            displayUrls.add(cameraImageUri.toString());
                            imageAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "Chụp ảnh thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });

    private void openCamera() {
        File file = new File(getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
        cameraLauncher.launch(cameraImageUri);
    }

    // =========================
    // Lưu bài viết
    // =========================

    private void savePost() {
        String noiDung = edtNoiDung.getText().toString().trim();

        if (TextUtils.isEmpty(noiDung) && displayUrls.isEmpty()) {
            Toast.makeText(this, "Nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        btnPost.setText("Đang lưu...");

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Đang lưu bài viết...");
        dialog.setCancelable(false);
        dialog.show();

        newUploadedUrls.clear();

        if (newSelectedImages.isEmpty()) {
            updateFirestore(noiDung, existingImageUrls, dialog);
        } else {
            uploadNewImages(0, noiDung, dialog);
        }
    }

    private void uploadNewImages(int index, String noiDung, ProgressDialog dialog) {
        if (index >= newSelectedImages.size()) {
            List<String> finalUrls = new ArrayList<>(existingImageUrls);
            finalUrls.addAll(newUploadedUrls);
            updateFirestore(noiDung, finalUrls, dialog);
            return;
        }

        Uri uri = newSelectedImages.get(index);
        Log.d("EditPost", "Uploading image " + index + " : " + uri);

        imageRepository.uploadImage(this, uri, new ImageRepository.UploadCallback() {
            @Override
            public void onSuccess(String url) {
                newUploadedUrls.add(url);
                uploadNewImages(index + 1, noiDung, dialog);
            }

            @Override
            public void onError() {
                dialog.dismiss();
                btnPost.setEnabled(true);
                btnPost.setText("Lưu");
                Toast.makeText(EditPostActivity.this, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestore(String noiDung, List<String> finalImageUrls, ProgressDialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("noi_dung", noiDung);
        updates.put("danh_sach_anh", finalImageUrls);
        updates.put("da_chinh_sua", true);

        db.collection("bai_viet").document(postId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Đã lưu bài viết", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    btnPost.setEnabled(true);
                    btnPost.setText("Lưu");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}