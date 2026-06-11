package com.example.doanmxh.Message;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class ChatUser {
    private String username;

    public Boolean isDaDoc() {
        return daDoc;
    }

    public void setDaDoc(Boolean daDoc) {
        this.daDoc = daDoc;
    }
    private boolean isPinned;

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    private Boolean daDoc;
    @PropertyName("ten_dang_nhap")
    private String TenNguoiGui;
    @PropertyName("ten_dang_nhap")
    public String getTenNguoiGui() {
        return TenNguoiGui;
    }
    @PropertyName("ten_dang_nhap")
    public void setTenNguoiGui(String tenNguoiGui) {
        TenNguoiGui = tenNguoiGui;
    }

    private String Uid;

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    private String lastMessage;
    public ChatUser()
    {}
    public void setUsername(String username) {
        this.username = username;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setAvatarResId(String avatarResId) {
        this.avatarResId = avatarResId;
    }

    public void setChatTime(Timestamp chatTime) {
        this.chatTime = chatTime;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    private Timestamp chatTime;
    private String avatarResId; // Dùng ảnh mẫu từ hệ thống hoặc drawable
    private boolean isActive;

//    public String getId_nguoi_gui() {
//        return id_nguoi_gui;
//    }
//
//    public void setId_nguoi_gui(String id_nguoi_gui) {
//        this.id_nguoi_gui = id_nguoi_gui;
//    }

//    private String id_nguoi_gui;
private boolean chuaDoc; // true = có tin chưa đọc

    public boolean isChuaDoc() { return chuaDoc; }
    public void setChuaDoc(boolean chuaDoc) { this.chuaDoc = chuaDoc; }
    public ChatUser(String Uid,String username, String lastMessage, Timestamp chatTime, String avatarResId, boolean isActive, String TenNguoiGui) {
        this.Uid = Uid;
        this.username = username;
        this.lastMessage = lastMessage;
        this.chatTime = chatTime;
        this.avatarResId = avatarResId;
        this.isActive = isActive;
//        this.id_nguoi_gui = id_nguoi_gui;
        this.TenNguoiGui = TenNguoiGui;
    }

    // Các hàm Getter để lấy dữ liệu
    public String getUsername() { return username; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getChatTime() { return chatTime; }
    public String getAvatarResId() { return avatarResId; }
    public boolean isActive() { return isActive; }
}