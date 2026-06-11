package com.example.doanmxh.ProfilePage;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.HomePage.CommentAdapter;
import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.HomePage.PostAdapter;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.MainActivity;
import com.example.doanmxh.Message.ChatActivity;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserProfileActivity extends BaseActivity {
    private final Set<String> repostedByMe = new HashSet<>();

    private ShapeableImageView imgProfile;
    private TextView txtName, txtUsername, txtBio, txtSoLuongTheoDoi;
    private ImageView btnCancel,btnShare;
    private LinearLayout btnPost, btnRepost, btnComment;
    private View linePost, lineRepost, lineComment;
    private TextView txtTabPost, txtTabRepost, txtTabComment;
    private RecyclerView rvPosts;
    private LinearLayout layoutFollowingAvatars;
    private Button btnFollow,btnMessage;
    private FirebaseFirestore db;
    private String targetUid, myUid, avatarUrl = "";
    private PostAdapter postAdapter;
    private CommentAdapter commentAdapter;
    private List<PostModel> postList = new ArrayList<>();
    private List<CommentModel> commentList = new ArrayList<>();

    // Trạng thái follow hiện tại của nút btnFollow
    private boolean isFollowingTarget = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        enableImmersiveMode();

        db    = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        targetUid = getIntent().getStringExtra("user_uid");
        if (targetUid == null || targetUid.isEmpty()) { finish(); return; }

        // ── Ánh xạ ──────────────────────────────────────────
        imgProfile             = findViewById(R.id.imgProfile);
        txtName                = findViewById(R.id.txtName);
        txtUsername            = findViewById(R.id.txtUsername);
        txtBio                 = findViewById(R.id.txtTitle);
        txtSoLuongTheoDoi      = findViewById(R.id.txtSoNguoiTheoDoi);
        btnCancel              = findViewById(R.id.btn_cancel);
        btnPost                = findViewById(R.id.btnPost);
        btnRepost              = findViewById(R.id.btnRepost);
        btnComment             = findViewById(R.id.btnComment);
        linePost               = findViewById(R.id.linePost);
        lineRepost             = findViewById(R.id.lineRepost);
        lineComment            = findViewById(R.id.lineComment);
        txtTabPost             = findViewById(R.id.txtTabPost);
        txtTabRepost           = findViewById(R.id.txtTabRepost);
        txtTabComment          = findViewById(R.id.txtTabComment);
        rvPosts                = findViewById(R.id.rvMyThreads);
        btnFollow              = findViewById(R.id.btnFollow);
        btnShare               = findViewById(R.id.btnShareProfile);
        btnMessage             = findViewById(R.id.btnMessage);
        layoutFollowingAvatars = findViewById(R.id.layoutFollowingAvatars);

        btnCancel.setOnClickListener(v -> finish());

        // Ẩn btnFollow nếu đang xem profile của chính mình
        if (targetUid.equals(myUid)) {
            btnFollow.setVisibility(View.GONE);
        } else {
            btnFollow.setVisibility(View.VISIBLE);
            checkFollowStatus();
        }

        txtSoLuongTheoDoi.setOnClickListener(v ->
                new FollowBottomSheet(targetUid, () -> {})
                        .show(getSupportFragmentManager(), "FollowBottomSheet"));

        imgProfile.setOnClickListener(v -> {
            if (avatarUrl != null && !avatarUrl.isEmpty()) showFullImage(avatarUrl);
        });

        btnShare.setOnClickListener(v -> {
            String currentUsername = txtUsername.getText().toString();
            ShareQrBottomSheet sheet = new ShareQrBottomSheet(currentUsername);
            sheet.setScanRequestListener(() ->
                    startActivity(new Intent(UserProfileActivity.this, MainActivity.class)));
            sheet.show(getSupportFragmentManager(), "ShareQrBottomSheet");
        });
        btnMessage.setOnClickListener( v -> {
            Intent intent = new Intent(UserProfileActivity.this, ChatActivity.class);
            intent.putExtra("target_uid", targetUid);
            startActivity(intent);
        });

        // ── Adapter bài viết ────────────────────────────────
        postAdapter = new PostAdapter(postList, new PostAdapter.OnPostActionListener() {
            @Override public void onLikeClick(PostModel post, int position)        { handleLike(post, position); }
            @Override public void onCommentClick(PostModel post, int position)     { openPostDetail(post.getDocumentId()); }
            @Override
            public void onRepostClick(PostModel post, int position) {

                if (myUid == null) {
                    Toast.makeText(UserProfileActivity.this,
                            "Vui lòng đăng nhập",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String originalDocId = post.getDocumentId();

                // Đã repost -> cho hủy
                if (repostedByMe.contains(originalDocId)) {

                    new AlertDialog.Builder(UserProfileActivity.this)
                            .setTitle("Hủy đăng lại")
                            .setMessage("Bạn muốn xóa bài đăng lại này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {

                                db.collection("bai_viet")
                                        .whereEqualTo("nguoi_dung_id", myUid)
                                        .whereEqualTo("bai_viet_cha_id", originalDocId)
                                        .whereEqualTo("is_repost", true)
                                        .whereEqualTo("da_xoa", false)
                                        .get()
                                        .addOnSuccessListener(snap -> {

                                            if (snap.isEmpty()) return;

                                            WriteBatch batch = db.batch();

                                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                                batch.update(doc.getReference(),
                                                        "da_xoa",
                                                        true);
                                            }

                                            DocumentReference baiGocRef =
                                                    db.collection("bai_viet")
                                                            .document(originalDocId);

                                            batch.update(
                                                    baiGocRef,
                                                    "so_repost",
                                                    FieldValue.increment(-1)
                                            );

                                            batch.commit()
                                                    .addOnSuccessListener(unused -> {

                                                        repostedByMe.remove(originalDocId);

                                                        post.setRepostedByMe(false);
                                                        post.setSoRepost(
                                                                Math.max(0,
                                                                        post.getSoRepost() - 1)
                                                        );

                                                        postAdapter.notifyItemChanged(
                                                                position,
                                                                "REPOST_UPDATE"
                                                        );

                                                        Toast.makeText(
                                                                UserProfileActivity.this,
                                                                "Đã xóa bài đăng lại",
                                                                Toast.LENGTH_SHORT
                                                        ).show();
                                                    });
                                        });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();

                    return;
                }

                // Chưa repost -> đăng lại
                new AlertDialog.Builder(UserProfileActivity.this)
                        .setTitle("Đăng lại bài viết")
                        .setMessage("Bạn muốn đăng lại bài này?")
                        .setPositiveButton("Đăng lại", (dialog, which) -> {

                            Map<String, Object> repost = new HashMap<>();

                            repost.put("nguoi_dung_id", myUid);
                            repost.put("noi_dung", "");
                            repost.put("ngay_tao", Timestamp.now());
                            repost.put("so_like", 0);
                            repost.put("so_binh_luan", 0);
                            repost.put("so_repost", 0);
                            repost.put("so_share", 0);
                            repost.put("da_xoa", false);
                            repost.put("che_do_xem", "public");
                            repost.put("hinh_anh", new ArrayList<>());
                            repost.put("danh_sach_anh", new ArrayList<>());
                            repost.put("bai_viet_cha_id", originalDocId);
                            repost.put("is_repost", true);

                            DocumentReference baiGocRef =
                                    db.collection("bai_viet")
                                            .document(originalDocId);

                            WriteBatch batch = db.batch();

                            DocumentReference repostRef =
                                    db.collection("bai_viet")
                                            .document();

                            batch.set(repostRef, repost);

                            batch.update(
                                    baiGocRef,
                                    "so_repost",
                                    FieldValue.increment(1)
                            );

                            batch.commit()
                                    .addOnSuccessListener(unused -> {

                                        repostedByMe.add(originalDocId);

                                        post.setRepostedByMe(true);
                                        post.setSoRepost(
                                                post.getSoRepost() + 1
                                        );

                                        postAdapter.notifyItemChanged(
                                                position,
                                                "REPOST_UPDATE"
                                        );

                                        Toast.makeText(
                                                UserProfileActivity.this,
                                                "Đã đăng lại!",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    UserProfileActivity.this,
                                                    "Lỗi: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT
                                            ).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
            @Override public void onShareClick(PostModel post, int position)       {}
            @Override public void onMoreOptionsClick(PostModel post, int position) {}
            @Override public void onAvatarClick(PostModel post, int position)      { openPostDetail(post.getDocumentId()); }
            @Override public void onCommentAvataClick(CommentModel comment, int position) {}
            @Override public void onAddFriendClick(PostModel post, int position)   { handleFollow(post, position); }
        });
        postAdapter.setOnItemClickListener(this::openPostDetail);

        // ── Adapter bình luận ───────────────────────────────
        commentAdapter = new CommentAdapter(commentList, "",
                new CommentAdapter.OnCommentActionListener() {
                    @Override public void onCommentClick(CommentModel c, int pos)               { openPostDetail(c.getPostId()); }
                    @Override public void onLikeClick(CommentModel c, int pos)                  {}
                    @Override public void onReplyClick(CommentModel c, int pos)                 {}
                    @Override public void onAvatarClick(CommentModel c, int pos)                {}
                    @Override public void onAddFriendClick(CommentModel c, int pos)             {}
                    @Override public void onEditComment(CommentModel c, int pos, String s)      {}
                    @Override public void onDeleteComment(CommentModel c, int pos)              {}
                });

        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);
        db.collection("nguoi_dung")
                .document(targetUid)
                .get()
                .addOnSuccessListener(doc -> {

                    boolean isPrivate =
                            Boolean.TRUE.equals(doc.getBoolean("private"));

                    // Nếu là chủ tài khoản thì luôn xem được
                    boolean canView =
                            !isPrivate ||
                                    (myUid != null && myUid.equals(targetUid)) ||
                                    isFollowingTarget;

                    if (canView) {

                        btnPost.setOnClickListener(v -> {
                            setActiveTab(0);
                            rvPosts.setAdapter(postAdapter);
                            loadPosts();
                        });

                        btnRepost.setOnClickListener(v -> {
                            setActiveTab(1);
                            rvPosts.setAdapter(postAdapter);
                            loadReposts();
                        });

                        btnComment.setOnClickListener(v -> {
                            setActiveTab(2);
                            rvPosts.setAdapter(commentAdapter);
                            loadComments();
                        });



                        setActiveTab(0);
                        loadPosts();

                    } else {

                        btnPost.setVisibility(View.GONE);
                        btnRepost.setVisibility(View.GONE);
                        btnComment.setVisibility(View.GONE);

                        rvPosts.setVisibility(View.GONE);
                    }
                });
        loadUserInfo();
        }


    // ════════════════════════════════════════════════════════
    //  CHECK & CẬP NHẬT TRẠNG THÁI FOLLOW (btnFollow trên profile)
    // ════════════════════════════════════════════════════════
    private void checkFollowStatus() {
        if (myUid == null) return;

        db.collection("nguoi_dung")
                .document(targetUid)
                .get()
                .addOnSuccessListener(userDoc -> {

//                    boolean isPrivate =
//                            Boolean.TRUE.equals(userDoc.getBoolean("private"));

//                    if (!isPrivate) {
//
//                        isFollowingTarget = false; // chưa follow
//
//                        updateFollowButton(false);
//                        setupFollowButtonListeners();
//                        checkProfileAccess();
//
//                        return;
//                    }

                    db.collection("nguoi_dung")
                            .document(myUid)
                            .collection("nguoi_dang_theo_doi")
                            .document(targetUid)
                            .get()
                            .addOnSuccessListener(doc -> {

                                isFollowingTarget = doc.exists();

                                updateFollowButton(isFollowingTarget);
                                setupFollowButtonListeners();
                                checkProfileAccess();
                            });
                });
    }

    private void updateFollowButton(boolean following) {
        if (following) {
            btnFollow.setText("Đang theo dõi");

            btnFollow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            getColor(R.color.bg_primary)
                    )
            );

            btnFollow.setTextColor(
                    getColor(R.color.text_primary)
            );

        } else {
            btnFollow.setText("Theo dõi");

            btnFollow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            getColor(R.color.text_primary)
                    )
            );

            btnFollow.setTextColor(
                    getColor(R.color.bg_primary)
            );
        }
    }

    private void setupFollowButtonListeners() {
        btnFollow.setOnClickListener(v -> {
            if (!isFollowingTarget) followTarget();
        });
        btnFollow.setOnLongClickListener(v -> {
            if (isFollowingTarget) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Hủy theo dõi")
                        .setMessage("Bạn có muốn hủy theo dõi không?")
                        .setPositiveButton("Hủy theo dõi", (dialog, which) -> unfollowTarget())
                        .setNegativeButton("Đóng", null)
                        .show();
            }
            return true;
        });
    }

    private void followTarget() {
        if (myUid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("nguoi_dung_id", targetUid);
        data.put("ngay_theo_doi", new Date());

        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_dang_theo_doi").document(targetUid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> followerData = new HashMap<>();
                    followerData.put("nguoi_dung_id", myUid);
                    followerData.put("ngay_theo_doi", new Date());
                    db.collection("nguoi_dung").document(targetUid)
                            .collection("nguoi_theo_doi").document(myUid).set(followerData);
                    db.collection("nguoi_dung").document(myUid)
                            .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));
                    db.collection("nguoi_dung").document(targetUid)
                            .update("so_nguoi_theo_doi", FieldValue.increment(1));

                    isFollowingTarget = true;
                    updateFollowButton(true);
                    // Refresh danh sách bài viết để cập nhật trạng thái follow trong item
                    loadPosts();
                });
    }

    private void unfollowTarget() {
        if (myUid == null) return;
        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_dang_theo_doi").document(targetUid)
                .delete()
                .addOnSuccessListener(unused -> {
                    db.collection("nguoi_dung").document(targetUid)
                            .collection("nguoi_theo_doi").document(myUid).delete();
                    db.collection("nguoi_dung").document(myUid)
                            .update("so_nguoi_dang_theo_doi", FieldValue.increment(-1));
                    db.collection("nguoi_dung").document(targetUid)
                            .update("so_nguoi_theo_doi", FieldValue.increment(-1));

                    isFollowingTarget = false;
                    updateFollowButton(false);
                    loadPosts();
                });
    }

    // ════════════════════════════════════════════════════════
    //  LOAD USER INFO
    // ════════════════════════════════════════════════════════
    private void loadUserInfo() {
        db.collection("nguoi_dung").document(targetUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    avatarUrl = doc.getString("anh_dai_dien");

                    txtName.setText(
                            doc.getString("ho_va_ten") != null
                                    ? doc.getString("ho_va_ten")
                                    : "No Name");

                    txtUsername.setText(
                            doc.getString("ten_dang_nhap") != null
                                    ? doc.getString("ten_dang_nhap")
                                    : "@user");

                    txtBio.setText(
                            doc.getString("tieu_su") != null
                                    ? doc.getString("tieu_su")
                                    : "");

                    Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .circleCrop()
                            .into(imgProfile);

                    boolean isPrivate =
                            Boolean.TRUE.equals(doc.getBoolean("private"));

                    if (!isPrivate) {

                        Long theodoi = doc.getLong("so_nguoi_theo_doi");

                        txtSoLuongTheoDoi.setVisibility(View.VISIBLE);
                        layoutFollowingAvatars.setVisibility(View.VISIBLE);

                        txtSoLuongTheoDoi.setText(
                                (theodoi != null ? theodoi : 0)
                                        + " người theo dõi");

                        loadFollowingAvatars();

                    } else {

                        txtSoLuongTheoDoi.setVisibility(View.GONE);
                        layoutFollowingAvatars.setVisibility(View.GONE);
                    }
                });
    }
    private void checkProfileAccess() {
        db.collection("nguoi_dung")
                .document(targetUid)
                .get()
                .addOnSuccessListener(doc -> {

                    boolean isPrivate =
                            Boolean.TRUE.equals(doc.getBoolean("private"));

                    boolean canView =
                            !isPrivate ||
                                    (myUid != null && myUid.equals(targetUid)) ||
                                    isFollowingTarget;

                    if (canView) {

                        btnPost.setVisibility(View.VISIBLE);
                        btnRepost.setVisibility(View.VISIBLE);
                        btnComment.setVisibility(View.VISIBLE);
                        rvPosts.setVisibility(View.VISIBLE);

                        btnPost.setOnClickListener(v -> {
                            setActiveTab(0);
                            rvPosts.setAdapter(postAdapter);
                            loadPosts();
                        });

                        btnRepost.setOnClickListener(v -> {
                            setActiveTab(1);
                            rvPosts.setAdapter(postAdapter);
                            loadReposts();
                        });

                        btnComment.setOnClickListener(v -> {
                            setActiveTab(2);
                            rvPosts.setAdapter(commentAdapter);
                            loadComments();
                        });

                        setActiveTab(0);
                        loadPosts();

                    } else {

                        btnPost.setVisibility(View.GONE);
                        btnRepost.setVisibility(View.GONE);
                        btnComment.setVisibility(View.GONE);
                        rvPosts.setVisibility(View.GONE);
                    }
                });
    }

    // ════════════════════════════════════════════════════════
    //  LOAD BÀI VIẾT
    // ════════════════════════════════════════════════════════
    private void loadPosts() {
        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", targetUid)
                .whereEqualTo("da_xoa", false)
                .whereEqualTo("is_repost", false)
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .limit(20).get()
                .addOnSuccessListener(qs -> {
                    List<PostModel> temp = new ArrayList<>();
                    int[] pending = {qs.size()};
                    if (pending[0] == 0) { postList.clear(); postAdapter.notifyDataSetChanged(); return; }

                    for (var doc : qs.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        if (post == null) { pending[0]--; continue; }
                        post.setDocumentId(doc.getId());

                        final String ownerUid = doc.getString("nguoi_dung_id");

                        Runnable finish = () -> {
                            temp.add(post); pending[0]--;
                            if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
                        };

                        Runnable checkFollow = () -> {
                            // Không check follow nếu: chưa login, không có ownerUid, hoặc là chính mình
                            if (myUid == null || ownerUid == null || myUid.equals(ownerUid)) {
                                post.setFollowing(false);
                                finish.run();
                                return;
                            }
                            db.collection("nguoi_dung").document(myUid)
                                    .collection("nguoi_dang_theo_doi").document(ownerUid).get()
                                    .addOnSuccessListener(followDoc -> {
                                        post.setFollowing(followDoc.exists());
                                        finish.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        post.setFollowing(false);
                                        finish.run();
                                    });
                        };

                        Runnable checkLike = () -> {
                            if (myUid == null) {
                                post.setLikedByMe(false);
                                checkFollow.run();
                                return;
                            }
                            db.collection("bai_viet").document(post.getDocumentId())
                                    .collection("luot_thich").document(myUid).get()
                                    .addOnSuccessListener(likeDoc -> {
                                        post.setLikedByMe(likeDoc.exists());
                                        checkFollow.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        post.setLikedByMe(false);
                                        checkFollow.run();
                                    });
                        };

                        if (ownerUid != null) {
                            db.collection("nguoi_dung").document(ownerUid).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            post.setHoVaTen(userDoc.getString("ho_va_ten"));
                                            post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                            post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                            post.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                        }
                                        checkLike.run();
                                    })
                                    .addOnFailureListener(e -> checkLike.run());
                        } else {
                            checkLike.run();
                        }
                    }
                });
    }
    private void checkRepost(PostModel post, Runnable onDone) {

        if (myUid == null) {
            continueLoadParent(post, onDone);
            return;
        }

        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", myUid)
                .whereEqualTo("bai_viet_cha_id", post.getDocumentId())
                .whereEqualTo("is_repost", true)
                .whereEqualTo("da_xoa", false)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    boolean reposted = !snapshot.isEmpty();

                    post.setRepostedByMe(reposted);

                    if (reposted) {
                        repostedByMe.add(post.getDocumentId());
                    } else {
                        repostedByMe.remove(post.getDocumentId());
                    }

                    continueLoadParent(post, onDone);
                })
                .addOnFailureListener(e ->
                        continueLoadParent(post, onDone));
    }

    private void continueLoadParent(PostModel post, Runnable onDone) {

        String chaId = post.getBaiVietChaId();

        if (chaId == null || chaId.isEmpty()) {
            onDone.run();
        } else {
            loadBaiVietCha(post, chaId, onDone);
        }
    }
    private void loadBaiVietCha(PostModel post, String chaId, Runnable onDone) {
        db.collection("bai_viet").document(chaId).get()
                .addOnSuccessListener(chaDoc -> {
                    if (!chaDoc.exists()) { onDone.run(); return; }
                    PostModel postCha = chaDoc.toObject(PostModel.class);
                    postCha.setDocumentId(chaDoc.getId());
                    String chaUid = chaDoc.getString("nguoi_dung_id");
                    if (chaUid != null && !chaUid.isEmpty()) {
                        db.collection("nguoi_dung").document(chaUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        postCha.setHoVaTen(userDoc.getString("ho_va_ten"));
                                        postCha.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                        postCha.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                        postCha.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                    }
                                    post.setPostCha(postCha);
                                    onDone.run();
                                })
                                .addOnFailureListener(e -> { post.setPostCha(postCha); onDone.run(); });
                    } else {
                        post.setPostCha(postCha);
                        onDone.run();
                    }
                })
                .addOnFailureListener(e -> onDone.run());
    }

    // ════════════════════════════════════════════════════════
    //  LOAD REPOST
    // ════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════
