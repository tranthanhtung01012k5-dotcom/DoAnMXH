package com.example.doanmxh.HomePage;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;

public class PostModel {

    private int id;
    private String nguoiDungId;
    private String noiDung;
    private Timestamp ngayTao;
    private Timestamp ngayCapNhat;
    private boolean daXoa;
    private int baiVietGocId;
    private String cheDoXem;
    private int soLuotThich;
    private int soBinhLuan;

    // ── Từ collection nguoi_dung ──
    private String hoVaTen;
    private String tenDangNhap;
    private String anhDaiDien;
    private boolean verified;
    private String documentId;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    // ── Ảnh / video ──
    private List<String> danhSachAnh;
    private List<String> danhSachVideo;

    // ✅ Firestore bắt buộc constructor rỗng
    public PostModel() {
    }

    // =========================
    // Getter + Setter Firestore
    // =========================
// Trong PostModel.java
    private boolean likedByMe = false;

    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    @PropertyName("id")
    public int getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(int id) {
        this.id = id;
    }

    @PropertyName("nguoi_dung_id")
    public String getNguoiDungId() {
        return nguoiDungId;
    }

    @PropertyName("nguoi_dung_id")
    public void setNguoiDungId(String nguoiDungId) {
        this.nguoiDungId = nguoiDungId;
    }

    @PropertyName("noi_dung")
    public String getNoiDung() {
        return noiDung;
    }

    @PropertyName("noi_dung")
    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    @PropertyName("ngay_tao")
    public Timestamp getNgayTao() {
        return ngayTao;
    }

    @PropertyName("ngay_tao")
    public void setNgayTao(Timestamp ngayTao) {
        this.ngayTao = ngayTao;
    }

    @PropertyName("ngay_cap_nhat")
    public Timestamp getNgayCapNhat() {
        return ngayCapNhat;
    }

    @PropertyName("ngay_cap_nhat")
    public void setNgayCapNhat(Timestamp ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    @PropertyName("da_xoa")
    public boolean isDaXoa() {
        return daXoa;
    }

    @PropertyName("da_xoa")
    public void setDaXoa(boolean daXoa) {
        this.daXoa = daXoa;
    }

    @PropertyName("bai_viet_goc_id")
    public int getBaiVietGocId() {
        return baiVietGocId;
    }

    @PropertyName("bai_viet_goc_id")
    public void setBaiVietGocId(int baiVietGocId) {
        this.baiVietGocId = baiVietGocId;
    }

    @PropertyName("che_do_xem")
    public String getCheDoXem() {
        return cheDoXem;
    }

    @PropertyName("che_do_xem")
    public void setCheDoXem(String cheDoXem) {
        this.cheDoXem = cheDoXem;
    }

    @PropertyName("so_like")
    public int getSoLuotThich() {
        return soLuotThich;
    }

    @PropertyName("so_like")
    public void setSoLuotThich(int soLuotThich) {
        this.soLuotThich = soLuotThich;
    }

    @PropertyName("so_binh_luan")
    public int getSoBinhLuan() {
        return soBinhLuan;
    }

    @PropertyName("so_binh_luan")
    public void setSoBinhLuan(int soBinhLuan) {
        this.soBinhLuan = soBinhLuan;
    }

    @PropertyName("danh_sach_anh")
    public List<String> getDanhSachAnh() {
        return danhSachAnh;
    }

    @PropertyName("danh_sach_anh")
    public void setDanhSachAnh(List<String> danhSachAnh) {
        this.danhSachAnh = danhSachAnh;
    }

    @PropertyName("danh_sach_video")
    public List<String> getDanhSachVideo() {
        return danhSachVideo;
    }

    @PropertyName("danh_sach_video")
    public void setDanhSachVideo(List<String> danhSachVideo) {
        this.danhSachVideo = danhSachVideo;
    }
    private boolean isFollowing; // mình đã follow tác giả chưa

    public boolean isFollowing() { return isFollowing; }
    public void setFollowing(boolean following) { isFollowing = following; }
    // =========================
    // Dữ liệu set thủ công
    // =========================

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

    // =========================
    // Helper
    // =========================

    public String getAnhDauTien() {
        if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
            return danhSachAnh.get(0);
        }
        return null;
    }

    public boolean isRepost() {
        return baiVietGocId > 0;
    }
}