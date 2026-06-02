package com.example.doanmxh.Message;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageFragment extends Fragment {
    private FirebaseFirestore db;
    private ListenerRegistration friendStoryListener;
    private FirebaseAuth auth;
    private ImageView    btnNewMessage, imgMyAvatar;
    private LinearLayout layoutMyStory;
    private RecyclerView rvFriendStories, rvChatList;
    private TextView tvMyStory;
    private FriendStoryAdapter friendStoryAdapter;
    private ChatListAdapter    chatListAdapter;
    private ListenerRegistration chatListListener;
    private List<FriendStoryItem> friendStoryList;
    private List<ChatUser>        chatList;
    // ── Thêm fields quản lý listener ─────────────────────
    private final Map<String, ListenerRegistration> friendInfoListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> chatUserListeners   = new HashMap<>();
    public static MessageFragment newInstance() {
        return new MessageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo list TRƯỚC khi làm gì khác
        friendStoryList = new ArrayList<>();
        chatList = new ArrayList<>();

        initViews(view);
        setupRecyclerViews();   // adapter nhận list rỗng, không crash
        createMockData();       // data load xong thì notify
        setupClickListeners();
    }

    private void initViews(View view) {
        btnNewMessage    = view.findViewById(R.id.btnNewMessage);
        imgMyAvatar      = view.findViewById(R.id.imgMyAvatar);
        layoutMyStory    = view.findViewById(R.id.layoutMyStory);
        rvFriendStories  = view.findViewById(R.id.rvFriendStories);
        rvChatList       = view.findViewById(R.id.rvChatList);
        tvMyStory = view.findViewById(R.id.tvMyStory);
    }

//    private void createMockData() {
//        int av = android.R.drawable.sym_def_app_icon;
//
//        // Hàng story + online gộp chung
//        friendStoryList = new ArrayList<>();
////        friendStoryList.add(new FriendStoryItem("Huy",   av, true,  FriendStoryItem.StoryState.NEW,  "Đang nghĩ về..."));
////        friendStoryList.add(new FriendStoryItem("Bảo",   av, true,  FriendStoryItem.StoryState.NEW,  "Một mình 1nG"));
////        friendStoryList.add(new FriendStoryItem("Han",   av, true,  FriendStoryItem.StoryState.SEEN, ""));
////        friendStoryList.add(new FriendStoryItem("Châu",  av, false, FriendStoryItem.StoryState.NEW,  "Cà phê sáng ☕"));
////        friendStoryList.add(new FriendStoryItem("Minh",  av, true,  FriendStoryItem.StoryState.NONE, ""));
////        friendStoryList.add(new FriendStoryItem("Trang", av, false, FriendStoryItem.StoryState.SEEN, "Ngủ sớm nha"));
//            String uid = auth.getCurrentUser().getUid();
//        db.collection("nguoi_dung").document(uid)
//                .get()
//                .addOnSuccessListener( documentSnapshot -> {
//                            String myAvatar = documentSnapshot.getString("anh_dai_dien");
//                            if (myAvatar != null) {
//                                Glide.with(this)
//                                        .load(myAvatar)
//                                        .placeholder(R.drawable.ic_placeholder_avatar)
//                                        .into(imgMyAvatar);
//                            }
//                            tvMyStory.setText(documentSnapshot.getString("ho_va_ten"));
//                });
//            db.collection("nguoi_dung").document(uid)
//                    .collection("nguoi_dang_theo_doi").get()
//                                .addOnSuccessListener( a -> {
//                                    for (var doc : a) {
//                                        FriendStoryItem friendStoryItem = new FriendStoryItem();
//                                        db.collection("nguoi_dung").document(doc.getId())
//                                                .get().addOnSuccessListener( b-> {
//                                                    friendStoryItem.setUid(b.getId());
//                                                    friendStoryItem.setName(b.getString("ho_va_ten"));
//                                                    friendStoryItem.setAvatarRes(b.getString("anh_dai_dien"));
//                                                    friendStoryItem.setOnline(b.getBoolean("trang_thai_hoat_dong"));
//                                                    friendStoryItem.setStoryState(FriendStoryItem.StoryState.NEW);
//                                                    friendStoryItem.setStatusPreview(b.getString("ten_dang_nhap"));
//                                                    friendStoryList.add(friendStoryItem);
//                                                    friendStoryAdapter.notifyItemInserted(
//                                                            friendStoryList.size() - 1);
//                                                });
//
//                                    }
//                                });
//
//        // Danh sách chat
//        chatList = new ArrayList<>();
////        chatList.add(new ChatUser("tung_trv",    "🤫🤫",                                          "bây giờ", av, true));
////        chatList.add(new ChatUser("alex_dev",    "Dự án code xong chưa bác ơi? Nhìn cuốn ghê á!", "10 phút",  av, true));
////        chatList.add(new ChatUser("elena.codes", "Haha chuẩn bài rồi",                            "2 giờ",   av, false));
////        chatList.add(new ChatUser("tom_tech",    "Giao diện mượt đấy!",                           "Hôm qua", av, true));
////        chatList.add(new ChatUser("coder_vibe",  "[Hình ảnh]",                                    "3 ngày",  av, false));
//        db.collection("nguoi_dung").document(uid)
//                .collection("nguoi_dang_theo_doi").get()
//                .addOnSuccessListener( a -> {
//                    for (var doc : a) {
//                        ChatUser chatUser = new ChatUser();
////                        Timestamp date = new Timestamp();
//                        db.collection("nguoi_dung").document(doc.getId())
//                                .get().addOnSuccessListener( b-> {
//                                    chatUser.setUsername(b.getString("ho_va_ten"));
//                                    chatUser.setLastMessage(b.getString("ten_dang_nhap"));
//                                    chatUser.setChatTime(new Timestamp(new Date()));
//                                    chatUser.setAvatarResId(b.getString("anh_dai_dien"));
//                                    chatUser.setActive(b.getBoolean("trang_thai_hoat_dong"));
//                                    chatList.add(chatUser);
//                                    chatListAdapter.notifyItemInserted(
//                                            chatList.size() - 1);
//                                });
//
//                    }
//                });
//    }
    private void createMockData() {
        String uid = auth.getCurrentUser().getUid();

        // Load avatar + tên mình
        db.collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String myAvatar = doc.getString("anh_dai_dien");
                    if (myAvatar != null) {
                        Glide.with(this).load(myAvatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .into(imgMyAvatar);
                    }
                    tvMyStory.setText(doc.getString("ho_va_ten"));
                });

        // Load story bạn bè (giữ nguyên)
        friendStoryListener = db.collection("nguoi_dung").document(uid)
                .collection("nguoi_dang_theo_doi")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    // Remove tất cả listener cũ trước khi reset
                    for (ListenerRegistration reg : friendInfoListeners.values()) reg.remove();
                    friendInfoListeners.clear();

                    friendStoryList.clear();
                    friendStoryAdapter.notifyDataSetChanged();

                    List<String> docIds = new ArrayList<>();
                    for (var doc : snapshots.getDocuments()) docIds.add(doc.getId());
                    if (docIds.isEmpty()) return;

                    for (String docId : docIds) {
                        // ✅ Dùng addSnapshotListener thay vì get()
                        ListenerRegistration reg = db.collection("nguoi_dung").document(docId)
                                .addSnapshotListener((b, err) -> {
                                    if (err != null || b == null || !b.exists()) return;

                                    // Tìm item đã có trong list chưa
                                    FriendStoryItem existing = null;
                                    int existingIndex = -1;
                                    for (int i = 0; i < friendStoryList.size(); i++) {
                                        if (docId.equals(friendStoryList.get(i).getUid())) {
                                            existing = friendStoryList.get(i);
                                            existingIndex = i;
                                            break;
                                        }
                                    }

                                    FriendStoryItem item = (existing != null) ? existing : new FriendStoryItem();
                                    item.setUid(b.getId());
                                    item.setName(b.getString("ho_va_ten"));
                                    item.setAvatarRes(b.getString("anh_dai_dien"));
                                    item.setOnline(Boolean.TRUE.equals(b.getBoolean("trang_thai_hoat_dong")));
                                    item.setStoryState(FriendStoryItem.StoryState.NEW);
                                    item.setStatusPreview(b.getString("ten_dang_nhap"));
                                    item.setLastActive(b.getTimestamp("lan_cuoi_hoat_dong"));

                                    if (existingIndex >= 0) {
                                        friendStoryList.set(existingIndex, item); // update
                                    } else {
                                        friendStoryList.add(item); // thêm mới
                                    }

                                    // Re-sort mỗi khi có thay đổi
                                    friendStoryList.sort((a1, a2) -> {
                                        if (a1.isOnline() && !a2.isOnline()) return -1;
                                        if (!a1.isOnline() && a2.isOnline()) return 1;
                                        if (!a1.isOnline() && !a2.isOnline()) {
                                            Timestamp t1 = a1.getLastActive();
                                            Timestamp t2 = a2.getLastActive();
                                            if (t1 == null && t2 == null) return 0;
                                            if (t1 == null) return 1;
                                            if (t2 == null) return -1;
                                            return t2.compareTo(t1);
                                        }
                                        return 0;
                                    });

                                    friendStoryAdapter.notifyDataSetChanged();
                                });

                        friendInfoListeners.put(docId, reg);
                    }
                });

        // Load danh sách chat ← mới
        loadChatList();
    }
    private void loadChatList() {
        String uid = auth.getCurrentUser().getUid();

        if (chatListListener != null) {
            chatListListener.remove();
        }

        chatListListener = db.collection("cuoc_tro_chuyen")
                .whereArrayContains("thanh_vien", uid)
                .orderBy("thoi_gian_cuoi", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("FIRESTORE", "Listener error", e);
                        return;
                    }

                    if (snapshots == null) return;
                    // Hủy toàn bộ user listeners cũ TRƯỚC khi tạo mới
                    for (ListenerRegistration reg : chatUserListeners.values()) {
                        reg.remove();
                    }
                    chatUserListeners.clear();

                    List<String> validIds        = new ArrayList<>();
                    Map<String, String>    targetUidMap  = new LinkedHashMap<>();
                    Map<String, String>    lastMsgMap    = new LinkedHashMap<>();
                    Map<String, Timestamp> timeMap       = new LinkedHashMap<>();
                    Map<String, String>    senderMap     = new LinkedHashMap<>();
                    Map<String, String>    idTinNhanMap  = new LinkedHashMap<>();
                    Log.d("VALID_IDS", validIds.toString());
                    Log.d("SNAPSHOT_SIZE", String.valueOf(snapshots.size()));
                    for (var doc : snapshots.getDocuments()) {
                        Log.d("DOC_ID", doc.getId());
                        List<String> members = (List<String>) doc.get("thanh_vien");
                        if (members == null) continue;

                        String targetUid = null;
                        for (String m : members) {
                            if (!m.equals(uid)) { targetUid = m; break; }
                        }
                        if (targetUid == null) continue;

                        String cid = doc.getId();
                        validIds.add(cid);
                        targetUidMap.put(cid, targetUid);
                        lastMsgMap.put(cid, doc.getString("tin_nhan_cuoi"));
                        timeMap.put(cid, doc.getTimestamp("thoi_gian_cuoi"));
                        senderMap.put(cid, doc.getString("nguoi_gui_cuoi_id"));
                        idTinNhanMap.put(cid, doc.getString("id_tin_nhan_cuoi"));
                        Log.d("DEBUG_CHAT", "cid=" + cid + ", targetUid=" + targetUid + ", lastMsg=" + lastMsgMap.get(cid)+", time=" + timeMap.get(cid) + ", sender=" + senderMap.get(cid) + ", idTinNhan=" + idTinNhanMap.get(cid));
                    }

                    int total = validIds.size();
                    if (total == 0) {
                        chatList.clear();
                        chatListAdapter.notifyDataSetChanged();
                        return;
                    }

                    Map<String, ChatUser> tempMap = new LinkedHashMap<>();
                    for (String cid : validIds) tempMap.put(cid, null);

                    // Dùng AtomicInteger để thread-safe
                    AtomicInteger loadedCount = new AtomicInteger(0);

                    for (String cid : validIds) {
                        String finalTargetUid = targetUidMap.get(cid);
                        String lastMsg        = lastMsgMap.get(cid);
                        Timestamp time        = timeMap.get(cid);
                        String idNguoiGui     = senderMap.get(cid);
                        String idTinNhanCuoi  = idTinNhanMap.get(cid);

                        // ✅ Dùng addSnapshotListener để real-time update avatar/tên/trạng thái
                        ListenerRegistration reg = db.collection("nguoi_dung").document(finalTargetUid)
                                .addSnapshotListener((userDoc, err) -> {
                                    if (err != null || userDoc == null || !userDoc.exists()) return;

                                    ChatUser chatUser = new ChatUser();
                                    chatUser.setUid(finalTargetUid);
                                    chatUser.setUsername(userDoc.getString("ho_va_ten"));
                                    chatUser.setAvatarResId(userDoc.getString("anh_dai_dien"));
                                    chatUser.setActive(Boolean.TRUE.equals(
                                            userDoc.getBoolean("trang_thai_hoat_dong")));
                                    chatUser.setTenNguoiGui(
                                            (idNguoiGui != null && idNguoiGui.equals(uid)) ? "Bạn: " : "");
                                    chatUser.setLastMessage(getLastMessagePreview(lastMsg));
                                    chatUser.setChatTime(time);

                                    // ✅ Kiểm tra xem cid này đã được load chưa (tránh đếm 2 lần)
                                    boolean isFirstLoad = tempMap.get(cid) == null;

                                    if (idTinNhanCuoi != null && idNguoiGui != null && !idNguoiGui.equals(uid)) {
                                        db.collection("cuoc_tro_chuyen").document(cid)
                                                .collection("tin_nhan").document(idTinNhanCuoi)
                                                .get()
                                                .addOnSuccessListener(msgDoc -> {
                                                    boolean daDoc = Boolean.TRUE.equals(msgDoc.getBoolean("da_doc"));
                                                    chatUser.setChuaDoc(!daDoc);
                                                    tempMap.put(cid, chatUser);

                                                    if (isFirstLoad) {
                                                        // Lần đầu load: đếm để biết khi nào xong
                                                        if (loadedCount.incrementAndGet() == total) {
                                                            checkAndUpdate(total, total, tempMap);
                                                        }
                                                    } else {
                                                        // Lần sau (real-time update): update thẳng luôn
                                                        checkAndUpdate(total, total, tempMap);
                                                    }
                                                })
                                                .addOnFailureListener(err2 -> {
                                                    chatUser.setChuaDoc(false);
                                                    tempMap.put(cid, chatUser);
                                                    if (isFirstLoad) {
                                                        if (loadedCount.incrementAndGet() == total) {
                                                            checkAndUpdate(total, total, tempMap);
                                                        }
                                                    } else {
                                                        checkAndUpdate(total, total, tempMap);
                                                    }
                                                });
                                    } else {
                                        chatUser.setChuaDoc(false);
                                        tempMap.put(cid, chatUser);
                                        if (isFirstLoad) {
                                            if (loadedCount.incrementAndGet() == total) {
                                                checkAndUpdate(total, total, tempMap);
                                            }
                                        } else {
                                            checkAndUpdate(total, total, tempMap);
                                        }
                                    }
                                    Log.d("DEBUG_COUNT", "cid=" + cid + " loaded=" + loadedCount.get() + " total=" + total);

                                });

                        chatUserListeners.put(cid, reg);
                        Log.d("MAP_KEYS",
                                chatUserListeners.keySet().toString());
                    }
                });
    }
    private void checkAndUpdate(int loaded, int total, Map<String, ChatUser> tempMap) {
        if (loaded == total) {
            chatList.clear();
            for (ChatUser u : tempMap.values()) {
                if (u != null) chatList.add(u);
            }
            chatListAdapter.notifyDataSetChanged();
        }
    }
    private String getLastMessagePreview(String lastMsg) {
        if (lastMsg == null || lastMsg.isEmpty()) {
            return "Bắt đầu 1 đoạn chat";
        }

        // Kiểm tra nếu là URL ảnh (imgbb hoặc http chứa đuôi ảnh)
        if (isImageUrl(lastMsg)) {
            return "Đã gửi 1 ảnh";
        }

        return lastMsg;
    }

    private boolean isImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase().trim();
        return lower.startsWith("http") && (
                lower.contains("i.ibb.co") ||
                        lower.contains("ibb.co") ||
                        lower.endsWith(".jpg") ||
                        lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") ||
                        lower.endsWith(".gif") ||
                        lower.endsWith(".webp")
        );
    }
    private void setupRecyclerViews() {
        // Hàng story + online bạn bè (ngang)
        rvFriendStories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        friendStoryAdapter = new FriendStoryAdapter(friendStoryList);
        friendStoryAdapter.setOnItemClickListener(item -> {
//                Toast.makeText(requireContext(),
//                        item.getStoryState() != FriendStoryItem.StoryState.NONE
//                                ? "Xem story: " + item.getName()
//                                : "Chat với: "  + item.getName(),
//                        Toast.LENGTH_SHORT).show()
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("target_uid", item.getUid());
            startActivity(intent);
        });
        rvFriendStories.setAdapter(friendStoryAdapter);

        // Danh sách chat chính (dọc)
        rvChatList.setLayoutManager(new LinearLayoutManager(requireContext()));

        chatListAdapter = new ChatListAdapter(chatList);
        chatListAdapter.setOnItemClickListener(chatUser -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("target_uid", chatUser.getUid());
            startActivity(intent);
        });
        rvChatList.setAdapter(chatListAdapter);
    }

    private void setupClickListeners() {
//        btnBack.setOnClickListener(v -> {
//            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
//                getParentFragmentManager().popBackStack();
//            } else if (getActivity() != null) {
//                getActivity().onBackPressed();
//            }
//        });

        btnNewMessage.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tạo cuộc hội thoại mới", Toast.LENGTH_SHORT).show()
        );

        layoutMyStory.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tạo tin mới", Toast.LENGTH_SHORT).show()
        );
    }
    @Override
    public void onResume() {
        super.onResume();
        loadChatList();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (friendStoryListener != null) friendStoryListener.remove();
        if (chatListListener != null) chatListListener.remove();

        for (ListenerRegistration reg : friendInfoListeners.values()) {
            reg.remove();
        }

        for (ListenerRegistration reg : chatUserListeners.values()) {
            reg.remove();
        }

        friendInfoListeners.clear();
        chatUserListeners.clear();
    }
}