package com.example.doanmxh.ProfilePage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.HomePage.PostAdapter;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SavedPostsActivity extends BaseActivity {

    private final List<PostModel> postList = new ArrayList<>();

    private PostAdapter postAdapter;
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

        txtTitle.setText("Đã lưu");
        txtEmpty.setText("Bạn chưa lưu bài viết nào.");

        btnBack.setOnClickListener(v -> finish());

        postAdapter = new PostAdapter(postList, null);

        postAdapter.setOnItemClickListener(postId -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
            startActivity(intent);
        });

        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);

        loadSavedPosts();
    }

    private void loadSavedPosts() {

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            showEmpty();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);

        postList.clear();

        db.collection("nguoi_dung")
                .document(uid)
                .collection("bai_viet_luu_tru")
                .orderBy("ngay_luu", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {

                        progressBar.setVisibility(View.GONE);
                        txtEmpty.setVisibility(View.VISIBLE);
                        postAdapter.notifyDataSetChanged();
                        return;
                    }

                    int total = snapshot.size();
                    int[] loaded = {0};

                    for (DocumentSnapshot savedDoc : snapshot.getDocuments()) {

                        String postId = savedDoc.getId();

                        db.collection("bai_viet")
                                .document(postId)
                                .get()
                                .addOnSuccessListener(postDoc -> {

                                    if (postDoc.exists()
                                            && !Boolean.TRUE.equals(postDoc.getBoolean("da_xoa"))) {

                                        PostModel post =
                                                postDoc.toObject(PostModel.class);

                                        if (post != null) {

                                            post.setDocumentId(postDoc.getId());

                                            String ownerUid =
                                                    postDoc.getString("nguoi_dung_id");

                                            if (ownerUid != null) {

                                                db.collection("nguoi_dung")
                                                        .document(ownerUid)
                                                        .get()
                                                        .addOnSuccessListener(userDoc -> {

                                                            post.setHoVaTen(
                                                                    userDoc.getString("ho_va_ten"));

                                                            post.setTenDangNhap(
                                                                    userDoc.getString("ten_dang_nhap"));

                                                            post.setAnhDaiDien(
                                                                    userDoc.getString("anh_dai_dien"));

                                                            post.setVerified(
                                                                    Boolean.TRUE.equals(
                                                                            userDoc.getBoolean("verified")));

                                                            postList.add(post);

                                                            loaded[0]++;

                                                            if (loaded[0] == total) {
                                                                finishLoading();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {

                                                            loaded[0]++;

                                                            if (loaded[0] == total) {
                                                                finishLoading();
                                                            }
                                                        });

                                            } else {

                                                postList.add(post);

                                                loaded[0]++;

                                                if (loaded[0] == total) {
                                                    finishLoading();
                                                }
                                            }

                                            return;
                                        }
                                    }

                                    loaded[0]++;

                                    if (loaded[0] == total) {
                                        finishLoading();
                                    }

                                })
                                .addOnFailureListener(e -> {

                                    loaded[0]++;

                                    if (loaded[0] == total) {
                                        finishLoading();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);
                    txtEmpty.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            this,
                            "Không thể tải bài viết đã lưu",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void finishLoading() {

        progressBar.setVisibility(View.GONE);

        txtEmpty.setVisibility(
                postList.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        postAdapter.notifyDataSetChanged();
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.VISIBLE);
    }
}