//  LOAD REPOST
// ════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════
//  LOAD REPOST
// ════════════════════════════════════════════════════════
    private void loadReposts() {
        postList.clear();
        postAdapter.notifyDataSetChanged();

        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", targetUid)
                .whereEqualTo("da_xoa", false)
                .whereEqualTo("is_repost", true)
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) { postAdapter.notifyDataSetChanged(); return; }

                    List<String> parentIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String pid = doc.getString("bai_viet_cha_id");
                        if (pid != null && !pid.isEmpty() && !parentIds.contains(pid)) {
                            parentIds.add(pid);
                        }
                    }

                    if (parentIds.isEmpty()) { postAdapter.notifyDataSetChanged(); return; }

                    List<PostModel> tempList = new ArrayList<>();
                    int[] loadedCount = {0};
                    int total = parentIds.size();

                    for (String parentId : parentIds) {
                        loadRepostWithContext(parentId, tempList, loadedCount, total);
                    }
                })
                .addOnFailureListener(e -> Log.e("REPOST_DEBUG", "Query FAIL: ", e));
    }

    private void loadRepostWithContext(String postId, List<PostModel> tempList, int[] loadedCount, int total) {
        db.collection("bai_viet").document(postId).get()
                .addOnSuccessListener(postDoc -> {
                    if (!postDoc.exists()) {
                        checkRepostAndNotify(tempList, loadedCount, total);
                        return;
                    }

                    String postUserId = postDoc.getString("nguoi_dung_id");
                    boolean isRepost  = Boolean.TRUE.equals(postDoc.getBoolean("is_repost"));

                    db.collection("nguoi_dung").document(postUserId).get()
                            .addOnSuccessListener(userDoc -> {
                                PostModel post = buildRepostModel(postDoc, userDoc);

                                if (isRepost) {
                                    String grandParentId = postDoc.getString("bai_viet_cha_id");
                                    if (grandParentId != null && !grandParentId.isEmpty()) {
                                        db.collection("bai_viet").document(grandParentId).get()
                                                .addOnSuccessListener(gpDoc -> {
                                                    if (!gpDoc.exists()) {
                                                        checkLikeAndFollow(post, tempList, loadedCount, total);
                                                        return;
                                                    }
                                                    String gpUserId = gpDoc.getString("nguoi_dung_id");
                                                    db.collection("nguoi_dung").document(gpUserId).get()
                                                            .addOnSuccessListener(gpUserDoc -> {
                                                                PostModel grandParentPost = buildRepostModel(gpDoc, gpUserDoc);
                                                                post.setPostCha(grandParentPost);
                                                                checkLikeAndFollow(post, tempList, loadedCount, total);
                                                            })
                                                            .addOnFailureListener(e -> checkLikeAndFollow(post, tempList, loadedCount, total));
                                                })
                                                .addOnFailureListener(e -> checkLikeAndFollow(post, tempList, loadedCount, total));
                                    } else {
                                        checkLikeAndFollow(post, tempList, loadedCount, total);
                                    }
                                } else {
                                    checkLikeAndFollow(post, tempList, loadedCount, total);
                                }
                            })
                            .addOnFailureListener(e -> checkRepostAndNotify(tempList, loadedCount, total));
                })
                .addOnFailureListener(e -> checkRepostAndNotify(tempList, loadedCount, total));
    }

    private void checkLikeAndFollow(PostModel post, List<PostModel> tempList, int[] loadedCount, int total) {
        final String ownerUid = post.getNguoiDungId();

        Runnable finish = () -> {
            tempList.add(post); loadedCount[0]++;
            if (loadedCount[0] == total) {
                postList.clear();
                postList.addAll(tempList);
                postAdapter.notifyDataSetChanged();
            }
        };

        Runnable checkFollow = () -> {
            if (myUid == null || ownerUid == null || myUid.equals(ownerUid)) {
                post.setFollowing(false);
                finish.run();
                return;
            }
            db.collection("nguoi_dung").document(myUid)
                    .collection("nguoi_dang_theo_doi").document(ownerUid).get()
                    .addOnSuccessListener(followDoc -> {
                        post.setFollowing(followDoc.exists());
                        finish.run();
                    })
                    .addOnFailureListener(e -> {
                        post.setFollowing(false);
                        finish.run();
                    });
        };

        if (myUid == null) {
            post.setLikedByMe(false);
            checkFollow.run();
            return;
        }
        db.collection("bai_viet").document(post.getDocumentId())
                .collection("luot_thich").document(myUid).get()
                .addOnSuccessListener(likeDoc -> {
                    post.setLikedByMe(likeDoc.exists());
                    checkFollow.run();
                })
                .addOnFailureListener(e -> {
                    post.setLikedByMe(false);
                    checkFollow.run();
                });
    }

    private void checkRepostAndNotify(List<PostModel> tempList, int[] loadedCount, int total) {
        loadedCount[0]++;
        if (loadedCount[0] == total) {
            postList.clear();
            postList.addAll(tempList);
            postAdapter.notifyDataSetChanged();
        }
    }

    private PostModel buildRepostModel(DocumentSnapshot doc, DocumentSnapshot userDoc) {
        PostModel post = new PostModel();
        post.setDocumentId(doc.getId());
        post.setNguoiDungId(doc.getString("nguoi_dung_id"));
        post.setNoiDung(doc.getString("noi_dung"));
        post.setDanhSachAnh((List<String>) doc.get("danh_sach_anh"));
        post.setRepost(Boolean.TRUE.equals(doc.getBoolean("is_repost")));
        post.setBaiVietChaId(doc.getString("bai_viet_cha_id"));
        post.setSoLuotThich(doc.getLong("so_like")       != null ? doc.getLong("so_like").intValue()       : 0);
        post.setSoBinhLuan(doc.getLong("so_binh_luan")   != null ? doc.getLong("so_binh_luan").intValue()   : 0);
        post.setSoRepost(doc.getLong("so_repost")        != null ? doc.getLong("so_repost").intValue()      : 0);
        post.setSoShare(doc.getLong("so_share")          != null ? doc.getLong("so_share").intValue()       : 0);
        post.setHoVaTen(userDoc.getString("ho_va_ten"));
        post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
        post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
        post.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
        return post;
    }

    // ════════════════════════════════════════════════════════
