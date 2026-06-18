package com.example.doanmxh.HomePage;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class PostModel {

    // =========================================================
    // Firestore Fields
    // =========================================================
    private String videoUrl;
    private String videoThumbnail;
    // PostModel.java
    private boolean isLoading = false;

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }
    public List<MediaItem> getMediaList() {
        List<MediaItem> list = new ArrayList<>();

        if (danhSachVideo != null) {
            for (String url : danhSachVideo) {
                list.add(new MediaItem(url, MediaItem.Type.VIDEO));
            }
        }

        if (danhSachAnh != null) {
            for (String url : danhSachAnh) {
                list.add(new MediaItem(url, MediaItem.Type.IMAGE));
            }
        }

        return list;
    }

    private boolean repostedByMe = false;


    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getVideoThumbnail() { return videoThumbnail; }
    public void setVideoThumbnail(String v) { this.videoThumbnail = v; }
    public boolean isRepostedByMe() { return repostedByMe; }
    public void setRepostedByMe(boolean repostedByMe) { this.repostedByMe = repostedByMe; }
    @PropertyName("id")
    private int id = 0;

    @PropertyName("nguoi_dung_id")
    private String nguoiDungId;

    @PropertyName("noi_dung")
    private String noiDung;

    @PropertyName("ngay_tao")
    private Timestamp ngayTao;

    @PropertyName("ngay_cap_nhat")
    private Timestamp ngayCapNhat;

    @PropertyName("da_xoa")
    private boolean daXoa = false;

    @PropertyName("bai_viet_goc_id")
    private int baiVietGocId = 0;

    @PropertyName("bai_viet_cha_id")
    private String baiVietChaId;

    @PropertyName("che_do_xem")
    private String cheDoXem;

    @PropertyName("so_like")
    private int soLuotThich = 0;

    @PropertyName("so_binh_luan")
    private int soBinhLuan = 0;

    @PropertyName("so_repost")
    private int soRepost = 0;

    @PropertyName("so_share")
    private int soShare = 0;

    @PropertyName("danh_sach_anh")
    private List<String> danhSachAnh;

    @PropertyName("danh_sach_video")
    private List<String> danhSachVideo;

    @PropertyName("danh_sach_audio")
    public List<String> getDanhSachAudio() {
        return danhSachAudio;
    }

    @PropertyName("danh_sach_audio")
    public void setDanhSachAudio(List<String> danhSachAudio) {
        this.danhSachAudio = danhSachAudio;
    }

    // =========================================================
    // Runtime Only
    // =========================================================
    @PropertyName("danh_sach_audio")
    private List<String> danhSachAudio;
    @PropertyName("liked_by_me")
    private boolean likedByMe = false;
    private boolean expanded = false;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    @PropertyName("is_following")
    private boolean isFollowing = false;

    @PropertyName("is_repost")
    private boolean isRepost = false;

    @PropertyName("document_id")
    private String documentId;

    @PropertyName("post_cha")
    private PostModel postCha;

    @PropertyName("top_comment")
    private CommentModel topComment;

    @PropertyName("top_replies")
    private List<CommentModel> topReplies;

    // =========================================================
    // User Info
    // =========================================================

    @PropertyName("ho_va_ten")
    private String hoVaTen;

    @PropertyName("ten_dang_nhap")
    private String tenDangNhap;

    @PropertyName("anh_dai_dien")
    private String anhDaiDien;

    @PropertyName("verified")
    private boolean verified = false;

    // =========================================================
    // Constructor
    // =========================================================

    public PostModel() {
    }

    // =========================================================
    // Getter / Setter
    // =========================================================

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

    @PropertyName("bai_viet_cha_id")
    public String getBaiVietChaId() {
        return baiVietChaId;
    }

    @PropertyName("bai_viet_cha_id")
    public void setBaiVietChaId(String baiVietChaId) {
        this.baiVietChaId = baiVietChaId;
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

    @PropertyName("so_repost")
    public int getSoRepost() {
        return soRepost;
    }

    @PropertyName("so_repost")
    public void setSoRepost(int soRepost) {
        this.soRepost = soRepost;
    }

    @PropertyName("so_share")
    public int getSoShare() {
        return soShare;
    }

    @PropertyName("so_share")
    public void setSoShare(int soShare) {
        this.soShare = soShare;
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

    @PropertyName("liked_by_me")
    public boolean isLikedByMe() {
        return likedByMe;
    }

    @PropertyName("liked_by_me")
    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    @PropertyName("is_following")
    public boolean isFollowing() {
        return isFollowing;
    }

    @PropertyName("is_following")
    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    @PropertyName("is_repost")
    public boolean isRepost() {
        return isRepost;
    }

    @PropertyName("is_repost")
    public void setRepost(boolean repost) {
        isRepost = repost;
    }

    @PropertyName("post_cha")
    public PostModel getPostCha() {
        return postCha;
    }

    @PropertyName("post_cha")
    public void setPostCha(PostModel postCha) {
        this.postCha = postCha;
    }

    @PropertyName("top_comment")
    public CommentModel getTopComment() {
        return topComment;
    }

    @PropertyName("top_comment")
    public void setTopComment(CommentModel topComment) {
        this.topComment = topComment;
    }

    @PropertyName("top_replies")
    public List<CommentModel> getTopReplies() {
        return topReplies;
    }

    @PropertyName("top_replies")
    public void setTopReplies(List<CommentModel> topReplies) {
        this.topReplies = topReplies;
    }

    @PropertyName("ho_va_ten")
    public String getHoVaTen() {
        return hoVaTen;
    }

    @PropertyName("ho_va_ten")
    public void setHoVaTen(String hoVaTen) {
        this.hoVaTen = hoVaTen;
    }

    @PropertyName("ten_dang_nhap")
    public String getTenDangNhap() {
        return tenDangNhap;
    }

    @PropertyName("ten_dang_nhap")
    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    @PropertyName("anh_dai_dien")
    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    @PropertyName("anh_dai_dien")
    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    @PropertyName("verified")
    public boolean isVerified() {
        return verified;
    }

    @PropertyName("verified")
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @PropertyName("document_id")
    public String getDocumentId() {
        return documentId;
    }

    @PropertyName("document_id")
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // =========================================================
    // Helper
    // =========================================================

    public String getAnhDauTien() {
        if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
            return danhSachAnh.get(0);
        }
        return null;
    }
}