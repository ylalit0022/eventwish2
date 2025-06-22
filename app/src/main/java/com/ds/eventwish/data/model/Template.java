package com.ds.eventwish.data.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.converter.CategoryIconConverter;
import com.ds.eventwish.data.converter.DateConverter;
import com.ds.eventwish.data.converter.StringListConverter;
import com.ds.eventwish.utils.NumberFormatter;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Data model class for a template
 */
@Entity(
    tableName = "templates",
    foreignKeys = {
        @ForeignKey(
            entity = Category.class,
            parentColumns = "id",
            childColumns = "categoryId",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index(value = {"categoryId"}),
        @Index(value = {"lastUpdated"}),
        @Index(value = {"isLiked"}),
        @Index(value = {"isFavorited"})
    }
)
@TypeConverters({DateConverter.class})
public class Template {
    @PrimaryKey
    @NonNull
    private String id;
    
    private String title;
    
    @ColumnInfo(name = "categoryId")
    private String categoryId;
    
    private String previewUrl;
    
    @SerializedName("likes")
    private long likeCount;
    
    @SerializedName("favorites") 
    private long favoriteCount;
    
    @SerializedName("sharedCount")
    private long shareCount;
    
    @ColumnInfo(name = "isLiked")
    private boolean isLiked;
    
    @ColumnInfo(name = "isFavorited")
    private boolean isFavorited;
    
    @ColumnInfo(name = "lastUpdated")
    private Date lastUpdated;
    
    private boolean likeChanged;
    private boolean favoriteChanged;
    
    @SerializedName("htmlContent")
    @ColumnInfo(name = "htmlContent")
    private String htmlContent;
    
    @SerializedName("cssContent")
    @ColumnInfo(name = "cssContent")
    private String cssContent;
    
    @SerializedName("jsContent")
    @ColumnInfo(name = "jsContent")
    private String jsContent;
    
    private boolean recommended;
    
    @SerializedName("createdAt")
    private Date createdAt;

    // Default constructor required by Room
    public Template() {
        // Required empty constructor for Firestore
    }

    @Ignore
    public Template(String id, String title, String categoryId, String previewUrl) {
        this.id = id;
        this.title = title;
        this.categoryId = categoryId;
        this.previewUrl = previewUrl;
        this.likeCount = 0;
        this.favoriteCount = 0;
        this.shareCount = 0;
        this.isLiked = false;
        this.isFavorited = false;
        this.lastUpdated = new Date();
        this.likeChanged = false;
        this.favoriteChanged = false;
        this.recommended = false;
        this.createdAt = new Date();
    }

    @Ignore
    public Template(String id, String title, String categoryId, String previewUrl, 
                   boolean isLiked, boolean isFavorited, long likeCount) {
        this(id, title, categoryId, previewUrl);
        this.isLiked = isLiked;
        this.isFavorited = isFavorited;
        // Ensure count is never negative
        this.likeCount = Math.max(0L, likeCount);
    }

    @Ignore
    public Template(String id, String title, String categoryId, String previewUrl, 
                   boolean isLiked, boolean isFavorited, long likeCount, long favoriteCount) {
        this(id, title, categoryId, previewUrl);
        this.isLiked = isLiked;
        this.isFavorited = isFavorited;
        // Ensure counts are never negative
        this.likeCount = Math.max(0L, likeCount);
        this.favoriteCount = Math.max(0L, favoriteCount);
    }

    @Ignore
    public Template(Template other) {
        this.id = other.id;
        this.title = other.title;
        this.categoryId = other.categoryId;
        this.previewUrl = other.previewUrl;
        this.likeCount = other.likeCount;
        this.favoriteCount = other.favoriteCount;
        this.shareCount = other.shareCount;
        this.isLiked = other.isLiked;
        this.isFavorited = other.isFavorited;
        this.lastUpdated = other.lastUpdated;
        this.likeChanged = other.likeChanged;
        this.favoriteChanged = other.favoriteChanged;
        this.htmlContent = other.htmlContent;
        this.cssContent = other.cssContent;
        this.jsContent = other.jsContent;
        this.recommended = other.recommended;
        this.createdAt = other.createdAt;
    }

    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { 
        // Log the count change for debugging
        if (this.likeCount != likeCount) {
            Log.d("Template", "Template " + getId() + " like count changed: " + this.likeCount + " -> " + likeCount);
        }
        // Ensure count is never negative
        this.likeCount = Math.max(0L, likeCount); 
    }
    
    /**
     * Get formatted like count (e.g., 1K, 1.2K, 1M)
     * @return Formatted like count as a string
     */
    public String getFormattedLikeCount() {
        return NumberFormatter.formatCount(likeCount);
    }

    public long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(long favoriteCount) { 
        // Log the count change for debugging
        if (this.favoriteCount != favoriteCount) {
            Log.d("Template", "Template " + getId() + " favorite count changed: " + this.favoriteCount + " -> " + favoriteCount);
        }
        // Ensure count is never negative
        this.favoriteCount = Math.max(0L, favoriteCount); 
    }
    
    /**
     * Get formatted favorite count (e.g., 1K, 1.2K, 1M)
     * @return Formatted favorite count as a string
     */
    public String getFormattedFavoriteCount() {
        return NumberFormatter.formatCount(favoriteCount);
    }

    public long getShareCount() { return shareCount; }
    public void setShareCount(long shareCount) { 
        // Log the count change for debugging
        if (this.shareCount != shareCount) {
            Log.d("Template", "Template " + getId() + " share count changed: " + this.shareCount + " -> " + shareCount);
        }
        // Ensure count is never negative
        this.shareCount = Math.max(0L, shareCount); 
    }
    
    /**
     * Get formatted share count (e.g., 1K, 1.2K, 1M)
     * @return Formatted share count as a string
     */
    public String getFormattedShareCount() {
        return NumberFormatter.formatCount(shareCount);
    }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { 
        // Log the state change for debugging
        if (this.isLiked != liked) {
            Log.d("Template", "Template " + getId() + " liked state changed: " + this.isLiked + " -> " + liked);
        }
        this.isLiked = liked;
        this.likeChanged = true;
    }

    public boolean isFavorited() { return isFavorited; }
    public void setFavorited(boolean favorited) { 
        // Log the state change for debugging
        if (this.isFavorited != favorited) {
            Log.d("Template", "Template " + getId() + " favorited state changed: " + this.isFavorited + " -> " + favorited);
        }
        this.isFavorited = favorited;
        this.favoriteChanged = true;
    }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isLikeChanged() { return likeChanged; }
    public void setLikeChanged(boolean likeChanged) { this.likeChanged = likeChanged; }

    public boolean isFavoriteChanged() { return favoriteChanged; }
    public void setFavoriteChanged(boolean favoriteChanged) { this.favoriteChanged = favoriteChanged; }

    // Additional getters and setters for missing fields
    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
    
    public String getCssContent() { return cssContent; }
    public void setCssContent(String cssContent) { this.cssContent = cssContent; }
    
    public String getJsContent() { return jsContent; }
    public void setJsContent(String jsContent) { this.jsContent = jsContent; }
    
    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public long getCreatedAtTimestamp() { 
        return createdAt != null ? createdAt.getTime() : 0;
    }
    
    // Alias methods for backward compatibility
    public String getName() { return getTitle(); }
    public void setName(String name) { setTitle(name); }
    
    public String getCategory() { return getCategoryId(); }
    public void setCategory(String category) { setCategoryId(category); }
    
    public String getThumbnailUrl() { return getPreviewUrl(); }
    public void setThumbnailUrl(String url) { setPreviewUrl(url); }
    
    public String getImageUrl() { return getPreviewUrl(); }
    
    public String getHtml() { return getHtmlContent(); }
    public void setHtml(String html) { setHtmlContent(html); }
    
    public void clearChangeFlags() {
        this.likeChanged = false;
        this.favoriteChanged = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Template template = (Template) o;
        return likeCount == template.likeCount &&
               favoriteCount == template.favoriteCount &&
               shareCount == template.shareCount &&
               isLiked == template.isLiked &&
               isFavorited == template.isFavorited &&
               likeChanged == template.likeChanged &&
               favoriteChanged == template.favoriteChanged &&
               recommended == template.recommended &&
               Objects.equals(id, template.id) &&
               Objects.equals(title, template.title) &&
               Objects.equals(categoryId, template.categoryId) &&
               Objects.equals(previewUrl, template.previewUrl) &&
               Objects.equals(lastUpdated, template.lastUpdated) &&
               Objects.equals(htmlContent, template.htmlContent) &&
               Objects.equals(cssContent, template.cssContent) &&
               Objects.equals(jsContent, template.jsContent) &&
               Objects.equals(createdAt, template.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, categoryId, previewUrl, likeCount, favoriteCount, shareCount,
                          isLiked, isFavorited, lastUpdated, likeChanged, favoriteChanged,
                          htmlContent, cssContent, jsContent, recommended, createdAt);
    }
}
