package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
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

    private TextView txtName, txtUsername,
            txtTitle, txtSoNguoiTheoDoi;

    private ShapeableImageView imgProfile;

    private ImageView btnLanguage, btnMenu;

    private Button editProfile, shareProfile;

    private FirebaseAuth auth;

    private FirebaseFirestore db;

    private LinearLayout layoutFollowingAvatars;

    private String currentAvatarUrl = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );

        anhXa(view);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        editProfile.setOnClickListener(v -> {

            Intent intent =
                    new Intent(getActivity(),
                            EditProfileActivity.class);

            startActivity(intent);
        });

        btnMenu.setOnClickListener(v -> logout());

        // Click avatar profile
        imgProfile.setOnClickListener(v -> {

            if (currentAvatarUrl != null
                    && !currentAvatarUrl.isEmpty()) {

                showFullAvatar(currentAvatarUrl);
            }
        });

        loadUserInfo();

        return view;
    }

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

            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("nguoi_dung")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) return;

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
                );
    }

    private void loadFollowingAvatars(String uid) {

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

                    loadAvatarsIntoLayout(followingUids);
                });
    }

    private void loadAvatarsIntoLayout(List<String> uids) {

        layoutFollowingAvatars.removeAllViews();

        int sizeDp = 28;

        int overlapDp = -8;

        int sizePx =
                (int) (sizeDp
                        * getResources()
                        .getDisplayMetrics().density);

        int overlapPx =
                (int) (overlapDp
                        * getResources()
                        .getDisplayMetrics().density);

        int maxShow = Math.min(uids.size(), 3);

        for (int i = 0; i < maxShow; i++) {

            String targetUid = uids.get(i);

            ShapeableImageView ivAvatar =
                    new ShapeableImageView(requireContext());

            ivAvatar.setShapeAppearanceModel(
                    ivAvatar.getShapeAppearanceModel()
                            .toBuilder()
                            .setAllCornerSizes(sizePx / 2f)
                            .build()
            );

            ivAvatar.setStrokeWidth(2f);

            ivAvatar.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            sizePx,
                            sizePx
                    );

            if (i > 0) {

                params.setMarginStart(overlapPx);
            }

            ivAvatar.setLayoutParams(params);

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

                        // Click avatar
                        ivAvatar.setOnClickListener(v -> {

                            if (avatarUrl != null
                                    && !avatarUrl.isEmpty()) {

                                showFullAvatar(avatarUrl);
                            }
                        });
                    });

            layoutFollowingAvatars.addView(ivAvatar);
        }

        // Nếu > 3 người
        if (uids.size() > 3) {

            ImageView ivMore =
                    new ImageView(requireContext());

            LinearLayout.LayoutParams moreParams =
                    new LinearLayout.LayoutParams(
                            sizePx,
                            sizePx
                    );

            moreParams.setMarginStart(overlapPx);

            ivMore.setLayoutParams(moreParams);

            ivMore.setImageResource(
                    R.drawable.ic_more_horiz_24
            );

            ivMore.setBackgroundResource(
                    R.drawable.bg_circle_dark
            );

            ivMore.setPadding(8, 8, 8, 8);

            ivMore.setOnClickListener(v -> {

                Toast.makeText(
                        requireContext(),
                        "Danh sách người theo dõi",
                        Toast.LENGTH_SHORT
                ).show();
            });

            layoutFollowingAvatars.addView(ivMore);
        }
    }

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

        super.onResume();

        loadUserInfo();
    }

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

        requireActivity().finish();
    }
}