package com.example.doanmxh.HomePage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.doanmxh.Mention.MentionHelper;
import com.example.doanmxh.Notifications.NotificationsFragment;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.example.doanmxh.Search.SearchActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment {

    // ─────────────────────────────────────────────────────────────────────────
    // CACHE CONSTANTS
    // ─────────────────────────────────────────────────────────────────────────

    private static final String PREF_NAME        = "home_feed_cache";
    private static final String KEY_POSTS        = "cached_posts";
    private static final String KEY_LAST_UPDATE  = "last_update_ts";
    /** Số bài tối đa lưu vào cache */
    private static final int    CACHE_MAX_POSTS  = 50;
    /** Cache hết hạn sau 30 phút (ms) — sau đó skeleton được ưu tiên hơn cache */
    private static final long   CACHE_TTL_MS     = 30 * 60 * 1000L;

    // ─────────────────────────────────────────────────────────────────────────

    private com.google.android.material.imageview.ShapeableImageView ivUserAvatarPost;
    private EditText tvQuickPostHint;
    private ImageButton btnSearch;
    private com.google.android.material.button.MaterialButton btnQuickPost;
    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeRefresh;
    private PostAdapter adapter;
    private List<PostModel> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private MentionHelper mentionHelper;

    private final Set<String> repostedByMe    = new HashSet<>();
    private final Set<String> processingLikes = new HashSet<>();

    // ── Cache follow / block để không query lại mỗi bài ─────────────────────
    private final Set<String> followingSet = new HashSet<>();
    private final Set<String> blockedSet   = new HashSet<>();
    private boolean followingLoaded = false;
    private boolean blockedLoaded   = false;

    // ── Flag: đã load xong lần đầu chưa (để phân biệt skeleton vs realtime) ─
    private boolean firstLoadDone = false;

    // ── Flag: đã hiển thị cache chưa (tránh show cache 2 lần) ───────────────
    private boolean cacheDisplayed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        enableImmersiveMode();

        rvFeed       = view.findViewById(R.id.rvFeed);
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

        ivUserAvatarPost = view.findViewById(R.id.ivUserAvatarPost);
        tvQuickPostHint  = view.findViewById(R.id.tvQuickPostHint);
        btnQuickPost     = view.findViewById(R.id.btnQuickPost);
        btnSearch        = view.findViewById(R.id.btnSearch);

        // Load avatar người dùng hiện tại
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (myUid != null) {
            db.collection("nguoi_dung").document(myUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String avatar = doc.getString("anh_dai_dien");
                            if (!TextUtils.isEmpty(avatar) && isAdded()) {
                                Glide.with(this)
                                        .load(avatar)
                                        .placeholder(R.drawable.ic_placeholder_avatar)
                                        .into(ivUserAvatarPost);
                            }
                        }
                    });
        }

        btnQuickPost.setOnClickListener(v -> {
            String content = tvQuickPostHint.getText().toString().trim();
            if (content.isEmpty()) {
                tvQuickPostHint.setError("Vui lòng nhập nội dung");
                return;
            }
            quickPost(content);
        });

        btnSearch.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));

        mentionHelper = new MentionHelper(requireContext(), tvQuickPostHint, db, null);

        adapter = new PostAdapter(postList, new PostAdapter.OnPostActionListener() {

            @Override
            public void onLikeClick(PostModel post, int position) {
                if (post.isLoading()) return;

                String docId = post.getDocumentId();
                if (processingLikes.contains(docId)) return;
                processingLikes.add(docId);

                if (getContext() == null) { processingLikes.remove(docId); return; }

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

                if (currentUid == null) {
                    processingLikes.remove(docId);
                    Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                PostModel currentPost = null;
                int currentPosition  = -1;
                for (int i = 0; i < postList.size(); i++) {
                    if (postList.get(i).getDocumentId() != null
                            && postList.get(i).getDocumentId().equals(docId)) {
                        currentPost     = postList.get(i);
                        currentPosition = i;
                        break;
                    }
                }
                if (currentPost == null) { processingLikes.remove(docId); return; }

                final PostModel finalPost     = currentPost;
                final int       finalPosition = currentPosition;

                db.collection("bai_viet").document(docId)
                        .collection("luot_thich").document(currentUid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                // Unlike
                                db.collection("bai_viet").document(docId)
                                        .collection("luot_thich").document(currentUid).delete()
                                        .addOnSuccessListener(unused -> {
                                            db.collection("bai_viet").document(docId)
                                                    .update("so_like", FieldValue.increment(-1));
                                            finalPost.setLikedByMe(false);
                                            finalPost.setSoLuotThich(Math.max(0, finalPost.getSoLuotThich() - 1));
                                            adapter.notifyItemChanged(finalPosition, "LIKE_UPDATE");
                                            saveFeedCache(); // Lưu cache sau khi unlike
                                        })
                                        .addOnCompleteListener(t -> processingLikes.remove(docId));
                            } else {
                                // Like
                                db.collection("nguoi_dung").document(currentUid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            String hoVaTen = userDoc.exists()
                                                    ? userDoc.getString("ho_va_ten") : "Ẩn danh";

                                            Map<String, Object> likeData = new HashMap<>();
                                            likeData.put("nguoi_dung_id", currentUid);
                                            likeData.put("ho_va_ten", hoVaTen);
                                            likeData.put("ngay_like", new Date());

                                            db.collection("bai_viet").document(docId)
                                                    .collection("luot_thich").document(currentUid)
                                                    .set(likeData)
                                                    .addOnSuccessListener(unused -> {
                                                        db.collection("bai_viet").document(docId)
                                                                .update("so_like", FieldValue.increment(1));
                                                        finalPost.setLikedByMe(true);
                                                        finalPost.setSoLuotThich(finalPost.getSoLuotThich() + 1);
                                                        adapter.notifyItemChanged(finalPosition, "LIKE_UPDATE");
                                                        NotificationsFragment.sendLikeNotification(
                                                                finalPost.getNguoiDungId(), currentUid, docId);
                                                        saveFeedCache(); // Lưu cache sau khi like
                                                    })
                                                    .addOnCompleteListener(t -> processingLikes.remove(docId));
                                        })
                                        .addOnFailureListener(e -> processingLikes.remove(docId));
                            }
                        })
                        .addOnFailureListener(e -> processingLikes.remove(docId));
            }

            @Override
            public void onAvatarClick(PostModel post, int position) {
                if (getContext() == null || post.isLoading()) return;
                startActivity(new Intent(getActivity(), UserProfileActivity.class)
                        .putExtra("user_uid", post.getNguoiDungId()));
            }

            @Override
            public void onAddFriendClick(PostModel post, int position) {
                if (getContext() == null || post.isLoading()) return;

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                String authorUid  = post.getNguoiDungId();

                if (currentUid == null) {
                    Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (authorUid == null || authorUid.equals(currentUid)) return;

                Map<String, Object> followerEntry = new HashMap<>();
                followerEntry.put("nguoi_dung_id", currentUid);
                followerEntry.put("ngay_theo_doi", new Date());

                db.collection("nguoi_dung").document(authorUid)
                        .collection("nguoi_theo_doi").document(currentUid)
                        .set(followerEntry)
                        .addOnSuccessListener(unused -> {
                            if (getContext() == null) return;
                            db.collection("nguoi_dung").document(authorUid)
                                    .update("so_nguoi_theo_doi", FieldValue.increment(1));

                            Map<String, Object> followingEntry = new HashMap<>();
                            followingEntry.put("nguoi_dung_id", authorUid);
                            followingEntry.put("ngay_theo_doi", new Date());

                            db.collection("nguoi_dung").document(currentUid)
                                    .collection("nguoi_dang_theo_doi").document(authorUid)
                                    .set(followingEntry);
                            db.collection("nguoi_dung").document(currentUid)
                                    .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));

                            followingSet.add(authorUid);

                            NotificationsFragment.sendFollowNotification(authorUid, currentUid);
                            Toast.makeText(getContext(),
                                    "Đã theo dõi @" + post.getTenDangNhap(),
                                    Toast.LENGTH_SHORT).show();

                            for (int i = 0; i < postList.size(); i++) {
                                PostModel item = postList.get(i);
                                if (authorUid.equals(item.getNguoiDungId())) {
                                    item.setFollowing(true);
                                    adapter.notifyItemChanged(i);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCommentClick(PostModel post, int position) {
                if (post.isLoading()) return;
                Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getDocumentId());
                startActivity(intent);
            }

            @Override
            public void onRepostClick(PostModel post, int position) {
                if (getContext() == null || post.isLoading()) return;

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (currentUid == null) {
                    Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                String originalDocId = post.getDocumentId();

                if (repostedByMe.contains(originalDocId)) {
                    new androidx.appcompat.app.AlertDialog.Builder(getContext())
                            .setTitle("Hủy đăng lại")
                            .setMessage("Bạn muốn xóa bài đăng lại này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
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
                                                batch.update(doc.getReference(), "da_xoa", true);
                                            }
                                            batch.update(
                                                    db.collection("bai_viet").document(originalDocId),
                                                    "so_repost", FieldValue.increment(-1));
                                            batch.commit().addOnSuccessListener(unused -> {
                                                repostedByMe.remove(originalDocId);
                                                post.setRepostedByMe(false);
                                                post.setSoRepost(Math.max(0, post.getSoRepost() - 1));
                                                adapter.notifyItemChanged(position, "REPOST_UPDATE");
                                                saveFeedCache(); // Lưu cache
                                                Toast.makeText(getContext(), "Đã xóa bài đăng lại", Toast.LENGTH_SHORT).show();
                                            }).addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                    return;
                }

                new androidx.appcompat.app.AlertDialog.Builder(getContext())
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

                            WriteBatch batch      = db.batch();
                            DocumentReference ref = db.collection("bai_viet").document();
                            batch.set(ref, repost);
                            batch.update(db.collection("bai_viet").document(originalDocId),
                                    "so_repost", FieldValue.increment(1));
                            batch.commit().addOnSuccessListener(unused -> {
                                repostedByMe.add(originalDocId);
                                post.setRepostedByMe(true);
                                post.setSoRepost(post.getSoRepost() + 1);
                                adapter.notifyItemChanged(position, "REPOST_UPDATE");
                                if (!currentUid.equals(post.getNguoiDungId())) {
                                    NotificationsFragment.sendRepostNotification(
                                            post.getNguoiDungId(), currentUid, post.getDocumentId());
                                }
                                saveFeedCache(); // Lưu cache
                                Toast.makeText(getContext(), "Đã đăng lại!", Toast.LENGTH_SHORT).show();
                            }).addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onShareClick(PostModel post, int position) {
                if (post == null || !isAdded() || post.isLoading()) return;
                ShareBottom sheet = ShareBottom.newInstance("Xem bài này nè!", post.getDocumentId());
                sheet.setOnShareDoneListener(() -> {
                    if (!isAdded()) return;
                    postList.clear();
                    adapter.notifyDataSetChanged();
                    if (listenerRegistration != null) listenerRegistration.remove();
                    listenerRegistration = null;
                    loadFromFirestore();
                });
                sheet.show(getParentFragmentManager(), "ShareBottom");
            }

            @Override
            public void onMoreOptionsClick(PostModel post, int position) {
                if (post.isLoading()) return;
                PostOptionBottomSheet sheet = new PostOptionBottomSheet(post.getDocumentId());
                sheet.setOnPostDeletedListener((deletedId, originalPostId) -> {
                    if (originalPostId != null) {
                        repostedByMe.remove(originalPostId);
                        for (int i = 0; i < postList.size(); i++) {
                            PostModel item = postList.get(i);
                            if (item.getDocumentId() != null
                                    && item.getDocumentId().equals(originalPostId)) {
                                item.setRepostedByMe(false);
                                item.setSoRepost(Math.max(0, item.getSoRepost() - 1));
                                adapter.notifyItemChanged(i, "REPOST_UPDATE");
                                break;
                            }
                        }
                    }
                    saveFeedCache(); // Lưu cache sau khi xóa
                });
                sheet.setOnPostHiddenListener(hiddenPostId -> {
                    for (int i = 0; i < postList.size(); i++) {
                        if (postList.get(i).getDocumentId() != null
                                && postList.get(i).getDocumentId().equals(hiddenPostId)) {
                            postList.remove(i);
                            adapter.notifyItemRemoved(i);
                            saveFeedCache(); // Lưu cache sau khi ẩn
                            break;
                        }
                    }
                });
                sheet.show(getChildFragmentManager(), "post_options");
            }

            @Override
            public void onCommentAvataClick(CommentModel comment, int position) {
                startActivity(new Intent(getActivity(), UserProfileActivity.class)
                        .putExtra("user_uid", comment.getNguoiDungId()));
            }
        });

        rvFeed.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(0xFFFFFFFF);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF222222);
        swipeRefresh.setOnRefreshListener(() -> {
            resetAndReload();
            swipeRefresh.setRefreshing(false);
        });

        return view;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (postList.isEmpty() && listenerRegistration == null) {
            resetAndReload();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Lưu cache khi người dùng rời màn hình
        saveFeedCache();
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
        if (mentionHelper != null) mentionHelper.dismiss();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESET & RELOAD
    // ─────────────────────────────────────────────────────────────────────────

    private void resetAndReload() {
        postList.clear();
        adapter.notifyDataSetChanged();
        repostedByMe.clear();
        firstLoadDone  = false;
        cacheDisplayed = false;

        followingSet.clear();
        blockedSet.clear();
        followingLoaded = false;
        blockedLoaded   = false;

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (myUid == null) {
            loadFromFirestore();
            return;
        }

        preloadFollowAndBlock(myUid, this::loadFromFirestore);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRELOAD CACHE (follow + block)
    // ─────────────────────────────────────────────────────────────────────────

    private void preloadFollowAndBlock(String myUid, Runnable onDone) {
        final int[] done = {0};
        final int total  = 2;

        Runnable check = () -> {
            done[0]++;
            if (done[0] == total) onDone.run();
        };

        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_dang_theo_doi").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments())
                        followingSet.add(doc.getId());
                    followingLoaded = true;
                    check.run();
                })
                .addOnFailureListener(e -> check.run());

        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_bi_chan").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments())
                        blockedSet.add(doc.getId());
                    blockedLoaded = true;
                    check.run();
                })
                .addOnFailureListener(e -> check.run());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CACHE — SAVE
    // Lưu tối đa CACHE_MAX_POSTS bài (chỉ bài content thật, bỏ skeleton)
    // Được gọi sau khi Firestore load xong, sau thao tác like/repost/xóa
    // ─────────────────────────────────────────────────────────────────────────

    private void saveFeedCache() {
        if (!isAdded() || getContext() == null) return;

        try {
            JSONArray jsonArray = new JSONArray();
            int count = 0;

            for (PostModel post : postList) {
                if (post.isLoading()) continue;                  // bỏ skeleton
                if (count >= CACHE_MAX_POSTS) break;

                JSONObject obj = new JSONObject();
                obj.put("documentId",   nvl(post.getDocumentId()));
                obj.put("nguoiDungId",  nvl(post.getNguoiDungId()));
                obj.put("noiDung",      nvl(post.getNoiDung()));
                obj.put("hoVaTen",      nvl(post.getHoVaTen()));
                obj.put("tenDangNhap",  nvl(post.getTenDangNhap()));
                obj.put("anhDaiDien",   nvl(post.getAnhDaiDien()));
                obj.put("soLuotThich",  post.getSoLuotThich());
                obj.put("soBinhLuan",   post.getSoBinhLuan());
                obj.put("soRepost",     post.getSoRepost());
                obj.put("likedByMe",    post.isLikedByMe());
                obj.put("repostedByMe", post.isRepostedByMe());
                obj.put("following",    post.isFollowing());
                obj.put("verified",     post.isVerified());
                obj.put("baiVietChaId", nvl(post.getBaiVietChaId()));

                // Timestamp → epoch seconds
                if (post.getNgayTao() != null)
                    obj.put("ngayTaoSec", post.getNgayTao().getSeconds());

                // Danh sách ảnh
                if (post.getDanhSachAnh() != null) {
                    JSONArray imgs = new JSONArray();
                    for (String url : post.getDanhSachAnh()) imgs.put(url);
                    obj.put("danhSachAnh", imgs);
                }

                // Top comment (nếu có)
                if (post.getTopComment() != null) {
                    obj.put("topComment", serializeComment(post.getTopComment()));
                }

                // Bài cha (tóm tắt: id + nội dung + tên tác giả)
                if (post.getPostCha() != null) {
                    PostModel cha = post.getPostCha();
                    JSONObject chaObj = new JSONObject();
                    chaObj.put("documentId",  nvl(cha.getDocumentId()));
                    chaObj.put("noiDung",     nvl(cha.getNoiDung()));
                    chaObj.put("hoVaTen",     nvl(cha.getHoVaTen()));
                    chaObj.put("tenDangNhap", nvl(cha.getTenDangNhap()));
                    chaObj.put("anhDaiDien",  nvl(cha.getAnhDaiDien()));
                    chaObj.put("verified",    cha.isVerified());
                    if (cha.getDanhSachAnh() != null) {
                        JSONArray imgs = new JSONArray();
                        for (String url : cha.getDanhSachAnh()) imgs.put(url);
                        chaObj.put("danhSachAnh", imgs);
                    }
                    obj.put("postCha", chaObj);
                }

                jsonArray.put(obj);
                count++;
            }

            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_POSTS, jsonArray.toString())
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply();

            Log.d("HomeFragment", "Cache saved: " + count + " bài");

        } catch (JSONException e) {
            Log.e("HomeFragment", "Lỗi lưu cache: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CACHE — LOAD & DISPLAY
    // Trả về true nếu có cache hợp lệ và đã hiển thị
    // ─────────────────────────────────────────────────────────────────────────

    private boolean loadAndShowCache() {
        if (!isAdded() || getContext() == null) return false;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String json    = prefs.getString(KEY_POSTS, null);
        long   savedAt = prefs.getLong(KEY_LAST_UPDATE, 0);

        if (json == null || json.isEmpty()) return false;

        // Cache quá cũ → không dùng (để skeleton xuất hiện)
        boolean isExpired = (System.currentTimeMillis() - savedAt) > CACHE_TTL_MS;
        if (isExpired) {
            Log.d("HomeFragment", "Cache hết hạn, dùng skeleton");
            return false;
        }

        try {
            JSONArray jsonArray = new JSONArray(json);
            List<PostModel> cachedPosts = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj  = jsonArray.getJSONObject(i);
                PostModel  post = new PostModel();

                post.setDocumentId(obj.optString("documentId", null));
                post.setNguoiDungId(obj.optString("nguoiDungId", null));
                post.setNoiDung(obj.optString("noiDung", ""));
                post.setHoVaTen(obj.optString("hoVaTen", ""));
                post.setTenDangNhap(obj.optString("tenDangNhap", ""));
                post.setAnhDaiDien(obj.optString("anhDaiDien", ""));
                post.setSoLuotThich(obj.optInt("soLuotThich", 0));
                post.setSoBinhLuan(obj.optInt("soBinhLuan", 0));
                post.setSoRepost(obj.optInt("soRepost", 0));
                post.setLikedByMe(obj.optBoolean("likedByMe", false));
                post.setRepostedByMe(obj.optBoolean("repostedByMe", false));
                post.setFollowing(obj.optBoolean("following", false));
                post.setVerified(obj.optBoolean("verified", false));
                post.setBaiVietChaId(obj.optString("baiVietChaId", ""));
                post.setLoading(false);

                if (obj.has("ngayTaoSec")) {
                    long sec = obj.getLong("ngayTaoSec");
                    post.setNgayTao(new Timestamp(sec, 0));
                }

                if (obj.has("danhSachAnh")) {
                    JSONArray imgs = obj.getJSONArray("danhSachAnh");
                    List<String> imgList = new ArrayList<>();
                    for (int j = 0; j < imgs.length(); j++) imgList.add(imgs.getString(j));
                    post.setDanhSachAnh(imgList);
                }

                if (obj.has("topComment")) {
                    post.setTopComment(deserializeComment(obj.getJSONObject("topComment")));
                }

                if (obj.has("postCha")) {
                    JSONObject chaObj = obj.getJSONObject("postCha");
                    PostModel cha     = new PostModel();
                    cha.setDocumentId(chaObj.optString("documentId", null));
                    cha.setNoiDung(chaObj.optString("noiDung", ""));
                    cha.setHoVaTen(chaObj.optString("hoVaTen", ""));
                    cha.setTenDangNhap(chaObj.optString("tenDangNhap", ""));
                    cha.setAnhDaiDien(chaObj.optString("anhDaiDien", ""));
                    cha.setVerified(chaObj.optBoolean("verified", false));
                    if (chaObj.has("danhSachAnh")) {
                        JSONArray imgs = chaObj.getJSONArray("danhSachAnh");
                        List<String> imgList = new ArrayList<>();
                        for (int j = 0; j < imgs.length(); j++) imgList.add(imgs.getString(j));
                        cha.setDanhSachAnh(imgList);
                    }
                    post.setPostCha(cha);
                }

                // Đồng bộ repostedByMe set
                if (post.isRepostedByMe() && post.getDocumentId() != null) {
                    repostedByMe.add(post.getDocumentId());
                }

                cachedPosts.add(post);
            }

            if (cachedPosts.isEmpty()) return false;

            postList.clear();
            postList.addAll(cachedPosts);
            adapter.notifyDataSetChanged();
            if (!postList.isEmpty()) rvFeed.scrollToPosition(0);

            Log.d("HomeFragment", "Hiển thị cache: " + cachedPosts.size() + " bài");
            return true;

        } catch (JSONException e) {
            Log.e("HomeFragment", "Lỗi đọc cache: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CACHE — CLEAR (gọi khi logout hoặc cần reset hoàn toàn)
    // ─────────────────────────────────────────────────────────────────────────

    public void clearFeedCache() {
        if (getContext() == null) return;
        requireContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
        Log.d("HomeFragment", "Cache cleared");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: serialize / deserialize Comment cho cache
    // ─────────────────────────────────────────────────────────────────────────

    private JSONObject serializeComment(CommentModel c) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("documentId", nvl(c.getDocumentId()));
        obj.put("noiDung",    nvl(c.getNoiDung()));
        obj.put("hoVaTen",    nvl(c.getHoVaTen()));
        obj.put("anhDaiDien", nvl(c.getAnhDaiDien()));
        obj.put("soLike",     c.getSoLike());
        if (c.getNgayTao() != null)
            obj.put("ngayTaoSec", c.getNgayTao().getSeconds());
        return obj;
    }

    private CommentModel deserializeComment(JSONObject obj) throws JSONException {
        CommentModel c = new CommentModel();
        c.setDocumentId(obj.optString("documentId", null));
        c.setNoiDung(obj.optString("noiDung", ""));
        c.setHoVaTen(obj.optString("hoVaTen", ""));
        c.setAnhDaiDien(obj.optString("anhDaiDien", ""));
        c.setSoLike(obj.optInt("soLike", 0));
        if (obj.has("ngayTaoSec"))
            c.setNgayTao(new Timestamp(obj.getLong("ngayTaoSec"), 0));
        return c;
    }

    /** Trả về chuỗi rỗng thay vì null để JSONObject không throw */
    private String nvl(String s) {
        return s != null ? s : "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD FEED
    // Flow: Cache → (nếu trống/hết hạn → Skeleton) → Firestore realtime
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromFirestore() {

        // ── PHASE 0: Hiển thị cache ngay lập tức ──────────────────────────
        //   Nếu cache hợp lệ → dùng cache, không skeleton
        //   Nếu cache hết hạn / trống → dùng skeleton như cũ
        if (!cacheDisplayed) {
            cacheDisplayed = true;
            boolean cacheShown = loadAndShowCache();
            if (!cacheShown) {
                showSkeletons(6);
            }
        }

        listenerRegistration = db.collection("bai_viet")
                .whereEqualTo("da_xoa", false)
                .whereIn("che_do_xem", Arrays.asList("public", ""))
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e("HomeFragment", "Lỗi load feed: " + error.getMessage());
                        // Lỗi mạng → xóa skeleton nếu còn, giữ nguyên cache
                        clearSkeletons();
                        return;
                    }
                    if (snapshots == null) return;

                    String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

                    // ─────────────── LOAD FIRST TIME ───────────────
                    if (!firstLoadDone) {
                        firstLoadDone = true;

                        List<DocumentChange> addedDocs = new ArrayList<>();
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() != DocumentChange.Type.ADDED) continue;
                            String nguoiDungId = dc.getDocument().getString("nguoi_dung_id");
                            if (!blockedSet.contains(nguoiDungId)) {
                                addedDocs.add(dc);
                            }
                        }

                        if (addedDocs.isEmpty()) {
                            // Không có bài nào → xóa skeleton (cache đã hiển thị rồi thì giữ)
                            clearSkeletons();
                            return;
                        }

                        // ── PHASE 1: Build PostModel từ Firestore ─────────────────
                        List<PostModel> phase1List = new ArrayList<>();
                        for (DocumentChange dc : addedDocs) {
                            PostModel post = dc.getDocument().toObject(PostModel.class);
                            post.setDocumentId(dc.getDocument().getId());
                            post.setLoading(false);

                            String baiVietChaId = dc.getDocument().getString("bai_viet_cha_id");
                            if (baiVietChaId != null && !baiVietChaId.isEmpty())
                                post.setBaiVietChaId(baiVietChaId);

                            // Khôi phục trạng thái like/repost từ cache
                            // (sẽ được ghi đè đúng bởi phase 3, nhưng tránh blink)
                            PostModel cached = findCachedPost(post.getDocumentId());
                            if (cached != null) {
                                post.setLikedByMe(cached.isLikedByMe());
                                post.setRepostedByMe(cached.isRepostedByMe());
                                post.setHoVaTen(cached.getHoVaTen());
                                post.setTenDangNhap(cached.getTenDangNhap());
                                post.setAnhDaiDien(cached.getAnhDaiDien());
                                post.setVerified(cached.isVerified());
                                post.setFollowing(cached.isFollowing());
                                post.setTopComment(cached.getTopComment());
                                post.setPostCha(cached.getPostCha());
                            }

                            phase1List.add(post);
                        }

                        // Sắp xếp và hiển thị (thay thế cache/skeleton)
                        sortAndRefresh(phase1List);

                        // Lưu cache ngay sau khi có dữ liệu Firestore đầu tiên
                        saveFeedCache();

                        // ── PHASE 2: Load user info + like/repost ngầm ────────────
                        for (PostModel post : new ArrayList<>(phase1List)) {
                            String nguoiDungId = post.getNguoiDungId();
                            loadUserDataAsync(post, nguoiDungId, myUid);
                        }

                        // ─────────────── REALTIME UPDATE ───────────────
                    } else {

                        boolean changed = false;

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            String docId = dc.getDocument().getId();

                            switch (dc.getType()) {

                                case ADDED: {
                                    PostModel post = dc.getDocument().toObject(PostModel.class);
                                    post.setDocumentId(docId);
                                    post.setLoading(false);

                                    String baiVietChaId = dc.getDocument().getString("bai_viet_cha_id");
                                    if (baiVietChaId != null && !baiVietChaId.isEmpty())
                                        post.setBaiVietChaId(baiVietChaId);

                                    String nguoiDungId = dc.getDocument().getString("nguoi_dung_id");
                                    if (blockedSet.contains(nguoiDungId)) break;

                                    checkBiHanChe(docId, myUid, () -> {
                                        postList.add(0, post);
                                        adapter.notifyItemInserted(0);
                                        rvFeed.scrollToPosition(0);
                                        loadUserDataAsync(post, nguoiDungId, myUid);
                                    }, () -> {});
                                    changed = true;
                                    break;
                                }

                                case MODIFIED: {
                                    PostModel newPost = dc.getDocument().toObject(PostModel.class);
                                    newPost.setDocumentId(docId);
                                    for (int i = 0; i < postList.size(); i++) {
                                        PostModel old = postList.get(i);
                                        if (old.getDocumentId() != null
                                                && old.getDocumentId().equals(docId)) {
                                            newPost.setTopComment(old.getTopComment());
                                            newPost.setTopReplies(old.getTopReplies());
                                            newPost.setFollowing(old.isFollowing());
                                            newPost.setHoVaTen(old.getHoVaTen());
                                            newPost.setAnhDaiDien(old.getAnhDaiDien());
                                            newPost.setTenDangNhap(old.getTenDangNhap());
                                            newPost.setVerified(old.isVerified());
                                            newPost.setLikedByMe(old.isLikedByMe());
                                            newPost.setSoLuotThich(old.getSoLuotThich());
                                            newPost.setPostCha(old.getPostCha());
                                            newPost.setBaiVietChaId(old.getBaiVietChaId());
                                            newPost.setLoading(false);
                                            postList.set(i, newPost);
                                            adapter.notifyItemChanged(i, "LIKE_UPDATE");
                                            changed = true;
                                            break;
                                        }
                                    }
                                    break;
                                }

                                case REMOVED: {
                                    for (int i = 0; i < postList.size(); i++) {
                                        if (postList.get(i).getDocumentId() != null
                                                && postList.get(i).getDocumentId().equals(docId)) {
                                            postList.remove(i);
                                            adapter.notifyItemRemoved(i);
                                            changed = true;
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }

                        // Lưu cache sau mỗi batch realtime update
                        if (changed) saveFeedCache();
                    }

                    Log.d("HomeFragment", "Feed: " + postList.size() + " bài");
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: tìm bài trong postList hiện tại theo documentId (để lấy cache state)
    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    private PostModel findCachedPost(String docId) {
        if (docId == null) return null;
        for (PostModel p : postList) {
            if (docId.equals(p.getDocumentId())) return p;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SKELETON HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void showSkeletons(int count) {
        if (!postList.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            PostModel sk = new PostModel();
            sk.setLoading(true);
            postList.add(sk);
        }
        adapter.notifyDataSetChanged();
    }

    private void clearSkeletons() {
        for (int i = postList.size() - 1; i >= 0; i--) {
            if (postList.get(i).isLoading()) {
                postList.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 2 — LOAD USER DATA NGẦM
    // ─────────────────────────────────────────────────────────────────────────

    private void loadUserDataAsync(PostModel post, String nguoiDungId, String myUid) {
        if (!isAdded()) return;

        if (nguoiDungId == null || nguoiDungId.isEmpty()) {
            post.setFollowing(false);
            loadLikeRepostAsync(post, myUid);
            checkAndLoadChaAsync(post);
            return;
        }

        db.collection("nguoi_dung").document(nguoiDungId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!isAdded()) return;
                    if (userDoc.exists()) {
                        post.setHoVaTen(userDoc.getString("ho_va_ten"));
                        post.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                        post.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                        post.setVerified(Boolean.TRUE.equals(userDoc.getBoolean("verified")));
                    }
                    post.setFollowing(followingSet.contains(nguoiDungId));

                    int idx = findPostIndex(post.getDocumentId());
                    if (idx >= 0) adapter.notifyItemChanged(idx, "USER_UPDATE");

                    loadLikeRepostAsync(post, myUid);
                    checkAndLoadChaAsync(post);
                    loadTopCommentAsync(post);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e("HomeFragment", "Lỗi load user: " + e.getMessage());
                    loadLikeRepostAsync(post, myUid);
                    checkAndLoadChaAsync(post);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 3a — LOAD LIKE + REPOST STATE NGẦM
    // ─────────────────────────────────────────────────────────────────────────

    private void loadLikeRepostAsync(PostModel post, String myUid) {
        if (myUid == null || !isAdded()) return;

        String docId = post.getDocumentId();
        if (docId == null) return;

        db.collection("bai_viet").document(docId)
                .collection("luot_thich").document(myUid).get()
                .addOnSuccessListener(likeDoc -> {
                    if (!isAdded()) return;
                    post.setLikedByMe(likeDoc.exists());

                    int idx = findPostIndex(docId);
                    if (idx >= 0) adapter.notifyItemChanged(idx, "LIKE_UPDATE");

                    db.collection("bai_viet")
                            .whereEqualTo("nguoi_dung_id", myUid)
                            .whereEqualTo("bai_viet_cha_id", docId)
                            .whereEqualTo("is_repost", true)
                            .whereEqualTo("da_xoa", false)
                            .get()
                            .addOnSuccessListener(repostSnap -> {
                                if (!isAdded()) return;
                                if (!repostSnap.isEmpty()) {
                                    repostedByMe.add(docId);
                                    post.setRepostedByMe(true);
                                    int i = findPostIndex(docId);
                                    if (i >= 0) adapter.notifyItemChanged(i, "REPOST_UPDATE");
                                }
                                // Lưu cache sau khi có đủ like/repost state
                                saveFeedCache();
                            });
                })
                .addOnFailureListener(e -> post.setLikedByMe(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 3b — LOAD BÀI VIẾT CHA NGẦM
    // ─────────────────────────────────────────────────────────────────────────

    private void checkAndLoadChaAsync(PostModel post) {
        String chaId = post.getBaiVietChaId();
        if (chaId == null || chaId.isEmpty()) return;

        db.collection("bai_viet").document(chaId).get()
                .addOnSuccessListener(chaDoc -> {
                    if (!isAdded() || !chaDoc.exists()) return;

                    PostModel postCha = chaDoc.toObject(PostModel.class);
                    postCha.setDocumentId(chaDoc.getId());

                    String chaUid = chaDoc.getString("nguoi_dung_id");
                    if (chaUid != null && !chaUid.isEmpty()) {
                        db.collection("nguoi_dung").document(chaUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (!isAdded()) return;
                                    if (userDoc.exists()) {
                                        postCha.setHoVaTen(userDoc.getString("ho_va_ten"));
                                        postCha.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                        postCha.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                        postCha.setVerified(Boolean.TRUE.equals(
                                                userDoc.getBoolean("verified")));
                                    }
                                    post.setPostCha(postCha);
                                    int idx = findPostIndex(post.getDocumentId());
                                    if (idx >= 0) adapter.notifyItemChanged(idx, "USER_UPDATE");
                                })
                                .addOnFailureListener(e -> {
                                    post.setPostCha(postCha);
                                    int idx = findPostIndex(post.getDocumentId());
                                    if (idx >= 0) adapter.notifyItemChanged(idx, "USER_UPDATE");
                                });
                    } else {
                        post.setPostCha(postCha);
                        int idx = findPostIndex(post.getDocumentId());
                        if (idx >= 0) adapter.notifyItemChanged(idx, "USER_UPDATE");
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 3c — LOAD TOP COMMENT NGẦM
    // ─────────────────────────────────────────────────────────────────────────

    private void loadTopCommentAsync(@NonNull PostModel post) {
        if (!isAdded()) return;

        db.collection("bai_viet").document(post.getDocumentId())
                .collection("binh_luan")
                .orderBy("so_like", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || snapshot.isEmpty()) return;

                    Map<String, CommentModel> allComments = new HashMap<>();
                    Map<String, List<CommentModel>> replyMap = new HashMap<>();

                    int total    = snapshot.size();
                    int[] loaded = {0};

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        CommentModel comment = doc.toObject(CommentModel.class);
                        if (comment == null) {
                            loaded[0]++;
                            if (loaded[0] == total)
                                finishCommentLoadAsync(allComments, replyMap, post);
                            continue;
                        }

                        comment.setDocumentId(doc.getId());
                        allComments.put(comment.getDocumentId(), comment);

                        String parentId = comment.getBinhLuanChaId();
                        if (parentId != null && !parentId.isEmpty()) {
                            if (!replyMap.containsKey(parentId))
                                replyMap.put(parentId, new ArrayList<>());
                            replyMap.get(parentId).add(comment);
                        }

                        String uid = comment.getNguoiDungId();
                        if (uid != null && !uid.isEmpty()) {
                            db.collection("nguoi_dung").document(uid).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            comment.setHoVaTen(userDoc.getString("ho_va_ten"));
                                            comment.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                        }
                                        loaded[0]++;
                                        if (loaded[0] == total)
                                            finishCommentLoadAsync(allComments, replyMap, post);
                                    })
                                    .addOnFailureListener(e -> {
                                        loaded[0]++;
                                        if (loaded[0] == total)
                                            finishCommentLoadAsync(allComments, replyMap, post);
                                    });
                        } else {
                            loaded[0]++;
                            if (loaded[0] == total)
                                finishCommentLoadAsync(allComments, replyMap, post);
                        }
                    }
                })
                .addOnFailureListener(e -> { /* comment load fail → bỏ qua */ });
    }

    private void finishCommentLoadAsync(Map<String, CommentModel> allComments,
                                        Map<String, List<CommentModel>> replyMap,
                                        PostModel post) {
        if (!isAdded() || allComments.isEmpty()) return;

        CommentModel mostLiked = null;
        for (CommentModel c : allComments.values()) {
            if (mostLiked == null || c.getSoLike() > mostLiked.getSoLike())
                mostLiked = c;
        }
        if (mostLiked == null) return;

        CommentModel root = mostLiked;
        while (root.getBinhLuanChaId() != null && !root.getBinhLuanChaId().isEmpty()) {
            CommentModel parent = allComments.get(root.getBinhLuanChaId());
            if (parent == null) break;
            root = parent;
        }

        post.setTopComment(root);

        List<CommentModel> replies = replyMap.get(root.getDocumentId());
        if (replies != null) {
            Collections.sort(replies, (a, b) -> {
                Timestamp t1 = a.getNgayTao(), t2 = b.getNgayTao();
                if (t1 == null || t2 == null) return 0;
                return t1.compareTo(t2);
            });
            post.setTopReplies(replies);
        }

        int idx = findPostIndex(post.getDocumentId());
        if (idx >= 0) adapter.notifyItemChanged(idx, "COMMENT_UPDATE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER — tìm index của post trong list theo documentId
    // ─────────────────────────────────────────────────────────────────────────

    private int findPostIndex(String docId) {
        if (docId == null) return -1;
        for (int i = 0; i < postList.size(); i++) {
            if (docId.equals(postList.get(i).getDocumentId())) return i;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK BỊ HẠN CHẾ
    // ─────────────────────────────────────────────────────────────────────────

    private void checkBiHanChe(String postDocId, String myUid,
                               Runnable onNotRestricted, Runnable onRestricted) {
        if (myUid == null || postDocId == null) { onNotRestricted.run(); return; }

        db.collection("bai_viet").document(postDocId)
                .collection("nguoi_dang_han_che").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) onRestricted.run();
                    else onNotRestricted.run();
                })
                .addOnFailureListener(e -> onNotRestricted.run());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SORT & REFRESH
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // QUICK POST
    // ─────────────────────────────────────────────────────────────────────────

    private void quickPost(String content) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (myUid == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        btnQuickPost.setEnabled(false);

        Map<String, Object> post = new HashMap<>();
        post.put("nguoi_dung_id", myUid);
        post.put("noi_dung", content);
        post.put("ngay_tao", Timestamp.now());
        post.put("da_xoa", false);
        post.put("che_do_xem", "public");
        post.put("so_like", 0);
        post.put("so_binh_luan", 0);
        post.put("so_repost", 0);
        post.put("so_share", 0);
        post.put("danh_sach_anh", new ArrayList<>());
        post.put("bai_viet_cha_id", "");
        post.put("is_repost", false);

        db.collection("bai_viet").add(post)
                .addOnSuccessListener(ref -> {
                    tvQuickPostHint.setText("");
                    tvQuickPostHint.clearFocus();
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    requireContext().getSystemService(
                                            android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(tvQuickPostHint.getWindowToken(), 0);
                    btnQuickPost.setEnabled(true);
                    saveHashtags(content, ref.getId());
                    Toast.makeText(getContext(), "Đã đăng!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnQuickPost.setEnabled(true);
                    Toast.makeText(getContext(), "Lỗi đăng bài: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveHashtags(String noiDung, String postId) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\w+");
        java.util.regex.Matcher matcher = pattern.matcher(noiDung);

        while (matcher.find()) {
            String tag = matcher.group().substring(1).toLowerCase();
            DocumentReference tagRef = db.collection("hashtag").document(tag);

            Map<String, Object> tagData = new HashMap<>();
            tagData.put("ten", tag);
            tagData.put("so_bai_viet", FieldValue.increment(1));
            tagData.put("lan_cuoi_su_dung", Timestamp.now());
            tagRef.set(tagData, SetOptions.merge());

            Map<String, Object> postRef = new HashMap<>();
            postRef.put("bai_viet_id", postId);
            postRef.put("ngay_tao", Timestamp.now());
            tagRef.collection("bai_viet").document(postId).set(postRef);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMMERSIVE MODE
    // ─────────────────────────────────────────────────────────────────────────

    private void enableImmersiveMode() {
        if (getActivity() == null) return;
        Window window = getActivity().getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}