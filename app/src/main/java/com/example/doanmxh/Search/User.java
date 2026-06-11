package com.example.doanmxh.Search;

import com.google.firebase.firestore.PropertyName;

public class User {

    private String uid;
    private String username;
    private String fullname;
    private String avatar;
    private boolean isFollowed;

    public User() {}

    public User(String uid, String username, String fullname, String avatar) {
        this.uid = uid;
        this.username = username;
        this.fullname = fullname;
        this.avatar = avatar;
    }

    // uid — Firestore field: "uid"
    @PropertyName("uid")
    public String getUid() { return uid; }
    @PropertyName("uid")
    public void setUid(String uid) { this.uid = uid; }

    // username — Firestore field: "ten_dang_nhap"
    @PropertyName("ten_dang_nhap")
    public String getUsername() { return username; }
    @PropertyName("ten_dang_nhap")
    public void setUsername(String username) { this.username = username; }

    // fullname — Firestore field: "ho_va_ten"
    @PropertyName("ho_va_ten")
    public String getFullname() { return fullname; }
    @PropertyName("ho_va_ten")
    public void setFullname(String fullname) { this.fullname = fullname; }

    // avatar — Firestore field: "anh_dai_dien"
    @PropertyName("anh_dai_dien")
    public String getAvatar() { return avatar; }
    @PropertyName("anh_dai_dien")
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isFollowed() { return isFollowed; }
    public void setFollowed(boolean followed) { isFollowed = followed; }
}