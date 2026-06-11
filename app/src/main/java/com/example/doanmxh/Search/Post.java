package com.example.doanmxh.Search;

import com.google.firebase.Timestamp;
import java.util.List;

public class Post {

    private String        uid;
    private String        bai_viet_cha_id;
    private String        nguoi_dung_id;
    private String        noi_dung;
    private Timestamp     ngay_tao;
    private List<String>  danh_sach_anh;
    private List<String>  hinh_anh;
    private long          so_like;
    private long          so_binh_luan;
    private boolean       da_chinh_sua;
    private boolean       da_xoa;
    private boolean       is_repost;

    public Post() {}

    // uid
    public String getUid()              { return uid; }
    public void setUid(String uid)      { this.uid = uid; }

    // bai_viet_cha_id
    public String getBai_viet_cha_id()                      { return bai_viet_cha_id; }
    public void setBai_viet_cha_id(String bai_viet_cha_id)  { this.bai_viet_cha_id = bai_viet_cha_id; }

    // nguoi_dung_id
    public String getNguoi_dung_id()                        { return nguoi_dung_id; }
    public void setNguoi_dung_id(String nguoi_dung_id)      { this.nguoi_dung_id = nguoi_dung_id; }

    // noi_dung
    public String getNoi_dung()                  { return noi_dung; }
    public void setNoi_dung(String noi_dung)     { this.noi_dung = noi_dung; }

    // ngay_tao
    public Timestamp getNgay_tao()               { return ngay_tao; }
    public void setNgay_tao(Timestamp ngay_tao)  { this.ngay_tao = ngay_tao; }

    // danh_sach_anh
    public List<String> getDanh_sach_anh()                      { return danh_sach_anh; }
    public void setDanh_sach_anh(List<String> danh_sach_anh)    { this.danh_sach_anh = danh_sach_anh; }

    // hinh_anh
    public List<String> getHinh_anh()                   { return hinh_anh; }
    public void setHinh_anh(List<String> hinh_anh)      { this.hinh_anh = hinh_anh; }

    // so_like
    public long getSo_like()              { return so_like; }
    public void setSo_like(long so_like)  { this.so_like = so_like; }

    // so_binh_luan
    public long getSo_binh_luan()                   { return so_binh_luan; }
    public void setSo_binh_luan(long so_binh_luan)  { this.so_binh_luan = so_binh_luan; }

    // da_chinh_sua
    public boolean isDa_chinh_sua()                     { return da_chinh_sua; }
    public void setDa_chinh_sua(boolean da_chinh_sua)   { this.da_chinh_sua = da_chinh_sua; }

    // da_xoa
    public boolean isDa_xoa()                  { return da_xoa; }
    public void setDa_xoa(boolean da_xoa)      { this.da_xoa = da_xoa; }

    // is_repost
    public boolean isIs_repost()                    { return is_repost; }
    public void setIs_repost(boolean is_repost)     { this.is_repost = is_repost; }
}