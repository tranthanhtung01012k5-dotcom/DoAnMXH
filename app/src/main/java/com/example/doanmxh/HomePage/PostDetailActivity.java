package com.example.doanmxh.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";
    private String replyToCommentId = null;
    private RecyclerView rvDetail;
    private EditText edtComment;
    private ImageButton btnSend, btnBack;
    private ShapeableImageView ivMyAvatar;

    private FirebaseFirestore db;
    private String postId;
    private String myUid;

    private PostModel currentPost;
    private List<PostModel> postWrap = new ArrayList<>();
    private List<CommentModel> commentList = new ArrayList<>();

    private PostAdapter postAdapter;
    private CommentAdapter commentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        rvDetail = findViewById(R.id.rvDetail);
        edtComment = findViewById(R.id.edtComment);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        ivMyAvatar = findViewById(R.id.ivMyAvatar);

        // ── Avatar người dùng hiện tại ──
        if (myUid != null) {
            db.collection("nguoi_dung").document(myUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String anh = doc.getString("anh_dai_dien");
                            if (anh != null && !anh.isEmpty()) {
                                Glide.with(this).load(anh).circleCrop().into(ivMyAvatar);
                            }
                        }
                    });
        }

        // ── Adapter bài viết ──
        postAdapter = new PostAdapter(postWrap, new PostAdapter.OnPostActionListener() {
            @Override
            public void onLikeClick(PostModel post, int position) {
                handleLikePost(post, position);
            }

            @Override
            public void onCommentClick(PostModel post, int position) {
            }
            @Override
            public void onCommentAvataClick(CommentModel post, int position) {
            }

            @Override
            public void onRepostClick(PostModel post, int position) {
            }

            @Override
            public void onShareClick(PostModel post, int position) {
            }

            @Override
            public void onMoreOptionsClick(PostModel post, int position) {
            }

            @Override
            public void onAvatarClick(PostModel post, int position) {
                Intent intent = new Intent(PostDetailActivity.this, UserProfileActivity.class);
                intent.putExtra("user_uid", post.getNguoiDungId());
                Log.d("UserID" ,post.getNguoiDungId());
                startActivity(intent);
            }

            @Override
            public void onAddFriendClick(PostModel post, int position) {
                if (myUid == null) {
                    Toast.makeText(PostDetailActivity.this,
                            "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                String authorUid = post.getNguoiDungId();

                if (authorUid == null || authorUid.equals(myUid)) return;

                // Chỉ lưu trong subcollection của user
                Map<String, Object> followerEntry = new HashMap<>();
                followerEntry.put("nguoi_dung_id", myUid);
                followerEntry.put("ngay_theo_doi", new Date());

                db.collection("nguoi_dung")
                        .document(authorUid)
                        .collection("nguoi_theo_doi")
                        .document(myUid)
                        .set(followerEntry)
                        .addOnSuccessListener(unused -> {

                            // tăng số follower của tác giả
                            db.collection("nguoi_dung")
                                    .document(authorUid)
                                    .update("so_nguoi_theo_doi",
                                            FieldValue.increment(1));

                            Map<String, Object> followingEntry = new HashMap<>();
                            followingEntry.put("nguoi_dung_id", authorUid);
                            followingEntry.put("ngay_theo_doi", new Date());

                            // lưu danh sách đang theo dõi
                            db.collection("nguoi_dung")
                                    .document(myUid)
                                    .collection("nguoi_dang_theo_doi")
                                    .document(authorUid)
                                    .set(followingEntry);

                            // tăng số đang theo dõi
                            db.collection("nguoi_dung")
                                    .document(myUid)
                                    .update("so_nguoi_dang_theo_doi",
                                            FieldValue.increment(1));

                            post.setFollowing(true);
                            postAdapter.notifyItemChanged(position);

                            Toast.makeText(PostDetailActivity.this,
                                    "Đã theo dõi @" + post.getTenDangNhap(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // ── Adapter bình luận ──
        commentAdapter = new CommentAdapter(
                commentList,
                postId,
                new CommentAdapter.OnCommentActionListener() {

                    @Override
                    public void onReplyClick(CommentModel comment, int position) {
                        edtComment.setText("@" + comment.getHoVaTen() + " ");
                        edtComment.setSelection(edtComment.getText().length());
                        edtComment.requestFocus();
                        replyToCommentId = comment.getDocumentId();
                    }

                    @Override
                    public void onLikeClick(CommentModel comment, int position) {
                        // xử lý nếu cần
                    }

                    @Override
                    public void onAvatarClick(CommentModel comment, int position) {
                        Intent intent = new Intent(
                                PostDetailActivity.this,
                                UserProfileActivity.class
                        );
                        intent.putExtra("user_uid", comment.getNguoiDungId());
                        startActivity(intent);
                    }

                    @Override
                    public void onAddFriendClick(CommentModel comment, int position) {
                        if (myUid == null) {
                            Toast.makeText(PostDetailActivity.this,
                                    "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String authorUid = comment.getNguoiDungId();
                        if (authorUid == null || authorUid.equals(myUid)) return;

                        String docId = myUid + "_" + authorUid;

                        Map<String, Object> followData = new HashMap<>();
                        followData.put("nguoi_theo_doi_id", myUid);
                        followData.put("nguoi_duoc_theo_doi_id", authorUid);
                        followData.put("ngay_theo_doi", new Date());

                        db.collection("theo_doi").document(docId)
                                .set(followData)
                                .addOnSuccessListener(unused -> {
                                    // Tăng số người theo dõi của tác giả
                                    db.collection("nguoi_dung")
                                            .document(authorUid)
                                            .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));

                                    Map<String, Object> followerEntry = new HashMap<>();
                                    followerEntry.put("nguoi_dung_id", myUid);
                                    followerEntry.put("ngay_theo_doi", new Date());
                                    db.collection("nguoi_dung")
                                            .document(authorUid)
                                            .collection("nguoi_dang_theo_doi")
                                            .document(myUid)
                                            .set(followerEntry);

                                    // Tăng số người đang theo dõi của người dùng hiện tại
                                    db.collection("nguoi_dung")
                                            .document(myUid)
                                            .update("so_nguoi_theo_doi", FieldValue.increment(1));

                                    Map<String, Object> followingEntry = new HashMap<>();
                                    followingEntry.put("nguoi_dung_id", authorUid);
                                    followingEntry.put("ngay_theo_doi", new Date());
                                    db.collection("nguoi_dung")
                                            .document(myUid)
                                            .collection("nguoi_theo_doi")
                                            .document(authorUid)
                                            .set(followingEntry);

                                    comment.setFollowing(true);
                                    commentAdapter.notifyItemChanged(position);
                                    Toast.makeText(PostDetailActivity.this,
                                            "Đã theo dõi @" + comment.getHoVaTen(),
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(PostDetailActivity.this,
                                                "Lỗi: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                        Log.d("FOLLOW_CLICK", "clicked user: " + authorUid);
                    }
                }   // ← đóng new CommentAdapter.OnCommentActionListener()
        );          // ← đóng new CommentAdapter(...)

        // ── ConcatAdapter — bài viết ở trên, bình luận ở dưới ──
        ConcatAdapter concatAdapter = new ConcatAdapter(postAdapter, commentAdapter);
        rvDetail.setLayoutManager(new LinearLayoutManager(this));
        rvDetail.setAdapter(concatAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendComment());

        loadPost();
        loadComments();
    }

    private void loadPost() {
        db.collection("bai_viet")
                .document(postId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    currentPost = doc.toObject(PostModel.class);
                    currentPost.setDocumentId(doc.getId());

                    String nguoiDungId = doc.getString("nguoi_dung_id");
                    if (nguoiDungId == null) return;

                    db.collection("nguoi_dung")
                            .document(nguoiDungId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    currentPost.setHoVaTen(userDoc.getString("ho_va_ten"));
                                    currentPost.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                    currentPost.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                    currentPost.setVerified(
                                            Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                }

                                if (myUid != null) {
                                    db.collection("bai_viet")
                                            .document(postId)
                                            .collection("luot_thich")
                                            .document(myUid)
                                            .get()
                                            .addOnSuccessListener(likeDoc -> {
                                                currentPost.setLikedByMe(likeDoc.exists());

                                                String authorUid = currentPost.getNguoiDungId();
                                                if (authorUid != null && authorUid.equals(myUid)) {
                                                    currentPost.setFollowing(true);
                                                    postWrap.clear();
                                                    postWrap.add(currentPost);
                                                    postAdapter.notifyDataSetChanged();
                                                    return;
                                                }

                                                db.collection("theo_doi")
                                                        .document(myUid + "_" + authorUid)
                                                        .get()
                                                        .addOnSuccessListener(followDoc -> {
                                                            currentPost.setFollowing(followDoc.exists());
                                                            postWrap.clear();
                                                            postWrap.add(currentPost);
                                                            postAdapter.notifyDataSetChanged();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            currentPost.setFollowing(false);
                                                            postWrap.clear();
                                                            postWrap.add(currentPost);
                                                            postAdapter.notifyDataSetChanged();
                                                        });
                                            });
                                } else {
                                    currentPost.setFollowing(false);
                                    postWrap.clear();
                                    postWrap.add(currentPost);
                                    postAdapter.notifyDataSetChanged();
                                }
                            });
                });
    }

    private void loadComments() {
        db.collection("bai_viet")
                .document(postId)
                .collection("binh_luan")
                .orderBy("ngay_tao", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) return;

                    List<CommentModel> roots = new ArrayList<>();
                    Map<String, List<CommentModel>> replyMap = new HashMap<>();

                    // ✅ Giữ lại map comment cũ để preserve avatar/tên
                    Map<String, CommentModel> oldCommentMap = new HashMap<>();
                    for (CommentModel c : commentList) {
                        oldCommentMap.put(c.getDocumentId(), c);
                    }

                    commentList.clear();

                    List<CommentModel> allNewComments = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {

                        CommentModel comment = new CommentModel();
                        comment.setDocumentId(doc.getId());
                        comment.setNoiDung(doc.getString("noi_dung"));
                        comment.setNguoiDungId(doc.getString("nguoi_dung_id"));
                        comment.setBinhLuanChaId(doc.getString("binh_luan_cha_id"));

                        if (doc.getTimestamp("ngay_tao") != null) {
                            comment.setNgayTao(doc.getTimestamp("ngay_tao"));
                        }

                        Long soLikeLong = doc.getLong("so_like");
                        comment.setSoLike(soLikeLong != null ? soLikeLong.intValue() : 0);

                        // ✅ Lấy lại avatar/tên/like/follow từ comment cũ nếu có
                        CommentModel old = oldCommentMap.get(doc.getId());
                        if (old != null) {
                            comment.setHoVaTen(old.getHoVaTen());
                            comment.setAnhDaiDien(old.getAnhDaiDien());
                            comment.setLikedByMe(old.isLikedByMe());
                            comment.setFollowing(old.isFollowing());
                        } else {
                            comment.setLikedByMe(false);
                            comment.setFollowing(false);

                            // ✅ Chỉ load user info nếu là comment MỚI (chưa có trong oldMap)
                            String uid = comment.getNguoiDungId();
                            if (uid != null && !uid.isEmpty()) {
                                final CommentModel finalComment = comment;
                                db.collection("nguoi_dung").document(uid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                finalComment.setHoVaTen(userDoc.getString("ho_va_ten"));
                                                finalComment.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                            }
                                            // ✅ Notify đúng index
                                            int idx = commentList.indexOf(finalComment);
                                            if (idx >= 0) commentAdapter.notifyItemChanged(idx);
                                        });

                                if (myUid != null) {
                                    db.collection("bai_viet").document(postId)
                                            .collection("binh_luan").document(doc.getId())
                                            .collection("luot_thich").document(myUid).get()
                                            .addOnSuccessListener(likeDoc -> {
                                                finalComment.setLikedByMe(likeDoc.exists());
                                                int idx = commentList.indexOf(finalComment);
                                                if (idx >= 0) commentAdapter.notifyItemChanged(idx);
                                            });

                                    db.collection("theo_doi")
                                            .document(myUid + "_" + uid).get()
                                            .addOnSuccessListener(followDoc -> {
                                                finalComment.setFollowing(followDoc.exists());
                                                int idx = commentList.indexOf(finalComment);
                                                if (idx >= 0) commentAdapter.notifyItemChanged(idx);
                                            });
                                }
                            }
                        }

                        allNewComments.add(comment);

                        String parentId = comment.getBinhLuanChaId();
                        if (parentId == null || parentId.isEmpty()) {
                            roots.add(comment);
                        } else {
                            if (!replyMap.containsKey(parentId)) {
                                replyMap.put(parentId, new ArrayList<>());
                            }
                            replyMap.get(parentId).add(comment);
                        }
                    }

                    // Build final list
                    // Thay đoạn "Build final list" cũ bằng đoạn này

// Build final list - đệ quy để hiện đủ 3 lớp
                    for (CommentModel root : roots) {
                        commentList.add(root);
                        addRepliesRecursive(root.getDocumentId(), replyMap);
                    }

                    commentAdapter.notifyDataSetChanged();
                });
    }
    private void addRepliesRecursive(
            String parentId,
            Map<String, List<CommentModel>> replyMap
    ) {
        List<CommentModel> replies = replyMap.get(parentId);
        if (replies == null) return;

        for (CommentModel reply : replies) {
            commentList.add(reply);
            // Tiếp tục đệ quy xuống lá tiếp theo
            addRepliesRecursive(reply.getDocumentId(), replyMap);
        }
    }
    private void sendComment() {

        String noiDung =
                edtComment.getText().toString().trim();

        if (noiDung.isEmpty()) return;

        if (myUid == null) {

            Toast.makeText(this,
                    "Vui lòng đăng nhập",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        Map<String, Object> comment =
                new HashMap<>();

        comment.put("nguoi_dung_id", myUid);

        comment.put("noi_dung", noiDung);

        comment.put("ngay_tao", new Date());

        comment.put(
                "binh_luan_cha_id",
                replyToCommentId != null
                        ? replyToCommentId
                        : ""
        );

        comment.put("so_like", 0);

        db.collection("bai_viet")
                .document(postId)
                .collection("binh_luan")
                .add(comment)

                .addOnSuccessListener(ref -> {

                    edtComment.setText("");

                    replyToCommentId = null;

                    db.collection("bai_viet")
                            .document(postId)
                            .update(
                                    "so_binh_luan",
                                    FieldValue.increment(1)
                            );

                    if (currentPost != null) {

                        currentPost.setSoBinhLuan(
                                currentPost.getSoBinhLuan() + 1
                        );

                        postAdapter.notifyItemChanged(0, "LIKE_UPDATE");
                    }
                });
    }

    private void handleLikePost(PostModel post, int position) {
        if (myUid == null) return;

        db.collection("bai_viet").document(postId)
                .collection("luot_thich").document(myUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        doc.getReference().delete();
                        db.collection("bai_viet").document(postId)
                                .update("so_like", FieldValue.increment(-1));
                        post.setLikedByMe(false);
                        post.setSoLuotThich(Math.max(0, post.getSoLuotThich() - 1));
                        postAdapter.notifyItemChanged(0, "LIKE_UPDATE");
                    } else {
                        db.collection("nguoi_dung").document(myUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    Map<String, Object> likeData = new HashMap<>();
                                    likeData.put("nguoi_dung_id", myUid);
                                    likeData.put("ho_va_ten",
                                            userDoc.exists() ? userDoc.getString("ho_va_ten") : "Ẩn danh");
                                    likeData.put("ngay_like", new Date());

                                    db.collection("bai_viet").document(postId)
                                            .collection("luot_thich").document(myUid)
                                            .set(likeData);
                                    db.collection("bai_viet").document(postId)
                                            .update("so_like", FieldValue.increment(1));
                                    post.setLikedByMe(true);
                                    post.setSoLuotThich(post.getSoLuotThich() + 1);
                                    postAdapter.notifyItemChanged(0, "LIKE_UPDATE");
                                });
                    }
                });
    }
}