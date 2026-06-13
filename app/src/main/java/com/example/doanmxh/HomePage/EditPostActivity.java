package com.example.doanmxh.HomePage;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.CreatePage.AudioAdapter;
import com.example.doanmxh.CreatePage.AudioRecordBottomSheet;
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
    private final List<String> existingVideoUrls = new ArrayList<>();
    private final List<Uri> newSelectedVideos = new ArrayList<>();
    private final List<String> newUploadedVideoUrls = new ArrayList<>();
    // Thêm field này (chưa có)
    private final List<String> newUploadedAudioUrls = new ArrayList<>();
    private final List<String> newUploadedVideoUrls_cam = new ArrayList<>(); // dùng chung newUploadedVideoUrls đã có
    private RecyclerView rvAudio;

    private final List<String> existingAudioUrls = new ArrayList<>();
    private final List<Uri> newSelectedAudios = new ArrayList<>();

    private AudioAdapter audioAdapter;
    private ShapeableImageView imgAvatar;
    private TextView txtUsername;
    private EditText edtNoiDung;
    private MaterialButton btnPost;
    private ImageButton btnBack, btnAnh, btnCamera,btnMic;
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
        btnMic  = findViewById(R.id.btnMic);
        rvImages   = findViewById(R.id.rvImages);

        btnPost.setText("Lưu");
        btnMic.setOnClickListener(v -> showAudioOptions());
        // Setup adapter duy nhất
        imageAdapter = new ImageAdapter(displayUrls, position -> {

            String item = displayUrls.get(position);

            if (item.startsWith(ImageAdapter.VIDEO_PREFIX)) {

                String videoValue =
                        item.substring(ImageAdapter.VIDEO_PREFIX.length());

                existingVideoUrls.remove(videoValue);

                for (int i = 0; i < newSelectedVideos.size(); i++) {

                    if (newSelectedVideos.get(i)
                            .toString()
                            .equals(videoValue)) {

                        newSelectedVideos.remove(i);
                        break;
                    }
                }

            } else {

                existingImageUrls.remove(item);

                for (int i = 0; i < newSelectedImages.size(); i++) {

                    if (newSelectedImages.get(i)
                            .toString()
                            .equals(item)) {

                        newSelectedImages.remove(i);
                        break;
                    }
                }
            }
        });
        rvAudio = findViewById(R.id.rvAudio);
        List<String> audioPathList = new ArrayList<>();
        for (Uri uri : newSelectedAudios) {
            audioPathList.add(uri.toString());
        }
        audioAdapter = new AudioAdapter(
                audioPathList,
                position -> {

                    String audio = audioAdapter.getItem(position);

                    if (existingAudioUrls.contains(audio)) {
                        existingAudioUrls.remove(audio);
                    }

                    for (int i = 0; i < newSelectedAudios.size(); i++) {

                        if (newSelectedAudios.get(i).toString().equals(audio)) {
                            newSelectedAudios.remove(i);
                            break;
                        }
                    }

                    audioAdapter.removeItem(position);

                    if (audioAdapter.getItemCount() == 0) {
                        rvAudio.setVisibility(View.GONE);
                    }
                }
        );

        rvAudio.setLayoutManager(new LinearLayoutManager(this));
        rvAudio.setAdapter(audioAdapter);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));
        rvImages.setAdapter(imageAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnAnh.setOnClickListener(v -> openGallery());
        btnCamera.setOnClickListener(v -> showCameraOptions());
        btnPost.setOnClickListener(v -> savePost());

        loadUserInfo();
        loadPostContent();
    }

    // =========================
    // Load user info
    // =========================
    private void showAudioOptions() {
        String[] options = {"Tự ghi âm", "Chọn file âm thanh"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn nguồn âm thanh")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Ghi âm trực tiếp
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.RECORD_AUDIO)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                            Toast.makeText(this,
                                    "Vui lòng cấp quyền Micro để thu âm!",
                                    Toast.LENGTH_SHORT).show();
                            requestPermissions(
                                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                                    1001);
                            return;
                        }

                        AudioRecordBottomSheet bottomSheet = new AudioRecordBottomSheet(audioUri -> {
                            if (audioUri != null) {
                                newSelectedAudios.add(audioUri);
                                audioAdapter.addItem(audioUri.toString());
                                rvAudio.setVisibility(View.VISIBLE);
                            }
                        });
                        bottomSheet.show(getSupportFragmentManager(), "AudioRecordBottomSheet");

                    } else {
                        // Chọn file từ bộ nhớ
                        openAudioPicker();
                    }
                }).show();
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickAudioLauncher.launch(intent);
    }
    private final ActivityResultLauncher<Intent> pickAudioLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK
                                || result.getData() == null) return;
                        Intent data = result.getData();

                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri uri = clipData.getItemAt(i).getUri();
                                newSelectedAudios.add(uri);
                                audioAdapter.addItem(uri.toString());
                            }
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            newSelectedAudios.add(uri);
                            audioAdapter.addItem(uri.toString());
                        }

                        rvAudio.setVisibility(View.VISIBLE);
                        Toast.makeText(this,
                                "Đã chọn " + newSelectedAudios.size() + " âm thanh",
                                Toast.LENGTH_SHORT).show();
                    });
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
                    List<String> videos =
                            (List<String>) doc.get("danh_sach_video");

                    if (videos != null) {
                        existingVideoUrls.addAll(videos);

                        for (String v : videos) {
                            displayUrls.add(ImageAdapter.VIDEO_PREFIX + v);
                            imageAdapter.notifyDataSetChanged();
                        }
                    }
                    List<String> audios =
                            (List<String>) doc.get("danh_sach_audio");

                    if (audios != null) {

                        existingAudioUrls.addAll(audios);

                        List<Uri> temp = new ArrayList<>();

                        for (String url : audios) {
                            temp.add(Uri.parse(url));
                        }

                        audioAdapter.updateList(audios);

                        rvAudio.setVisibility(
                                temp.isEmpty()
                                        ? View.GONE
                                        : View.VISIBLE
                        );
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
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "image/*",
                        "video/*"
                });
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
                                    String type = getContentResolver().getType(uri);

                                    if (type != null && type.startsWith("video")) {

                                        newSelectedVideos.add(uri);

                                        displayUrls.add(
                                                ImageAdapter.VIDEO_PREFIX +
                                                        uri.toString()
                                        );

                                    } else {

                                        newSelectedImages.add(uri);

                                        displayUrls.add(uri.toString());
                                    }
                                }
                            }else if (data.getData() != null) {

                                Uri uri = data.getData();
                                String type = getContentResolver().getType(uri);

                                if (type != null && type.startsWith("video")) {

                                    newSelectedVideos.add(uri);

                                    displayUrls.add(
                                            ImageAdapter.VIDEO_PREFIX +
                                                    uri.toString()
                                    );

                                } else {

                                    newSelectedImages.add(uri);
                                    displayUrls.add(uri.toString());
                                }
                            }
                            imageAdapter.notifyDataSetChanged();
                        }
                    });

    // =========================
    // Chụp ảnh từ camera
    // =========================
    private void uploadNewVideos(
            int index,
            String noiDung,
            List<String> finalImageUrls,
            ProgressDialog dialog
    ) {

        if (index >= newSelectedVideos.size()) {
            List<String> finalVideoUrls = new ArrayList<>(existingVideoUrls);
            finalVideoUrls.addAll(newUploadedVideoUrls);

            // ✅ Gọi upload audio tiếp theo, không gọi updateFirestore ngay
            newUploadedAudioUrls.clear();
            uploadNewAudios(0, noiDung, finalImageUrls, finalVideoUrls, dialog);
            return;
        }

        Uri uri = newSelectedVideos.get(index);

        imageRepository.uploadVideo(
                this,
                uri,
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        newUploadedVideoUrls.add(url);
                        uploadNewVideos(
                                index + 1,
                                noiDung,
                                finalImageUrls,
                                dialog
                        );
                    }

                    @Override
                    public void onError() {
                        dialog.dismiss();
                    }
                }
        );
    }
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

    private void showCameraOptions() {
        String[] options = {"Chụp ảnh", "Quay video"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn chức năng")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openVideoCamera();
                }).show();
    }

    private void openCamera() {
        File file = new File(getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
        cameraLauncher.launch(cameraImageUri);
    }

    private void openVideoCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 60);
        intent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1);
        videoCameraLauncher.launch(intent);
    }
    private final ActivityResultLauncher<Intent> videoCameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            Uri videoUri = result.getData().getData();
                            newSelectedVideos.add(videoUri);
                            displayUrls.add(ImageAdapter.VIDEO_PREFIX + videoUri.toString());
                            imageAdapter.notifyDataSetChanged();
                        }
                    });

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
        newUploadedVideoUrls.clear();
        newUploadedAudioUrls.clear();

