package com.example.doanmxh.Notifications;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.HomePage.PostDetailActivity;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
public class NotificationsFragment extends Fragment
        implements NotificationAdapter.OnNotificationActionListener {

    private static final String TAG = "NotificationsFragment";

    // ─── Tab IDs ──────────────────────────────────────────────────────────────
    private static final int TAB_ALL          = R.id.btnAll;
    private static final int TAB_UNREAD          = R.id.btnUnread;
    private static final int TAB_FOLLOW       = R.id.btnFollow;
    private static final int TAB_CONVERSATION = R.id.btnConversation;
    private static final int TAB_MENTION      = R.id.btnMention;
    private static final int TAB_FOLLOWING    = R.id.btnFollowing;
    private static final int TAB_REPOST       = R.id.btnRepost;

    // ─── Views ────────────────────────────────────────────────────────────────
    private Button     btnAll, btnFollow, btnConversation, btnMention, btnFollowing, btnRepost,btnUnread;
    private RecyclerView recyclerView;
    private TextView     tvEmpty;

    // ─── Data ─────────────────────────────────────────────────────────────────
    private final List<NotificationModel> allItems     = new ArrayList<>();
    private final List<NotificationModel> displayItems = new ArrayList<>();
    private NotificationAdapter adapter;

    // ─── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private String               myUid;
    private ListenerRegistration listenerReg;

    private int currentTabId = TAB_ALL;

    public interface OnUnreadCountListener {
        void onUnreadCountChanged(int count);
    }

    private OnUnreadCountListener unreadCountListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnUnreadCountListener) {
            unreadCountListener = (OnUnreadCountListener) context;
        }
    }

    // Hàm đếm và gửi số chưa đọc — gọi sau mỗi lần allItems thay đổi
    private void notifyUnreadCount() {
        if (unreadCountListener == null) return;
        int count = 0;
        for (NotificationModel item : allItems) {
            if (!item.isRead()) count++;
        }
        unreadCountListener.onUnreadCountChanged(count);
    }
    // ══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db    = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        btnAll          = view.findViewById(R.id.btnAll);
        btnUnread          = view.findViewById(R.id.btnUnread);
        btnFollow       = view.findViewById(R.id.btnFollow);
        btnConversation = view.findViewById(R.id.btnConversation);
        btnMention      = view.findViewById(R.id.btnMention);
        btnFollowing    = view.findViewById(R.id.btnFollowing);
        btnRepost       = view.findViewById(R.id.btnRepost);
        recyclerView    = view.findViewById(R.id.recyclerView);
        tvEmpty         = view.findViewById(R.id.tvEmpty);

        setupAdapter();
        setupTabButtons();
        loadNotificationsCache(); // hiện cache ngay
