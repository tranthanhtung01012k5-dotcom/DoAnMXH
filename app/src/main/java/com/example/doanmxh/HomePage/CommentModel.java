package com.example.doanmxh.HomePage;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class CommentModel {

    private String postId;
    private String documentId;

    @PropertyName("nguoi_dung_id")
    private String nguoiDungId;

    private String hoVaTen;

    @PropertyName("ten_dang_nhap")
    private String tenDangNhap;

    @PropertyName("anh_dai_dien")
    private String anhDaiDien;

    @PropertyName("noi_dung")
    private String noiDung;

    private Timestamp ngayTao;
    private String binhLuanChaId;
    private boolean verified;

    @PropertyName("so_like")
    private int soLike = 0;

    private boolean likedByMe = false;
    private boolean isFollowing = false;

    public CommentModel() {}

    // ── postId ──
    public String getPostId()               { return postId; }
    public void setPostId(String postId)    { this.postId = postId; }

    // ── documentId ──
    public String getDocumentId()                       { return documentId; }
    public void setDocumentId(String documentId)        { this.documentId = documentId; }

    // ── nguoiDungId ──
    @PropertyName("nguoi_dung_id")
    public String getNguoiDungId()                      { return nguoiDungId; }
    @PropertyName("nguoi_dung_id")
    public void setNguoiDungId(String nguoiDungId)      { this.nguoiDungId = nguoiDungId; }

    // ── hoVaTen ──
    public String getHoVaTen()                          { return hoVaTen; }
    public void setHoVaTen(String hoVaTen)              { this.hoVaTen = hoVaTen; }

    // ── tenDangNhap ──
    @PropertyName("ten_dang_nhap")
    public String getTenDangNhap()                      { return tenDangNhap; }
    @PropertyName("ten_dang_nhap")
    public void setTenDangNhap(String tenDangNhap)      { this.tenDangNhap = tenDangNhap; }

    // ── anhDaiDien ──
    @PropertyName("anh_dai_dien")
    public String getAnhDaiDien()                       { return anhDaiDien; }
    @PropertyName("anh_dai_dien")
    public void setAnhDaiDien(String anhDaiDien)        { this.anhDaiDien = anhDaiDien; }

    // ── noiDung ──
    @PropertyName("noi_dung")
    public String getNoiDung()                          { return noiDung; }
    @PropertyName("noi_dung")
    public void setNoiDung(String noiDung)              { this.noiDung = noiDung; }

    // ── ngayTao ──
    public Timestamp getNgayTao()                       { return ngayTao; }
    public void setNgayTao(Timestamp ngayTao)           { this.ngayTao = ngayTao; }

    // ── binhLuanChaId ──
    public String getBinhLuanChaId()                    { return binhLuanChaId; }
    public void setBinhLuanChaId(String id)             { this.binhLuanChaId = id; }

    // ── verified ──
    public boolean isVerified()                         { return verified; }
    public void setVerified(boolean verified)           { this.verified = verified; }

    // ── soLike ──
    @PropertyName("so_like")
    public int getSoLike()                              { return soLike; }
    @PropertyName("so_like")
    public void setSoLike(int soLike)                   { this.soLike = soLike; }

    // ── likedByMe ──
    public boolean isLikedByMe()                        { return likedByMe; }
    public void setLikedByMe(boolean likedByMe)         { this.likedByMe = likedByMe; }

    // ── isFollowing ──
    public boolean isFollowing()                        { return isFollowing; }
    public void setFollowing(boolean following)         { this.isFollowing = following; }
}