package com.example.doanmxh.Message;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MessageFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ImageView    btnNewMessage, imgMyAvatar;
    private LinearLayout layoutMyStory;
    private RecyclerView rvFriendStories, rvChatList;
    private TextView tvMyStory;
    private FriendStoryAdapter friendStoryAdapter;
    private ChatListAdapter    chatListAdapter;

    private List<FriendStoryItem> friendStoryList;
    private List<ChatUser>        chatList;

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

        initViews(view);
        createMockData();
        setupRecyclerViews();
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

    private void createMockData() {
        int av = android.R.drawable.sym_def_app_icon;

        // Hàng story + online gộp chung
        friendStoryList = new ArrayList<>();
//        friendStoryList.add(new FriendStoryItem("Huy",   av, true,  FriendStoryItem.StoryState.NEW,  "Đang nghĩ về..."));
//        friendStoryList.add(new FriendStoryItem("Bảo",   av, true,  FriendStoryItem.StoryState.NEW,  "Một mình 1nG"));
//        friendStoryList.add(new FriendStoryItem("Han",   av, true,  FriendStoryItem.StoryState.SEEN, ""));
//        friendStoryList.add(new FriendStoryItem("Châu",  av, false, FriendStoryItem.StoryState.NEW,  "Cà phê sáng ☕"));
//        friendStoryList.add(new FriendStoryItem("Minh",  av, true,  FriendStoryItem.StoryState.NONE, ""));
//        friendStoryList.add(new FriendStoryItem("Trang", av, false, FriendStoryItem.StoryState.SEEN, "Ngủ sớm nha"));
            String uid = auth.getCurrentUser().getUid();
        db.collection("nguoi_dung").document(uid)
                .get()
                .addOnSuccessListener( documentSnapshot -> {
                            String myAvatar = documentSnapshot.getString("anh_dai_dien");
                            if (myAvatar != null) {
                                Glide.with(this)
                                        .load(myAvatar)
                                        .placeholder(R.drawable.ic_placeholder_avatar)
                                        .into(imgMyAvatar);
                            }
                            tvMyStory.setText(documentSnapshot.getString("ho_va_ten"));
                });
            db.collection("nguoi_dung").document(uid)
                    .collection("nguoi_dang_theo_doi").get()
                                .addOnSuccessListener( a -> {
                                    for (var doc : a) {
                                        FriendStoryItem friendStoryItem = new FriendStoryItem();
                                        db.collection("nguoi_dung").document(doc.getId())
                                                .get().addOnSuccessListener( b-> {
                                                    friendStoryItem.setName(b.getString("ho_va_ten"));
                                                    friendStoryItem.setAvatarRes(b.getString("anh_dai_dien"));
                                                    friendStoryItem.setOnline(b.getBoolean("trang_thai_hoat_dong"));
                                                    friendStoryItem.setStoryState(FriendStoryItem.StoryState.NEW);
                                                    friendStoryItem.setStatusPreview(b.getString("ten_dang_nhap"));
                                                    friendStoryList.add(friendStoryItem);
                                                    friendStoryAdapter.notifyItemInserted(
                                                            friendStoryList.size() - 1);
                                                });

                                    }
                                });

        // Danh sách chat
        chatList = new ArrayList<>();
        chatList.add(new ChatUser("tung_trv",    "🤫🤫",                                          "bây giờ", av, true));
        chatList.add(new ChatUser("alex_dev",    "Dự án code xong chưa bác ơi? Nhìn cuốn ghê á!", "10 phút",  av, true));
        chatList.add(new ChatUser("elena.codes", "Haha chuẩn bài rồi",                            "2 giờ",   av, false));
        chatList.add(new ChatUser("tom_tech",    "Giao diện mượt đấy!",                           "Hôm qua", av, true));
        chatList.add(new ChatUser("coder_vibe",  "[Hình ảnh]",                                    "3 ngày",  av, false));
    }

    private void setupRecyclerViews() {
        // Hàng story + online bạn bè (ngang)
        rvFriendStories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        friendStoryAdapter = new FriendStoryAdapter(friendStoryList);
        friendStoryAdapter.setOnItemClickListener(item ->
                Toast.makeText(requireContext(),
                        item.getStoryState() != FriendStoryItem.StoryState.NONE
                                ? "Xem story: " + item.getName()
                                : "Chat với: "  + item.getName(),
                        Toast.LENGTH_SHORT).show()
        );
        rvFriendStories.setAdapter(friendStoryAdapter);

        // Danh sách chat chính (dọc)
        rvChatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatListAdapter = new ChatListAdapter(chatList);
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
}