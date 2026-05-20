package com.example.doanmxh.ProfilePage;

<<<<<<< HEAD
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
=======
import android.content.Intent;
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmxh.Log_Res.LoginActivity;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

<<<<<<< HEAD
    private TextView txtName, txtUsername,
            txtTitle, txtSoNguoiTheoDoi;

    private ShapeableImageView imgProfile;

    private ImageView btnLanguage, btnMenu;

    private Button editProfile, shareProfile;

    private FirebaseAuth auth;

    private FirebaseFirestore db;

    private LinearLayout layoutFollowingAvatars;

    private String currentAvatarUrl = "";

=======
    private TextView txtName, txtUsername, txtTitle,txtSoNguoiTheoDoi;
    private ImageView imgProfile;

    private ImageView btnLanguage, btnMenu;
    private Button editProfile, shareProfile;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout layoutFollowingAvatars;

>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

<<<<<<< HEAD
        View view = inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );

        anhXa(view);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

=======
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // init view
        txtName = view.findViewById(R.id.txtName);
        txtUsername = view.findViewById(R.id.txtUsername);
        imgProfile = view.findViewById(R.id.imgProfile);
        editProfile = view.findViewById(R.id.btnEditProfile);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtSoNguoiTheoDoi = view.findViewById(R.id.txtSoNguoiTheoDoi);
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        editProfile.setOnClickListener(v -> {

            Intent intent =
                    new Intent(getActivity(),
                            EditProfileActivity.class);

            startActivity(intent);
<<<<<<< HEAD
        });

        btnMenu.setOnClickListener(v -> logout());

        // Click avatar profile
        imgProfile.setOnClickListener(v -> {

            if (currentAvatarUrl != null
                    && !currentAvatarUrl.isEmpty()) {

                showFullAvatar(currentAvatarUrl);
            }
        });
=======
        });        editProfile = view.findViewById(R.id.btnEditProfile);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnMenu = view.findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> logout());
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        layoutFollowingAvatars = view.findViewById(R.id.layoutFollowingAvatars);
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900

        loadUserInfo();

        return view;
    }

<<<<<<< HEAD
    private void anhXa(View view) {

        txtName = view.findViewById(R.id.txtName);

        txtUsername = view.findViewById(R.id.txtUsername);

        txtTitle = view.findViewById(R.id.txtTitle);

        txtSoNguoiTheoDoi =
                view.findViewById(R.id.txtSoNguoiTheoDoi);

        imgProfile = view.findViewById(R.id.imgProfile);

        editProfile = view.findViewById(R.id.btnEditProfile);

        btnLanguage = view.findViewById(R.id.btnLanguage);

        btnMenu = view.findViewById(R.id.btnMenu);

        layoutFollowingAvatars =
                view.findViewById(R.id.layoutFollowingAvatars);
    }

    private void loadUserInfo() {

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    getContext(),
                    "Chưa đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();

=======
    private void loadUserInfo() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("nguoi_dung")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) return;

<<<<<<< HEAD
                    String name =
                            documentSnapshot.getString("ho_va_ten");

                    String username =
                            documentSnapshot.getString("ten_dang_nhap");

                    String avatar =
                            documentSnapshot.getString("anh_dai_dien");

                    String title =
                            documentSnapshot.getString("tieu_su");

                    Long theodoi =
                            documentSnapshot.getLong("so_nguoi_theo_doi");

                    txtName.setText(
                            name != null ? name : "No Name"
                    );

                    txtUsername.setText(
                            username != null ? username : "user"
                    );

                    txtTitle.setText(
                            title != null ? title : "No Title"
                    );

                    txtSoNguoiTheoDoi.setText(
                            (theodoi != null ? theodoi : 0)
                                    + " người theo dõi"
                    );

                    currentAvatarUrl = avatar;

                    Glide.with(requireContext())
                            .load(avatar)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .error(R.drawable.ic_placeholder_avatar)
                            .circleCrop()
                            .into(imgProfile);

                    loadFollowingAvatars(uid);
                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                getContext(),
                                "Lỗi tải dữ liệu user",
                                Toast.LENGTH_SHORT
                        ).show()
=======
                    String name     = documentSnapshot.getString("ho_va_ten");
                    String username = documentSnapshot.getString("ten_dang_nhap");
                    String avatar   = documentSnapshot.getString("anh_dai_dien");
                    String title    = documentSnapshot.getString("tieu_su");
                    Long theodoi    = documentSnapshot.getLong("so_nguoi_theo_doi");

                    txtName.setText(name != null ? name : "No Name");
                    txtUsername.setText(username != null ? username : "user");

                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(requireContext())
                                .load(avatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .into(imgProfile);
                    }

                    txtTitle.setText(title != null ? title : "No Title");
                    txtSoNguoiTheoDoi.setText(
                            (theodoi != null ? theodoi : 0) + " người theo dõi");

                    // ── Load avatar những người mình đang follow ──
                    loadFollowingAvatars(uid);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu user",
                                Toast.LENGTH_SHORT).show()
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
                );
    }

    private void loadFollowingAvatars(String uid) {
<<<<<<< HEAD

        db.collection("nguoi_dung")
                .document(uid)
                .collection("nguoi_theo_doi")
                .limit(5)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) return;

                    List<String> followingUids =
                            new ArrayList<>();

                    for (var doc :
                            querySnapshot.getDocuments()) {

                        String followingUid =
                                doc.getString("nguoi_dung_id");

                        if (followingUid != null) {

                            followingUids.add(followingUid);
                        }
                    }

