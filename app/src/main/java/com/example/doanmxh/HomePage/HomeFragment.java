package com.example.doanmxh.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.DocumentSnapshot;
public class HomeFragment extends Fragment {

    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeRefresh;
    private PostAdapter adapter;
    private List<PostModel> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvFeed = view.findViewById(R.id.rvFeed);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        if (rvFeed == null || swipeRefresh == null) {
            Log.e("HomeFragment", "View null");
            return view;
        }

        db = FirebaseFirestore.getInstance();

        if (db == null) {
            Log.e("HomeFragment", "Firestore null");
            return view;
        }

        adapter = new PostAdapter(postList, new PostAdapter.OnPostActionListener() {

            @Override
            public void onLikeClick(PostModel post, int position) {
                if (getContext() == null) return;

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

                if (currentUid == null) {
                    Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("bai_viet")
                        .document(post.getDocumentId())
                        .collection("luot_thich")
                        .document(currentUid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                // Đã like → bỏ like
                                db.collection("bai_viet")
                                        .document(post.getDocumentId())
                                        .collection("luot_thich")
                                        .document(currentUid)
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            db.collection("bai_viet")
                                                    .document(post.getDocumentId())
                                                    .update("so_like", FieldValue.increment(-1));
                                            post.setLikedByMe(false);
                                            post.setSoLuotThich(Math.max(0, post.getSoLuotThich() - 1));
                                            adapter.notifyItemChanged(position);
                                        });
                            } else {
                                // Chưa like → lấy tên user rồi thêm like
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
                                                    .document(post.getDocumentId())
                                                    .collection("luot_thich")
                                                    .document(currentUid)
                                                    .set(likeData)
                                                    .addOnSuccessListener(unused -> {
                                                        db.collection("bai_viet")
                                                                .document(post.getDocumentId())
                                                                .update("so_like", FieldValue.increment(1));
                                                        post.setLikedByMe(true);
                                                        post.setSoLuotThich(post.getSoLuotThich() + 1);
                                                        adapter.notifyItemChanged(position);
                                                    });
                                        });
                            }
                        })
                        .addOnFailureListener(e ->
                                Log.e("HomeFragment", "Lỗi like: " + e.getMessage())
                        );
            }

            @Override
            public void onAvatarClick(PostModel post, int position) {

                if (getContext() == null) return;

                Intent intent =
                        new Intent(getActivity(),
                                UserProfileActivity.class);

                intent.putExtra("user_uid",
                        post.getNguoiDungId());

                startActivity(intent);
            }

            @Override
            public void onAddFriendClick(PostModel post, int position) {
                if (getContext() == null) return;

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

                if (currentUid == null) {
                    Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                String docId = currentUid + "_" + post.getNguoiDungId();

                // ── 1. Lưu vào collection "theo_doi" ──
                Map<String, Object> followData = new HashMap<>();
                followData.put("nguoi_theo_doi_id", currentUid);
                followData.put("nguoi_duoc_theo_doi_id", post.getNguoiDungId());
                followData.put("ngay_theo_doi", new Date());

                db.collection("theo_doi").document(docId)
                        .set(followData)
                        .addOnSuccessListener(unused -> {
                            if (getContext() == null) return;

                            String authorUid = post.getNguoiDungId();

                            // ── 2. Phía tác giả bài (người được follow) ──
                            // Tăng so_nguoi_theo_doi
                            db.collection("nguoi_dung")
                                    .document(authorUid)
                                    .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));

                            // Thêm currentUid vào subcollection "nguoi_theo_doi" của tác giả
                            // set() tự tạo document nếu chưa có
                            Map<String, Object> followerEntry = new HashMap<>();
                            followerEntry.put("nguoi_dung_id", currentUid);
                            followerEntry.put("ngay_theo_doi", new Date());

                            db.collection("nguoi_dung")
                                    .document(authorUid)
                                    .collection("nguoi_dang_theo_doi")
                                    .document(currentUid)
                                    .set(followerEntry);

                            // ── 3. Phía người dùng hiện tại (người nhấn follow) ──
                            // Tăng so_nguoi_dang_theo_doi
                            db.collection("nguoi_dung")
                                    .document(currentUid)
                                    .update("so_nguoi_theo_doi", FieldValue.increment(1));

                            // Thêm authorUid vào subcollection "nguoi_dang_theo_doi" của currentUid
                            Map<String, Object> followingEntry = new HashMap<>();
                            followingEntry.put("nguoi_dung_id", authorUid);
                            followingEntry.put("ngay_theo_doi", new Date());

                            db.collection("nguoi_dung")
                                    .document(currentUid)
                                    .collection("nguoi_theo_doi")
                                    .document(authorUid)
                                    .set(followingEntry);

                            // ── 4. Cập nhật UI ──
                            Toast.makeText(getContext(),
                                    "Đã theo dõi @" + post.getTenDangNhap(),
                                    Toast.LENGTH_SHORT).show();
                            post.setFollowing(true);
                            adapter.notifyItemChanged(position);
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCommentClick(PostModel post, int position) {
                Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getDocumentId());
                startActivity(intent);
            }

            @Override
            public void onRepostClick(PostModel post, int position) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Đã repost", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShareClick(PostModel post, int position) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Chia sẻ", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMoreOptionsClick(PostModel post, int position) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Tùy chọn", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCommentAvatarClick(CommentModel comment, int position) {

                Intent intent =
                        new Intent(HomeFragment.this,
                                UserProfileActivity.class);

                intent.putExtra(
                        "user_id",
                        comment.getNguoiDungId()
                );

                startActivity(intent);
            }
