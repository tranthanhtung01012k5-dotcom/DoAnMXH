package com.example.doanmxh.HomePage;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.Mention.MentionHelper;
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
    private final Map<String, CommentModel> userInfoCache = new HashMap<>();
    private PostAdapter postAdapter;
    private CommentAdapter commentAdapter;

    // ── MentionHelper ──────────────────────────────────────────────────────
    private MentionHelper mentionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        enableImmersiveMode();

        getWindow().getDecorView().post(() -> {
            View commentBar = findViewById(R.id.layoutInput);
            if (commentBar == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                commentBar.setOnApplyWindowInsetsListener((v, insets) -> {
                    int imeInset = insets.getInsets(WindowInsets.Type.ime()).bottom;
                    int navInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                            v.getPaddingRight(), Math.max(imeInset, navInset));
                    return insets;
                });
            }
        });

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        db     = FirebaseFirestore.getInstance();
        myUid  = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        rvDetail   = findViewById(R.id.rvDetail);
        edtComment = findViewById(R.id.edtComment);
        btnSend    = findViewById(R.id.btnSend);
        btnBack    = findViewById(R.id.btnBack);
        ivMyAvatar = findViewById(R.id.ivMyAvatar);

        // ── Avatar người dùng hiện tại ────────────────────────────────────
        if (myUid != null) {
            db.collection("nguoi_dung").document(myUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String anh = doc.getString("anh_dai_dien");
                            if (anh != null && !anh.isEmpty())
                                Glide.with(this).load(anh).circleCrop().into(ivMyAvatar);
                        }
                    });
        }

        // ── Khởi tạo MentionHelper — 1 dòng duy nhất ─────────────────────
        mentionHelper = new MentionHelper(this, edtComment, db, null);

        // ════════════════════════════════════════════════════
        //  ADAPTER BÀI VIẾT
        // ════════════════════════════════════════════════════
        postAdapter = new PostAdapter(postWrap, new PostAdapter.OnPostActionListener() {
            @Override public void onLikeClick(PostModel post, int position) {
                handleLikePost(post, position);
            }
            @Override public void onCommentClick(PostModel post, int position) {}
            @Override public void onCommentAvataClick(CommentModel post, int position) {}

            @Override
            public void onRepostClick(PostModel post, int position) {
                if (myUid == null) {
                    Toast.makeText(PostDetailActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }
                new androidx.appcompat.app.AlertDialog.Builder(PostDetailActivity.this)
                        .setTitle("Đăng lại bài viết")
                        .setMessage("Bạn muốn đăng lại bài này?")
                        .setPositiveButton("Đăng lại", (dialog, which) -> {
                            Map<String, Object> repost = new HashMap<>();
                            repost.put("nguoi_dung_id", myUid);
                            repost.put("noi_dung", "");
                            repost.put("ngay_tao", new Date());
                            repost.put("so_like", 0);
                            repost.put("so_binh_luan", 0);
                            repost.put("da_xoa", false);
                            repost.put("hinh_anh", new ArrayList<>());
                            repost.put("bai_viet_cha_id", post.getDocumentId());
                            repost.put("is_repost", true);

                            db.collection("bai_viet").add(repost)
                                    .addOnSuccessListener(ref -> Toast.makeText(PostDetailActivity.this,
                                            "Đã đăng lại bài viết", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(PostDetailActivity.this,
                                            "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override public void onShareClick(PostModel post, int position) {}
            @Override public void onMoreOptionsClick(PostModel post, int position) {}

            @Override
            public void onAvatarClick(PostModel post, int position) {
                Intent intent = new Intent(PostDetailActivity.this, UserProfileActivity.class);
                intent.putExtra("user_uid", post.getNguoiDungId());
                startActivity(intent);
            }

            @Override
            public void onAddFriendClick(PostModel post, int position) {
                if (myUid == null) {
                    Toast.makeText(PostDetailActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                                    .collection("nguoi_theo_doi").document(myUid)
                                    .set(followerEntry);
                            db.collection("nguoi_dung").document(authorUid)
                                    .update("so_nguoi_theo_doi", FieldValue.increment(1));

                            post.setFollowing(true);
                            postAdapter.notifyItemChanged(position);
                            Toast.makeText(PostDetailActivity.this,
                                    "Đã theo dõi @" + post.getTenDangNhap(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(PostDetailActivity.this,
                                "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        // ════════════════════════════════════════════════════
        //  ADAPTER BÌNH LUẬN
        // ════════════════════════════════════════════════════
        commentAdapter = new CommentAdapter(
                commentList, postId,
                new CommentAdapter.OnCommentActionListener() {
                    @Override public void onCommentClick(CommentModel comment, int position) {}

                    @Override
                    public void onReplyClick(CommentModel comment, int position) {
                        Log.d("REPLY_DEBUG", "=== onReplyClick được gọi ===");
                        if (comment == null) return;

                        // Mở bàn phím
                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager)
                                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.showSoftInput(edtComment,
                                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);

                        String tenDangNhap = comment.getTenDangNhap();
                        if (tenDangNhap == null || tenDangNhap.isEmpty()) {
                            if (comment.getNguoiDungId() != null) {
                                db.collection("nguoi_dung").document(comment.getNguoiDungId()).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                comment.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                                setReplyMention(comment);
                                            }
                                        });
                            }
                            return;
                        }
                        setReplyMention(comment);
                    }

                    @Override
                    public void onLikeClick(CommentModel comment, int position) {}

                    @Override
                    public void onAvatarClick(CommentModel comment, int position) {
                        Intent intent = new Intent(PostDetailActivity.this, UserProfileActivity.class);
                        intent.putExtra("user_uid", comment.getNguoiDungId());
                        startActivity(intent);
                    }

                    @Override
                    public void onAddFriendClick(CommentModel comment, int position) {
                        if (myUid == null) {
                            Toast.makeText(PostDetailActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String authorUid = comment.getNguoiDungId();
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
                                            .collection("nguoi_theo_doi").document(myUid)
                                            .set(followerEntry);
                                    db.collection("nguoi_dung").document(authorUid)
                                            .update("so_nguoi_theo_doi", FieldValue.increment(1));

                                    comment.setFollowing(true);
                                    commentAdapter.notifyItemChanged(position);
                                    Toast.makeText(PostDetailActivity.this,
                                            "Đã theo dõi @" + comment.getHoVaTen(), Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(PostDetailActivity.this,
                                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onEditComment(CommentModel comment, int position, String newContent) {
                        Toast.makeText(PostDetailActivity.this, "Đã cập nhật bình luận", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDeleteComment(CommentModel comment, int position) {
                        db.collection("bai_viet").document(postId)
                                .update("so_binh_luan", FieldValue.increment(-1))
                                .addOnSuccessListener(unused -> {
                                    if (currentPost != null) {
                                        currentPost.setSoBinhLuan(
                                                Math.max(0, currentPost.getSoBinhLuan() - 1));
                                        postAdapter.notifyItemChanged(0, "LIKE_UPDATE");
                                    }
                                });
                        Toast.makeText(PostDetailActivity.this, "Đã xóa bình luận", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ConcatAdapter concatAdapter = new ConcatAdapter(postAdapter, commentAdapter);
        rvDetail.setLayoutManager(new LinearLayoutManager(this));
        rvDetail.setAdapter(concatAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendComment());

        loadPost();
        loadComments();
    }

    // ════════════════════════════════════════════════════════
    //  SET MENTION KHI NHẤN NÚT REPLY (không qua popup)
    // ════════════════════════════════════════════════════════
    private void setReplyMention(CommentModel comment) {
        String mention  = "@" + comment.getTenDangNhap();
        String fullText = mention + " ";

        SpannableStringBuilder spannable = new SpannableStringBuilder(fullText);
        spannable.setSpan(new ClickableSpan() {
            @Override public void onClick(@NonNull View widget) {
                String uid = comment.getNguoiDungId();
                if (uid == null || uid.isEmpty()) return;
                Intent intent = new Intent(PostDetailActivity.this, UserProfileActivity.class);
                intent.putExtra("user_uid", uid);
                startActivity(intent);
            }
            @Override public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(Color.parseColor("#4A90E2"));
                ds.setFakeBoldText(true);
                ds.setUnderlineText(false);
            }
        }, 0, mention.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        edtComment.setText(spannable);
        edtComment.setSelection(spannable.length());
        edtComment.requestFocus();
        replyToCommentId = comment.getDocumentId();

        edtComment.postDelayed(() ->
                edtComment.setMovementMethod(new ArrowKeyMovementMethod() {
                    @Override
                    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX();
                            int y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();
                            Layout layout = widget.getLayout();
                            if (layout != null) {
                                int line = layout.getLineForVertical(y);
                                int off  = layout.getOffsetForHorizontal(line, x);
                                ClickableSpan[] spans = buffer.getSpans(off, off, ClickableSpan.class);
                                if (spans.length > 0) { spans[0].onClick(widget); return true; }
                            }
                        }
                        return super.onTouchEvent(widget, buffer, event);
                    }
                }), 300);
    }

    // ════════════════════════════════════════════════════════
    //  LOAD BÀI VIẾT
    // ════════════════════════════════════════════════════════
    private void loadPost() {
        db.collection("bai_viet").document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    PostModel post = doc.toObject(PostModel.class);
                    if (post == null) return;
                    post.setDocumentId(doc.getId());

                    String baiVietChaId = doc.getString("bai_viet_cha_id");
                    if (baiVietChaId != null && !baiVietChaId.isEmpty())
                        post.setBaiVietChaId(baiVietChaId);

                    currentPost = post;
                    String nguoiDungId = post.getNguoiDungId();

                    Runnable finishTask = () -> {
                        postWrap.clear();
                        postWrap.add(currentPost);
                        postAdapter.notifyDataSetChanged();
                    };

                    if (nguoiDungId != null && !nguoiDungId.isEmpty()) {
                        db.collection("nguoi_dung").document(nguoiDungId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        post.setHoVaTen(userDoc.getString("ho_va_ten"));
                                        post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                        post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                        post.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                    }
                                    loadLikeFollowAndRepost(post, finishTask);
                                })
                                .addOnFailureListener(e -> loadLikeFollowAndRepost(post, finishTask));
                    } else {
                        loadLikeFollowAndRepost(post, finishTask);
                    }
                });
    }

    private void loadLikeFollowAndRepost(PostModel post, Runnable onDone) {
        if (myUid == null) {
            post.setLikedByMe(false);
            post.setFollowing(false);
            checkRepost(post, onDone);
            return;
        }
        db.collection("bai_viet").document(post.getDocumentId())
                .collection("luot_thich").document(myUid).get()
                .addOnSuccessListener(likeDoc -> {
                    post.setLikedByMe(likeDoc.exists());
                    String authorUid = post.getNguoiDungId();
                    if (authorUid != null && !authorUid.equals(myUid)) {
                        db.collection("nguoi_dung").document(myUid)
                                .collection("nguoi_dang_theo_doi").document(authorUid).get()
                                .addOnSuccessListener(followDoc -> {
                                    post.setFollowing(followDoc.exists());
                                    checkRepost(post, onDone);
                                })
                                .addOnFailureListener(e -> { post.setFollowing(false); checkRepost(post, onDone); });
                    } else {
                        post.setFollowing(false);
                        checkRepost(post, onDone);
                    }
                })
                .addOnFailureListener(e -> { post.setLikedByMe(false); checkRepost(post, onDone); });
    }

    private void checkRepost(PostModel post, Runnable onDone) {
        String chaId = post.getBaiVietChaId();
        if (chaId == null || chaId.isEmpty()) { onDone.run(); return; }
        loadBaiVietCha(post, chaId, onDone);
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
    //  LOAD BÌNH LUẬN
    // ════════════════════════════════════════════════════════
    private void loadComments() {
        db.collection("bai_viet").document(postId)
                .collection("binh_luan")
                .orderBy("ngay_tao", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<CommentModel> roots = new ArrayList<>();
                    Map<String, List<CommentModel>> replyMap = new HashMap<>();

                    Map<String, CommentModel> oldCommentMap = new HashMap<>();
                    for (CommentModel c : commentList) oldCommentMap.put(c.getDocumentId(), c);
                    commentList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        CommentModel comment = new CommentModel();
                        comment.setDocumentId(doc.getId());
                        comment.setNoiDung(doc.getString("noi_dung"));
                        comment.setNguoiDungId(doc.getString("nguoi_dung_id"));
                        comment.setBinhLuanChaId(doc.getString("binh_luan_cha_id"));

                        if (doc.getTimestamp("ngay_tao") != null)
                            comment.setNgayTao(doc.getTimestamp("ngay_tao"));

                        Long soLikeLong = doc.getLong("so_like");
                        comment.setSoLike(soLikeLong != null ? soLikeLong.intValue() : 0);

                        CommentModel old = oldCommentMap.get(doc.getId());
                        if (old != null) {
                            comment.setLikedByMe(old.isLikedByMe());
                            comment.setFollowing(old.isFollowing());
                        }

                        String uid = comment.getNguoiDungId();
                        CommentModel cached = uid != null ? userInfoCache.get(uid) : null;

                        if (cached != null) {
                            comment.setHoVaTen(cached.getHoVaTen());
                            comment.setAnhDaiDien(cached.getAnhDaiDien());
                            comment.setTenDangNhap(cached.getTenDangNhap());
                            comment.setVerified(cached.isVerified());
                            if (old == null && myUid != null)
                                loadLikeAndFollow(comment, doc.getId(), uid);
                        } else if (uid != null && !uid.isEmpty()) {
                            final CommentModel finalComment = comment;
                            db.collection("nguoi_dung").document(uid).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            finalComment.setHoVaTen(userDoc.getString("ho_va_ten"));
                                            finalComment.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                            finalComment.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                            finalComment.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                            userInfoCache.put(uid, finalComment);
                                        }
                                        int idx = commentList.indexOf(finalComment);
                                        if (idx >= 0) commentAdapter.notifyItemChanged(idx);
                                    });
                            if (myUid != null) loadLikeAndFollow(comment, doc.getId(), uid);
                        }

                        String parentId = comment.getBinhLuanChaId();
                        if (parentId == null || parentId.isEmpty()) roots.add(comment);
                        else replyMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(comment);
                    }

                    for (CommentModel root : roots) {
                        commentList.add(root);
                        addRepliesRecursive(root.getDocumentId(), replyMap);
                    }
                    commentAdapter.notifyDataSetChanged();
                });
    }

    private void loadLikeAndFollow(CommentModel comment, String commentId, String authorUid) {
        db.collection("bai_viet").document(postId)
                .collection("binh_luan").document(commentId)
                .collection("luot_thich").document(myUid).get()
                .addOnSuccessListener(likeDoc -> {
                    comment.setLikedByMe(likeDoc.exists());
                    int idx = commentList.indexOf(comment);
                    if (idx >= 0) commentAdapter.notifyItemChanged(idx);
                });
        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_dang_theo_doi").document(authorUid).get()
                .addOnSuccessListener(followDoc -> {
                    comment.setFollowing(followDoc.exists());
                    int idx = commentList.indexOf(comment);
                    if (idx >= 0) commentAdapter.notifyItemChanged(idx);
                });
    }

    private void addRepliesRecursive(String parentId,
                                     Map<String, List<CommentModel>> replyMap) {
        List<CommentModel> replies = replyMap.get(parentId);
        if (replies == null) return;
        for (CommentModel reply : replies) {
            commentList.add(reply);
            addRepliesRecursive(reply.getDocumentId(), replyMap);
        }
    }

    // ════════════════════════════════════════════════════════
    //  GỬI BÌNH LUẬN
    // ════════════════════════════════════════════════════════
    private void sendComment() {
        String noiDung = edtComment.getText().toString().trim();
        if (noiDung.isEmpty()) return;

        if (myUid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> comment = new HashMap<>();
        comment.put("nguoi_dung_id", myUid);
        comment.put("noi_dung", noiDung);
        comment.put("ngay_tao", new Date());
        comment.put("binh_luan_cha_id", replyToCommentId != null ? replyToCommentId : "");
        comment.put("so_like", 0);

        db.collection("bai_viet").document(postId)
                .collection("binh_luan").add(comment)
                .addOnSuccessListener(ref -> {
                    edtComment.setText("");
                    replyToCommentId = null;

                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(edtComment.getWindowToken(), 0);
                    edtComment.clearFocus();

                    db.collection("bai_viet").document(postId)
                            .update("so_binh_luan", FieldValue.increment(1));

                    if (currentPost != null) {
                        currentPost.setSoBinhLuan(currentPost.getSoBinhLuan() + 1);
                        postAdapter.notifyItemChanged(0, "LIKE_UPDATE");
                    }
                });
    }

    // ════════════════════════════════════════════════════════
    //  LIKE BÀI VIẾT
    // ════════════════════════════════════════════════════════
    private void handleLikePost(PostModel post, int position) {
        if (myUid == null) return;
        db.collection("bai_viet").document(postId)
                .collection("luot_thich").document(myUid).get()
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
                                            .collection("luot_thich").document(myUid).set(likeData);
                                    db.collection("bai_viet").document(postId)
                                            .update("so_like", FieldValue.increment(1));
                                    post.setLikedByMe(true);
                                    post.setSoLuotThich(post.getSoLuotThich() + 1);
                                    postAdapter.notifyItemChanged(0, "LIKE_UPDATE");
                                });
                    }
                });
    }

    // ════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mentionHelper != null) mentionHelper.dismiss(); // tránh window leak
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}