//        if (newSelectedImages.isEmpty() && newSelectedVideos.isEmpty()) {
//
//            updateFirestore(
//                    noiDung,
//                    existingImageUrls,
//                    existingVideoUrls,
//                    existingAudioUrls,
//                    dialog
//            );
//
//        } else {
//
//            uploadNewImages(0, noiDung, dialog);
//        }
        if (newSelectedImages.isEmpty() && newSelectedVideos.isEmpty()) {
            // ✅ Vẫn cần upload audio mới nếu có
            newUploadedAudioUrls.clear();
            uploadNewAudios(0, noiDung, existingImageUrls, existingVideoUrls, dialog);
        } else {
            uploadNewImages(0, noiDung, dialog);
        }
    }

    private void uploadNewImages(int index, String noiDung, ProgressDialog dialog) {
        if (index >= newSelectedImages.size()) {
//            List<String> finalUrls = new ArrayList<>(existingImageUrls);
//            finalUrls.addAll(newUploadedUrls);
//            updateFirestore(noiDung, finalUrls, dialog);
            List<String> finalImageUrls =
                    new ArrayList<>(existingImageUrls);

            finalImageUrls.addAll(newUploadedUrls);

            uploadNewVideos(
                    0,
                    noiDung,
                    finalImageUrls,
                    dialog
            );
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
    private void uploadNewAudios(
            int index,
            String noiDung,
            List<String> finalImageUrls,
            List<String> finalVideoUrls,
            ProgressDialog dialog
    ) {
        if (index >= newSelectedAudios.size()) {
            // Ghép audio cũ + audio mới upload xong
            List<String> finalAudioUrls = new ArrayList<>(existingAudioUrls);
            finalAudioUrls.addAll(newUploadedAudioUrls);
            updateFirestore(noiDung, finalImageUrls, finalVideoUrls, finalAudioUrls, dialog);
            return;
        }

        Uri uri = newSelectedAudios.get(index);
        imageRepository.uploadAudio(this, uri, new ImageRepository.UploadCallback() {
            @Override
            public void onSuccess(String url) {
                newUploadedAudioUrls.add(url);
                uploadNewAudios(index + 1, noiDung, finalImageUrls, finalVideoUrls, dialog);
            }
            @Override
            public void onError() {
                dialog.dismiss();
                btnPost.setEnabled(true);
                btnPost.setText("Lưu");
                Toast.makeText(EditPostActivity.this, "Upload audio thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestore(
            String noiDung,
            List<String> finalImageUrls,
            List<String> finalVideoUrls,
            List<String> finalAudioUrls,  // ✅ Thêm param này
            ProgressDialog dialog) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("noi_dung", noiDung);
        updates.put("danh_sach_anh", finalImageUrls);
        updates.put("danh_sach_video", finalVideoUrls);
        updates.put("danh_sach_audio", finalAudioUrls); // ✅ Thêm dòng này
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