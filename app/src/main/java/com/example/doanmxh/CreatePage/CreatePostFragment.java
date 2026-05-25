package com.example.doanmxh.CreatePage;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.HomePage.ImageAdapter;
import com.example.doanmxh.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePostFragment extends Fragment {

    private Uri cameraImageUri;
    private EditText edtNoiDung;
    private MaterialButton btnDang;
    private ImageButton btnAnh;
    private RecyclerView recyclerAnh;

    private TextView txtUsername;
    private ShapeableImageView imgAvatar;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    private final List<Uri> selectedImages = new ArrayList<>();
    private final List<String> displayUris = new ArrayList<>();
    private final List<String> imageUrls = new ArrayList<>();
    private ImageAdapter adapter;

    private static final String TAG = "UPLOAD_DEBUG";
    private final ImageRepository imageRepository = new ImageRepository();

    public CreatePostFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create, container, false);

        edtNoiDung  = view.findViewById(R.id.edtNoiDung);
        btnDang     = view.findViewById(R.id.btnPost);
        btnAnh      = view.findViewById(R.id.btnAnh);
        recyclerAnh = view.findViewById(R.id.rvImages);
        txtUsername = view.findViewById(R.id.txtUsername);
        imgAvatar   = view.findViewById(R.id.imgAvatar);

        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth    = FirebaseAuth.getInstance();

        loadUserInfo();

        // ✅ Dùng ImageAdapter có nút X
        adapter = new ImageAdapter(displayUris, position -> {
            // displayUris đã bị xóa bên trong ImageAdapter
            // Xóa Uri tương ứng trong selectedImages
            if (position < selectedImages.size()) {
                selectedImages.remove(position);
            }
        });
        recyclerAnh.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerAnh.setAdapter(adapter);

        btnAnh.setOnClickListener(v -> openGallery());

        ImageButton btnCamera = view.findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(v -> openCamera());

        btnDang.setOnClickListener(v -> dangBai());

        return view;
    }

    // =========================
    // Load thông tin user
    // =========================

    private void loadUserInfo() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String hoTen = doc.getString("ho_va_ten");
                    String avatar = doc.getString("anh_dai_dien");

                    txtUsername.setText(hoTen != null ? hoTen : "Người dùng");

                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(requireContext())
                                .load(avatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .into(imgAvatar);
                    }
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
                        if (result.getResultCode() == requireActivity().RESULT_OK
                                && result.getData() != null) {

                            Intent data = result.getData();

                            if (data.getClipData() != null) {
                                ClipData clipData = data.getClipData();
                                for (int i = 0; i < clipData.getItemCount(); i++) {
                                    Uri uri = clipData.getItemAt(i).getUri();
                                    selectedImages.add(uri);
                                    displayUris.add(uri.toString()); // ✅
                                }
                            } else if (data.getData() != null) {
                                selectedImages.add(data.getData());
                                displayUris.add(data.getData().toString()); // ✅
                            }

                            adapter.notifyDataSetChanged();
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
                            selectedImages.add(cameraImageUri);
                            displayUris.add(cameraImageUri.toString()); // ✅
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Chụp ảnh thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });

    private void openCamera() {
        File file = new File(
                requireContext().getCacheDir(),
                "camera_" + System.currentTimeMillis() + ".jpg"
        );
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );
        cameraLauncher.launch(cameraImageUri);
    }

    // =========================
    // Đăng bài
    // =========================

    private void dangBai() {
        String noiDung = edtNoiDung.getText().toString().trim();

        if (TextUtils.isEmpty(noiDung) && displayUris.isEmpty()) { // ✅ check displayUris
            Toast.makeText(requireContext(), "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Đang đăng bài...");
        dialog.setCancelable(false);
        dialog.show();

        imageUrls.clear();

        if (selectedImages.isEmpty()) {
            savePost(noiDung, dialog);
        } else {
            uploadImages(0, noiDung, dialog);
        }
    }

    // =========================
    // Upload ảnh
    // =========================

    private void uploadImages(int index, String noiDung, ProgressDialog dialog) {
        Log.d(TAG, "Bắt đầu upload index = " + index);

        if (index >= selectedImages.size()) {
            Log.d(TAG, "Upload xong tất cả ảnh. Gọi savePost()");
            savePost(noiDung, dialog);
            return;
        }

        Uri uri = selectedImages.get(index);
        Log.d(TAG, "Đang upload ảnh thứ " + index + " URI = " + uri);

        imageRepository.uploadImage(requireContext(), uri,
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        Log.d(TAG, "Upload OK index = " + index + " URL = " + url);
                        imageUrls.add(url);
                        uploadImages(index + 1, noiDung, dialog);
                    }

                    @Override
                    public void onError() {
                        Log.e(TAG, "Upload FAIL index = " + index);
                        dialog.dismiss();
                        Toast.makeText(requireContext(),
                                "Upload ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================
    // Lưu Firestore
    // =========================

    private void savePost(String noiDung, ProgressDialog dialog) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> post = new HashMap<>();
        post.put("nguoi_dung_id", uid);
        post.put("noi_dung", noiDung);
        post.put("ngay_tao", Timestamp.now());
        post.put("da_xoa", false);
        post.put("che_do_xem", "public");
        post.put("so_like", 0);
        post.put("so_binh_luan", 0);
        post.put("so_share", 0);
        post.put("danh_sach_anh", imageUrls);
        post.put("bai_viet_cha_id", "");
        post.put("is_repost", false);
        post.put("danh_sach_video", new ArrayList<>());

        db.collection("bai_viet").add(post)
                .addOnSuccessListener(ref -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Đăng bài thành công", Toast.LENGTH_SHORT).show();

                    // ✅ Clear đủ 3 list
                    edtNoiDung.setText("");
                    selectedImages.clear();
                    displayUris.clear();
                    imageUrls.clear();
                    adapter.notifyDataSetChanged();

                    requireActivity().runOnUiThread(() -> {
                        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
                        nav.setSelectedItemId(R.id.homeFragment);
                    });
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Đăng bài thất bại", Toast.LENGTH_SHORT).show();
                });
    }
}