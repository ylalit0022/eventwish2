package com.ds.eventwish.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "flashy_messages")
public class FlashyMessageEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private int priority;
    private boolean isRead;
    private boolean isDisplaying;

    public FlashyMessageEntity(@NonNull String id, String title, String message) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.priority = 1;
        this.isRead = false;
        this.isDisplaying = false;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isDisplaying() {
        return isDisplaying;
    }

    public void setDisplaying(boolean displaying) {
        isDisplaying = displaying;
    }
} 