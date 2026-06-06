package com.example.doanmxh.ProfilePage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LikedCommentsActivity extends BaseActivity {
    private final List<LikedCommentAdapter.LikedCommentItem> items = new ArrayList<>();
    private LikedCommentAdapter adapter;
    private TextView txtEmpty;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_activity_posts);

        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView txtTitle = findViewById(R.id.txtTitle);
        RecyclerView rvPosts = findViewById(R.id.rvPosts);
        txtEmpty = findViewById(R.id.txtEmpty);
        progressBar = findViewById(R.id.progressBar);

        txtTitle.setText("Bình luận đã thích");
        txtEmpty.setText("Bạn chưa thích bình luận nào.");
        btnBack.setOnClickListener(v -> finish());

        adapter = new LikedCommentAdapter(items, this::openPostDetail);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);

        loadLikedComments();
    }

    private void loadLikedComments() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            showEmpty();
            return;
        }

        db.collectionGroup("binh_luan")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    int[] pending = {snapshot.size()};
                    for (DocumentSnapshot commentDoc : snapshot.getDocuments()) {
                        DocumentReference commentRef = commentDoc.getReference();
                        commentRef.collection("luot_thich")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(likeDoc -> {
                                    if (likeDoc.exists()) {
                                        loadComment(commentDoc, pending);
                                    } else {
                                        finishOne(pending);
                                    }
                                })
                                .addOnFailureListener(e -> finishOne(pending));
                    }
                })
                .addOnFailureListener(e -> {
                    showEmpty();
                    Toast.makeText(this, "Không thể tải bình luận đã thích", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComment(DocumentSnapshot commentDoc, int[] pending) {
        CommentModel comment = commentDoc.toObject(CommentModel.class);
        if (comment == null || commentDoc.getReference().getParent() == null) {
            finishOne(pending);
            return;
        }

        DocumentReference postRef = commentDoc.getReference().getParent().getParent();
        if (postRef == null) {
            finishOne(pending);
            return;
        }

        comment.setDocumentId(commentDoc.getId());
        comment.setPostId(postRef.getId());
        loadPostAndAuthor(comment, postRef, pending);
    }

    private void loadPostAndAuthor(CommentModel comment, DocumentReference postRef, int[] pending) {
        postRef.get()
                .addOnSuccessListener(postDoc -> {
                    if (!postDoc.exists() || Boolean.TRUE.equals(postDoc.getBoolean("da_xoa"))) {
                        finishOne(pending);
                        return;
                    }

                    String postContent = postDoc.getString("noi_dung");
                    enrichAuthorAndAdd(comment, postContent, pending);
                })
                .addOnFailureListener(e -> enrichAuthorAndAdd(comment, "", pending));
    }

    private void enrichAuthorAndAdd(CommentModel comment, String postContent, int[] pending) {
        if (comment.getNguoiDungId() == null || comment.getNguoiDungId().isEmpty()) {
            items.add(new LikedCommentAdapter.LikedCommentItem(comment, postContent));
            finishOne(pending);
            return;
        }

        db.collection("nguoi_dung")
                .document(comment.getNguoiDungId())
                .get()
                .addOnSuccessListener(userDoc -> {
                    comment.setHoVaTen(userDoc.getString("ho_va_ten"));
                    comment.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                    comment.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                    comment.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                    items.add(new LikedCommentAdapter.LikedCommentItem(comment, postContent));
                    finishOne(pending);
                })
                .addOnFailureListener(e -> {
                    items.add(new LikedCommentAdapter.LikedCommentItem(comment, postContent));
                    finishOne(pending);
                });
    }

    private void finishOne(int[] pending) {
        pending[0]--;
        if (pending[0] == 0) {
            progressBar.setVisibility(View.GONE);
            txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.VISIBLE);
    }

    private void openPostDetail(String postId) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        startActivity(intent);
    }
}
