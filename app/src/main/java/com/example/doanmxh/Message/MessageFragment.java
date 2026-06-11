package com.example.doanmxh.Message;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import java.util.HashSet;
import java.util.List;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MessageFragment extends Fragment {
    private FirebaseFirestore db;
    private ListenerRegistration friendStoryListener;
    private Set<String> pinnedIds = new HashSet<>();
    private FirebaseAuth auth;
    private ImageView btnNewMessage, imgMyAvatar;
    private TextView  btnUnread,btnInbox;
    private LinearLayout layoutMyStory;
    private RecyclerView rvFriendStories, rvChatList;
    private TextView tvMyStory;
    private FriendStoryAdapter friendStoryAdapter;
    private ChatListAdapter    chatListAdapter;
    private ListenerRegistration chatListListener;
    private List<FriendStoryItem> friendStoryList;
    private EditText edtSearch;
    private List<ChatUser>        chatList;       // toàn bộ danh sách gốc
    private List<ChatUser>        chatListFull;   // ✅ bản sao đầy đủ để filter

    // ── Trạng thái filter ────────────────────────────────
    private boolean isShowingUnread = false;       // ✅ đang lọc chưa đọc hay không

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
        db   = FirebaseFirestore.getInstance();

        friendStoryList = new ArrayList<>();
        chatList        = new ArrayList<>();
        chatListFull    = new ArrayList<>(); // ✅ khởi tạo

        initViews(view);
        setupRecyclerViews();
        setupSearch();
        createMockData();
        setupClickListeners();
        btnNewMessage.setOnClickListener( v ->
        {
            Intent intent = new Intent(requireContext(), NewMessageActivity.class);
            startActivity(intent);
        });
    }

    // ════════════════════════════════════════════════════
    //  INIT VIEWS
    // ════════════════════════════════════════════════════
    private void initViews(View view) {
        btnNewMessage   = view.findViewById(R.id.btnNewMessage);
        btnUnread       = view.findViewById(R.id.btnUnread);
        imgMyAvatar     = view.findViewById(R.id.imgMyAvatar);
        layoutMyStory   = view.findViewById(R.id.layoutMyStory);
        rvFriendStories = view.findViewById(R.id.rvFriendStories);
        rvChatList      = view.findViewById(R.id.rvChatList);
        tvMyStory       = view.findViewById(R.id.tvMyStory);
        btnInbox        = view.findViewById(R.id.btnInbox);
        edtSearch       = view.findViewById(R.id.edtSearch);

    }

    // ════════════════════════════════════════════════════
    //  LOAD DỮ LIỆU
    // ════════════════════════════════════════════════════
    private void createMockData() {
        String uid = auth.getCurrentUser().getUid();

        // Load avatar + tên mình
        db.collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String myAvatar = doc.getString("anh_dai_dien");
                    if (myAvatar != null) {
                        Glide.with(this).load(myAvatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .into(imgMyAvatar);
                    }
                    tvMyStory.setText(doc.getString("ho_va_ten"));
                });

        // Load story bạn bè (realtime)
        friendStoryListener = db.collection("nguoi_dung").document(uid)
                .collection("nguoi_dang_theo_doi")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || !isAdded()) return;

                    for (ListenerRegistration reg : friendInfoListeners.values()) reg.remove();
                    friendInfoListeners.clear();

                    friendStoryList.clear();
                    friendStoryAdapter.notifyDataSetChanged();

                    List<String> docIds = new ArrayList<>();
                    for (var doc : snapshots.getDocuments()) docIds.add(doc.getId());
                    if (docIds.isEmpty()) return;

                    for (String docId : docIds) {
                        ListenerRegistration reg = db.collection("nguoi_dung").document(docId)
                                .addSnapshotListener((b, err) -> {
                                    if (err != null || b == null || !b.exists() || !isAdded()) return;

                                    FriendStoryItem existing    = null;
                                    int             existingIdx = -1;
                                    for (int i = 0; i < friendStoryList.size(); i++) {
                                        if (docId.equals(friendStoryList.get(i).getUid())) {
                                            existing    = friendStoryList.get(i);
                                            existingIdx = i;
                                            break;
                                        }
                                    }

                                    FriendStoryItem item = existing != null ? existing : new FriendStoryItem();
                                    item.setUid(b.getId());
                                    item.setName(b.getString("ho_va_ten"));
                                    item.setAvatarRes(b.getString("anh_dai_dien"));
                                    item.setOnline(Boolean.TRUE.equals(b.getBoolean("trang_thai_hoat_dong")));
                                    item.setStoryState(FriendStoryItem.StoryState.NEW);
                                    item.setStatusPreview(b.getString("ten_dang_nhap"));
                                    item.setLastActive(b.getTimestamp("lan_cuoi_hoat_dong"));

                                    if (existingIdx >= 0) {
                                        friendStoryList.set(existingIdx, item);
                                    } else {
                                        friendStoryList.add(item);
                                    }

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

        // ✅ Đã xóa đoạn BottomMessageMore sai chỗ ở đây
    }
    // ════════════════════════════════════════════════════
    //  LOAD CHAT LIST (realtime)
    // ════════════════════════════════════════════════════
    private void loadChatList() {
        String uid = auth.getCurrentUser().getUid();

        if (chatListListener != null) chatListListener.remove();

        // 1. Load pinned
        db.collection("nguoi_dung").document(uid)
                .collection("tin_nhan_ghim")
                .get()
                .addOnSuccessListener(pinnedSnapshot -> {

                    Set<String> pinnedIds = new HashSet<>();
                    for (var doc : pinnedSnapshot.getDocuments()) {
                        pinnedIds.add(doc.getId());
                    }

                    // 2. Load archived
                    db.collection("nguoi_dung").document(uid)
                            .collection("cuoc_tro_luu_tru")
                            .get()
                            .addOnSuccessListener(archivedSnapshot -> {

                                Set<String> archivedIds = new HashSet<>();
                                for (var doc : archivedSnapshot.getDocuments()) {
                                    archivedIds.add(doc.getId());
                                }

                                startChatListListener(uid, archivedIds, pinnedIds);
                            })
                            .addOnFailureListener(e -> {
                                startChatListListener(uid, new HashSet<>(), pinnedIds);
                            });
                });
    }

    private void startChatListListener(
            String uid,
            Set<String> archivedIds,
            Set<String> pinnedIds
    ) {
        chatListListener = db.collection("cuoc_tro_chuyen")
                .whereArrayContains("thanh_vien", uid)
                .orderBy("thoi_gian_cuoi", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {

                    if (e != null || snapshots == null || !isAdded()) return;

                    for (ListenerRegistration reg : chatUserListeners.values()) reg.remove();
                    chatUserListeners.clear();

                    List<String> validIds = new ArrayList<>();
                    Map<String, String> targetUidMap = new LinkedHashMap<>();
                    Map<String, String> lastMsgMap = new LinkedHashMap<>();
                    Map<String, Timestamp> timeMap = new LinkedHashMap<>();
                    Map<String, String> senderMap = new LinkedHashMap<>();
                    Map<String, String> idTinNhanMap = new LinkedHashMap<>();

                    for (var doc : snapshots.getDocuments()) {

                        String cid = doc.getId();

                        if (archivedIds.contains(cid)) continue;

                        List<String> members = (List<String>) doc.get("thanh_vien");
                        if (members == null) continue;

                        String targetUid = null;
                        for (String m : members) {
                            if (!m.equals(uid)) {
                                targetUid = m;
                                break;
                            }
                        }
                        if (targetUid == null) continue;

                        validIds.add(cid);

                        targetUidMap.put(cid, targetUid);
                        lastMsgMap.put(cid, doc.getString("tin_nhan_cuoi"));
                        timeMap.put(cid, doc.getTimestamp("thoi_gian_cuoi"));
                        senderMap.put(cid, doc.getString("nguoi_gui_cuoi_id"));
                        idTinNhanMap.put(cid, doc.getString("id_tin_nhan_cuoi"));
                    }

                    if (validIds.isEmpty()) {
                        chatListFull.clear();
                        applyFilter();
                        return;
                    }

                    Map<String, ChatUser> tempMap = new LinkedHashMap<>();
                    AtomicInteger loaded = new AtomicInteger(0);

                    for (String cid : validIds) {

                        String targetUid = targetUidMap.get(cid);
                        String lastMsg = lastMsgMap.get(cid);
                        Timestamp time = timeMap.get(cid);
                        String sender = senderMap.get(cid);
                        String msgId = idTinNhanMap.get(cid);

                        boolean isPinned = pinnedIds.contains(cid);

                        db.collection("nguoi_dung")
                                .document(targetUid)
                                .addSnapshotListener((userDoc, err) -> {

                                    if (err != null || userDoc == null || !userDoc.exists() || !isAdded())
                                        return;

                                    ChatUser u = new ChatUser();
                                    u.setUid(targetUid);
                                    u.setUsername(userDoc.getString("ho_va_ten"));
                                    u.setAvatarResId(userDoc.getString("anh_dai_dien"));
                                    u.setActive(Boolean.TRUE.equals(userDoc.getBoolean("trang_thai_hoat_dong")));
                                    u.setLastMessage(getLastMessagePreview(lastMsg));
                                    u.setChatTime(time);
                                    u.setPinned(isPinned);

                                    if (sender != null && sender.equals(uid)) {
                                        u.setTenNguoiGui("Bạn: ");
                                    }

                                    if (msgId != null && sender != null && !sender.equals(uid)) {
                                        db.collection("cuoc_tro_chuyen")
                                                .document(cid)
                                                .collection("tin_nhan")
                                                .document(msgId)
                                                .get()
                                                .addOnSuccessListener(m -> {
                                                    u.setChuaDoc(!Boolean.TRUE.equals(m.getBoolean("da_doc")));
                                                    tempMap.put(cid, u);
                                                    if (loaded.incrementAndGet() == validIds.size()) {
                                                        flushToList(tempMap);
                                                    }
                                                });
                                    } else {
                                        u.setChuaDoc(false);
                                        tempMap.put(cid, u);
                                        if (loaded.incrementAndGet() == validIds.size()) {
                                            flushToList(tempMap);
                                        }
                                    }
                                });

                        chatUserListeners.put(cid,
                                db.collection("nguoi_dung")
                                        .document(targetUid)
                                        .addSnapshotListener((d, error) -> {
                                            if (error != null) {
                                                Log.e("FIRESTORE", "Listener error", error);
                                                return;
                                            }
                                        })
                        );
                    }
                });
    }
//    private void flushToList(Map<String, ChatUser> tempMap) {
//        List<ChatUser> list = new ArrayList<>();
//
//        for (ChatUser u : tempMap.values()) {
//            if (u != null) list.add(u);
//        }
//
//        list.sort((a, b) -> {
//            // 🔥 pinned luôn lên đầu
//            if (a.isPinned() && !b.isPinned()) return -1;
//            if (!a.isPinned() && b.isPinned()) return 1;
//
//            // nếu cùng trạng thái pinned → sort theo thời gian
//            if (a.getChatTime() == null || b.getChatTime() == null) return 0;
//
//            return b.getChatTime().compareTo(a.getChatTime());
//        });
//
//        chatListFull.clear();
//        chatListFull.addAll(list);
//
//        applyFilter();
//    }

    // ════════════════════════════════════════════════════
    //  FLUSH + FILTER
    // ════════════════════════════════════════════════════

    /** Cập nhật chatListFull từ tempMap rồi áp filter hiện tại */
    private void flushToList(Map<String, ChatUser> tempMap) {

        List<ChatUser> list = new ArrayList<>();

        for (ChatUser u : tempMap.values()) {
            if (u != null) list.add(u);
        }

        list.sort((a, b) -> {

            if (a.isPinned() && !b.isPinned()) return -1;
            if (!a.isPinned() && b.isPinned()) return 1;

            if (a.getChatTime() == null || b.getChatTime() == null) return 0;

            return b.getChatTime().compareTo(a.getChatTime());
        });

        chatListFull.clear();
        chatListFull.addAll(list);

        applyFilter();
    }

    /**
     * Áp filter dựa vào trạng thái isShowingUnread.
     * Gọi mỗi khi dữ liệu thay đổi HOẶC user nhấn nút.
     */
    private void applyFilter() {
        if (!isAdded()) return;

        // Ưu tiên filter search nếu đang có keyword
        String keyword = edtSearch != null ? edtSearch.getText().toString().trim() : "";
        if (!keyword.isEmpty()) {
            filterChatList(keyword); // giữ search + filter unread
        } else {
            chatList.clear();
            if (isShowingUnread) {
                for (ChatUser u : chatListFull) {
                    if (u.isChuaDoc()) chatList.add(u);
                }
            } else {
                chatList.addAll(chatListFull);
            }
            chatListAdapter.notifyDataSetChanged();
        }

        updateUnreadBadge();
    }

    /** Cập nhật badge số lượng tin chưa đọc trên nút btnUnread */
    private void updateUnreadBadge() {
        if (!isAdded()) return;

        if (isShowingUnread) {
            // btnUnread active
            if (btnUnread != null) {
                btnUnread.setTextColor(getResources().getColor(R.color.colorPrimary, null));
                btnUnread.setBackgroundResource(R.drawable.bg_chip_selected);
            }
            // btnInbox inactive
            if (btnInbox != null) {
                btnInbox.setTextColor(getResources().getColor(R.color.text_primary, null));
                btnInbox.setBackgroundColor(getResources().getColor(R.color.bg_primary, null));
            }
        } else {
            // btnInbox active
            if (btnInbox != null) {
                btnInbox.setTextColor(getResources().getColor(R.color.colorPrimary, null));
                btnInbox.setBackgroundResource(R.drawable.bg_chip_selected);
            }
            // btnUnread inactive
            if (btnUnread != null) {
                btnUnread.setTextColor(getResources().getColor(R.color.text_primary, null));
                btnUnread.setBackgroundColor(getResources().getColor(R.color.bg_primary, null));
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  HELPER
    // ════════════════════════════════════════════════════
    private void checkAndUpdate(int loaded, int total, Map<String, ChatUser> tempMap) {
        if (loaded == total) {
            flushToList(tempMap);
        }
    }

    private String getLastMessagePreview(String lastMsg) {
        if (lastMsg == null || lastMsg.isEmpty()) return "Bắt đầu 1 đoạn chat";
        if (isImageUrl(lastMsg)) return "Đã gửi 1 ảnh";
        return lastMsg;
    }

    private boolean isImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase().trim();
        return lower.startsWith("http") && (
                lower.contains("i.ibb.co") || lower.contains("ibb.co") ||
                        lower.endsWith(".jpg")  || lower.endsWith(".jpeg") ||
                        lower.endsWith(".png")  || lower.endsWith(".gif")  ||
                        lower.endsWith(".webp"));
    }

    // ════════════════════════════════════════════════════
    //  SETUP RECYCLERVIEW
    // ════════════════════════════════════════════════════
    private void setupRecyclerViews() {
        // Story hàng ngang
        rvFriendStories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        friendStoryAdapter = new FriendStoryAdapter(friendStoryList);
        friendStoryAdapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("target_uid", item.getUid());
            startActivity(intent);
        });
        rvFriendStories.setAdapter(friendStoryAdapter);

        // Chat list dọc
        rvChatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatListAdapter = new ChatListAdapter(chatList, ChatListAdapter.Mode.CHAT_LIST);

        // ✅ Click thường → mở chat
        chatListAdapter.setOnItemClickListener(chatUser -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("target_uid", chatUser.getUid());
            startActivity(intent);
        });

        // ✅ Long click → mở BottomSheet + reload khi lưu xong
        chatListAdapter.setOnChatLongClickListener(chatUser -> {
//            boolean isPinned = pinnedIds.contains(chatUser.getUid());

            BottomMessageMore sheet = BottomMessageMore.newInstance(
                    chatUser.getUid(),
                    chatUser.getUsername(),
                    chatUser.getLastMessage(),
                    chatUser.getAvatarResId()
//                    isPinned
            );

            Log.d("BOTTOM_TEST",
                    "SHEET = " + sheet);

            Log.d("BOTTOM_TEST",
                    "INSTANCE = " + System.identityHashCode(sheet));

            sheet.setOnActionDoneListener(() -> {
                Log.d("BOTTOM_TEST", "LOAD CHAT LIST");
                pinnedIds.clear();   // 🔥 thêm dòng này

                loadChatList();
//                Toast.makeText(requireContext(), "Lưu thành công", Toast.LENGTH_SHORT).show();
            });

            Log.d("BOTTOM_TEST",
                    "LISTENER SET DONE");

            sheet.show(getChildFragmentManager(), "BottomMessageMore");
//            sheet.setOnActionDoneListener();
        });

        rvChatList.setAdapter(chatListAdapter);
    }
    // ════════════════════════════════════════════════════
    //  CLICK LISTENERS
    // ════════════════════════════════════════════════════
    private void setupClickListeners() {
        btnNewMessage.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tạo cuộc hội thoại mới", Toast.LENGTH_SHORT).show());

        // ✅ btnInbox — quay về danh sách đầy đủ, tắt filter
        btnInbox.setOnClickListener(v -> {
            if (isShowingUnread) {
                isShowingUnread = false;
                applyFilter();
            }
        });

        btnUnread.setOnClickListener(v -> {
            isShowingUnread = !isShowingUnread;
            applyFilter();
            String msg = isShowingUnread ? "Đang lọc tin chưa đọc" : "Hiển thị tất cả";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        layoutMyStory.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tạo tin mới", Toast.LENGTH_SHORT).show());
    }

    // ════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════
    @Override
    public void onResume() {
        super.onResume();
        loadChatList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (friendStoryListener != null) friendStoryListener.remove();
        if (chatListListener    != null) chatListListener.remove();

        for (ListenerRegistration reg : friendInfoListeners.values()) reg.remove();
        for (ListenerRegistration reg : chatUserListeners.values())   reg.remove();

        friendInfoListeners.clear();
        chatUserListeners.clear();
    }
    private void setupSearch() {
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChatList(s.toString().trim());
            }
        });
    }

    private void filterChatList(String keyword) {
        chatList.clear();

        // Lấy base list theo filter unread hiện tại
        List<ChatUser> base = new ArrayList<>();
        if (isShowingUnread) {
            for (ChatUser u : chatListFull) {
                if (u.isChuaDoc()) base.add(u);
            }
        } else {
            base.addAll(chatListFull);
        }

        // Nếu không có keyword thì hiện hết
        if (keyword.isEmpty()) {
            chatList.addAll(base);
            chatListAdapter.notifyDataSetChanged();
            return;
        }

        // Filter theo tên
        String lower = keyword.toLowerCase();
        for (ChatUser u : base) {
            boolean match = u.getUsername() != null
                    && u.getUsername().toLowerCase().contains(lower);
            if (match) chatList.add(u);
        }

        chatListAdapter.notifyDataSetChanged();
    }
}