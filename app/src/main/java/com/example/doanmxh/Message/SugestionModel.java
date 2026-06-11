package com.example.doanmxh.Message;

import com.google.firebase.firestore.PropertyName;

public class SugestionModel {
    @PropertyName("ten_dang_nhap")
    private String tenDangNhap;
    @PropertyName("anh_dai_dien")
    private String anhDaiDien;
    private String uid;
    @PropertyName("ho_va_ten")
    private String hoVaTen;

    public SugestionModel(String tenDangNhap, String anhDaiDien, String uid, String hoVaTen) {
        this.tenDangNhap = tenDangNhap;
        this.anhDaiDien = anhDaiDien;
        this.uid = uid;
        this.hoVaTen = hoVaTen;
    }
    public SugestionModel() {}
    public String getHoVaTen() {
        return hoVaTen;
    }

    public void setHoVaTen(String hoVaTen) {
        this.hoVaTen = hoVaTen;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }
}
