package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.HomePage.CommentAdapter;
import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.HomePage.PostAdapter;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private ShapeableImageView imgProfile;
    private TextView txtName, txtUsername, txtBio, txtSoLuongTheoDoi;
    private ImageView btnCancel;
    private LinearLayout btnPost, btnRepost, btnComment;
    private View linePost, lineRepost, lineComment;
    private TextView txtTabPost, txtTabRepost, txtTabComment;
    private RecyclerView rvPosts;
    private LinearLayout layoutFollowingAvatars;

    private FirebaseFirestore db;
    private String targetUid, myUid, avatarUrl = "";

    private PostAdapter postAdapter;
    private CommentAdapter commentAdapter;
    private List<PostModel> postList = new ArrayList<>();
    private List<CommentModel> commentList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        enableImmersiveMode();
        Intent intent = getIntent();

        Log.d("RECEIVE_UID", "intent = " + intent);
        Log.d("RECEIVE_UID", "extra = " + intent.getStringExtra("user_uid"));
        db    = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        targetUid = intent.getStringExtra("user_uid");
        Log.d("RECEIVE_UID", "targetUid = " + targetUid);
        if (targetUid == null || targetUid.isEmpty()) { finish(); return; }

        // ── Ánh xạ ──────────────────────────────────────────
        imgProfile          = findViewById(R.id.imgProfile);
        txtName             = findViewById(R.id.txtName);
        txtUsername         = findViewById(R.id.txtUsername);
        txtBio              = findViewById(R.id.txtTitle);
        txtSoLuongTheoDoi   = findViewById(R.id.txtSoNguoiTheoDoi);
        btnCancel           = findViewById(R.id.btn_cancel);
        btnPost             = findViewById(R.id.btnPost);
        btnRepost           = findViewById(R.id.btnRepost);
        btnComment          = findViewById(R.id.btnComment);
        linePost            = findViewById(R.id.linePost);
        lineRepost          = findViewById(R.id.lineRepost);
        lineComment         = findViewById(R.id.lineComment);
        txtTabPost          = findViewById(R.id.txtTabPost);
        txtTabRepost        = findViewById(R.id.txtTabRepost);
        txtTabComment       = findViewById(R.id.txtTabComment);
        rvPosts             = findViewById(R.id.rvMyThreads);
        layoutFollowingAvatars = findViewById(R.id.layoutFollowingAvatars);

        btnCancel.setOnClickListener(v -> finish());
        txtSoLuongTheoDoi.setOnClickListener(v -> {

            FollowBottomSheet bottomSheet =
                    new FollowBottomSheet(
                            targetUid,
                            () -> {
                                // callback dismiss nếu cần
                            }
                    );

            bottomSheet.show(
                    getSupportFragmentManager(),
                    "FollowBottomSheet"
            );
        });
        imgProfile.setOnClickListener(v -> {
            if (avatarUrl != null && !avatarUrl.isEmpty()) showFullImage(avatarUrl);
        });

        // ── Adapter bài viết ────────────────────────────────
        postAdapter = new PostAdapter(postList, new PostAdapter.OnPostActionListener() {
            @Override
            public void onLikeClick(PostModel post, int position) {
                handleLike(post, position);
            }
            @Override public void onCommentClick(PostModel post, int position) {
                openPostDetail(post.getDocumentId());
            }
            @Override public void onRepostClick(PostModel post, int position) {}
            @Override public void onShareClick(PostModel post, int position) {}
            @Override public void onMoreOptionsClick(PostModel post, int position) {}
            @Override public void onAvatarClick(PostModel post, int position) {
                openPostDetail(post.getDocumentId());
            }
            @Override public void onCommentAvataClick(CommentModel comment, int position) {}
            @Override public void onAddFriendClick(PostModel post, int position) {
                handleFollow(post, position);
            }
        });
        postAdapter.setOnItemClickListener(this::openPostDetail);

        // ── Adapter bình luận ───────────────────────────────
        commentAdapter = new CommentAdapter(commentList, "",
                new CommentAdapter.OnCommentActionListener() {
                    @Override public void onCommentClick(CommentModel c, int pos) {
                        openPostDetail(c.getPostId());
                    }
                    @Override public void onLikeClick(CommentModel c, int pos) {}
                    @Override public void onReplyClick(CommentModel c, int pos) {}
                    @Override public void onAvatarClick(CommentModel c, int pos) {}
                    @Override public void onAddFriendClick(CommentModel c, int pos) {}
                    @Override public void onEditComment(CommentModel c, int pos, String s) {}
                    @Override public void onDeleteComment(CommentModel c, int pos) {}
                });

        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);

        // ── Tab click ───────────────────────────────────────
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

        // ── Load mặc định ───────────────────────────────────
        loadUserInfo();
        setActiveTab(0);
        loadPosts();
    }

    // ════════════════════════════════════════════════════════
    //  LOAD USER INFO
    // ════════════════════════════════════════════════════════
    private void loadUserInfo() {
        db.collection("nguoi_dung").document(targetUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    avatarUrl = doc.getString("anh_dai_dien");
                    txtName.setText(doc.getString("ho_va_ten") != null ? doc.getString("ho_va_ten") : "No Name");
                    txtUsername.setText(doc.getString("ten_dang_nhap") != null ? doc.getString("ten_dang_nhap") : "@user");
                    txtBio.setText(doc.getString("tieu_su") != null ? doc.getString("tieu_su") : "");
                    Long theodoi = doc.getLong("so_nguoi_theo_doi");
                    txtSoLuongTheoDoi.setText((theodoi != null ? theodoi : 0) + " người theo dõi");
                    Glide.with(this).load(avatarUrl)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .circleCrop().into(imgProfile);
                    loadFollowingAvatars();
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

                        String ownerUid = doc.getString("nguoi_dung_id");
                        Runnable checkLike = () -> {
                            if (myUid == null) {
                                temp.add(post); pending[0]--;
                                if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
                                return;
                            }
                            db.collection("bai_viet").document(post.getDocumentId())
                                    .collection("luot_thich").document(myUid).get()
                                    .addOnSuccessListener(likeDoc -> {
                                        post.setLikedByMe(likeDoc.exists());
                                        temp.add(post); pending[0]--;
                                        if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
                                    })
                                    .addOnFailureListener(e -> {
                                        temp.add(post); pending[0]--;
                                        if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
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
                                    }).addOnFailureListener(e -> checkLike.run());
                        } else checkLike.run();
                    }
                });
    }

    // ════════════════════════════════════════════════════════
    //  LOAD REPOST
    // ════════════════════════════════════════════════════════
    private void loadReposts() {
        db.collection("nguoi_dung").document(targetUid).get()
                .addOnSuccessListener(myUserDoc -> {
                    String myHoVaTen     = myUserDoc.getString("ho_va_ten");
                    String myTenDangNhap = myUserDoc.getString("ten_dang_nhap");
                    String myAnhDaiDien  = myUserDoc.getString("anh_dai_dien");
                    boolean myVerified   = Boolean.TRUE.equals(myUserDoc.getBoolean("verified"));

                    db.collection("bai_viet")
                            .whereEqualTo("nguoi_dung_id", targetUid)
                            .whereEqualTo("da_xoa", false)
                            .whereEqualTo("is_repost", true)
                            .orderBy("ngay_tao", Query.Direction.DESCENDING).get()
                            .addOnSuccessListener(qs -> {
                                List<PostModel> temp = new ArrayList<>();
                                int[] pending = {qs.size()};
                                if (pending[0] == 0) { postList.clear(); postAdapter.notifyDataSetChanged(); return; }

                                for (var repostDoc : qs.getDocuments()) {
                                    String parentId = repostDoc.getString("bai_viet_cha_id");
                                    if (parentId == null) { pending[0]--; continue; }

                                    db.collection("bai_viet").document(parentId).get()
                                            .addOnSuccessListener(parentDoc -> {
                                                if (!parentDoc.exists()) { pending[0]--; return; }
                                                PostModel post = parentDoc.toObject(PostModel.class);
                                                if (post == null) { pending[0]--; return; }
                                                post.setDocumentId(parentDoc.getId());
                                                post.setRepost(true);
                                                post.setNguoiDungId(targetUid);
                                                post.setHoVaTen(myHoVaTen);
                                                post.setTenDangNhap(myTenDangNhap);
                                                post.setAnhDaiDien(myAnhDaiDien);
                                                post.setVerified(myVerified);

                                                String ownerUid = parentDoc.getString("nguoi_dung_id");
                                                if (ownerUid != null) {
                                                    db.collection("nguoi_dung").document(ownerUid).get()
                                                            .addOnSuccessListener(ownerDoc -> {
                                                                PostModel postCha = new PostModel();
                                                                postCha.setDocumentId(parentDoc.getId());
                                                                postCha.setNoiDung(post.getNoiDung());
                                                                postCha.setDanhSachAnh(post.getDanhSachAnh());
                                                                if (ownerDoc.exists()) {
                                                                    postCha.setHoVaTen(ownerDoc.getString("ho_va_ten"));
                                                                    postCha.setTenDangNhap(ownerDoc.getString("ten_dang_nhap"));
                                                                    postCha.setAnhDaiDien(ownerDoc.getString("anh_dai_dien"));
                                                                }
                                                                post.setPostCha(postCha);
                                                                post.setNoiDung("");
                                                                checkLikeAndAdd(post, temp, pending);
                                                            })
                                                            .addOnFailureListener(e -> checkLikeAndAdd(post, temp, pending));
                                                } else checkLikeAndAdd(post, temp, pending);
                                            })
                                            .addOnFailureListener(e -> { pending[0]--; });
                                }
                            });
                });
    }

    private void checkLikeAndAdd(PostModel post, List<PostModel> temp, int[] pending) {
        if (myUid == null) {
            temp.add(post); pending[0]--;
            if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
            return;
        }
        db.collection("bai_viet").document(post.getDocumentId())
                .collection("luot_thich").document(myUid).get()
                .addOnSuccessListener(likeDoc -> {
                    post.setLikedByMe(likeDoc.exists());
                    temp.add(post); pending[0]--;
                    if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
                })
                .addOnFailureListener(e -> {
                    temp.add(post); pending[0]--;
                    if (pending[0] == 0) { postList.clear(); postList.addAll(temp); postAdapter.notifyDataSetChanged(); }
                });
    }

    // ════════════════════════════════════════════════════════
    //  LOAD BÌNH LUẬN
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
                        String finalPostId = postId;

                        String ownerUid = comment.getNguoiDungId();
                        Runnable finish = () -> {
                            temp.add(comment); pending[0]--;
                            if (pending[0] == 0) { commentList.clear(); commentList.addAll(temp); commentAdapter.notifyDataSetChanged(); }
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
                                        if (myUid != null && finalPostId != null) {
                                            db.collection("bai_viet").document(finalPostId)
                                                    .collection("binh_luan").document(comment.getDocumentId())
                                                    .collection("luot_thich").document(myUid).get()
                                                    .addOnSuccessListener(likeDoc -> { comment.setLikedByMe(likeDoc.exists()); finish.run(); })
                                                    .addOnFailureListener(e -> finish.run());
                                        } else finish.run();
                                    }).addOnFailureListener(e -> finish.run());
                        } else finish.run();
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

    private void setActiveTab(int tab) {
        linePost.setBackgroundColor(Color.parseColor("#1C1C1E"));
        lineRepost.setBackgroundColor(Color.parseColor("#1C1C1E"));
        lineComment.setBackgroundColor(Color.parseColor("#1C1C1E"));
        txtTabPost.setTextColor(Color.parseColor("#636366"));
        txtTabRepost.setTextColor(Color.parseColor("#636366"));
        txtTabComment.setTextColor(Color.parseColor("#636366"));
        switch (tab) {
            case 0: linePost.setBackgroundColor(Color.WHITE);    txtTabPost.setTextColor(Color.WHITE);    break;
            case 1: lineRepost.setBackgroundColor(Color.WHITE);  txtTabRepost.setTextColor(Color.WHITE);  break;
            case 2: lineComment.setBackgroundColor(Color.WHITE); txtTabComment.setTextColor(Color.WHITE); break;
        }
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

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}