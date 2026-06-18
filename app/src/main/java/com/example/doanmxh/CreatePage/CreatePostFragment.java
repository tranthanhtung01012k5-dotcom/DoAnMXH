package com.example.doanmxh.CreatePage;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.doanmxh.HomePage.ImageAdapter;
import com.example.doanmxh.Mention.MentionHelper;
import com.example.doanmxh.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePostFragment extends Fragment {

    private static final String TAG = "UPLOAD_DEBUG";
    private static final String UPLOAD_PRESET = "DoAnMXH";

    // Prefix để phân biệt các loại dữ liệu
    private static final String VIDEO_PREFIX = "video:";
    private static final String AUDIO_PREFIX = "audio:";

    // Danh sách quản lý Uri
    private final List<Uri> selectedAudioUris = new ArrayList<>();
    private final List<String> audioUrls = new ArrayList<>();
    private final List<String> videoUrls = new ArrayList<>();
    private final List<Uri> selectedImages = new ArrayList<>();
    private final List<String> imageUrls = new ArrayList<>();
    private final List<Uri> selectedVideoUris = new ArrayList<>();
    private final List<String> displayUris = new ArrayList<>(); // Chỉ chứa Ảnh và Video

    private Uri cameraImageUri;

    // Giao diện
    private RecyclerView rvAudio;
    private AudioAdapter audioAdapter;
    private EditText edtNoiDung;
    private MaterialButton btnDang;
    private ImageButton btnAnh;
    private RecyclerView recyclerAnh;
    private TextView txtUsername;
    private ShapeableImageView imgAvatar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    private ImageAdapter adapter;
    private MentionHelper mentionHelper;

    public CreatePostFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create, container, false);

        edtNoiDung = view.findViewById(R.id.edtNoiDung);
        btnDang = view.findViewById(R.id.btnPost);
        btnAnh = view.findViewById(R.id.btnAnh);
        recyclerAnh = view.findViewById(R.id.rvImages);
        txtUsername = view.findViewById(R.id.txtUsername);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        rvAudio = view.findViewById(R.id.rvAudio);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        loadUserInfo();
        mentionHelper = new MentionHelper(requireContext(), edtNoiDung, db, null);

        // --- CẤU HÌNH RECYCLERVIEW ẢNH & VIDEO ---
        adapter = new ImageAdapter(displayUris, position -> {
            String item = displayUris.get(position);
            if (item.startsWith(VIDEO_PREFIX)) {
                String uriStr = item.substring(VIDEO_PREFIX.length());
                selectedVideoUris.removeIf(u -> u.toString().equals(uriStr));
            } else {
                int imgIndex = 0;
                for (int i = 0; i < position; i++) {
                    if (!displayUris.get(i).startsWith(VIDEO_PREFIX)) imgIndex++;
                }
                if (imgIndex < selectedImages.size()) {
                    selectedImages.remove(imgIndex);
                }
            }
        });
        recyclerAnh.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerAnh.setAdapter(adapter);

        // --- CẤU HÌNH RECYCLERVIEW AUDIO ---
        if (rvAudio != null) {
            List<String> audioPathList = new ArrayList<>();
            for (Uri uri : selectedAudioUris) {
                audioPathList.add(uri.toString());
            }
            audioAdapter = new AudioAdapter(
                    audioPathList,
                    position -> {
                        if (position >= 0 && position < selectedAudioUris.size()) {
                            selectedAudioUris.remove(position); // Xóa trong list Uri

                            // Sau khi xóa Uri, ta cần cập nhật lại list String cho Adapter
                            List<String> updatedPaths = new ArrayList<>();
                            for (Uri u : selectedAudioUris) updatedPaths.add(u.toString());
                            audioAdapter.updateList(updatedPaths);
                        }
                    });
            rvAudio.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAudio.setAdapter(audioAdapter);
        }

        // --- XỬ LÝ SỰ KIỆN CÁC NÚT BẤM ---
        btnAnh.setOnClickListener(v -> openGallery());

        ImageButton btnCamera = view.findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(v -> showCameraOptions());

