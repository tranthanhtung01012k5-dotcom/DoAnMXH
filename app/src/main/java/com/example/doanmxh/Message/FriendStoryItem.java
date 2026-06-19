package com.example.doanmxh.Message;

import com.google.firebase.Timestamp;

public class FriendStoryItem {
    public enum StoryState {
        NONE,       // Không có story
        NEW,        // Có story mới chưa xem  → ring gradient
        SEEN        // Story đã xem            → ring xám
    }
    private String uid;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    public String ghiChu;

    public String getGhiChu() {
        return ghiChu;
    }

    public Timestamp getTimeaAgoText() {
        return timeaAgoText;
    }

    public void setTimeaAgoText(Timestamp timeaAgoText) {
        this.timeaAgoText = timeaAgoText;
    }

    private Timestamp timeaAgoText;

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    private String name;
    private String avatarRes;
    private boolean online;
    private StoryState storyState;
    private String statusPreview;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarRes() {
        return avatarRes;
    }

    public void setAvatarRes(String avatarRes) {
        this.avatarRes = avatarRes;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public StoryState getStoryState() {
        return storyState;
    }

    public void setStoryState(StoryState storyState) {
        this.storyState = storyState;
    }

    public String getStatusPreview() {
        return statusPreview;
    }

    public void setStatusPreview(String statusPreview) {
        this.statusPreview = statusPreview;
    }

    // BẮT BUỘC cho Firebase
    public FriendStoryItem() {
    }
    private com.google.firebase.Timestamp lastActive;

    public com.google.firebase.Timestamp getLastActive() { return lastActive; }
    public void setLastActive(com.google.firebase.Timestamp lastActive) {
        this.lastActive = lastActive;
    }
    public FriendStoryItem(
            String uid,String name,
                           String avatarRes,
                           boolean online,
                           StoryState storyState,
                           String statusPreview) {
        this.uid = uid;
        this.name = name;
        this.avatarRes = avatarRes;
        this.online = online;
        this.storyState = storyState;
        this.statusPreview = statusPreview;
    }

    // getter setter...
}