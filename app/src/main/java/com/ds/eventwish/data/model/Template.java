package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;

public class Template {
    @SerializedName("_id")
    private String id;
    
    private String title;
    private String category;
    
    @SerializedName("htmlContent")
    private String htmlContent;
    
    @SerializedName("cssContent")
    private String cssContent;
    
    @SerializedName("jsContent")
    private String jsContent;
    
    @SerializedName("previewUrl")
    private String previewUrl;
    
    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;
    
    private boolean status;
    
    @SerializedName("categoryIcon")
    private String categoryIcon;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getHtmlContent() { return htmlContent; }
    public String getCssContent() { return cssContent; }
    public String getJsContent() { return jsContent; }
    public String getPreviewUrl() { return previewUrl; }
    public String getThumbnailUrl() { return thumbnailUrl != null ? thumbnailUrl : previewUrl; }
    public boolean isStatus() { return status; }
    public String getCategoryIcon() { return categoryIcon; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
    public void setCssContent(String cssContent) { this.cssContent = cssContent; }
    public void setJsContent(String jsContent) { this.jsContent = jsContent; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setStatus(boolean status) { this.status = status; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