=======
        db.collection("nguoi_dung")
                .document(uid)
                .collection("nguoi_theo_doi")
                .limit(5) // Chỉ lấy tối đa 5 avatar hiển thị
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;

                    // Lấy danh sách uid của những người đang follow
                    List<String> followingUids = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        String followingUid = doc.getString("nguoi_dung_id");
                        if (followingUid != null) followingUids.add(followingUid);
                    }

                    // Fetch avatar từng người rồi load vào layoutFollowingAvatars
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
                    loadAvatarsIntoLayout(followingUids);
                });
    }

    private void loadAvatarsIntoLayout(List<String> uids) {

        layoutFollowingAvatars.removeAllViews();

        int sizeDp = 28;
<<<<<<< HEAD

        int overlapDp = -8;

        int sizePx =
                (int) (sizeDp
                        * getResources()
                        .getDisplayMetrics().density);

        int overlapPx =
                (int) (overlapDp
                        * getResources()
                        .getDisplayMetrics().density);

=======
        int overlapDp = -8;

        int sizePx =
                (int) (sizeDp * getResources().getDisplayMetrics().density);

        int overlapPx =
                (int) (overlapDp * getResources().getDisplayMetrics().density);

        // Chỉ hiện tối đa 3 avatar
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        int maxShow = Math.min(uids.size(), 3);

        for (int i = 0; i < maxShow; i++) {

            String targetUid = uids.get(i);

            ShapeableImageView ivAvatar =
                    new ShapeableImageView(requireContext());

<<<<<<< HEAD
=======
            // Bo tròn
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            ivAvatar.setShapeAppearanceModel(
                    ivAvatar.getShapeAppearanceModel()
                            .toBuilder()
                            .setAllCornerSizes(sizePx / 2f)
                            .build()
            );

<<<<<<< HEAD
=======
            // Border trắng
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            ivAvatar.setStrokeWidth(2f);

            ivAvatar.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(
<<<<<<< HEAD
                            Color.WHITE
=======
                            android.graphics.Color.WHITE
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
                    )
            );

            LinearLayout.LayoutParams params =
<<<<<<< HEAD
                    new LinearLayout.LayoutParams(
                            sizePx,
                            sizePx
                    );

            if (i > 0) {

=======
                    new LinearLayout.LayoutParams(sizePx, sizePx);

            if (i > 0) {
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
                params.setMarginStart(overlapPx);
            }

            ivAvatar.setLayoutParams(params);

<<<<<<< HEAD
=======
            // Load avatar
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            db.collection("nguoi_dung")
                    .document(targetUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {

                        String avatarUrl =
                                userDoc.getString("anh_dai_dien");

                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .error(R.drawable.ic_placeholder_avatar)
                                .circleCrop()
                                .into(ivAvatar);
<<<<<<< HEAD

                        // Click avatar
                        ivAvatar.setOnClickListener(v -> {

                            if (avatarUrl != null
                                    && !avatarUrl.isEmpty()) {

                                showFullAvatar(avatarUrl);
                            }
                        });
=======
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
                    });

            layoutFollowingAvatars.addView(ivAvatar);
        }

<<<<<<< HEAD
        // Nếu > 3 người
        if (uids.size() > 3) {

            ImageView ivMore =
                    new ImageView(requireContext());

            LinearLayout.LayoutParams moreParams =
                    new LinearLayout.LayoutParams(
                            sizePx,
                            sizePx
                    );
=======
        // Nếu > 3 người → hiện icon ...
        if (uids.size() > 3) {

            ImageView ivMore = new ImageView(requireContext());

            LinearLayout.LayoutParams moreParams =
                    new LinearLayout.LayoutParams(sizePx, sizePx);
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900

            moreParams.setMarginStart(overlapPx);

            ivMore.setLayoutParams(moreParams);

<<<<<<< HEAD
            ivMore.setImageResource(
                    R.drawable.ic_more_horiz_24
            );

            ivMore.setBackgroundResource(
                    R.drawable.bg_circle_dark
            );
=======
            ivMore.setImageResource(R.drawable.ic_more_horiz_24);

            ivMore.setBackgroundResource(R.drawable.bg_circle_dark);
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900

            ivMore.setPadding(8, 8, 8, 8);

            ivMore.setOnClickListener(v -> {

                Toast.makeText(
                        requireContext(),
                        "Danh sách người theo dõi",
                        Toast.LENGTH_SHORT
                ).show();
<<<<<<< HEAD
=======

                // TODO:
                // mở Activity / BottomSheet danh sách following
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            });

            layoutFollowingAvatars.addView(ivMore);
        }
    }
<<<<<<< HEAD

    private void showFullAvatar(String imageUrl) {

        Dialog dialog =
                new Dialog(
                        requireContext(),
                        android.R.style.Theme_Black_NoTitleBar_Fullscreen
                );

        ImageView imageView =
                new ImageView(requireContext());

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

        Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_avatar)
                .error(R.drawable.ic_placeholder_avatar)
                .into(imageView);

        imageView.setOnClickListener(v ->
                dialog.dismiss());

        dialog.setContentView(imageView);

        dialog.show();
    }

    @Override
    public void onResume() {

=======
    @Override
    public void onResume() {
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        super.onResume();

        loadUserInfo();
    }
<<<<<<< HEAD

    private void logout() {

        auth.signOut();

        Toast.makeText(
                getContext(),
                "Đăng xuất thành công",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent =
                new Intent(getActivity(),
                        LoginActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

=======
    private void logout() {

        // 1. Đăng xuất Firebase Auth
        auth.signOut();

        // 2. Thông báo
        Toast.makeText(getContext(),
                "Đăng xuất thành công",
                Toast.LENGTH_SHORT).show();

        // 3. Chuyển về Login và xóa toàn bộ back stack
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 4. Kết thúc Activity hiện tại
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        requireActivity().finish();
    }
}