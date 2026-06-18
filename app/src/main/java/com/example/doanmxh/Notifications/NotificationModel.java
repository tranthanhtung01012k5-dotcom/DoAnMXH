package com.example.doanmxh.Notifications;

public class NotificationModel {

    private String  id;
    private String  senderId;
    private String  avatar;
    private String  name;
    private String  content;
    private String  time;
    private String  type;          // LIKE | FOLLOW | COMMENT | MENTION | REPOST | FOLLOWING
    private String  postId;
    private String  postImageUrl;
    private String postVideoUrl;
    private String postAudioUrl;
    private String  postSnippet;
    private boolean isRead;
    private boolean isFollowing;   // Trạng thái "đang theo dõi lại" (chỉ cho FOLLOW)

    // ─── Constructor đầy đủ ──────────────────────────────────────────────────
    public NotificationModel(String id, String senderId,
                             String avatar, String name,
                             String content, String time, String type,
                             String postId, String postImageUrl,String postVideoUrl,String postAudioUrl, String postSnippet,
                             boolean isRead, boolean isFollowing) {
        this.id           = id;
        this.senderId     = senderId;
        this.avatar       = avatar;
        this.name         = name;
        this.content      = content;
        this.time         = time;
        this.type         = type;
        this.postId       = postId;
        this.postImageUrl = postImageUrl;
        this.postVideoUrl = postVideoUrl;
        this.postAudioUrl = postAudioUrl;
        this.postSnippet  = postSnippet;
        this.isRead       = isRead;
        this.isFollowing  = isFollowing;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────
    public String  getId()           { return id; }
    public String  getSenderId()     { return senderId; }
    public String  getAvatar()       { return avatar; }
    public String  getName()         { return name; }
    public String  getContent()      { return content; }
    public String  getTime()         { return time; }
    public String  getType()         { return type; }
    public String  getPostId()       { return postId; }
    public String  getPostImageUrl() { return postImageUrl; }
    public String  getPostSnippet()  { return postSnippet; }
    public boolean isRead()          { return isRead; }
    public boolean isFollowing()     { return isFollowing; }

    // ─── Setters ─────────────────────────────────────────────────────────────
    public void setRead(boolean read)           { isRead = read; }
    public void setFollowing(boolean following) { isFollowing = following; }

    // ─── Helper: nội dung hiển thị mặc định theo type ────────────────────────
    public String getDisplayContent() {
        if (content != null && !content.isEmpty()) return content;
        switch (type == null ? "" : type) {
            case "LIKE":      return "đã thích bài viết của bạn.";
            case "FOLLOW":    return "đã bắt đầu theo dõi bạn.";
            case "LIKE_COMMENT":
                return "đã thích bình luận của bạn.";
            case "COMMENT":   return "đã bình luận về bài viết của bạn.";
            case "MENTION":   return "đã nhắc đến bạn trong một bài viết.";
            case "REPOST":    return "đã đăng lại bài viết của bạn.";
            case "FOLLOWING": return "đã đăng một bài viết mới.";
            default:          return "";
        }
    }
}