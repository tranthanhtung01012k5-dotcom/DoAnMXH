package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.HomePage.PostAdapter;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.Log_Res.LoginActivity;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private String myUid;
    private boolean isLoaded = false; // ← chống load lại

    RecyclerView rvMyThreads;
    private PostAdapter postAdapter;
    private List<PostModel> myPostList = new ArrayList<>();

    private TextView txtName, txtUsername, txtTitle, txtSoNguoiTheoDoi;
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

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        anhXa(view);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editProfile.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), EditProfileActivity.class))
        );

        btnMenu.setOnClickListener(v -> logout());

        imgProfile.setOnClickListener(v -> {
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty())
                showFullAvatar(currentAvatarUrl);
        });

        loadUserInfo();

        return view;
    }

    private void anhXa(View view) {
        txtName = view.findViewById(R.id.txtName);
        txtUsername = view.findViewById(R.id.txtUsername);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtSoNguoiTheoDoi = view.findViewById(R.id.txtSoNguoiTheoDoi);
        imgProfile = view.findViewById(R.id.imgProfile);
        editProfile = view.findViewById(R.id.btnEditProfile);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnMenu = view.findViewById(R.id.btnMenu);
        layoutFollowingAvatars = view.findViewById(R.id.layoutFollowingAvatars);

        rvMyThreads = view.findViewById(R.id.rvMyThreads);
        rvMyThreads.setLayoutManager(new LinearLayoutManager(getContext()));

        postAdapter = new PostAdapter(myPostList, new PostAdapter.OnPostActionListener() {

            @Override
            public void onLikeClick(PostModel post, int position) {
                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;
                if (currentUid == null) return;

                boolean liked = post.isLikedByMe();
                String postId = post.getDocumentId();

                post.setLikedByMe(!liked);
                post.setSoLuotThich(post.getSoLuotThich() + (liked ? -1 : 1));
                postAdapter.notifyItemChanged(position, "LIKE_UPDATE");

                com.google.firebase.firestore.DocumentReference likeRef =
                        db.collection("bai_viet").document(postId)
                                .collection("luot_thich").document(currentUid);
                com.google.firebase.firestore.DocumentReference postRef =
                        db.collection("bai_viet").document(postId);

                if (liked) {
                    likeRef.delete();
                    postRef.update("so_luot_thich",
                            com.google.firebase.firestore.FieldValue.increment(-1));
                } else {
                    java.util.Map<String, Object> likeData = new java.util.HashMap<>();
                    likeData.put("nguoi_dung_id", currentUid);
                    likeData.put("thoi_gian",
                            com.google.firebase.firestore.FieldValue.serverTimestamp());
                    likeRef.set(likeData);
                    postRef.update("so_luot_thich",
                            com.google.firebase.firestore.FieldValue.increment(1));
                }
            }

            @Override
            public void onCommentClick(PostModel post, int position) {
                // Click comment → mở PostDetail
                openPostDetail(post.getDocumentId());
            }

            @Override
            public void onRepostClick(PostModel post, int position) {}

            @Override
            public void onShareClick(PostModel post, int position) {}

            @Override
            public void onMoreOptionsClick(PostModel post, int position) {
                com.example.doanmxh.HomePage.PostOptionBottomSheet bottomSheet =
                        new com.example.doanmxh.HomePage.PostOptionBottomSheet(
                                post.getDocumentId());
                bottomSheet.show(getParentFragmentManager(), "PostOptionBottomSheet");
            }

            @Override
            public void onAvatarClick(PostModel post, int position) {
                // Click avatar trong post → mở PostDetail
                openPostDetail(post.getDocumentId());
            }

            @Override
            public void onCommentAvataClick(CommentModel comment, int position) {}

            @Override
            public void onAddFriendClick(PostModel post, int position) {}
        });

        rvMyThreads.setAdapter(postAdapter);

        // ← Click vào bất kỳ item nào → mở PostDetail
        rvMyThreads.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {});
        postAdapter.setOnItemClickListener(postId -> openPostDetail(postId));
    }

    private void openPostDetail(String postId) {
        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        startActivity(intent);
    }

    private void loadUserInfo() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    String name = documentSnapshot.getString("ho_va_ten");
                    String username = documentSnapshot.getString("ten_dang_nhap");
                    String avatar = documentSnapshot.getString("anh_dai_dien");
                    String title = documentSnapshot.getString("tieu_su");
                    Long theodoi = documentSnapshot.getLong("so_nguoi_theo_doi");

                    txtName.setText(name != null ? name : "No Name");
                    txtUsername.setText(username != null ? username : "user");
                    txtTitle.setText(title != null ? title : "No Title");
                    txtSoNguoiTheoDoi.setText(
                            (theodoi != null ? theodoi : 0) + " người theo dõi");

                    txtSoNguoiTheoDoi.setOnClickListener(v -> {
                        new FollowBottomSheet(uid)
                                .show(getParentFragmentManager(), "FollowBottomSheet");
                    });
                    layoutFollowingAvatars.setOnClickListener(v -> {
                        new FollowBottomSheet(uid)
                                .show(getParentFragmentManager(), "FollowBottomSheet");
                    });

                    currentAvatarUrl = avatar;
                    Glide.with(requireContext()).load(avatar)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .error(R.drawable.ic_placeholder_avatar)
                            .circleCrop().into(imgProfile);

                    loadFollowingAvatars(uid);

                    // ← Chỉ load posts lần đầu
                    if (!isLoaded) {
                        isLoaded = true;
                        loadMyThreads(uid);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu user",
                                Toast.LENGTH_SHORT).show());
    }

    private void loadMyThreads(String uid) {
        myUid = uid;

        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", uid)
                .orderBy("ngay_tao", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    myPostList.clear();

                    List<PostModel> tempList = new ArrayList<>();
                    int[] pendingCount = {querySnapshot.size()};

                    if (pendingCount[0] == 0) {
                        postAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (var doc : querySnapshot.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        if (post == null) {
                            pendingCount[0]--;
                            continue;
                        }

                        post.setDocumentId(doc.getId());
                        String nguoiDungId = doc.getString("nguoi_dung_id");

                        Runnable checkLike = () -> {
                            if (myUid != null) {
                                db.collection("bai_viet")
                                        .document(post.getDocumentId())
                                        .collection("luot_thich")
                                        .document(myUid).get()
                                        .addOnSuccessListener(likeDoc -> {
                                            post.setLikedByMe(likeDoc.exists());
                                            tempList.add(post);
                                            pendingCount[0]--;
                                            if (pendingCount[0] == 0) {
                                                myPostList.addAll(tempList);
                                                postAdapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            post.setLikedByMe(false);
                                            tempList.add(post);
                                            pendingCount[0]--;
                                            if (pendingCount[0] == 0) {
                                                myPostList.addAll(tempList);
                                                postAdapter.notifyDataSetChanged();
                                            }
                                        });
                            } else {
                                tempList.add(post);
                                pendingCount[0]--;
                                if (pendingCount[0] == 0) {
                                    myPostList.addAll(tempList);
                                    postAdapter.notifyDataSetChanged();
                                }
                            }
                        };

                        if (nguoiDungId != null && !nguoiDungId.isEmpty()) {
                            db.collection("nguoi_dung").document(nguoiDungId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            post.setHoVaTen(userDoc.getString("ho_va_ten"));
                                            post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                            post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                            post.setVerified(Boolean.TRUE.equals(
                                                    userDoc.getBoolean("verified")));
                                        }
                                        checkLike.run();
                                    })
                                    .addOnFailureListener(e -> checkLike.run());
                        } else {
                            checkLike.run();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("THREADS_ERROR", "Lỗi: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadFollowingAvatars(String uid) {
        db.collection("nguoi_dung").document(uid)
                .collection("nguoi_theo_doi").limit(5).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;
                    List<String> followingUids = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        String followingUid = doc.getString("nguoi_dung_id");
                        if (followingUid != null) followingUids.add(followingUid);
                    }
                    loadAvatarsIntoLayout(followingUids);
                });
    }

    private void loadAvatarsIntoLayout(List<String> uids) {
        layoutFollowingAvatars.removeAllViews();

        int sizePx = (int) (28 * getResources().getDisplayMetrics().density);
        int overlapPx = (int) (-8 * getResources().getDisplayMetrics().density);
        int maxShow = Math.min(uids.size(), 3);

        for (int i = 0; i < maxShow; i++) {
            String targetUid = uids.get(i);
            ShapeableImageView ivAvatar = new ShapeableImageView(requireContext());
            ivAvatar.setShapeAppearanceModel(ivAvatar.getShapeAppearanceModel()
                    .toBuilder().setAllCornerSizes(sizePx / 2f).build());
            ivAvatar.setStrokeWidth(2f);
            ivAvatar.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.WHITE));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            if (i > 0) params.setMarginStart(overlapPx);
            ivAvatar.setLayoutParams(params);

            db.collection("nguoi_dung").document(targetUid).get()
                    .addOnSuccessListener(userDoc -> {
                        String avatarUrl = userDoc.getString("anh_dai_dien");
                        Glide.with(requireContext()).load(avatarUrl)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .error(R.drawable.ic_placeholder_avatar)
                                .circleCrop().into(ivAvatar);
                        ivAvatar.setOnClickListener(v -> {
                            if (avatarUrl != null && !avatarUrl.isEmpty())
                                showFullAvatar(avatarUrl);
                        });
                    });
            layoutFollowingAvatars.addView(ivAvatar);
        }

        if (uids.size() > 3) {
            ImageView ivMore = new ImageView(requireContext());
            LinearLayout.LayoutParams moreParams =
                    new LinearLayout.LayoutParams(sizePx, sizePx);
            moreParams.setMarginStart(overlapPx);
            ivMore.setLayoutParams(moreParams);
            ivMore.setImageResource(R.drawable.ic_more_horiz_24);
            ivMore.setBackgroundResource(R.drawable.bg_circle_dark);
            ivMore.setPadding(8, 8, 8, 8);
            ivMore.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Danh sách người theo dõi",
                            Toast.LENGTH_SHORT).show());
            layoutFollowingAvatars.addView(ivMore);
        }
    }

    private void showFullAvatar(String imageUrl) {
        Dialog dialog = new Dialog(requireContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setBackgroundColor(Color.BLACK);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(requireContext()).load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_avatar)
                .error(R.drawable.ic_placeholder_avatar).into(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(imageView);
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Chỉ reload profile info, KHÔNG reload posts
        loadUserInfo();
    }

    private void logout() {
        auth.signOut();
        Toast.makeText(getContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}