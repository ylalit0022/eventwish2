package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;

public class CategoryIcon {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("categoryIcon")
    private String categoryIcon;

    public CategoryIcon(String id, String category, String categoryIcon) {
        this.id = id;
        this.category = category;
        this.categoryIcon = categoryIcon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }
}