//        ImageButton btnMic = view.findViewById(R.id.btnMicro);
//        btnMic.setOnClickListener(v -> {
//            // Kiểm tra quyền truy cập Micro
//            if (androidx.core.content.ContextCompat.checkSelfPermission(
//                    requireContext(),
//                    android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
//
//                Toast.makeText(requireContext(), "Vui lòng cấp quyền Micro để thu âm!", Toast.LENGTH_SHORT).show();
//                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1001);
//                return;
//            }
//
//            // Mở Bottom Sheet Thu Âm
//            AudioRecordBottomSheet bottomSheet = new AudioRecordBottomSheet(audioUri -> {
//                if (audioUri != null) {
//                    selectedAudioUris.add(audioUri);
//                    if (audioAdapter != null) {
//                        audioAdapter.addItem(audioUri.toString()); // ✅ Dùng addItem có sẵn
//                    }
//                }
//            });
//            bottomSheet.show(getChildFragmentManager(), "AudioRecordBottomSheet");
//        });
        ImageButton btnMic = view.findViewById(R.id.btnMicro);
        btnMic.setOnClickListener(v -> showAudioOptions());

        btnDang.setOnClickListener(v -> dangBai());

        return view;
    }
    private void showAudioOptions() {
        String[] options = {"Tự ghi âm", "Chọn file âm thanh"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Chọn nguồn âm thanh")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Ghi âm trực tiếp
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                requireContext(),
                                android.Manifest.permission.RECORD_AUDIO)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                            Toast.makeText(requireContext(),
                                    "Vui lòng cấp quyền Micro để thu âm!",
                                    Toast.LENGTH_SHORT).show();
                            requestPermissions(
                                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                                    1001);
                            return;
                        }

                        AudioRecordBottomSheet bottomSheet = new AudioRecordBottomSheet(audioUri -> {
                            if (audioUri != null) {
                                selectedAudioUris.add(audioUri);
                                if (audioAdapter != null) {
                                    audioAdapter.addItem(audioUri.toString());
                                }
                            }
                        });
                        bottomSheet.show(getChildFragmentManager(), "AudioRecordBottomSheet");

                    } else {
                        // Chọn file từ bộ nhớ
                        openAudioPicker();
                    }
                }).show();
    }
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickImagesLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != requireActivity().RESULT_OK || result.getData() == null) return;
                        Intent data = result.getData();

                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri uri = clipData.getItemAt(i).getUri();
                                String mimeType = requireContext().getContentResolver().getType(uri);
                                if (mimeType != null && mimeType.startsWith("video")) {
                                    selectedVideoUris.add(uri);
                                    displayUris.add(VIDEO_PREFIX + uri.toString());
                                } else {
                                    selectedImages.add(uri);
                                    displayUris.add(uri.toString());
                                }
                            }
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            String mimeType = requireContext().getContentResolver().getType(uri);
                            if (mimeType != null && mimeType.startsWith("video")) {
                                selectedVideoUris.add(uri);
                                displayUris.add(VIDEO_PREFIX + uri.toString());
                            } else {
                                selectedImages.add(uri);
                                displayUris.add(uri.toString());
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    result -> {
                        if (result && cameraImageUri != null) {
                            selectedImages.add(cameraImageUri);
                            displayUris.add(cameraImageUri.toString());
                            adapter.notifyDataSetChanged();
                        }
                    });

    private final ActivityResultLauncher<Intent> videoCameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            Uri videoUri = result.getData().getData();
                            selectedVideoUris.add(videoUri);
                            displayUris.add(VIDEO_PREFIX + videoUri.toString());
                            adapter.notifyDataSetChanged();
                        }
                    });

    private void openVideoCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 60);
        intent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1);
        videoCameraLauncher.launch(intent);
    }

    private void showCameraOptions() {
        String[] options = {"Chụp ảnh", "Quay video"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Chọn chức năng")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openVideoCamera();
                }).show();
    }

    private void openCamera() {
        File file = new File(requireContext().getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
        cameraLauncher.launch(cameraImageUri);
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
                        if (result.getResultCode() != requireActivity().RESULT_OK
                                || result.getData() == null) return;
                        Intent data = result.getData();

                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri uri = clipData.getItemAt(i).getUri();
                                selectedAudioUris.add(uri);
                                if (audioAdapter != null) {
                                    audioAdapter.addItem(uri.toString()); // ✅ addItem thay vì notify
                                }
                            }
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            selectedAudioUris.add(uri);
                            if (audioAdapter != null) {
                                audioAdapter.addItem(uri.toString()); // ✅
                            }
                        }

                        Toast.makeText(requireContext(),
                                "Đã chọn " + selectedAudioUris.size() + " âm thanh",
                                Toast.LENGTH_SHORT).show();
                    });
    // --- XỬ LÝ ĐĂNG BÀI CHUỖI TỰ ĐỘNG (TUẦN TỰ) ---
    private void dangBai() {
        String noiDung = edtNoiDung.getText().toString().trim();

        if (TextUtils.isEmpty(noiDung) && displayUris.isEmpty() && selectedAudioUris.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập nội dung hoặc chọn phương tiện đăng", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Đang chuẩn bị...");
        dialog.setCancelable(false);
        dialog.show();

        imageUrls.clear();
        videoUrls.clear();
        audioUrls.clear();

        if (!selectedImages.isEmpty()) {
            uploadImages(0, noiDung, dialog);
        } else if (!selectedAudioUris.isEmpty()) {
            uploadAudio(noiDung, dialog);
        } else if (!selectedVideoUris.isEmpty()) {
            uploadVideo(noiDung, dialog);
        } else {
            savePost(noiDung, dialog);
        }
    }

    private void uploadImages(int index, String noiDung, ProgressDialog dialog) {
        if (index >= selectedImages.size()) {
            if (!selectedAudioUris.isEmpty()) {
                uploadAudio(noiDung, dialog);
            } else if (!selectedVideoUris.isEmpty()) {
                uploadVideo(noiDung, dialog);
            } else {
                savePost(noiDung, dialog);
            }
            return;
        }

        Uri uri = selectedImages.get(index);
        dialog.setMessage("Đang upload ảnh " + (index + 1) + "/" + selectedImages.size() + "...");

        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "posts/images")
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        imageUrls.add(url);
                        uploadImages(index + 1, noiDung, dialog);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (dialog.isShowing()) dialog.dismiss();
                        Toast.makeText(requireContext(), "Upload ảnh lỗi: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch(requireContext());
    }

    private void uploadAudio(String noiDung, ProgressDialog dialog) {
        uploadAudioAt(0, noiDung, dialog);
    }

    private void uploadAudioAt(int index, String noiDung, ProgressDialog dialog) {
        if (index >= selectedAudioUris.size()) {
            if (!selectedVideoUris.isEmpty()) {
                uploadVideo(noiDung, dialog);
            } else {
                savePost(noiDung, dialog);
            }
            return;
        }

        Uri uri = selectedAudioUris.get(index);
        dialog.setMessage("Đang upload âm thanh " + (index + 1) + "/" + selectedAudioUris.size());

        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "posts/audio")
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        audioUrls.add(url);
                        uploadAudioAt(index + 1, noiDung, dialog);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (dialog.isShowing()) dialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi tải audio: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch(requireContext());
    }

    private void uploadVideo(String noiDung, ProgressDialog dialog) {
        uploadVideoAt(0, noiDung, dialog);
    }

    private void uploadVideoAt(int index, String noiDung, ProgressDialog dialog) {
        if (index >= selectedVideoUris.size()) {
            savePost(noiDung, dialog);
            return;
        }

        Uri uri = selectedVideoUris.get(index);
        dialog.setMessage("Đang upload video " + (index + 1) + "/" + selectedVideoUris.size() + "...");

        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "posts/videos")
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        videoUrls.add(url);
                        uploadVideoAt(index + 1, noiDung, dialog);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (dialog.isShowing()) dialog.dismiss();
                        Toast.makeText(requireContext(), "Upload video lỗi: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch(requireContext());
    }

    private void savePost(String noiDung, ProgressDialog dialog) {
        dialog.setMessage("Đang lưu bài viết...");
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> post = new HashMap<>();
        post.put("nguoi_dung_id", uid);
        post.put("noi_dung", noiDung);
        post.put("ngay_tao", Timestamp.now());
        post.put("da_xoa", false);
        post.put("che_do_xem", "public");
        post.put("so_like", 0);
        post.put("so_binh_luan", 0);
        post.put("so_repost", 0);
        post.put("so_share", 0);
        post.put("danh_sach_anh", imageUrls);
        post.put("danh_sach_video", videoUrls);
        post.put("danh_sach_audio", audioUrls);
        post.put("bai_viet_cha_id", "");
        post.put("is_repost", false);

        db.collection("bai_viet").add(post)
                .addOnSuccessListener(ref -> {
                    dialog.dismiss();
                    hideKeyboard();
                    saveHashtags(noiDung, ref.getId());

                    Toast.makeText(requireContext(), "Đăng bài thành công", Toast.LENGTH_SHORT).show();
                    String myUid = auth.getCurrentUser().getUid();
                    com.example.doanmxh.Notifications.NotificationsFragment
                            .sendNewPostToFollowers(myUid, ref.getId());
                    sendMentionNotificationsIfAny(noiDung, ref.getId());

                    // Reset giao diện
                    edtNoiDung.setText("");
                    selectedImages.clear();
                    selectedVideoUris.clear();
                    selectedAudioUris.clear();
                    imageUrls.clear();
                    videoUrls.clear();
                    audioUrls.clear();
                    displayUris.clear();

                    adapter.notifyDataSetChanged();
                    if (audioAdapter != null) audioAdapter.notifyDataSetChanged();

                    requireActivity().runOnUiThread(() -> {
                        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
                        if (nav != null) nav.setSelectedItemId(R.id.homeFragment);
                    });
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Đăng bài thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void sendMentionNotificationsIfAny(String content, String postId) {
        String myUid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;
        if (myUid == null || content == null || content.isEmpty()) return;

        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("@([\\p{L}\\p{N}_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String username = matcher.group(1);
            db.collection("nguoi_dung")
                    .whereEqualTo("ten_dang_nhap", username)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            String mentionedUid = query.getDocuments().get(0).getId();
                            com.example.doanmxh.Notifications.NotificationsFragment
                                    .sendMentionNotification(mentionedUid, myUid, postId);
                        }
                    });
        }
    }
    private void saveHashtags(String noiDung, String postId) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\w+");
        java.util.regex.Matcher matcher = pattern.matcher(noiDung);

        while (matcher.find()) {
            String tag = matcher.group().substring(1).toLowerCase();
            DocumentReference tagRef = db.collection("hashtag").document(tag);

            Map<String, Object> tagData = new HashMap<>();
            tagData.put("ten", tag);
            tagData.put("so_bai_viet", FieldValue.increment(1));
            tagData.put("lan_cuoi_su_dung", Timestamp.now());
            tagRef.set(tagData, com.google.firebase.firestore.SetOptions.merge());

            Map<String, Object> postRef = new HashMap<>();
            postRef.put("bai_viet_id", postId);
            postRef.put("ngay_tao", Timestamp.now());
            tagRef.collection("bai_viet").document(postId).set(postRef);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Giải phóng trình phát nhạc nếu đang có
        if (audioAdapter != null) {
            audioAdapter.stopPlayer();
        }

        // Giải phóng mention helper
        if (mentionHelper != null) {
            mentionHelper.dismiss();
        }
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}