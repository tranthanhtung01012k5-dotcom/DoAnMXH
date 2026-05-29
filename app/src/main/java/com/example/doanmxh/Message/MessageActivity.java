package com.example.doanmxh.Message;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doanmxh.R;
import java.util.ArrayList;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    private ImageView btnBack, btnNewMessage;
    private RecyclerView rvActiveFriends, rvChatList;

    private ActiveFriendsAdapter activeFriendsAdapter;
    private ChatListAdapter chatListAdapter;
    private List<ChatUser> mockDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Thay bằng super.onCreate(savedInstanceState) nếu gặp lỗi compile
        setContentView(R.layout.activity_chat_threads);

        // 1. Ánh xạ các View từ file XML chính
        initViews();

        // 2. Tạo dữ liệu ảo chuẩn UI Threads để test thử
        createMockData();

        // 3. Thiết lập danh sách bạn bè đang hoạt động (Cuộn Ngang)
        rvActiveFriends.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        activeFriendsAdapter = new ActiveFriendsAdapter(mockDataList);
        rvActiveFriends.setAdapter(activeFriendsAdapter);

        // 4. Thiết lập danh sách đoạn chat chính (Cuộn Dọc)
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        chatListAdapter = new ChatListAdapter(mockDataList);
        rvChatList.setAdapter(chatListAdapter);

        // 5. Cài đặt các sự kiện Click xử lý nút bấm
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNewMessage = findViewById(R.id.btnNewMessage);
        rvActiveFriends = findViewById(R.id.rvActiveFriends);
        rvChatList = findViewById(R.id.rvChatList);
    }

    private void createMockData() {
        mockDataList = new ArrayList<>();
        // Lấy tạm icon mặc định của hệ thống android để hiển thị làm avatar mẫu
        int defaultAvatar = android.R.drawable.sym_def_app_icon;

        mockDataList.add(new ChatUser("tung_trv", "🤫🤫", "bây giờ", defaultAvatar, true));
        mockDataList.add(new ChatUser("alex_dev", "Dự án code xong chưa bác ơi? Nhìn cuốn ghê á!", "10 phút", defaultAvatar, true));
        mockDataList.add(new ChatUser("elena.codes", "Haha chuẩn bài rồi", "2 giờ", defaultAvatar, false));
        mockDataList.add(new ChatUser("tom_tech", "Giao diện mượt đấy!", "Hôm qua", defaultAvatar, true));
        mockDataList.add(new ChatUser("coder_vibe", "[Hình ảnh]", "3 ngày", defaultAvatar, false));
    }

    private void setupClickListeners() {
        // Sự kiện cho nút quay lại đầu trang
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng màn hình tin nhắn hiện tại để quay về màn hình trước
            }
        });

        // Sự kiện tạo tin nhắn mới
        btnNewMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MessageActivity.this, "Tạo cuộc hội thoại mới", Toast.LENGTH_SHORT).show();
            }
        });
    }
}