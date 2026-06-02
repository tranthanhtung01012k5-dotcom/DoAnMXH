package com.example.doanmxh.Mention;

import com.google.firebase.firestore.PropertyName;

public class MentionUser {
    public String uid;
    public String hoVaTen;
    public String tenDangNhap;
    public String anhDaiDien;

    public MentionUser(String uid, String hoVaTen, String tenDangNhap, String anhDaiDien) {
        this.uid = uid;
        this.hoVaTen = hoVaTen;
        this.tenDangNhap = tenDangNhap;
        this.anhDaiDien = anhDaiDien;
    }
}