package com.example.doanmxh.Message;

import com.google.firebase.Timestamp;

import java.util.Date;

class SearchItem {
    String messageId;
    String content;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    Date timestamp;

    int position;

    public SearchItem(String messageId, String content, Date timestamp, int position) {
        this.messageId = messageId;
        this.content = content;
        this.timestamp = timestamp;
        this.position = position;
    }
}