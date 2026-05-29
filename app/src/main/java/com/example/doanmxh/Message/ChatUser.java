package com.example.doanmxh.Message;

public class ChatUser {
    private String username;
    private String lastMessage;
    private String chatTime;
    private int avatarResId; // Dùng ảnh mẫu từ hệ thống hoặc drawable
    private boolean isActive;

    public ChatUser(String username, String lastMessage, String chatTime, int avatarResId, boolean isActive) {
        this.username = username;
        this.lastMessage = lastMessage;
        this.chatTime = chatTime;
        this.avatarResId = avatarResId;
        this.isActive = isActive;
    }

    // Các hàm Getter để lấy dữ liệu
    public String getUsername() { return username; }
    public String getLastMessage() { return lastMessage; }
    public String getChatTime() { return chatTime; }
    public int getAvatarResId() { return avatarResId; }
    public boolean isActive() { return isActive; }
}