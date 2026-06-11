package com.example.doanmxh.Search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.HomePage.PostAdapter;
import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.HomePage.PostOptionBottomSheet;
import com.example.doanmxh.HomePage.ShareBottom;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchResultActivity extends AppCompatActivity {

    private RecyclerView          rvRelatedProfiles, rvTopPosts;
    private TextView              edtSearch;
    private FirebaseFirestore     db;
    private RelatedProfileAdapter userAdapter;
    private PostAdapter           postAdapter;

    private ArrayList<User>      userList = new ArrayList<>();
    private ArrayList<PostModel> postList = new ArrayList<>();
    private final Set<String> repostedByMe = new HashSet<>();
    private final Set<String> processingLikes = new HashSet<>();
    private String keyword;
    private ListenerRegistration listenerRegistration;
    private ImageView btnCancel;
    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        keyword = getIntent().getStringExtra("keyword");
        db      = FirebaseFirestore.getInstance();

        initViews();
        loadRelatedProfiles();
        loadTopPosts();
        btnCancel.setOnClickListener(v -> {
            finish();
        });
    }

    private void initViews() {
        edtSearch         = findViewById(R.id.edtSearch);
        rvRelatedProfiles = findViewById(R.id.rvRelatedProfiles);
        rvTopPosts        = findViewById(R.id.rvTopPosts);
        btnCancel         = findViewById(R.id.btnCancel);
        if (keyword != null) edtSearch.setText(keyword);

// Trong initViews(), thay dòng edtSearch.setOnClickListener cũ bằng:
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                keyword = s.toString().trim();

                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                searchRunnable = () -> {
                    postList.clear();
                    postAdapter.notifyDataSetChanged();
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                    loadRelatedProfiles();
                    loadTopPosts();
                };

                searchHandler.postDelayed(searchRunnable, 500); // đợi 500ms sau khi ngừng gõ mới search
            }
        });
        userAdapter = new RelatedProfileAdapter(userList);
        rvRelatedProfiles.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRelatedProfiles.setAdapter(userAdapter);

        // PostAdapter cần listener — truyền null nếu không xử lý action
        postAdapter = new PostAdapter(postList, new PostAdapter.OnPostActionListener()
        {
            @Override
            public void onLikeClick(PostModel post, int position) {

            String docId = post.getDocumentId();

            if (processingLikes.contains(docId)) return;
            processingLikes.add(docId);

            if (SearchResultActivity.this == null) {
                processingLikes.remove(docId);
                return;
            }

            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (currentUid == null) {
                processingLikes.remove(docId);
                Toast.makeText(SearchResultActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            // Luôn lấy post mới nhất từ postList theo documentId
            PostModel currentPost = null;
            int currentPosition = -1;
            for (int i = 0; i < postList.size(); i++) {
                if (docId.equals(postList.get(i).getDocumentId())) {
                    currentPost = postList.get(i);
                    currentPosition = i;
                    break;
                }
            }

            if (currentPost == null) {
                processingLikes.remove(docId);
                return;
            }

            final PostModel finalPost = currentPost;
            final int finalPosition = currentPosition;

            db.collection("bai_viet")
                    .document(docId)
                    .collection("luot_thich")
                    .document(currentUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            db.collection("bai_viet")
                                    .document(docId)
                                    .collection("luot_thich")
                                    .document(currentUid)
                                    .delete()
                                    .addOnSuccessListener(unused -> {
                                        db.collection("bai_viet")
                                                .document(docId)
                                                .update("so_like", FieldValue.increment(-1));
                                        finalPost.setLikedByMe(false);
                                        finalPost.setSoLuotThich(Math.max(0, finalPost.getSoLuotThich() - 1));
                                        postAdapter.notifyItemChanged(finalPosition, "LIKE_UPDATE");
                                        processingLikes.remove(docId);
                                    })
                                    .addOnFailureListener(e -> Log.e("HomeFragment", "Lỗi unlike: " + e.getMessage()))
                                    .addOnCompleteListener(t -> processingLikes.remove(docId)); // ✅ mở khóa
                        } else {
                            db.collection("nguoi_dung")
                                    .document(currentUid)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        String hoVaTen = userDoc.exists()
                                                ? userDoc.getString("ho_va_ten") : "Ẩn danh";

                                        Map<String, Object> likeData = new HashMap<>();
                                        likeData.put("nguoi_dung_id", currentUid);
                                        likeData.put("ho_va_ten", hoVaTen);
                                        likeData.put("ngay_like", new Date());

                                        db.collection("bai_viet")
                                                .document(docId)
                                                .collection("luot_thich")
                                                .document(currentUid)
                                                .set(likeData)
                                                .addOnSuccessListener(unused -> {
                                                    db.collection("bai_viet")
                                                            .document(docId)
                                                            .update("so_like", FieldValue.increment(1));
                                                    finalPost.setLikedByMe(true);
                                                    finalPost.setSoLuotThich(finalPost.getSoLuotThich() + 1);
                                                    postAdapter.notifyItemChanged(finalPosition, "LIKE_UPDATE");
                                                    processingLikes.remove(docId);
                                                })
                                                .addOnFailureListener(e -> Log.e("HomeFragment", "Lỗi like: " + e.getMessage()))
                                                .addOnCompleteListener(t -> processingLikes.remove(docId)); // ✅ mở khóa
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("HomeFragment", "Lỗi load user khi like: " + e.getMessage());
                                        processingLikes.remove(docId); // ✅ mở khóa khi lỗi
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HomeFragment", "Lỗi like: " + e.getMessage());
                        processingLikes.remove(docId); // ✅ mở khóa khi lỗi
                    });
        }

            @Override
            public void onAvatarClick(PostModel post, int position) {

            if (SearchResultActivity.this == null) return;

            Intent intent =
                    new Intent(SearchResultActivity.this,
                            UserProfileActivity.class);

            intent.putExtra("user_uid",
                    post.getNguoiDungId());
            Log.d("CLICK_AVATA", "user_uid: " + post.getNguoiDungId());
            startActivity(intent);
        }

            @Override
            public void onAddFriendClick(PostModel post, int position) {

            if (SearchResultActivity.this == null) return;

            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (currentUid == null) {
                Toast.makeText(SearchResultActivity.this,
                        "Vui lòng đăng nhập",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String authorUid = post.getNguoiDungId();

            if (authorUid == null || authorUid.equals(currentUid)) return;

            // ── Người theo dõi của tác giả ──
            Map<String, Object> followerEntry = new HashMap<>();
            followerEntry.put("nguoi_dung_id", currentUid);
            followerEntry.put("ngay_theo_doi", new Date());

            db.collection("nguoi_dung")
                    .document(authorUid)
                    .collection("nguoi_theo_doi")
                    .document(currentUid)
                    .set(followerEntry)
                    .addOnSuccessListener(unused -> {

                        if (SearchResultActivity.this == null) return;

                        // tăng follower cho tác giả
                        db.collection("nguoi_dung")
                                .document(authorUid)
                                .update("so_nguoi_theo_doi",
                                        FieldValue.increment(1));

                        // ── Người hiện tại đang theo dõi ai ──
                        Map<String, Object> followingEntry = new HashMap<>();
                        followingEntry.put("nguoi_dung_id", authorUid);
                        followingEntry.put("ngay_theo_doi", new Date());

                        db.collection("nguoi_dung")
                                .document(currentUid)
                                .collection("nguoi_dang_theo_doi")
                                .document(authorUid)
                                .set(followingEntry);

                        // tăng số đang theo dõi
                        db.collection("nguoi_dung")
                                .document(currentUid)
                                .update("so_nguoi_dang_theo_doi",
                                        FieldValue.increment(1));

                        // cập nhật UI
                        Toast.makeText(SearchResultActivity.this,
                                "Đã theo dõi @" + post.getTenDangNhap(),
                                Toast.LENGTH_SHORT).show();

//                            String authorUid = post.getNguoiDungId();

                        for (int i = 0; i < postList.size(); i++) {
                            PostModel item = postList.get(i);

                            if (authorUid.equals(item.getNguoiDungId())) {
                                item.setFollowing(true);
                                postAdapter.notifyItemChanged(i);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {

                        if (SearchResultActivity.this == null) return;

                        Toast.makeText(SearchResultActivity.this,
                                "Lỗi: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }

            @Override
            public void onCommentClick(PostModel post, int position) {
            Intent intent = new Intent(SearchResultActivity.this, PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getDocumentId());
            startActivity(intent);
        }

            @Override
            public void onRepostClick(PostModel post, int position) {
            if (SearchResultActivity.this == null) return;

            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

            if (currentUid == null) {
                Toast.makeText(SearchResultActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            String originalDocId = post.getDocumentId();

            // ── Nếu đã repost → hỏi có muốn hủy không ──
            if (repostedByMe.contains(originalDocId)) {
                new androidx.appcompat.app.AlertDialog.Builder(SearchResultActivity.this)
                        .setTitle("Hủy đăng lại")
                        .setMessage("Bạn muốn xóa bài đăng lại này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {

                            // Tìm bài repost của user này trong Firestore
                            db.collection("bai_viet")
                                    .whereEqualTo("nguoi_dung_id", currentUid)
                                    .whereEqualTo("bai_viet_cha_id", originalDocId)
                                    .whereEqualTo("is_repost", true)
                                    .whereEqualTo("da_xoa", false)
                                    .get()
                                    .addOnSuccessListener(snap -> {
                                        if (snap.isEmpty()) return;

                                        WriteBatch batch = db.batch();

                                        for (DocumentSnapshot doc : snap.getDocuments()) {
                                            // Đánh dấu da_xoa = true thay vì xóa hẳn
                                            batch.update(doc.getReference(), "da_xoa", true);
                                        }

                                        // Giảm so_repost của bài gốc
                                        DocumentReference baiGocRef = db.collection("bai_viet")
                                                .document(originalDocId);
                                        batch.update(baiGocRef, "so_repost", FieldValue.increment(-1));

                                        batch.commit()
                                                .addOnSuccessListener(unused -> {
                                                    repostedByMe.remove(originalDocId);
                                                    post.setRepostedByMe(false);
                                                    post.setSoRepost(Math.max(0, post.getSoRepost() - 1));
                                                    postAdapter.notifyItemChanged(position, "REPOST_UPDATE");
                                                    Toast.makeText(SearchResultActivity.this, "Đã xóa bài đăng lại", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(SearchResultActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(SearchResultActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return;
            }

            // ── Chưa repost → hỏi có muốn đăng lại không ──
            new androidx.appcompat.app.AlertDialog.Builder(SearchResultActivity.this)
                    .setTitle("Đăng lại bài viết")
                    .setMessage("Bạn muốn đăng lại bài này?")
                    .setPositiveButton("Đăng lại", (dialog, which) -> {

                        Map<String, Object> repost = new HashMap<>();
                        repost.put("nguoi_dung_id", currentUid);
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

                        DocumentReference baiGocRef = db.collection("bai_viet").document(originalDocId);
                        WriteBatch batch = db.batch();

                        DocumentReference repostRef = db.collection("bai_viet").document();
                        batch.set(repostRef, repost);
                        batch.update(baiGocRef, "so_repost", FieldValue.increment(1));

                        batch.commit()
                                .addOnSuccessListener(unused -> {
                                    repostedByMe.add(originalDocId);
                                    post.setRepostedByMe(true);
                                    post.setSoRepost(post.getSoRepost() + 1);
                                    postAdapter.notifyItemChanged(position, "REPOST_UPDATE");
                                    Toast.makeText(SearchResultActivity.this, "Đã đăng lại!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(SearchResultActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
            @Override
            public void onShareClick(PostModel post, int position) {
            if (post == null) return;
            if (isFinishing()) return;

            ShareBottom sheet = ShareBottom.newInstance("Xem bài này nè!", post.getDocumentId());

            // ✅ Thêm listener reload
            sheet.setOnShareDoneListener(() -> {
                if (isFinishing()) return;
                postList.clear();
                postAdapter.notifyDataSetChanged();
                if (listenerRegistration != null) listenerRegistration.remove();
                listenerRegistration = null;
                loadTopPosts();
            });

                sheet.show(getSupportFragmentManager(), "ShareBottom");
        }
            @Override
            public void onMoreOptionsClick(PostModel post, int position) {
            PostOptionBottomSheet sheet = new PostOptionBottomSheet(post.getDocumentId());

            sheet.setOnPostDeletedListener((deletedId, originalPostId) -> {
                // ✅ Nếu là bài repost bị xóa → reset trạng thái repost của bài gốc
                if (originalPostId != null) {
                    repostedByMe.remove(originalPostId);

                    // Cập nhật PostModel trong postList
                    for (int i = 0; i < postList.size(); i++) {
                        PostModel item = postList.get(i);
                        if (item.getDocumentId().equals(originalPostId)) {
                            item.setRepostedByMe(false);
                            item.setSoRepost(Math.max(0, item.getSoRepost() - 1));
                            postAdapter.notifyItemChanged(i, "REPOST_UPDATE");
                            break;
                        }
                    }
                }
                // Firestore listener tự bắt REMOVED event cho deletedId, không cần làm thêm
            });

            // ✅ Thêm listener xóa bài bị hạn chế ngay lập tức
            sheet.setOnPostHiddenListener(hiddenPostId -> {
                for (int i = 0; i < postList.size(); i++) {
                    if (postList.get(i).getDocumentId().equals(hiddenPostId)) {
                        postList.remove(i);
                        postAdapter.notifyItemRemoved(i);
                        break;
                    }
                }
            });

                sheet.show(getSupportFragmentManager(), "post_options");
        }

            @Override
            public void onCommentAvataClick(CommentModel comment, int position)
            {
                Intent intent = new Intent(SearchResultActivity.this,UserProfileActivity.class)
                        .putExtra("user_uid",comment.getNguoiDungId());
                startActivity(intent);
            }
        });
        rvTopPosts.setLayoutManager(new LinearLayoutManager(this));
        rvTopPosts.setAdapter(postAdapter);
    }

    private void loadRelatedProfiles() {
        if (keyword == null || keyword.isEmpty()) return;

        db.collection("nguoi_dung")
                .get()
                .addOnSuccessListener(query -> {
                    userList.clear();
                    for (User user : query.toObjects(User.class)) {
                        if (user.getUsername() != null &&
                                user.getUsername().toLowerCase()
                                        .contains(keyword.toLowerCase())) {
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                    rvRelatedProfiles.setVisibility(
                            userList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void loadTopPosts() {

        if (keyword == null || keyword.isEmpty()) return;

        String myUid =
                FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;
        Log.d("SEARCH_DEBUG", "=== loadTopPosts START, keyword=" + keyword);

        db.collection("bai_viet")
                .whereEqualTo("da_xoa", false)
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    Log.d("SEARCH_DEBUG", "Total docs fetched: " + query.size()); // ← SỐ BÀI TÌM ĐƯỢC

                    postList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {

                        PostModel post = doc.toObject(PostModel.class);

                        if (post == null) continue;

                        post.setDocumentId(doc.getId());

                        String content = post.getNoiDung();

                        if (content == null ||
                                !content.toLowerCase()
                                        .contains(keyword.toLowerCase())) {
                            Log.d("SEARCH_DEBUG", "Matched post: " + doc.getId() + " | content: " + content);

                            continue;
                        }
//                        Log.d("SEARCH_DEBUG", "Matched posts count: " + matched); // ← CÓ BÀI NÀO KHỚP KHÔNG?

                        String uid = post.getNguoiDungId();

                        if (uid == null) continue;

                        db.collection("nguoi_dung")
                                .document(uid)
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

                                    Runnable finishTask = () -> {

                                        loadTopComment(post, () -> {
                                            Log.d("SEARCH_DEBUG", "Adding post to list: " + post.getDocumentId());

                                            postList.add(post);
                                            Log.d("SEARCH_DEBUG", "postList size now: " + postList.size());

                                            postList.sort((a, b) -> {
                                                Timestamp t1 = a.getNgayTao();
                                                Timestamp t2 = b.getNgayTao();

                                                if (t1 == null || t2 == null)
                                                    return 0;

                                                return t2.compareTo(t1);
                                            });

                                            postAdapter.notifyDataSetChanged();

                                            rvTopPosts.setVisibility(
                                                    postList.isEmpty()
                                                            ? View.GONE
                                                            : View.VISIBLE);
                                        });
                                    };

                                    String chaId =
                                            doc.getString("bai_viet_cha_id");

                                    if (chaId != null && !chaId.isEmpty()) {

                                        post.setBaiVietChaId(chaId);

                                        loadBaiVietCha(
                                                post,
                                                chaId,
                                                finishTask
                                        );

                                    } else {
                                        if (myUid != null) {

                                            db.collection("bai_viet")
                                                    .document(post.getDocumentId())
                                                    .collection("luot_thich")
                                                    .document(myUid)
                                                    .get()
                                                    .addOnSuccessListener(likeDoc -> {

                                                        post.setLikedByMe(likeDoc.exists());

                                                        loadFollowAndRepost(post, myUid, finishTask);
                                                    });

                                        } else {
                                            finishTask.run();
                                        }

                                    }
                                });
                    }
                }).addOnFailureListener(e -> {
                    Log.e("SEARCH_DEBUG", "Query FAILED: " + e.getMessage()); // ← thêm dòng này
                });
    }
    private void loadFollowAndRepost(
            PostModel post,
            String myUid,
            Runnable onDone) {

        String authorUid = post.getNguoiDungId();

        // check repost
        db.collection("bai_viet")
                .whereEqualTo("nguoi_dung_id", myUid)
                .whereEqualTo("bai_viet_cha_id", post.getDocumentId())
                .whereEqualTo("is_repost", true)
                .whereEqualTo("da_xoa", false)
                .get()
                .addOnSuccessListener(repostSnap -> {

                    if (!repostSnap.isEmpty()) {
                        repostedByMe.add(post.getDocumentId());
                        post.setRepostedByMe(true);
                    }

                    // check follow
                    if (authorUid != null &&
                            !authorUid.equals(myUid)) {

                        db.collection("nguoi_dung")
                                .document(myUid)
                                .collection("nguoi_dang_theo_doi")
                                .document(authorUid)
                                .get()
                                .addOnSuccessListener(followDoc -> {

                                    post.setFollowing(
                                            followDoc.exists());

                                    onDone.run();
                                });

                    } else {

                        post.setFollowing(false);
                        onDone.run();
                    }
                })
                .addOnFailureListener(e -> onDone.run());
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
    private void loadTopComment(@NonNull PostModel post, Runnable onDone) {
        Log.d("TEST", "loadTopComment called");
        db.collection("bai_viet")
                .document(post.getDocumentId())
                .collection("binh_luan")
                .get()
                .addOnSuccessListener(snapshot -> {

                    Map<String, CommentModel> allComments =
                            new HashMap<>();

                    Map<String, List<CommentModel>> replyMap =
                            new HashMap<>();

                    int total = snapshot.size();

                    if (total == 0) {
                        onDone.run();
                        return;
                    }

                    int[] loaded = {0};

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Log.d("STEP", "doc found");

                        CommentModel comment =
                                doc.toObject(CommentModel.class);

                        if (comment == null) {

                            loaded[0]++;

                            if (loaded[0] == total) {

                                finishCommentLoad(
                                        allComments,
                                        replyMap,
                                        post,
                                        onDone
                                );
                            }
                            Log.d("STEP", "comment null");

                            continue;
                        }

                        comment.setDocumentId(doc.getId());

                        allComments.put(
                                comment.getDocumentId(),
                                comment
                        );

                        String parentId =
                                comment.getBinhLuanChaId();

                        if (parentId != null
                                && !parentId.isEmpty()) {

                            if (!replyMap.containsKey(parentId)) {

                                replyMap.put(parentId,
                                        new ArrayList<>());
                            }

                            replyMap.get(parentId)
                                    .add(comment);
                        }
                        Log.d("STEP",
                                "comment ok");
                        String uid =
                                comment.getNguoiDungId();

                        Log.d("STEP",
                                "uid = " + uid);
                        if (uid != null && !uid.isEmpty()) {
                            Log.d("STEP",
                                    "load user");
                            db.collection("nguoi_dung")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        Log.d("STEP",
                                                "user loaded");
                                        if (userDoc.exists()) {

                                            comment.setHoVaTen(
                                                    userDoc.getString(
                                                            "ho_va_ten"));

                                            comment.setAnhDaiDien(
                                                    userDoc.getString(
                                                            "anh_dai_dien"));
                                        }
//                                        Log.d(
//                                                "ALL_COMMENT",
//                                                "id = " + comment.getDocumentId()
//                                                        + "\nparent = " + comment.getBinhLuanChaId()
//                                                        + "\nuid = " + comment.getNguoiDungId()
//                                                        + "\nname = " + comment.getHoVaTen()
//                                                        + "\navatar = " + comment.getAnhDaiDien()
//                                                        + "\nlike = " + comment.getSoLike()
//                                                        + "\ncontent = " + comment.getNoiDung()
//                                        );
                                        loaded[0]++;

                                        if (loaded[0] == total) {

                                            finishCommentLoad(
                                                    allComments,
                                                    replyMap,
                                                    post,
                                                    onDone
                                            );
                                        }
                                    })
                                    .addOnFailureListener(e -> {

                                        loaded[0]++;

                                        if (loaded[0] == total) {

                                            finishCommentLoad(
                                                    allComments,
                                                    replyMap,
                                                    post,
                                                    onDone
                                            );
                                        }
                                    });

                        } else {

                            loaded[0]++;

                            if (loaded[0] == total) {

                                finishCommentLoad(
                                        allComments,
                                        replyMap,
                                        post,
                                        onDone
                                );
                            }
                            Log.d("STEP",
                                    "uid null");
                        }
                    }
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void finishCommentLoad(
            Map<String, CommentModel> allComments,
            Map<String, List<CommentModel>> replyMap,
            PostModel post,
            Runnable onDone
    ) {

        if (allComments.isEmpty()) {
            onDone.run();
            return;
        }

        // tìm comment/reply nhiều tim nhất
        CommentModel mostLiked = null;

        for (CommentModel c : allComments.values()) {

            if (mostLiked == null
                    || c.getSoLike() > mostLiked.getSoLike()) {

                mostLiked = c;
            }
        }

        if (mostLiked == null) {
            onDone.run();
            return;
        }

        // lần ngược về comment cha gốc
        CommentModel root = mostLiked;

        while (root.getBinhLuanChaId() != null
                && !root.getBinhLuanChaId().isEmpty()) {

            CommentModel parent =
                    allComments.get(root.getBinhLuanChaId());

            if (parent == null)
                break;

            root = parent;
        }

        // set top comment
        post.setTopComment(root);

        // load replies của comment cha
        List<CommentModel> replies =
                replyMap.get(root.getDocumentId());

        if (replies != null) {

            Collections.sort(replies,
                    (a, b) -> {

                        Timestamp t1 = a.getNgayTao();
                        Timestamp t2 = b.getNgayTao();

                        if (t1 == null || t2 == null)
                            return 0;

                        return t1.compareTo(t2);
                    });

            post.setTopReplies(replies);
        }

        onDone.run();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}