//        markAsRead();
        selectTab(TAB_ALL);
        startListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Setup
    // ══════════════════════════════════════════════════════════════════════════

    private void setupAdapter() {
        adapter = new NotificationAdapter(displayItems);
        adapter.setOnNotificationActionListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabButtons() {
        View.OnClickListener tabClick = v -> selectTab(v.getId());
        btnAll.setOnClickListener(tabClick);
        btnUnread.setOnClickListener(tabClick);
        btnFollow.setOnClickListener(tabClick);
        btnConversation.setOnClickListener(tabClick);
        btnMention.setOnClickListener(tabClick);
        btnFollowing.setOnClickListener(tabClick);
        btnRepost.setOnClickListener(tabClick);
    }

    private void selectTab(int tabId) {
        currentTabId = tabId;
        for (Button btn : new Button[]{btnAll,btnUnread, btnFollow, btnConversation,
                btnMention, btnFollowing, btnRepost}) {
            btn.setSelected(false);
            btn.setTypeface(null, Typeface.NORMAL);
        }
        Button active = requireView().findViewById(tabId);
        if (active != null) {
            active.setSelected(true);
            active.setTypeface(null, Typeface.BOLD);
        }
        filterAndShow();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Firestore real-time listener
    // ══════════════════════════════════════════════════════════════════════════

    private void startListening() {
        if (myUid == null) return;

        listenerReg = db.collection("notifications")
                .whereEqualTo("receiverId", myUid)
                .orderBy("time", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listener error: " + error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    allItems.clear();
                    List<DocumentSnapshot> docs = snapshots.getDocuments();

                    if (docs.isEmpty()) {
                        filterAndShow();
                        return;
                    }

                    // Giữ đúng thứ tự Firestore trả về (mới nhất trước)
                    final int total = docs.size();
                    final NotificationModel[] ordered = new NotificationModel[total]; // ← Mảng giữ thứ tự
                    final int[] done = {0};

                    for (int i = 0; i < total; i++) {
                        final int index = i; // ← Lưu index để đặt đúng vị trí
                        DocumentSnapshot doc = docs.get(i);
                        buildModel(doc, model -> {
                            ordered[index] = model; // ← Đặt vào đúng vị trí, không dùng add()
                            done[0]++;
                            if (done[0] == total) {
                                // Tất cả done → add vào allItems theo đúng thứ tự
                                for (NotificationModel m : ordered) {
                                    if (m != null) allItems.add(m);
                                }
                                saveNotificationsCache();
                                notifyUnreadCount();
                                filterAndShow();
                            }
                        });
                    }
                });
    }
    /**
     * Firestore doc → NotificationModel.
     *
     * Luồng fetch (như Threads):
     *  1. Lấy thông tin sender (avatar, tên)
     *  2. Kiểm tra mình có đang follow sender không
     *  3. Nếu type cần bài viết (LIKE, COMMENT, LIKE_COMMENT, REPOST, MENTION):
     *     → Fetch bài viết để lấy thumbnail + snippet
     *  4. Nếu type COMMENT hoặc LIKE_COMMENT, fetch thêm nội dung bình luận
     *     làm snippet (hiển thị preview dưới tên như Threads)
     */
    private void buildModel(DocumentSnapshot doc, OnModelReady callback) {
        String  id         = doc.getId();
        String  type       = doc.getString("type");
        String  senderId   = doc.getString("senderId");
        String  postId     = doc.getString("postId");
        String  commentId  = doc.getString("commentId");   // có với COMMENT & LIKE_COMMENT
        String  content    = doc.getString("content");
        Boolean readRaw    = doc.getBoolean("isRead");
        boolean isRead     = readRaw != null && readRaw;
        long    timeMs     = doc.getLong("time") != null ? doc.getLong("time") : 0L;
        String  timeStr    = formatTime(timeMs);

        if (senderId == null || senderId.isEmpty()) {
            callback.onReady(null);
            return;
        }

        // ── Bước 1: fetch thông tin sender ───────────────────────────────────
        db.collection("nguoi_dung").document(senderId).get()
                .addOnSuccessListener(userDoc -> {
                    String avatar = userDoc.getString("anh_dai_dien");
                    String name   = userDoc.getString("ho_va_ten");

                    Log.d("NOTI_AVATAR", "name=" + name);
                    Log.d("NOTI_AVATAR", "avatar=" + avatar);
                    // ── Bước 2: kiểm tra follow ──────────────────────────────
                    db.collection("nguoi_dung").document(myUid)
                            .collection("nguoi_dang_theo_doi").document(senderId).get()
                            .addOnSuccessListener(followDoc -> {
                                boolean isFollowing = followDoc.exists();

                                boolean isFollowOnlyType = isFollowOnlyType(type);
                                boolean needPost = postId != null && !postId.isEmpty()
                                        && !isFollowOnlyType;

                                if (needPost) {
                                    // ── Bước 3: fetch bài viết ───────────────
                                    db.collection("bai_viet").document(postId).get()
                                            .addOnSuccessListener(postDoc -> {
                                                String img     = null;
                                                String video     = null;
                                                String audio     = null;
                                                String snippet = null;
                                                if (postDoc.exists()) {
                                                    snippet = postDoc.getString("noi_dung");
                                                    List<String> imgs = (List<String>) postDoc.get("danh_sach_anh");
                                                    if (imgs != null && !imgs.isEmpty()) img = imgs.get(0);
                                                    List<String> vds = (List<String>) postDoc.get("danh_sach_video");
                                                    if (vds != null && !vds.isEmpty()) video = vds.get(0);
                                                    List<String> aus = (List<String>) postDoc.get("danh_sach_audio");
                                                    if (aus != null && !aus.isEmpty()) audio = aus.get(0);
                                                }

                                                boolean needComment = isCommentType(type)
                                                        && commentId != null && !commentId.isEmpty();

                                                if (needComment) {
                                                    // ── Bước 4: fetch nội dung bình luận ──
                                                    // Dùng làm snippet preview thay bài viết
                                                    final String postImg      = img;
                                                    final String postVideo = video;
                                                    final String postAudio = audio;
                                                    final String fallbackSnip = snippet; // effectively final để dùng trong lambda
                                                    db.collection("bai_viet").document(postId)
                                                            .collection("binh_luan").document(commentId).get()
                                                            .addOnSuccessListener(commentDoc -> {
                                                                // Tính toán giá trị cuối cùng trước, không gán lại biến
                                                                String cmtText = commentDoc.exists()
                                                                        ? commentDoc.getString("noi_dung") : null;
                                                                final String commentSnippet = (cmtText != null && !cmtText.isEmpty())
                                                                        ? cmtText : fallbackSnip;
                                                                callback.onReady(make(id, senderId, avatar, name,
                                                                        content, timeStr, type, postId,
                                                                        postImg,postVideo,postAudio, commentSnippet, isRead, isFollowing));
                                                            })
                                                            .addOnFailureListener(e ->
                                                                    // Fallback: dùng snippet bài viết
                                                                    callback.onReady(make(id, senderId, avatar, name,
                                                                            content, timeStr, type, postId,
                                                                            postImg,postVideo,postAudio, fallbackSnip, isRead, isFollowing)));
                                                } else {
                                                    callback.onReady(make(id, senderId, avatar, name,
                                                            content, timeStr, type, postId,
                                                            img,video,audio, snippet, isRead, isFollowing));
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    callback.onReady(make(id, senderId, avatar, name,
                                                            content, timeStr, type, postId,null,null,
                                                            null, null, isRead, isFollowing)));
                                } else {
                                    callback.onReady(make(id, senderId, avatar, name,
                                            content, timeStr, type, postId,null,null,
                                            null, null, isRead, isFollowing));
                                }
                            })
                            .addOnFailureListener(e ->
                                    callback.onReady(make(id, senderId, avatar, name,
                                            content, timeStr, type, postId,null,null,
                                            null, null, isRead, false)));
                })
                .addOnFailureListener(e -> callback.onReady(null));
    }
    private void saveNotificationsCache() {
        SharedPreferences pref =
                requireContext().getSharedPreferences("noti_cache", Context.MODE_PRIVATE);

        String json = new Gson().toJson(allItems);

        pref.edit()
                .putString("notifications", json)
                .apply();
    }
    private void loadNotificationsCache() {

        SharedPreferences pref =
                requireContext().getSharedPreferences("noti_cache", Context.MODE_PRIVATE);

        String json = pref.getString("notifications", null);

        if (json == null) return;

        List<NotificationModel> cache =
                new Gson().fromJson(
                        json,
                        new TypeToken<List<NotificationModel>>() {}.getType()
                );

        if (cache != null) {
            allItems.clear();
            allItems.addAll(cache);
            notifyUnreadCount();
            filterAndShow();
        }
    }
    private NotificationModel make(String id, String senderId, String avatar, String name,
                                   String content, String time, String type,
                                   String postId, String img,String video,String audio, String snippet,
                                   boolean isRead, boolean isFollowing) {
        return new NotificationModel(id, senderId, avatar, name, content, time,
                type != null ? type.toUpperCase() : "",
                postId, img,video,audio, snippet, isRead, isFollowing);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Lọc & hiển thị theo tab
    // ══════════════════════════════════════════════════════════════════════════

    private void filterAndShow() {
        displayItems.clear();
        for (NotificationModel item : allItems) {
            if (matchesTab(item)) displayItems.add(item);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private boolean matchesTab(NotificationModel item) {
        String t = item.getType() == null ? "" : item.getType();

        if (currentTabId == TAB_UNREAD)
            return !item.isRead();

        if (currentTabId == TAB_FOLLOW)
            return "FOLLOW".equals(t);

        if (currentTabId == TAB_CONVERSATION)
            return "COMMENT".equals(t)
                    || "LIKE".equals(t)
                    || "LIKE_COMMENT".equals(t);

        if (currentTabId == TAB_MENTION)
            return "MENTION".equals(t);

        if (currentTabId == TAB_FOLLOWING)
            return "FOLLOWING".equals(t);

        if (currentTabId == TAB_REPOST)
            return "REPOST".equals(t);

        return true; // TAB_ALL
    }

    private void updateEmptyView() {
        boolean empty = displayItems.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NotificationAdapter.OnNotificationActionListener
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onFollowToggle(NotificationModel item, int position) {
        if (myUid == null || item.getSenderId() == null) return;

        boolean nowFollowing = !item.isFollowing();
        item.setFollowing(nowFollowing);
        adapter.notifyItemChanged(position);

        String targetUid = item.getSenderId();
        if (nowFollowing) {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", targetUid);
            data.put("time", System.currentTimeMillis());
            db.collection("nguoi_dung").document(myUid)
                    .collection("nguoi_dang_theo_doi").document(targetUid).set(data)
                    .addOnFailureListener(e -> {
                        item.setFollowing(false);
                        adapter.notifyItemChanged(position);
                        showToast("Lỗi: " + e.getMessage());
                    });
        } else {
            db.collection("nguoi_dung").document(myUid)
                    .collection("nguoi_dang_theo_doi").document(targetUid).delete()
                    .addOnFailureListener(e -> {
                        item.setFollowing(true);
                        adapter.notifyItemChanged(position);
                        showToast("Lỗi: " + e.getMessage());
                    });
        }
    }

    @Override
    public void onItemClick(NotificationModel item) {
        markAsRead(item);
        if (item.getPostId() != null && !item.getPostId().isEmpty()) {
            Intent intent = new Intent(requireContext(), PostDetailActivity.class);
            intent.putExtra("post_id", item.getPostId());
            startActivity(intent);
        } else {
            openUserProfile(item.getSenderId());
        }
    }

    @Override
    public void onMoreClick(NotificationModel item, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Đánh dấu đã đọc");
        popup.getMenu().add(0, 2, 1, "Xem hồ sơ");
        popup.getMenu().add(0, 3, 2, "Xóa thông báo");
        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 1: markAsRead(item);                    return true;
                case 2: openUserProfile(item.getSenderId()); return true;
                case 3: deleteNotification(item);            return true;
            }
            return false;
        });
        popup.show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Firestore helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void markAsRead(NotificationModel item) {
        if (item.isRead()) return;

        item.setRead(true);

        int position = displayItems.indexOf(item);
        if (position >= 0) {
            adapter.notifyItemChanged(position); // UI update
        }

        notifyUnreadCount();    // badge giảm ngay
        saveNotificationsCache(); // cache đồng bộ ngay

        db.collection("notifications")
                .document(item.getId())
                .update("isRead", true)
                .addOnSuccessListener(unused ->
                        Log.d("NOTI", "Update success: " + item.getId()))
                .addOnFailureListener(e -> {
                    // Rollback
                    item.setRead(false);
                    if (position >= 0) adapter.notifyItemChanged(position);
                    notifyUnreadCount();
                    saveNotificationsCache();
                    Log.e("NOTI", "Update fail", e);
                });
    }

    private void deleteNotification(NotificationModel item) {
        db.collection("notifications").document(item.getId()).delete()
                .addOnSuccessListener(unused -> {
                    allItems.remove(item);
                    saveNotificationsCache(); // ← THÊM
                    notifyUnreadCount();      // ← THÊM
                    filterAndShow();
                })
                .addOnFailureListener(e -> showToast("Xóa thất bại: " + e.getMessage()));
    }

    public void markAllAsRead() {
        for (NotificationModel item : allItems) markAsRead(item);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════════════════════

    private void openUserProfile(String uid) {
        if (uid == null || uid.isEmpty()) return;
        try {
            Class<?> cls = Class.forName("com.example.doanmxh.ProfilePage.UserProfileActivity");
            Intent intent = new Intent(requireContext(), cls);
            intent.putExtra("user_uid", uid);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "UserProfileActivity chưa tồn tại");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private boolean isFollowOnlyType(String type) {
        return "follow".equalsIgnoreCase(type) || "following".equalsIgnoreCase(type);
    }

    /** Trả về true với các type liên quan đến bình luận (cần fetch nội dung BL). */
    private boolean isCommentType(String type) {
        return "comment".equalsIgnoreCase(type) || "like_comment".equalsIgnoreCase(type);
    }

    private String formatTime(long timeMs) {
        if (timeMs <= 0) return "";
        long diff    = System.currentTimeMillis() - timeMs;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours   = TimeUnit.MILLISECONDS.toHours(diff);
        long days    = TimeUnit.MILLISECONDS.toDays(diff);
        if (seconds < 60) return "vừa xong";
        if (minutes < 60) return minutes + " phút";
        if (hours < 24)   return hours + " giờ";
        if (days < 7)     return days + " ngày";
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timeMs));
    }

    private void showToast(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private interface OnModelReady {
        void onReady(@Nullable NotificationModel model);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Static helpers — gửi thông báo từ bất kỳ chỗ nào trong app
    // ══════════════════════════════════════════════════════════════════════════

    public static void sendFollowNotification(String receiverId, String senderId) {
        Map<String, Object> d = new HashMap<>();
        d.put("type", "follow");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("time", System.currentTimeMillis());
        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }

    /**
     * Gửi thông báo "đã bình luận bài viết của bạn" (type = COMMENT).
     *
     * @param receiverId  chủ bài viết
     * @param senderId    người bình luận
     * @param postId      id bài viết
     * @param commentId   id bình luận vừa tạo (để buildModel fetch nội dung preview)
     * @param content     nội dung bình luận (lưu thẳng vào notification để dùng khi offline)
     */
    public static void sendCommentNotification(String receiverId, String senderId,
                                               String postId, String commentId,
                                               String content) {
        if (receiverId == null || senderId == null) return;
        if (receiverId.equals(senderId)) return;
        Map<String, Object> d = new HashMap<>();
        d.put("type", "COMMENT");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("postId", postId);
        d.put("commentId", commentId);
        d.put("content", content);       // preview bình luận
        d.put("time", System.currentTimeMillis());
        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }

    /**
     * Gửi thông báo "đã thích bình luận của bạn" (type = LIKE_COMMENT).
     *
     * @param receiverId   chủ bình luận
     * @param senderId     người thích
     * @param commentId    id bình luận (để buildModel fetch text preview)
     * @param postId       id bài viết chứa bình luận
     * @param commentText  nội dung bình luận (preview dự phòng)
     */
    public static void sendLikeCommentNotification(String receiverId, String senderId,
                                                   String commentId, String postId,
                                                   String commentText) {
        if (receiverId == null || senderId == null) return;
        if (receiverId.equals(senderId)) return;
        Map<String, Object> d = new HashMap<>();
        d.put("type", "LIKE_COMMENT");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("postId", postId);
        d.put("commentId", commentId);
        d.put("content", commentText);   // nội dung BL để preview khi cần
        d.put("time", System.currentTimeMillis());
        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }

    public static void sendMentionNotification(String receiverId, String senderId, String postId) {
        if (receiverId == null || senderId == null) return;
        if (receiverId.equals(senderId)) return;
        Map<String, Object> d = new HashMap<>();
        d.put("type", "MENTION");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("postId", postId);
        d.put("time", System.currentTimeMillis());
        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }
    public static void sendMentionCommentNotification(String receiverId, String senderId, String commentId) {
        if (receiverId == null || senderId == null) return;
        if (receiverId.equals(senderId)) return;
        Map<String, Object> d = new HashMap<>();
        d.put("type", "mention");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("commentId", commentId);
        d.put("time", System.currentTimeMillis());
//        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }

    public static void sendLikeNotification(String receiverId, String senderId, String postId) {
        if (receiverId == null || senderId == null) return;
        if (receiverId.equals(senderId)) return;
        Map<String, Object> d = new HashMap<>();
        d.put("type", "LIKE");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("postId", postId);
        d.put("time", System.currentTimeMillis());
        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }

    public static void sendRepostNotification(String receiverId, String senderId, String postId) {
        if (receiverId == null || senderId == null) return;
        if (receiverId.equals(senderId)) return;
        Map<String, Object> d = new HashMap<>();
        d.put("type", "REPOST");
        d.put("receiverId", receiverId);
        d.put("senderId", senderId);
        d.put("postId", postId);
        d.put("time", System.currentTimeMillis());
        d.put("isRead", false);
        FirebaseFirestore.getInstance().collection("notifications").add(d);
    }

    /** Gửi thông báo "bài đăng mới" cho tất cả follower của tác giả. */
    public static void sendNewPostToFollowers(String authorId, String postId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("nguoi_dung").document(authorId)
                .collection("nguoi_theo_doi").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String fid = doc.getId();
                        if (fid.equals(authorId)) continue;
                        Map<String, Object> d = new HashMap<>();
                        d.put("type", "FOLLOWING");
                        d.put("receiverId", fid);
                        d.put("senderId", authorId);
                        d.put("postId", postId);
                        d.put("time", System.currentTimeMillis());
                        d.put("isRead", false);
                        db.collection("notifications").add(d);
                    }
                });
    }
}