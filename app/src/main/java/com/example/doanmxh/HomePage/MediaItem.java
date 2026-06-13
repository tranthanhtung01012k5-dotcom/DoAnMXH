package com.example.doanmxh.HomePage;
// MediaItem.java
public class MediaItem {
    public enum Type { IMAGE, VIDEO }

    private Type type;
    private String url;

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    private String thumbnail; // chỉ dùng cho video

    public MediaItem(String url, Type type) {
        this.url = url;
        this.type = type;
    }
    // getters/setters...
}
