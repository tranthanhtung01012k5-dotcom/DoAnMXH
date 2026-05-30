package com.example.doanmxh.Message;

public class FriendStoryItem {
    public enum StoryState {
        NONE,       // Không có story
        NEW,        // Có story mới chưa xem  → ring gradient
        SEEN        // Story đã xem            → ring xám
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

    public FriendStoryItem(String name,
                           String avatarRes,
                           boolean online,
                           StoryState storyState,
                           String statusPreview) {
        this.name = name;
        this.avatarRes = avatarRes;
        this.online = online;
        this.storyState = storyState;
        this.statusPreview = statusPreview;
    }

    // getter setter...
}