//  LOAD BÌNH LUẬN  (CommentModel không có isFollowing nên chỉ check like)
// ════════════════════════════════════════════════════════
    private void loadComments() {
        db.collectionGroup("binh_luan")
                .whereEqualTo("nguoi_dung_id", targetUid)
                .orderBy("ngay_tao", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(qs -> {
                    List<CommentModel> temp = new ArrayList<>();
                    int[] pending = {qs.size()};
                    if (pending[0] == 0) { commentList.clear(); commentAdapter.notifyDataSetChanged(); return; }

                    for (var doc : qs.getDocuments()) {
                        CommentModel comment = doc.toObject(CommentModel.class);
                        if (comment == null) { pending[0]--; continue; }
                        comment.setDocumentId(doc.getId());

                        String postId = null;
                        if (doc.getReference().getParent() != null
                                && doc.getReference().getParent().getParent() != null) {
                            postId = doc.getReference().getParent().getParent().getId();
                        }
                        comment.setPostId(postId);
                        final String finalPostId = postId;
                        final String ownerUid = comment.getNguoiDungId();

                        Runnable finish = () -> {
                            temp.add(comment); pending[0]--;
                            if (pending[0] == 0) { commentList.clear(); commentList.addAll(temp); commentAdapter.notifyDataSetChanged(); }
                        };

                        Runnable checkFollow = () -> {
                            if (myUid == null || ownerUid == null || myUid.equals(ownerUid)) {
                                comment.setFollowing(false);
                                finish.run();
                                return;
                            }
                            db.collection("nguoi_dung").document(myUid)
                                    .collection("nguoi_dang_theo_doi").document(ownerUid).get()
                                    .addOnSuccessListener(followDoc -> {
                                        comment.setFollowing(followDoc.exists());
                                        finish.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        comment.setFollowing(false);
                                        finish.run();
                                    });
                        };

                        Runnable checkLike = () -> {
                            if (myUid == null || finalPostId == null) {
                                comment.setLikedByMe(false);
                                checkFollow.run();
                                return;
                            }
                            db.collection("bai_viet").document(finalPostId)
                                    .collection("binh_luan").document(comment.getDocumentId())
                                    .collection("luot_thich").document(myUid).get()
                                    .addOnSuccessListener(likeDoc -> {
                                        comment.setLikedByMe(likeDoc.exists());
                                        checkFollow.run(); // ← xong like mới check follow
                                    })
                                    .addOnFailureListener(e -> {
                                        comment.setLikedByMe(false);
                                        checkFollow.run();
                                    });
                        };

                        if (ownerUid != null) {
                            db.collection("nguoi_dung").document(ownerUid).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            comment.setHoVaTen(userDoc.getString("ho_va_ten"));
                                            comment.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                            comment.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                            comment.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                        }
                                        checkLike.run();
                                    })
                                    .addOnFailureListener(e -> checkLike.run());
                        } else checkLike.run();
                    }
                });
    }
    // ════════════════════════════════════════════════════════
    //  FOLLOWING AVATARS
    // ════════════════════════════════════════════════════════
    private void loadFollowingAvatars() {
        db.collection("nguoi_dung").document(targetUid)
                .collection("nguoi_dang_theo_doi").limit(5).get()
                .addOnSuccessListener(qs -> {
                    List<String> uids = new ArrayList<>();
                    for (var doc : qs.getDocuments()) {
                        String u = doc.getString("nguoi_dung_id");
                        if (u != null) uids.add(u);
                    }
                    if (!uids.isEmpty()) loadAvatarsIntoLayout(uids);
                });
    }

    private void loadAvatarsIntoLayout(List<String> uids) {
        layoutFollowingAvatars.removeAllViews();
        int sizePx    = (int) (28 * getResources().getDisplayMetrics().density);
        int overlapPx = (int) (-8 * getResources().getDisplayMetrics().density);
        int maxShow   = Math.min(uids.size(), 3);

        for (int i = 0; i < maxShow; i++) {
            ShapeableImageView iv = new ShapeableImageView(this);
            iv.setShapeAppearanceModel(iv.getShapeAppearanceModel()
                    .toBuilder().setAllCornerSizes(sizePx / 2f).build());
            iv.setStrokeWidth(2f);
            iv.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(sizePx, sizePx);
            if (i > 0) p.setMarginStart(overlapPx);
            iv.setLayoutParams(p);

            String uid = uids.get(i);
            db.collection("nguoi_dung").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        String url = userDoc.getString("anh_dai_dien");
                        Glide.with(this).load(url)
                                .placeholder(R.drawable.ic_person_outline_24)
                                .circleCrop().into(iv);
                    });
            layoutFollowingAvatars.addView(iv);
        }
    }

    // ════════════════════════════════════════════════════════
    //  HANDLE LIKE
    // ════════════════════════════════════════════════════════
    private void handleLike(PostModel post, int position) {
        if (myUid == null) return;
        boolean liked  = post.isLikedByMe();
        String  postId = post.getDocumentId();
        post.setLikedByMe(!liked);
        post.setSoLuotThich(post.getSoLuotThich() + (liked ? -1 : 1));
        postAdapter.notifyItemChanged(position, "LIKE_UPDATE");

        var likeRef = db.collection("bai_viet").document(postId).collection("luot_thich").document(myUid);
        var postRef = db.collection("bai_viet").document(postId);
        if (liked) {
            likeRef.delete();
            postRef.update("so_luot_thich", FieldValue.increment(-1));
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("nguoi_dung_id", myUid);
            data.put("thoi_gian", FieldValue.serverTimestamp());
            likeRef.set(data);
            postRef.update("so_luot_thich", FieldValue.increment(1));
        }
    }

    // ════════════════════════════════════════════════════════
    //  HANDLE FOLLOW
    // ════════════════════════════════════════════════════════
    private void handleFollow(PostModel post, int position) {
        if (myUid == null) return;
        String authorUid = post.getNguoiDungId();
        if (authorUid == null || authorUid.equals(myUid)) return;

        Map<String, Object> followingEntry = new HashMap<>();
        followingEntry.put("nguoi_dung_id", authorUid);
        followingEntry.put("ngay_theo_doi", new Date());

        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_dang_theo_doi").document(authorUid)
                .set(followingEntry)
                .addOnSuccessListener(unused -> {
                    db.collection("nguoi_dung").document(myUid)
                            .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));
                    Map<String, Object> followerEntry = new HashMap<>();
                    followerEntry.put("nguoi_dung_id", myUid);
                    followerEntry.put("ngay_theo_doi", new Date());
                    db.collection("nguoi_dung").document(authorUid)
                            .collection("nguoi_theo_doi").document(myUid).set(followerEntry);
                    db.collection("nguoi_dung").document(authorUid)
                            .update("so_nguoi_theo_doi", FieldValue.increment(1));
                    post.setFollowing(true);
                    postAdapter.notifyItemChanged(position);
                });
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════
    private void openPostDetail(String postId) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        startActivity(intent);
    }

