package com.ds.eventwish.ui.home;

import java.util.Objects;

public class GreetingItem {
    private int id;
    private String title;
    private String category;
    private String imageUrl;
    private String shareTime;

    public String getShareTime() {
        return shareTime;
    }

    public void setShareTime(String shareTime) {
        this.shareTime = shareTime;
    }

    public GreetingItem(int id, String title, String category, String imageUrl) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GreetingItem that = (GreetingItem) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
