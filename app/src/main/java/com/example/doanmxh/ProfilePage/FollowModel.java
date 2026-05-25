package com.example.doanmxh.ProfilePage;

// FollowModel.java
public class FollowModel {

    private String uid;
    private String username;
    private String name;
    private String avatar;

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    private boolean isFollowing;
    public FollowModel() {
    }

    public FollowModel(String uid, String username, String name, String avatar) {
        this.uid = uid;
        this.username = username;
        this.name = name;
        this.avatar = avatar;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getAvatar() {
        return avatar;
    }
}