//    private void setActiveTab(int tab) {
//        linePost.setBackgroundColor(Color.parseColor("#1C1C1E"));
//        lineRepost.setBackgroundColor(Color.parseColor("#1C1C1E"));
//        lineComment.setBackgroundColor(Color.parseColor("#1C1C1E"));
//        txtTabPost.setTextColor(Color.parseColor("#636366"));
//        txtTabRepost.setTextColor(Color.parseColor("#636366"));
//        txtTabComment.setTextColor(Color.parseColor("#636366"));
//        switch (tab) {
//            case 0: linePost.setBackgroundColor(Color.WHITE);    txtTabPost.setTextColor(Color.WHITE);    break;
//            case 1: lineRepost.setBackgroundColor(Color.WHITE);  txtTabRepost.setTextColor(Color.WHITE);  break;
//            case 2: lineComment.setBackgroundColor(Color.WHITE); txtTabComment.setTextColor(Color.WHITE); break;
//        }
//    }
private void setActiveTab(int tab) {
    int colorActive = resolveColor(com.google.android.material.R.attr.colorOnSurface);
    int colorInactive = resolveColor(com.google.android.material.R.attr.colorOutline);

    // reset
    linePost.setBackgroundColor(colorInactive);
    lineComment.setBackgroundColor(colorInactive);
    lineRepost.setBackgroundColor(colorInactive);
    txtTabPost.setTextColor(colorInactive);
    txtTabComment.setTextColor(colorInactive);
    txtTabRepost.setTextColor(colorInactive);

    switch (tab) {
        case 0:
            linePost.setBackgroundColor(colorActive);
            txtTabPost.setTextColor(colorActive);
            break;
        case 1:
            lineRepost.setBackgroundColor(colorActive);
            txtTabRepost.setTextColor(colorActive);
            break;
        case 2:
            lineComment.setBackgroundColor(colorActive);
            txtTabComment.setTextColor(colorActive);
            break;
    }
}

    private int resolveColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        this.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void showFullImage(String url) {
        Dialog dialog = new Dialog(this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setBackgroundColor(Color.BLACK);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_placeholder_avatar)
                .error(R.drawable.ic_placeholder_avatar)
                .into(imageView);

        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(imageView);
        dialog.show();
    }

//    private void enableImmersiveMode() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            getWindow().setDecorFitsSystemWindows(false);
//            WindowInsetsController c = getWindow().getInsetsController();
//            if (c != null) {
//                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
//            }
//        } else {
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
//        }
//    }
}