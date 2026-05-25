package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
import com.example.doanmxh.HomePage.CommentAdapter;
import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.HomePage.PostAdapter;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.Log_Res.LoginActivity;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    private List<CommentModel> myCommentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private String myUid;
    private boolean isLoaded = false; // ← chống load lại
    private LinearLayout btnPost, btnComment, btnRepost;

    private View linePost, lineComment, lineRepost;

    private TextView txtTabPost, txtTabComment, txtTabRepost;
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
        enableImmersiveMode();
        anhXa(view);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editProfile.setOnClickListener(v -> {

            Log.d("EDIT_PROFILE_CLICK", "Đã nhấn nút Edit Profile");

            if (getActivity() == null) {
                Log.e("EDIT_PROFILE_CLICK", "Activity NULL");
                return;
            }

            Intent intent = new Intent(getActivity(), EditProfileActivity.class);

            Log.d("EDIT_PROFILE_CLICK", "Chuẩn bị mở EditProfileActivity");

            startActivity(intent);

        });
        shareProfile.setOnClickListener(v -> {

            ShareQrBottomSheet sheet =
                    new ShareQrBottomSheet(
                            txtUsername.getText().toString()
                    );

            sheet.show(
                    getParentFragmentManager(),
                    "ShareQrBottomSheet"
            );
        });
        btnPost.setOnClickListener(v -> {

            setActiveTab(0);

            rvMyThreads.setAdapter(postAdapter);

            if (auth.getCurrentUser() != null) {
                loadMyThreads(auth.getCurrentUser().getUid());
            }
        });

        btnRepost.setOnClickListener(v -> {

            setActiveTab(1);

            rvMyThreads.setAdapter(postAdapter);

            if (auth.getCurrentUser() != null) {
                loadRepostThreads(auth.getCurrentUser().getUid());
            }
        });

        btnComment.setOnClickListener(v -> {

            setActiveTab(2);

            rvMyThreads.setAdapter(commentAdapter);

            if (auth.getCurrentUser() != null) {

                loadMyComments(
                        auth.getCurrentUser().getUid()
                );
            }
        });
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
        shareProfile = view.findViewById(R.id.btnShareProfile);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnMenu = view.findViewById(R.id.btnMenu);
        layoutFollowingAvatars = view.findViewById(R.id.layoutFollowingAvatars);

        btnPost = view.findViewById(R.id.btnPost);
        btnComment = view.findViewById(R.id.btnComment);
        btnRepost = view.findViewById(R.id.btnRepost);

        linePost = view.findViewById(R.id.linePost);
        lineComment = view.findViewById(R.id.lineComment);
        lineRepost = view.findViewById(R.id.lineRepost);

        txtTabPost = view.findViewById(R.id.txtTabPost);
        txtTabComment = view.findViewById(R.id.txtTabComment);
        txtTabRepost = view.findViewById(R.id.txtTabRepost);

        rvMyThreads = view.findViewById(R.id.rvMyThreads);
        rvMyThreads.setLayoutManager(new LinearLayoutManager(getContext()));
        commentAdapter = new CommentAdapter(
                myCommentList,
                "",
                new CommentAdapter.OnCommentActionListener() {
                    @Override
                    public void onCommentClick(CommentModel comment,
                                               int position) {

                        Intent intent =
                                new Intent(getActivity(),
                                        PostDetailActivity.class);

                        intent.putExtra(
                                PostDetailActivity.EXTRA_POST_ID,
                                comment.getPostId()
                        );

                        startActivity(intent);
                    }
                    @Override
                    public void onLikeClick(CommentModel comment, int position) {

                    }

                    @Override
                    public void onReplyClick(CommentModel comment, int position) {

                    }

                    @Override
                    public void onAvatarClick(CommentModel comment, int position) {

                    }

                    @Override
                    public void onAddFriendClick(CommentModel comment, int position) {

                    }
                    @Override
                    public void  onEditComment(CommentModel comment, int position, String newContent)
                    {

                    }
                    @Override
                    public void onDeleteComment(CommentModel comment, int position) {}

                }
        );
        rvMyThreads.setAdapter(postAdapter);
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
                    Long theodoi = documentSnapshot.getLong("so_nguoi_dang_theo_doi");

                    txtName.setText(name != null ? name : "No Name");
                    txtUsername.setText(username != null ? username : "user");
                    txtTitle.setText(title != null ? title : "No Title");
                    txtSoNguoiTheoDoi.setText(
                            (theodoi != null ? theodoi : 0) + " người theo dõi");

                    txtSoNguoiTheoDoi.setOnClickListener(v -> openFollowSheet(uid));

                    layoutFollowingAvatars.setOnClickListener(v -> openFollowSheet(uid));

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
    private void openFollowSheet(String uid) {

        FollowBottomSheet sheet =
                new FollowBottomSheet(uid, () -> {

                    loadUserInfo();
                     loadMyThreads(uid);

                });

        sheet.show(getParentFragmentManager(), "FollowBottomSheet");
    }
    private void loadMyThreads(String uid) {
        myUid = uid;

        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", uid)
                .whereEqualTo("da_xoa",false)
                .whereEqualTo("is_repost", false)
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
                .collection("nguoi_dang_theo_doi").limit(5).get()
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
                                .placeholder(R.drawable.ic_person_outline_24)
                                .error(R.drawable.ic_person_outline_24)
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
    private void enableImmersiveMode() {
        // Kiểm tra an toàn xem Fragment đã được gắn vào Activity chưa
        if (getActivity() == null) return;

        Window window = getActivity().getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
    private void setActiveTab(int tab) {

        // reset line
        linePost.setBackgroundColor(Color.parseColor("#1C1C1E"));
        lineComment.setBackgroundColor(Color.parseColor("#1C1C1E"));
        lineRepost.setBackgroundColor(Color.parseColor("#1C1C1E"));

        // reset text
        txtTabPost.setTextColor(Color.parseColor("#636366"));
        txtTabComment.setTextColor(Color.parseColor("#636366"));
        txtTabRepost.setTextColor(Color.parseColor("#636366"));

        switch (tab) {

            case 0:

                linePost.setBackgroundColor(Color.WHITE);
                txtTabPost.setTextColor(Color.WHITE);

                break;
            case 1:

                lineRepost.setBackgroundColor(Color.WHITE);
                txtTabRepost.setTextColor(Color.WHITE);

                break;
            case 2:

                lineComment.setBackgroundColor(Color.WHITE);
                txtTabComment.setTextColor(Color.WHITE);

                break;


        }
    }
    private void loadRepostThreads(String uid) {

        Log.d("REPOST", "Bắt đầu load repost của uid = " + uid);

        myPostList.clear();
        postAdapter.notifyDataSetChanged();

        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", uid)
                .whereEqualTo("da_xoa", false)
                .whereEqualTo("is_repost", true)
                .orderBy("ngay_tao",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    Log.d("REPOST",
                            "Số repost tìm thấy = " + querySnapshot.size());

                    List<PostModel> tempList = new ArrayList<>();

                    int[] pending = {querySnapshot.size()};

                    if (pending[0] == 0) {

                        Log.d("REPOST", "Không có repost");

                        postAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (var repostDoc : querySnapshot.getDocuments()) {

                        String parentId =
                                repostDoc.getString("bai_viet_cha_id");

                        Log.d("REPOST",
                                "parentId = " + parentId);

                        if (parentId == null) {

                            pending[0]--;

                            continue;
                        }

                        db.collection("bai_viet")
                                .document(parentId)
                                .get()
                                .addOnSuccessListener(parentDoc -> {

                                    if (!parentDoc.exists()) {

                                        pending[0]--;

                                        return;
                                    }

                                    PostModel post =
                                            parentDoc.toObject(PostModel.class);

                                    if (post == null) {

                                        pending[0]--;

                                        return;
                                    }

                                    post.setDocumentId(parentDoc.getId());

                                    post.setRepost(true);

                                    String postOwnerUid =
                                            parentDoc.getString("nguoi_dung_id");

                                    // load thông tin user
                                    if (postOwnerUid != null &&
                                            !postOwnerUid.isEmpty()) {

                                        db.collection("nguoi_dung")
                                                .document(postOwnerUid)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {

                                                    if (userDoc.exists()) {

                                                        post.setHoVaTen(
                                                                userDoc.getString("ho_va_ten"));

                                                        post.setTenDangNhap(
                                                                userDoc.getString("ten_dang_nhap"));

                                                        post.setAnhDaiDien(
                                                                userDoc.getString("anh_dai_dien"));

                                                        post.setVerified(
                                                                Boolean.TRUE.equals(
                                                                        userDoc.getBoolean("verified")));
                                                    }

                                                    checkLikeAndFinish(
                                                            uid,
                                                            post,
                                                            tempList,
                                                            pending
                                                    );
                                                })
                                                .addOnFailureListener(e -> {

                                                    checkLikeAndFinish(
                                                            uid,
                                                            post,
                                                            tempList,
                                                            pending
                                                    );
                                                });

                                    } else {

                                        checkLikeAndFinish(
                                                uid,
                                                post,
                                                tempList,
                                                pending
                                        );
                                    }
                                })
                                .addOnFailureListener(e -> {

                                    Log.e("REPOST",
                                            "Lỗi load bài gốc: "
                                                    + e.getMessage());

                                    pending[0]--;

                                    if (pending[0] == 0) {

                                        myPostList.clear();
                                        myPostList.addAll(tempList);

                                        postAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {

                    Log.e("REPOST",
                            "Lỗi query repost: "
                                    + e.getMessage(), e);
                });
    }
    private void checkLikeAndFinish(
            String myUid,
            PostModel post,
            List<PostModel> tempList,
            int[] pending
    ) {

        db.collection("bai_viet")
                .document(post.getDocumentId())
                .collection("luot_thich")
                .document(myUid)
                .get()
                .addOnSuccessListener(likeDoc -> {

                    post.setLikedByMe(likeDoc.exists());

                    Log.d("REPOST",
                            "likedByMe = " + likeDoc.exists());

                    tempList.add(post);

                    pending[0]--;

                    Log.d("REPOST",
                            "pending = " + pending[0]);

                    if (pending[0] == 0) {

                        myPostList.clear();
                        myPostList.addAll(tempList);

                        Log.d("REPOST",
                                "Load repost hoàn tất: "
                                        + myPostList.size());

                        postAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {

                    post.setLikedByMe(false);

                    tempList.add(post);

                    pending[0]--;

                    if (pending[0] == 0) {

                        myPostList.clear();
                        myPostList.addAll(tempList);

                        postAdapter.notifyDataSetChanged();
                    }
                });
    }
    private void loadMyComments(String uid) {

        myCommentList.clear();

        db.collectionGroup("binh_luan")
                .whereEqualTo("nguoi_dung_id", uid)
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<CommentModel> tempList = new ArrayList<>();

                    int[] pending = {querySnapshot.size()};

                    if (pending[0] == 0) {

                        commentAdapter.notifyDataSetChanged();

                        return;
                    }

                    for (var doc : querySnapshot.getDocuments()) {

                        CommentModel comment =
                                doc.toObject(CommentModel.class);

                        if (comment == null) {

                            pending[0]--;

                            continue;
                        }

                        comment.setDocumentId(doc.getId());

                        String commentOwnerUid =
                                comment.getNguoiDungId();

                        // lấy commentId + postId
                        String postId = null;

                        if (doc.getReference().getParent() != null
                                && doc.getReference()
                                .getParent()
                                .getParent() != null) {

                            postId = doc.getReference()
                                    .getParent()
                                    .getParent()
                                    .getId();
                        }

                        String finalPostId = postId;
                        comment.setPostId(finalPostId);
                        Runnable checkLike = () -> {

                            if (finalPostId == null) {

                                tempList.add(comment);

                                pending[0]--;

                                if (pending[0] == 0) {

                                    myCommentList.clear();
                                    myCommentList.addAll(tempList);

                                    commentAdapter.notifyDataSetChanged();
                                }

                                return;
                            }

                            db.collection("bai_viet")
                                    .document(finalPostId)
                                    .collection("binh_luan")
                                    .document(comment.getDocumentId())
                                    .collection("luot_thich")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(likeDoc -> {

                                        comment.setLikedByMe(
                                                likeDoc.exists()
                                        );

                                        tempList.add(comment);

                                        pending[0]--;

                                        if (pending[0] == 0) {

                                            myCommentList.clear();
                                            myCommentList.addAll(tempList);

                                            commentAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> {

                                        comment.setLikedByMe(false);

                                        tempList.add(comment);

                                        pending[0]--;

                                        if (pending[0] == 0) {

                                            myCommentList.clear();
                                            myCommentList.addAll(tempList);

                                            commentAdapter.notifyDataSetChanged();
                                        }
                                    });
                        };

                        // load user info
                        if (commentOwnerUid != null &&
                                !commentOwnerUid.isEmpty()) {

                            db.collection("nguoi_dung")
                                    .document(commentOwnerUid)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {

                                        if (userDoc.exists()) {

                                            comment.setHoVaTen(
                                                    userDoc.getString("ho_va_ten"));

                                            comment.setTenDangNhap(
                                                    userDoc.getString("ten_dang_nhap"));

                                            comment.setAnhDaiDien(
                                                    userDoc.getString("anh_dai_dien"));

                                            comment.setVerified(
                                                    Boolean.TRUE.equals(
                                                            userDoc.getBoolean("verified")));
                                        }

                                        checkLike.run();
                                    })
                                    .addOnFailureListener(e -> {

                                        checkLike.run();
                                    });

                        } else {

                            checkLike.run();
                        }
                    }
                });
    }
}