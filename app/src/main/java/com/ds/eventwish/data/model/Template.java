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
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * Data model class for a template
 */
@Entity(
    tableName = "template",
    foreignKeys = {
        @ForeignKey(
            entity = Category.class,
            parentColumns = "id",
            childColumns = "category_id",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index(value = {"category_id"}),
        @Index(value = {"created_at"}),
        @Index(value = {"updated_at"}),
        @Index(value = {"is_featured"}),
        @Index(value = {"is_visible"}),
        @Index(value = {"is_liked"}),
        @Index(value = {"is_favorited"})
    }
)
public class Template {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    @SerializedName("_id")
    private String id;
    
    @ColumnInfo(name = "title")
    @SerializedName("title")
    private String title;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "content")
    @SerializedName("content")
    private String content;

    @ColumnInfo(name = "category_id")
    @SerializedName("categoryId")
    private String categoryId;

    @ColumnInfo(name = "tags")
    @SerializedName("tags")
    @TypeConverters(StringListConverter.class)
    private List<String> tags;

    @ColumnInfo(name = "created_at")
    @SerializedName("createdAt")
    @TypeConverters(DateConverter.class)
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    @SerializedName("updatedAt")
    @TypeConverters(DateConverter.class)
    private Date updatedAt;

    @ColumnInfo(name = "is_featured", defaultValue = "0")
    @SerializedName("isFeatured")
    private boolean isFeatured;

    @ColumnInfo(name = "is_visible", defaultValue = "1")
    @SerializedName("isVisible")
    private boolean isVisible;

    @ColumnInfo(name = "view_count", defaultValue = "0")
    @SerializedName("viewCount")
    private int viewCount;

    @ColumnInfo(name = "share_count", defaultValue = "0")
    @SerializedName("shareCount")
    private int shareCount;

    @ColumnInfo(name = "like_count", defaultValue = "0")
    @SerializedName("likeCount")
    private int likeCount;

    @ColumnInfo(name = "favorite_count", defaultValue = "0")
    @SerializedName("favoriteCount")
    private int favoriteCount;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "recipient_name")
    @SerializedName("recipientName")
    private String recipientName;

    @ColumnInfo(name = "sender_name")
    @SerializedName("senderName")
    private String senderName;

    @Ignore
    @SerializedName("template")
    private Template template;

    @ColumnInfo(name = "short_code")
    @SerializedName("shortCode")
    private String shortCode;
    
    @ColumnInfo(name = "html_content")
    @SerializedName("htmlContent")
    private String htmlContent;
    
    @ColumnInfo(name = "css_content")
    @SerializedName("cssContent")
    private String cssContent;
    
    @ColumnInfo(name = "js_content")
    @SerializedName("jsContent")
    private String jsContent;
    
    @ColumnInfo(name = "preview_url")
    @SerializedName("previewUrl")
    private String previewUrl;
    
    @ColumnInfo(name = "thumbnail_url")
    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;
    
    @ColumnInfo(name = "status")
    private boolean status;
    
    @ColumnInfo(name = "category_icon")
    @SerializedName("categoryIcon")
    @TypeConverters(CategoryIconConverter.class)
    private CategoryIcon categoryIcon;
    
    @ColumnInfo(name = "type", defaultValue = "html")
    private String type = "html";
    
    @ColumnInfo(name = "recommended", defaultValue = "0")
    private boolean recommended = false;

    @ColumnInfo(name = "is_liked", defaultValue = "0")
    private boolean isLiked;

    @ColumnInfo(name = "is_favorited", defaultValue = "0")
    private boolean isFavorited;

    private boolean likeChanged = false;
    private boolean favoriteChanged = false;

    // Default constructor required by Room
    public Template() {
    }

    @Ignore
    public Template(@NonNull String id) {
        this.id = id;
    }

    @Ignore
    public Template(String id, String name, String categoryId, String imageUrl,
                   boolean isLiked, boolean isFavorited, int likeCount) {
        this.id = id;
        this.title = name;
        this.categoryId = categoryId;
        this.previewUrl = imageUrl;
        this.isLiked = isLiked;
        this.isFavorited = isFavorited;
        this.likeCount = likeCount;
        this.favoriteCount = 0;
    }

    public boolean isRecommended() {
        return recommended;
    }
    
    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    // HTML content access with alternative method names for compatibility
    public String getHtml() {
        return htmlContent;
    }
    
    public void setHtml(String html) {
        this.htmlContent = html;
    }

    @NonNull
    public String getId() {
        return id;
    }

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
    
    // Get dates as formatted strings
    public String getCreatedAtString() { return createdAt != null ? createdAt.toString() : null; }
    public String getUpdatedAtString() { return updatedAt != null ? updatedAt.toString() : null; }
    
    @NonNull
    public String getName() {
        return title;
    }
    
    // Get createdAt as timestamp for comparison
    public long getCreatedAtTimestamp() {
        if (createdAt == null) {
            Log.d("Template", "Template ID: " + id + " has null createdAt date, using current time");
            // Use current time as fallback to ensure new templates without dates appear at top
            return System.currentTimeMillis();
        }
        try {
            return createdAt.getTime();
        } catch (Exception e) {
            Log.e("Template", "Error getting timestamp for template: " + id, e);
            return System.currentTimeMillis(); // Use current time as fallback
        }
    }

    public String getDescription() { return description; }
    public String getContent() { return content; }
    public String getCategoryId() { return categoryId; }
    public List<String> getTags() { return tags; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public boolean isFeatured() { return isFeatured; }
    public boolean isVisible() { return isVisible; }
    public int getViewCount() { return viewCount; }
    public int getShareCount() { return shareCount; }
    public int getLikeCount() { return likeCount; }
    public int getFavoriteCount() { return favoriteCount; }

    public void setId(@NonNull String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
    public void setCssContent(String cssContent) { this.cssContent = cssContent; }
    public void setJsContent(String jsContent) { this.jsContent = jsContent; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setStatus(boolean status) { this.status = status; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public void setDescription(String description) { this.description = description; }
    public void setContent(String content) { this.content = content; }
    public void setCategoryId(@NonNull String categoryId) { this.categoryId = categoryId; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setFeatured(boolean featured) { this.isFeatured = featured; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public boolean isLikeChanged() {
        return likeChanged;
    }
    
    public void setLikeChanged(boolean likeChanged) {
        this.likeChanged = likeChanged;
    }
    
    public boolean isFavoriteChanged() {
        return favoriteChanged;
    }
    
    public void setFavoriteChanged(boolean favoriteChanged) {
        this.favoriteChanged = favoriteChanged;
    }
    
    public void setLiked(boolean liked) {
        if (this.isLiked != liked) {
            this.isLiked = liked;
            this.likeChanged = true;
            // Reset favorite changed flag to prevent interference
            this.favoriteChanged = false;
        }
    }
    
    public void setFavorited(boolean favorited) {
        if (this.isFavorited != favorited) {
            this.isFavorited = favorited;
            this.favoriteChanged = true;
            // Reset like changed flag to prevent interference
            this.likeChanged = false;
        }
    }

    public boolean isLiked() {
        return isLiked;
    }

    public boolean isFavorited() {
        return isFavorited;
    }

    public void clearChangeFlags() {
        this.likeChanged = false;
        this.favoriteChanged = false;
    }

    /**
     * Convert to UI model
     */
    public com.ds.eventwish.ui.template.Template toUiModel() {
        return new com.ds.eventwish.ui.template.Template(
            id,
            title,
            categoryId,
            previewUrl,
            isLiked,
            isFavorited,
            likeCount
        );
    }

    /**
     * Create from UI model
     */
    public static Template fromUiModel(com.ds.eventwish.ui.template.Template uiTemplate) {
        Template template = new Template();
        template.setId(uiTemplate.getId());
        template.setTitle(uiTemplate.getName());
        template.setCategoryId(uiTemplate.getCategoryId());
        template.setPreviewUrl(uiTemplate.getImageUrl());
        template.setLiked(uiTemplate.isLiked());
        template.setFavorited(uiTemplate.isFavorited());
        template.setLikeCount(uiTemplate.getLikeCount());
        return template;
    }
}
