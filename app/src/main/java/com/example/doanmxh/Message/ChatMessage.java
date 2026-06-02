package com.example.doanmxh.Message;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatMessage {

    private String messageId;
    private String nguoiGuiId;
    private String noiDung;
    private Date thoiGian;
    private boolean daDoc;
    private String loai;
    private String reaction;
    // Sửa lại field postId
    private boolean da_xoa;

    public boolean isDa_xoa() {
        return da_xoa;
    }

    public void setDa_xoa(boolean da_xoa) {
        this.da_xoa = da_xoa;
    }
    @PropertyName("post_id")
    private String postId;

    @PropertyName("post_id")
    public String getPostId() { return postId; }

    @PropertyName("post_id")
    public void setPostId(String postId) { this.postId = postId; }
    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }
    public String getTenNguoiGui() {
        return tenNguoiGui;
    }

    public void setTenNguoiGui(String tenNguoiGui) {
        this.tenNguoiGui = tenNguoiGui;
    }

    private String tenNguoiGui;
    public ChatMessage() {}

    public ChatMessage(String nguoiGuiId, String noiDung, String loai) {
        this.nguoiGuiId = nguoiGuiId;
        this.noiDung    = noiDung;
        this.loai       = loai;
        this.daDoc      = false;
    }

    public String getMessageId()          { return messageId; }
    public void   setMessageId(String v)  { messageId = v; }

    @PropertyName("nguoi_gui_id")
    public String getNguoiGuiId()              { return nguoiGuiId; }
    @PropertyName("nguoi_gui_id")
    public void   setNguoiGuiId(String v)      { nguoiGuiId = v; }
    @PropertyName("reply_to_id")
    private String replyToId;

    @PropertyName("reply_to_content")
    private String replyToContent;
    @PropertyName("reply_to_id")
    public String getReplyToId() {
        return replyToId;
    }
    @PropertyName("reply_to_id")
    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }
    @PropertyName("reply_to_content")
    public String getReplyToContent() {
        return replyToContent;
    }
    @PropertyName("reply_to_content")
    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }
    @PropertyName("reply_to_sender_name")
    public String getReplyToSenderName() {
        return replyToSenderName;
    }
    @PropertyName("reply_to_sender_name")
    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }

    @PropertyName("reply_to_sender_name")
    private String replyToSenderName;

    // + getter/setter tương ứng
    @PropertyName("noi_dung")
    public String getNoiDung()                 { return noiDung; }
    @PropertyName("noi_dung")
    public void   setNoiDung(String v)         { noiDung = v; }

    @ServerTimestamp
    @PropertyName("thoi_gian")
    public Date   getThoiGian()                { return thoiGian; }
    @PropertyName("thoi_gian")
    public void   setThoiGian(Date v)          { thoiGian = v; }

    @PropertyName("da_doc")
    public boolean isDaDoc()                   { return daDoc; }
    @PropertyName("da_doc")
    public void    setDaDoc(boolean v)         { daDoc = v; }

    @PropertyName("loai")
    public String getLoai()                    { return loai; }
    @PropertyName("loai")
    public void   setLoai(String v)            { loai = v; }
}