//            @Override
//            public void onAvatarClick(PostModel post, int position) {
//                if (getContext() != null)
//                    Toast.makeText(getContext(), "@" + post.getTenDangNhap(),
//                            Toast.LENGTH_SHORT).show();
//            }
        });

        rvFeed.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(0xFFFFFFFF);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF222222);
        swipeRefresh.setOnRefreshListener(() -> {
            postList.clear();
            adapter.notifyDataSetChanged();
            if (listenerRegistration != null)
                listenerRegistration.remove();
            loadFromFirestore();
            swipeRefresh.setRefreshing(false);
        });

        return view;
    }

    private void loadFromFirestore() {

        listenerRegistration = db.collection("bai_viet")
                .whereEqualTo("da_xoa", false)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e("HomeFragment",
                                "Lỗi load feed: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null) return;

                    String myUid =
                            FirebaseAuth.getInstance().getCurrentUser() != null
                                    ? FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getUid()
                                    : null;

                    // ─────────────────────────────────────
                    // LOAD FIRST TIME
                    // ─────────────────────────────────────

                    if (postList.isEmpty()) {

                        List<PostModel> tempList =
                                new ArrayList<>();

                        int[] pendingCount = {0};

                        for (DocumentChange dc
                                : snapshots.getDocumentChanges()) {

                            if (dc.getType()
                                    != DocumentChange.Type.ADDED)
                                continue;

                            pendingCount[0]++;

                            PostModel post =
                                    dc.getDocument()
                                            .toObject(PostModel.class);

                            post.setDocumentId(
                                    dc.getDocument().getId());

                            String nguoiDungId =
                                    dc.getDocument()
                                            .getString("nguoi_dung_id");

                            Runnable finishTask = () -> {

                                loadTopComment(post, () -> {

                                    tempList.add(post);

                                    pendingCount[0]--;

                                    if (pendingCount[0] == 0) {

                                        sortAndRefresh(tempList);
                                    }
                                });
                            };

                            // load user
                            if (nguoiDungId != null
                                    && !nguoiDungId.isEmpty()) {

                                db.collection("nguoi_dung")
                                        .document(nguoiDungId)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {

                                            if (userDoc.exists()) {

                                                post.setHoVaTen(
                                                        userDoc.getString(
                                                                "ho_va_ten"));

                                                post.setTenDangNhap(
                                                        userDoc.getString(
                                                                "ten_dang_nhap"));

                                                post.setAnhDaiDien(
                                                        userDoc.getString(
                                                                "anh_dai_dien"));

                                                post.setVerified(
                                                        Boolean.TRUE.equals(
                                                                userDoc.getBoolean(
                                                                        "verified")
                                                        )
                                                );
                                            }

                                            // check like
                                            if (myUid != null) {

                                                db.collection("bai_viet")
                                                        .document(post.getDocumentId())
                                                        .collection("luot_thich")
                                                        .document(myUid)
                                                        .get()
                                                        .addOnSuccessListener(likeDoc -> {

                                                            post.setLikedByMe(
                                                                    likeDoc.exists());

                                                            String authorUid =
                                                                    post.getNguoiDungId();

                                                            // check follow
                                                            if (authorUid != null
                                                                    && !authorUid.equals(myUid)) {

                                                                db.collection("theo_doi")
                                                                        .document(
                                                                                myUid
                                                                                        + "_"
                                                                                        + authorUid)
                                                                        .get()
                                                                        .addOnSuccessListener(followDoc -> {

                                                                            post.setFollowing(
                                                                                    followDoc.exists());

                                                                            finishTask.run();
                                                                        })
                                                                        .addOnFailureListener(e -> {

                                                                            post.setFollowing(false);

                                                                            finishTask.run();
                                                                        });

                                                            } else {

                                                                post.setFollowing(false);

                                                                finishTask.run();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {

                                                            post.setLikedByMe(false);

                                                            finishTask.run();
                                                        });

                                            } else {

                                                post.setFollowing(false);

                                                finishTask.run();
                                            }

                                        })
                                        .addOnFailureListener(e -> {

                                            Log.e("HomeFragment",
                                                    "Lỗi load user: "
                                                            + e.getMessage());

                                            finishTask.run();
                                        });

                            } else {

                                finishTask.run();
                            }
                        }

                        if (pendingCount[0] == 0) {

                            sortAndRefresh(tempList);
                        }

                    }

                    // ─────────────────────────────────────
                    // REALTIME UPDATE
                    // ─────────────────────────────────────

                    else {

                        for (DocumentChange dc
                                : snapshots.getDocumentChanges()) {

                            PostModel post =
                                    dc.getDocument()
                                            .toObject(PostModel.class);

                            post.setDocumentId(
                                    dc.getDocument().getId());

                            String nguoiDungId =
                                    dc.getDocument()
                                            .getString("nguoi_dung_id");

                            switch (dc.getType()) {

                                case ADDED:

                                    if (nguoiDungId != null
                                            && !nguoiDungId.isEmpty()) {

                                        db.collection("nguoi_dung")
                                                .document(nguoiDungId)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {

                                                    if (userDoc.exists()) {

                                                        post.setHoVaTen(
                                                                userDoc.getString(
                                                                        "ho_va_ten"));

                                                        post.setTenDangNhap(
                                                                userDoc.getString(
                                                                        "ten_dang_nhap"));

                                                        post.setAnhDaiDien(
                                                                userDoc.getString(
                                                                        "anh_dai_dien"));

                                                        post.setVerified(
                                                                Boolean.TRUE.equals(
                                                                        userDoc.getBoolean(
                                                                                "verified")
                                                                )
                                                        );
                                                    }

                                                    loadTopComment(post, () -> {

                                                        postList.add(0, post);

                                                        adapter.notifyItemInserted(0);

                                                        rvFeed.scrollToPosition(0);
                                                    });
                                                });

                                    } else {

                                        loadTopComment(post, () -> {

                                            postList.add(0, post);

                                            adapter.notifyItemInserted(0);

                                            rvFeed.scrollToPosition(0);
                                        });
                                    }

                                    break;

                                case MODIFIED:

                                    for (int i = 0;
                                         i < postList.size();
                                         i++) {

                                        if (postList.get(i)
                                                .getDocumentId()
                                                .equals(post.getDocumentId())) {

                                            post.setLikedByMe(
                                                    postList.get(i)
                                                            .isLikedByMe());

                                            post.setFollowing(
                                                    postList.get(i)
                                                            .isFollowing());

                                            post.setHoVaTen(
                                                    postList.get(i)
                                                            .getHoVaTen());

                                            post.setTenDangNhap(
                                                    postList.get(i)
                                                            .getTenDangNhap());

                                            post.setAnhDaiDien(
                                                    postList.get(i)
                                                            .getAnhDaiDien());

                                            postList.set(i, post);

                                            adapter.notifyItemChanged(i);

                                            break;
                                        }
                                    }

                                    break;

                                case REMOVED:

                                    for (int i = 0;
                                         i < postList.size();
                                         i++) {

                                        if (postList.get(i)
                                                .getDocumentId()
                                                .equals(post.getDocumentId())) {

                                            postList.remove(i);

                                            adapter.notifyItemRemoved(i);

                                            break;
                                        }
                                    }

                                    break;
                            }
                        }
                    }

                    Log.d("HomeFragment",
                            "Feed: "
                                    + postList.size()
                                    + " bài");
                });
    }

    private void sortAndRefresh(List<PostModel> newPosts) {
        Collections.sort(newPosts, (a, b) -> {
            Timestamp dateA = a.getNgayTao();
            Timestamp dateB = b.getNgayTao();
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        postList.clear();
        postList.addAll(newPosts);
        adapter.notifyDataSetChanged();
        if (!postList.isEmpty()) rvFeed.scrollToPosition(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listenerRegistration == null) {
            postList.clear();
            adapter.notifyDataSetChanged();
            loadFromFirestore();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
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
                                        Log.d(
                                                "ALL_COMMENT",
                                                "id = " + comment.getDocumentId()
                                                        + "\nparent = " + comment.getBinhLuanChaId()
                                                        + "\nuid = " + comment.getNguoiDungId()
                                                        + "\nname = " + comment.getHoVaTen()
                                                        + "\navatar = " + comment.getAnhDaiDien()
                                                        + "\nlike = " + comment.getSo_like()
                                                        + "\ncontent = " + comment.getNoiDung()
                                        );
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
                    || c.getSo_like() > mostLiked.getSo_like()) {

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
}