package com.example.doanmxh.CreatePage;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.protobuf.TimestampProto;

import java.util.Date;
import java.util.List;

public class CreateModel {

    @Exclude
    private String documentId;

    private int id;

    @PropertyName("nguoi_dung_id")
    private String nguoiDungId;

    @PropertyName("noi_dung")
    private String noiDung;

    public Timestamp getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(Timestamp ngayTao) {
        this.ngayTao = ngayTao;
    }

    @PropertyName("ngay_tao")
    private Timestamp ngayTao;

    @PropertyName("ngay_cap_nhat")
    private Date ngayCapNhat;

    @PropertyName("da_xoa")
    private boolean daXoa;

    @PropertyName("bai_viet_goc_id")
    private int baiVietGocId;

    @PropertyName("che_do_xem")
    private String cheDoXem;

    @PropertyName("so_like")
    private int soLike;

    @PropertyName("so_binh_luan")
    private int soComment;

    @PropertyName("so_share")
    private int soShare;

    @PropertyName("danh_sach_anh")
    private List<String> danhSachAnh;

    @PropertyName("danh_sach_video")
    private List<String> danhSachVideo;

    // ===== Dữ liệu user =====

    @Exclude
    private String hoVaTen;

    @Exclude
    private String tenDangNhap;

    @Exclude
    private String anhDaiDien;

    @Exclude
    private boolean verified;

    @Exclude
    private boolean likedByMe = false;

    public CreateModel() {
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNguoiDungId() {
        return nguoiDungId;
    }

    public void setNguoiDungId(String nguoiDungId) {
        this.nguoiDungId = nguoiDungId;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

//    public Date getNgayTao() {
//        return ngayTao;
//    }
//
//    public void setNgayTao(Date ngayTao) {
//        this.ngayTao = ngayTao;
//    }

    public Date getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(Date ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    public boolean isDaXoa() {
        return daXoa;
    }

    public void setDaXoa(boolean daXoa) {
        this.daXoa = daXoa;
    }

    public int getBaiVietGocId() {
        return baiVietGocId;
    }

    public void setBaiVietGocId(int baiVietGocId) {
        this.baiVietGocId = baiVietGocId;
    }

    public String getCheDoXem() {
        return cheDoXem;
    }

    public void setCheDoXem(String cheDoXem) {
        this.cheDoXem = cheDoXem;
    }

    public int getSoLike() {
        return soLike;
    }

    public void setSoLike(int soLike) {
        this.soLike = soLike;
    }

    public int getSoComment() {
        return soComment;
    }

    public void setSoComment(int soComment) {
        this.soComment = soComment;
    }

    public int getSoShare() {
        return soShare;
    }

    public void setSoShare(int soShare) {
        this.soShare = soShare;
    }

    public List<String> getDanhSachAnh() {
        return danhSachAnh;
    }

    public void setDanhSachAnh(List<String> danhSachAnh) {
        this.danhSachAnh = danhSachAnh;
    }

    public List<String> getDanhSachVideo() {
        return danhSachVideo;
    }

    public void setDanhSachVideo(List<String> danhSachVideo) {
        this.danhSachVideo = danhSachVideo;
    }

    public String getHoVaTen() {
        return hoVaTen;
    }

    public void setHoVaTen(String hoVaTen) {
        this.hoVaTen = hoVaTen;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    @Exclude
    public String getAnhDauTien() {
        if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
            return danhSachAnh.get(0);
        }
        return null;
    }

    @Exclude
    public boolean isRepost() {
        return baiVietGocId > 0;
    }
}