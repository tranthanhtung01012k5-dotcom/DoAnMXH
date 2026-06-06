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
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LikedPostsActivity extends BaseActivity {
    private final List<ActivityPostAdapter.ActivityPostItem> items = new ArrayList<>();
    private ActivityPostAdapter adapter;
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

        txtTitle.setText("Bài viết đã thích");
        txtEmpty.setText("Bạn chưa thích bài viết nào.");
        btnBack.setOnClickListener(v -> finish());

        adapter = new ActivityPostAdapter(items, false, this::openPostDetail);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);

        loadLikedPosts();
    }

    private void loadLikedPosts() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            showEmpty();
            return;
        }

        db.collection("bai_viet")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, DocumentReference> postRefs = new LinkedHashMap<>();
                    int[] pending = {snapshot.size()};

                    if (pending[0] == 0) {
                        showEmpty();
                        return;
                    }

                    for (DocumentSnapshot postDoc : snapshot.getDocuments()) {
                        if (Boolean.TRUE.equals(postDoc.getBoolean("da_xoa"))) {
                            finishLikeLookup(pending, postRefs);
                            continue;
                        }

                        DocumentReference postRef = postDoc.getReference();
                        postRef.collection("luot_thich")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(likeDoc -> {
                                    if (likeDoc.exists()) {
                                        postRefs.put(postRef.getId(), postRef);
                                    }
                                    finishLikeLookup(pending, postRefs);
                                })
                                .addOnFailureListener(e -> finishLikeLookup(pending, postRefs));
                    }
                })
                .addOnFailureListener(e -> {
                    showEmpty();
                    Toast.makeText(this, "Không thể tải bài viết đã thích", Toast.LENGTH_SHORT).show();
                });
    }

    private void finishLikeLookup(int[] pending, Map<String, DocumentReference> postRefs) {
        pending[0]--;
        if (pending[0] != 0) return;

        if (postRefs.isEmpty()) {
            showEmpty();
            return;
        }

        loadPosts(postRefs);
    }

    private void loadPosts(Map<String, DocumentReference> postRefs) {
        int[] pending = {postRefs.size()};
        for (DocumentReference postRef : postRefs.values()) {
            postRef.get()
                    .addOnSuccessListener(postDoc -> {
                        PostModel post = postDoc.toObject(PostModel.class);
                        if (post == null || post.isDaXoa()) {
                            finishOne(pending);
                            return;
                        }

                        post.setDocumentId(postDoc.getId());
                        post.setLikedByMe(true);
                        enrichAuthorAndAdd(post, pending);
                    })
                    .addOnFailureListener(e -> finishOne(pending));
        }
    }

    private void enrichAuthorAndAdd(PostModel post, int[] pending) {
        if (post.getNguoiDungId() == null || post.getNguoiDungId().isEmpty()) {
            items.add(new ActivityPostAdapter.ActivityPostItem(post, null));
            finishOne(pending);
            return;
        }

        db.collection("nguoi_dung")
                .document(post.getNguoiDungId())
                .get()
                .addOnSuccessListener(userDoc -> {
                    post.setHoVaTen(userDoc.getString("ho_va_ten"));
                    post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                    post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                    post.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                    items.add(new ActivityPostAdapter.ActivityPostItem(post, null));
                    finishOne(pending);
                })
                .addOnFailureListener(e -> {
                    items.add(new ActivityPostAdapter.ActivityPostItem(post, null));
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
