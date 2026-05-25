package com.example.doanmxh.HomePage;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class
CommentModel {
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    //    public int getLevel() {
//        return level;
//    }
//
//    public void setLevel(int level) {
//        this.level = level;
//    }
//
//    private int level = 0;
    private String postId;
    private String documentId;
    @PropertyName("nguoi_dung_id")
    private String nguoiDungId;
    private String hoVaTen;
    @PropertyName("ten_dang_nhap")
    private String tenDangNhap;
    @PropertyName("ten_dang_nhap")

    public String getTenDangNhap() {
        return tenDangNhap;
    }
    @PropertyName("ten_dang_nhap")

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    @PropertyName("anh_dai_dien")
    private String anhDaiDien;
    @PropertyName("noi_dung")
    private String noiDung;
    private Timestamp ngayTao;
    private String binhLuanChaId;
    private boolean verified;

    public int getSo_like() {
        return so_like;
    }

    public void setSo_like(int soTim) {
        this.so_like = soTim;
    }

    // Thêm vào CommentModel

    private int so_like;
    private int soLike = 0;
    private boolean likedByMe = false;

    public int getSoLike()              { return soLike; }
    public boolean isLikedByMe()        { return likedByMe; }
    public void setSoLike(int soLike)   { this.soLike = soLike; }
    public void setLikedByMe(boolean v) { this.likedByMe = v; }
    private boolean isFollowing; // mình đã follow tác giả chưa

    public boolean isFollowing() { return isFollowing; }
    public void setFollowing(boolean following) { isFollowing = following; }
    public CommentModel() {}

    // ── Getter ──
    public String getDocumentId()   { return documentId; }
    @PropertyName("nguoi_dung_id")
    public String getNguoiDungId()  { return nguoiDungId; }
    public String getHoVaTen()      { return hoVaTen; }
    public String getAnhDaiDien()   { return anhDaiDien; }
    @PropertyName("noi_dung")
    public String getNoiDung()      { return noiDung; }
    public Timestamp getNgayTao()        { return ngayTao; }
    public String getBinhLuanChaId(){ return binhLuanChaId; }
    public boolean isVerified()     { return verified; }

    // ── Setter ──
    public void setDocumentId(String documentId)     { this.documentId = documentId; }
    @PropertyName("nguoi_dung_id")
    public void setNguoiDungId(String nguoiDungId)   { this.nguoiDungId = nguoiDungId; }
    public void setHoVaTen(String hoVaTen)           { this.hoVaTen = hoVaTen; }
    public void setAnhDaiDien(String anhDaiDien)     { this.anhDaiDien = anhDaiDien; }
    @PropertyName("noi_dung")
    public void setNoiDung(String noiDung)           { this.noiDung = noiDung; }
    public void setNgayTao(Timestamp ngayTao)             { this.ngayTao = ngayTao; }
    public void setBinhLuanChaId(String id)          { this.binhLuanChaId = id; }
    public void setVerified(boolean verified)        { this.verified = verified; }
}