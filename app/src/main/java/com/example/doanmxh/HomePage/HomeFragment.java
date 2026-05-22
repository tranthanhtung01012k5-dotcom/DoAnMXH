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
import androidx.appcompat.app.AppCompatActivity;
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

                // ✅ Luôn lấy post mới nhất từ postList theo documentId
                String docId = post.getDocumentId();
                PostModel currentPost = null;
                int currentPosition = -1;
                for (int i = 0; i < postList.size(); i++) {
                    if (postList.get(i).getDocumentId().equals(docId)) {
                        currentPost = postList.get(i);
                        currentPosition = i;
                        break;
                    }
                }
                if (currentPost == null) return;

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
                                            adapter.notifyItemChanged(finalPosition, "LIKE_UPDATE");
                                        });
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
                                                        adapter.notifyItemChanged(finalPosition, "LIKE_UPDATE");
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
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;

                if (currentUid == null) {
                    Toast.makeText(getContext(),
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

                            if (getContext() == null) return;

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
                            Toast.makeText(getContext(),
                                    "Đã theo dõi @" + post.getTenDangNhap(),
                                    Toast.LENGTH_SHORT).show();

                            post.setFollowing(true);
                            adapter.notifyItemChanged(position, "LIKE_UPDATE");
                        })
                        .addOnFailureListener(e -> {

                            if (getContext() == null) return;

                            Toast.makeText(getContext(),
                                    "Lỗi: " + e.getMessage(),
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
                PostOptionBottomSheet sheet = new PostOptionBottomSheet(post.getDocumentId());
                sheet.setOnPostDeletedListener(deletedId -> {
                    // Firestore listener tự bắt REMOVED event, không cần làm thêm
                });
                sheet.show(getChildFragmentManager(), "post_options");
            }

            @Override
            public void onCommentAvataClick(CommentModel comment, int position)
            {
                Intent intent = new Intent(getActivity(),UserProfileActivity.class)
                        .putExtra("user_uid",comment.getNguoiDungId());
                startActivity(intent);
            }
        });

        rvFeed.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(0xFFFFFFFF);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF222222);
        // swipeRefresh - bỏ dòng lỗi, giữ nguyên flow
        swipeRefresh.setOnRefreshListener(() -> {
            postList.clear();
            adapter.notifyDataSetChanged();
            if (listenerRegistration != null)
                listenerRegistration.remove();
            listenerRegistration = null;
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

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {

                            String docId = dc.getDocument().getId();

                            switch (dc.getType()) {

                                // ─────────────── ADDED ───────────────
                                case ADDED: {

                                    PostModel post =
                                            dc.getDocument().toObject(PostModel.class);

                                    post.setDocumentId(docId);

                                    String nguoiDungId =
                                            dc.getDocument().getString("nguoi_dung_id");

                                    Runnable insertTask = () -> {

                                        loadTopComment(post, () -> {

                                            postList.add(0, post);
                                            adapter.notifyItemInserted(0);
                                            rvFeed.scrollToPosition(0);
                                        });
                                    };

                                    if (nguoiDungId != null && !nguoiDungId.isEmpty()) {

                                        db.collection("nguoi_dung")
                                                .document(nguoiDungId)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {

                                                    if (userDoc.exists()) {
                                                        post.setHoVaTen(userDoc.getString("ho_va_ten"));
                                                        post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                                        post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                                        post.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                                                    }

                                                    insertTask.run();
                                                });

                                    } else {
                                        insertTask.run();
                                    }

                                    break;
                                }

                                // ─────────────── MODIFIED ───────────────
                                // MODIFIED case - thêm payload + giữ avatar/tên
                                case MODIFIED: {
                                    PostModel newPost = dc.getDocument().toObject(PostModel.class);
                                    newPost.setDocumentId(docId);

                                    for (int i = 0; i < postList.size(); i++) {
                                        PostModel old = postList.get(i);

                                        if (old.getDocumentId().equals(docId)) {

                                            // Giữ lại UI state - KHÔNG override likedByMe vì onLikeClick đã set rồi
                                            newPost.setTopComment(old.getTopComment());
                                            newPost.setTopReplies(old.getTopReplies());
                                            newPost.setFollowing(old.isFollowing());

                                            // Giữ avatar + tên
                                            newPost.setHoVaTen(old.getHoVaTen());
                                            newPost.setAnhDaiDien(old.getAnhDaiDien());
                                            newPost.setTenDangNhap(old.getTenDangNhap());
                                            newPost.setVerified(old.isVerified());

                                            // ✅ Giữ likedByMe từ local state (onLikeClick đã cập nhật đúng)
                                            newPost.setLikedByMe(old.isLikedByMe());

                                            // ✅ Giữ so_like từ local nếu Firestore chưa kịp sync
                                            // (tránh giật số khi Firestore trả về giá trị cũ)
                                            newPost.setSoLuotThich(old.getSoLuotThich());

                                            postList.set(i, newPost);
                                            adapter.notifyItemChanged(i, "LIKE_UPDATE");
                                            break;
                                        }
                                    }
                                    break;
                                }

                                // ─────────────── REMOVED ───────────────
                                case REMOVED: {

                                    String removedId = docId;

                                    for (int i = 0; i < postList.size(); i++) {

                                        if (postList.get(i).getDocumentId().equals(removedId)) {

                                            postList.remove(i);
                                            adapter.notifyItemRemoved(i);
                                            break;
                                        }
                                    }

                                    break;
                                }
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