package com.example.doanmxh.HomePage;


import com.google.firebase.Timestamp;

import java.util.Date;

public class CommentModel {
//    public int getLevel() {
//        return level;
//    }
//
//    public void setLevel(int level) {
//        this.level = level;
//    }
//
//    private int level = 0;
    private String documentId;
    private String nguoiDungId;
    private String hoVaTen;
    private String anhDaiDien;
    private String noiDung;
    private Timestamp ngayTao;
    private String binhLuanChaId;
    private boolean verified;
    // Thêm vào CommentModel
    private int soLike = 0;
    private boolean likedByMe = false;

    public int getSoLike()              { return soLike; }
    public boolean isLikedByMe()        { return likedByMe; }
    public void setSoLike(int soLike)   { this.soLike = soLike; }
    public void setLikedByMe(boolean v) { this.likedByMe = v; }
    public CommentModel() {}

    // ── Getter ──
    public String getDocumentId()   { return documentId; }
    public String getNguoiDungId()  { return nguoiDungId; }
    public String getHoVaTen()      { return hoVaTen; }
    public String getAnhDaiDien()   { return anhDaiDien; }
    public String getNoiDung()      { return noiDung; }
    public Timestamp getNgayTao()        { return ngayTao; }
    public String getBinhLuanChaId(){ return binhLuanChaId; }
    public boolean isVerified()     { return verified; }

    // ── Setter ──
    public void setDocumentId(String documentId)     { this.documentId = documentId; }
    public void setNguoiDungId(String nguoiDungId)   { this.nguoiDungId = nguoiDungId; }
    public void setHoVaTen(String hoVaTen)           { this.hoVaTen = hoVaTen; }
    public void setAnhDaiDien(String anhDaiDien)     { this.anhDaiDien = anhDaiDien; }
    public void setNoiDung(String noiDung)           { this.noiDung = noiDung; }
    public void setNgayTao(Timestamp ngayTao)             { this.ngayTao = ngayTao; }
    public void setBinhLuanChaId(String id)          { this.binhLuanChaId = id; }
    public void setVerified(boolean verified)        { this.verified = verified; }
}