package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a shared wish in the local database
 */
@Entity(tableName = "shared_wishes")
public class SharedWishEntity {
    
    @PrimaryKey
    @NonNull
    private String shortCode;
    
    private String templateId;
    private String customizedHtml;
    private String shareUrl;
    private long createdAt;
    
    /**
     * Default constructor for Room
     */
    public SharedWishEntity() {
        this.shortCode = "";
    }
    
    /**
     * Constructor with all fields
     * @param shortCode Unique short code for the wish
     * @param templateId ID of the template used
     * @param customizedHtml Customized HTML content
     * @param shareUrl URL for sharing
     * @param createdAt Timestamp when created
     */
    public SharedWishEntity(@NonNull String shortCode, String templateId, String customizedHtml, 
                             String shareUrl, long createdAt) {
        this.shortCode = shortCode;
        this.templateId = templateId;
        this.customizedHtml = customizedHtml;
        this.shareUrl = shareUrl;
        this.createdAt = createdAt;
    }
    
    @NonNull
    public String getShortCode() {
        return shortCode;
    }
    
    public void setShortCode(@NonNull String shortCode) {
        this.shortCode = shortCode;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getCustomizedHtml() {
        return customizedHtml;
    }
    
    public void setCustomizedHtml(String customizedHtml) {
        this.customizedHtml = customizedHtml;
    }
    
    public String getShareUrl() {
        return shareUrl;
    }
    
    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 