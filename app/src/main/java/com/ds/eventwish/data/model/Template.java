package com.ds.eventwish.data.model;

import android.util.Log;

import androidx.room.TypeConverters;

import com.ds.eventwish.data.converter.CategoryIconConverter;
import com.google.gson.annotations.SerializedName;

public class Template {
    @SerializedName("_id")
    private String id;
    
    private String title;
    private String category;
    @SerializedName("recipientName")
    private String recipientName;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("template")
    private Template template;

    @SerializedName("shortCode")
    private String shortCode;
    
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
    @TypeConverters(CategoryIconConverter.class)
    private CategoryIcon categoryIcon;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters
    public String getId() { return id; }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getHtmlContent() { return htmlContent; }
    public String getCssContent() { return cssContent; }
    public String getJsContent() { return jsContent; }
    public String getPreviewUrl() { return previewUrl; }
    public String getThumbnailUrl() { return thumbnailUrl != null ? thumbnailUrl : previewUrl; }
    public boolean isStatus() { return status; }
    public CategoryIcon getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(CategoryIcon categoryIcon) { this.categoryIcon = categoryIcon; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    
    // Get createdAt as timestamp for comparison
    public long getCreatedAtTimestamp() {
        try {
            if (createdAt == null) {
                android.util.Log.d("Template", "createdAt is null");
                return 0;
            }
            
            android.util.Log.d("Template", "Raw createdAt value: " + createdAt);
            
            // If createdAt is in ISO format (e.g., "2023-01-01T12:00:00.000Z")
            if (createdAt.contains("T")) {
                // Simple conversion - just get the milliseconds since epoch
                return java.time.Instant.parse(createdAt).toEpochMilli();
            }
            
            // Try parsing as Unix timestamp (seconds since epoch)
            try {
                // If it's a numeric string, it might be a Unix timestamp
                long timestamp = Long.parseLong(createdAt);
                // If it's in seconds (Unix timestamp), convert to milliseconds
                if (timestamp < 2000000000L) { // If less than year ~2033, it's likely seconds
                    timestamp *= 1000;
                }
                android.util.Log.d("Template", "Parsed as numeric timestamp: " + timestamp);
                return timestamp;
            } catch (NumberFormatException e) {
                // Not a numeric timestamp
                android.util.Log.d("Template", "Not a numeric timestamp: " + e.getMessage());
            }
            
            return 0;
        } catch (Exception e) {
            android.util.Log.e("Template", "Error parsing date: " + e.getMessage() + " for value: " + createdAt);
            return 0;
        }
    }

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
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
