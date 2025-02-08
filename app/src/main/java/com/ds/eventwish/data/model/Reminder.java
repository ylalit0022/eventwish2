package com.ds.eventwish.data.model;

public class Reminder {
    private long id;
    private String title;
    private String description;
    private long dateTime;
    private boolean isCompleted;

    public Reminder(String title, String description, long dateTime) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.isCompleted = false;
    }

    // Getters and setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}