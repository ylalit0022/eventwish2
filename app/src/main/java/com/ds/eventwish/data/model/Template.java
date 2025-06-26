package com.ds.eventwish.data.model;

import android.text.TextUtils;
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
    
    /**
     * Enum for template types with proper validation and helper methods
     */
    public enum TemplateType {
        HTML("html", "Interactive templates with HTML, CSS, and JavaScript"),
        IMAGE("image", "Static image-based templates"),
        VIDEO("video", "Video-based templates and animations");
        
        private final String value;
        private final String description;
        
        TemplateType(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Parse template type from string value
         * @param value String value of template type
         * @return TemplateType enum or HTML as default
         */
        public static TemplateType fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return HTML; // Default fallback
            }
            
            String normalizedValue = value.toLowerCase().trim();
            for (TemplateType type : values()) {
                if (type.value.equals(normalizedValue)) {
                    return type;
                }
            }
            
            // Fallback to HTML for unknown types
            Log.w("Template", "Unknown template type: " + value + ", defaulting to HTML");
            return HTML;
        }
        
        /**
         * Check if template type supports interactive editing
         * @return true if template supports editing
         */
        public boolean supportsEditing() {
            return this == HTML;
        }
        
        /**
         * Check if template type requires media player
         * @return true if template needs media player
         */
        public boolean requiresMediaPlayer() {
            return this == VIDEO;
        }
        
        /**
         * Check if template type is image-based
         * @return true if template is image-based
         */
        public boolean isImageBased() {
            return this == IMAGE;
        }
        
        /**
         * Get appropriate MIME type for template rendering
         * @return MIME type string
         */
        public String getMimeType() {
            switch (this) {
                case HTML:
                    return "text/html";
                case VIDEO:
                    return "video/*";
                case IMAGE:
                default:
                    return "image/*";
            }
        }
    }
    
    /**
     * Enum for template interaction types
     */
    public enum InteractionType {
        VIEW("view", "View template"),
        EDIT("edit", "Edit template"),
        PREVIEW("preview", "Preview template"),
        SHARE("share", "Share template");
        
        private final String value;
        private final String description;
        
        InteractionType(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
    }

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
    

    
    @SerializedName("createdAt")
    private Date createdAt;
    
    // Creator Information (from backend Template.js)
    @SerializedName("creatorId")
    private String creatorId;
    
    @SerializedName("creatorUid")
    private String creatorUid;  // Firebase UID of the template creator
    
    @SerializedName("creatorName")
    private String creatorName;
    
    @SerializedName("creatorProfilePhoto")
    private String creatorProfilePhoto;
    
    @SerializedName("generatedByUser")
    private String generatedByUser;
    
    @SerializedName("generatedByUserUid")
    private String generatedByUserUid;  // Firebase UID of the user who generated this template

    // Media URLs (from backend Template.js)
    @SerializedName("videoUrl")
    @ColumnInfo(name = "videoUrl")
    private String videoUrl;
    
    @SerializedName("imageUrl")
    @ColumnInfo(name = "imageUrl")
    private String imageUrl;

    // Monetization & Access Control (from backend Template.js)
    @SerializedName("status")
    @ColumnInfo(name = "status")
    private boolean status = true;
    
    @SerializedName("isPremium")
    @ColumnInfo(name = "isPremium")
    private boolean isPremium = false;
    
    @SerializedName("isFeatured")
    @ColumnInfo(name = "isFeatured")
    private boolean isFeatured = false;
    
    @SerializedName("isTrending")
    @ColumnInfo(name = "isTrending")
    private boolean isTrending = false;
    
    @SerializedName("isFlagged")
    @ColumnInfo(name = "isFlagged")
    private boolean isFlagged = false;
    
    @SerializedName("isLowPerforming")
    @ColumnInfo(name = "isLowPerforming")
    private boolean isLowPerforming = false;
    
    @SerializedName("price")
    @ColumnInfo(name = "price")
    private double price = 0.0;
    
    @SerializedName("moderationStatus")
    @ColumnInfo(name = "moderationStatus")
    private String moderationStatus = "approved"; // approved, pending, rejected

    // Engagement Metrics (from backend Template.js)
    @SerializedName("usageCount")
    @ColumnInfo(name = "usageCount")
    private long usageCount = 0;
    
    @SerializedName("viewCount")
    @ColumnInfo(name = "viewCount")
    private long viewCount = 0;
    
    @SerializedName("downloadCount")
    @ColumnInfo(name = "downloadCount")
    private long downloadCount = 0;
    
    @SerializedName("reportCount")
    @ColumnInfo(name = "reportCount")
    private long reportCount = 0;
    
    @SerializedName("rating")
    @ColumnInfo(name = "rating")
    private double rating = 0.0;
    
    @SerializedName("ratingCount")
    @ColumnInfo(name = "ratingCount")
    private long ratingCount = 0;

    // Weekly Metrics (from backend Template.js)
    @SerializedName("weeklyUsageCount")
    @ColumnInfo(name = "weeklyUsageCount")
    private long weeklyUsageCount = 0;
    
    @SerializedName("weeklyLikes")
    @ColumnInfo(name = "weeklyLikes")
    private long weeklyLikes = 0;
    
    @SerializedName("weeklyFavorites")
    @ColumnInfo(name = "weeklyFavorites")
    private long weeklyFavorites = 0;
    
    @SerializedName("weeklyViewCount")
    @ColumnInfo(name = "weeklyViewCount")
    private long weeklyViewCount = 0;
    
    @SerializedName("weeklySharedCount")
    @ColumnInfo(name = "weeklySharedCount")
    private long weeklySharedCount = 0;
    
    @SerializedName("weeklyDownloadCount")
    @ColumnInfo(name = "weeklyDownloadCount")
    private long weeklyDownloadCount = 0;
    
    @SerializedName("weeklyReportCount")
    @ColumnInfo(name = "weeklyReportCount")
    private long weeklyReportCount = 0;
    
    @SerializedName("weeklyScoreLastReset")
    @ColumnInfo(name = "weeklyScoreLastReset")
    @TypeConverters(DateConverter.class)
    private Date weeklyScoreLastReset;

    // AI Metadata (from backend Template.js)
    @SerializedName("isAIGenerated")
    @ColumnInfo(name = "isAIGenerated")
    private boolean isAIGenerated = false;
    
    @SerializedName("aiPrompt")
    @ColumnInfo(name = "aiPrompt")
    private String aiPrompt = "";
    
    @SerializedName("aiModel")
    @ColumnInfo(name = "aiModel")
    private String aiModel = "";
    
    @SerializedName("aiStyle")
    @ColumnInfo(name = "aiStyle")
    private String aiStyle = "";
    
    @SerializedName("aiGenerationStage")
    @ColumnInfo(name = "aiGenerationStage")
    private String aiGenerationStage = "initial"; // initial, on_edit, variation
    
    @SerializedName("generationMetadata")
    @ColumnInfo(name = "generationMetadata")
    private String generationMetadata; // JSON string for nested object

    // Categorization & Tags (from backend Template.js)
    @SerializedName("festivalTag")
    @ColumnInfo(name = "festivalTag")
    private String festivalTag = "";
    
    @SerializedName("tags")
    @ColumnInfo(name = "tags")
    @TypeConverters(StringListConverter.class)
    private List<String> tags;
    
    @SerializedName("styleTags")
    @ColumnInfo(name = "styleTags")
    @TypeConverters(StringListConverter.class)
    private List<String> styleTags;
    
    @SerializedName("searchKeywords")
    @ColumnInfo(name = "searchKeywords")
    @TypeConverters(StringListConverter.class)
    private List<String> searchKeywords;
    
    @SerializedName("variationOf")
    @ColumnInfo(name = "variationOf")
    private String variationOf; // Template ID this is a variation of
    
    @SerializedName("relatedTemplates")
    @ColumnInfo(name = "relatedTemplates")
    @TypeConverters(StringListConverter.class)
    private List<String> relatedTemplates; // List of related template IDs

    // Visibility & Ranking (from backend Template.js)
    @SerializedName("templateType")
    @ColumnInfo(name = "templateType")
    private String templateType = "html"; // html, image, video
    
    @SerializedName("visibilityScore")
    @ColumnInfo(name = "visibilityScore")
    private double visibilityScore = 0.0;
    
    @SerializedName("boostPriority")
    @ColumnInfo(name = "boostPriority")
    private double boostPriority = 0.0;
    
    @SerializedName("lastBoostedAt")
    @ColumnInfo(name = "lastBoostedAt")
    @TypeConverters(DateConverter.class)
    private Date lastBoostedAt;
    
    @SerializedName("ignoredByUsers")
    @ColumnInfo(name = "ignoredByUsers")
    @TypeConverters(StringListConverter.class)
    private List<String> ignoredByUsers; // List of user IDs who ignored this template

    // Customization Options (from backend Template.js)
    @SerializedName("customizationOptions")
    @ColumnInfo(name = "customizationOptions")
    private String customizationOptions; // JSON string for nested object with allowNameEdit, allowPhotoEdit, etc.

    // Legacy & Compatibility (from backend Template.js)
    @SerializedName("categoryIcon")
    @ColumnInfo(name = "categoryIcon")
    private String categoryIcon;
    
    @SerializedName("language")
    @ColumnInfo(name = "language")
    private String language; // Language ID reference
    
    @SerializedName("region")
    @ColumnInfo(name = "region")
    private String region; // Region ID reference
    
    @SerializedName("experimentTag")
    @ColumnInfo(name = "experimentTag")
    private String experimentTag = "";
    
    @SerializedName("performanceLog")
    @ColumnInfo(name = "performanceLog")
    private String performanceLog; // JSON string for nested object with dailyUsage, weeklyUsage, lastUsedAt

    @SerializedName("overlayHtmlTemplate")
    private String overlayHtmlTemplate;

    @SerializedName("overlayCssTemplate")
    private String overlayCssTemplate;

    @SerializedName("overlayJsTemplate")
    private String overlayJsTemplate;

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

        this.createdAt = other.createdAt != null ? other.createdAt : new Date();
        this.creatorId = other.creatorId;
        this.creatorUid = other.creatorUid;
        this.creatorName = other.creatorName;
        this.creatorProfilePhoto = other.creatorProfilePhoto;
        this.generatedByUser = other.generatedByUser;
        this.generatedByUserUid = other.generatedByUserUid;
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
    

    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public long getCreatedAtTimestamp() { 
        return createdAt != null ? createdAt.getTime() : 0;
    }
    
    // Creator Information getters and setters
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    
    public String getCreatorUid() { return creatorUid; }
    public void setCreatorUid(String creatorUid) { this.creatorUid = creatorUid; }
    
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    
    public String getCreatorProfilePhoto() { return creatorProfilePhoto; }
    public void setCreatorProfilePhoto(String creatorProfilePhoto) { this.creatorProfilePhoto = creatorProfilePhoto; }
    
    public String getGeneratedByUser() { return generatedByUser; }
    public void setGeneratedByUser(String generatedByUser) { this.generatedByUser = generatedByUser; }
    
    public String getGeneratedByUserUid() { return generatedByUserUid; }
    public void setGeneratedByUserUid(String generatedByUserUid) { this.generatedByUserUid = generatedByUserUid; }
    
    // Media URLs getters and setters
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Monetization & Access Control getters and setters
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    
    public boolean isPremium() { return isPremium; }
    public void setIsPremium(boolean isPremium) { this.isPremium = isPremium; }
    
    public boolean isFeatured() { return isFeatured; }
    public void setIsFeatured(boolean isFeatured) { this.isFeatured = isFeatured; }
    
    public boolean isTrending() { return isTrending; }
    public void setIsTrending(boolean isTrending) { this.isTrending = isTrending; }
    
    public boolean isFlagged() { return isFlagged; }
    public void setIsFlagged(boolean isFlagged) { this.isFlagged = isFlagged; }
    
    public boolean isLowPerforming() { return isLowPerforming; }
    public void setIsLowPerforming(boolean isLowPerforming) { this.isLowPerforming = isLowPerforming; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = Math.max(0.0, price); }
    
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }

    // Engagement Metrics getters and setters
    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = Math.max(0L, usageCount); }
    
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = Math.max(0L, viewCount); }
    
    public long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(long downloadCount) { this.downloadCount = Math.max(0L, downloadCount); }
    
    public long getReportCount() { return reportCount; }
    public void setReportCount(long reportCount) { this.reportCount = Math.max(0L, reportCount); }
    
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = Math.max(0.0, Math.min(5.0, rating)); }
    
    public long getRatingCount() { return ratingCount; }
    public void setRatingCount(long ratingCount) { this.ratingCount = Math.max(0L, ratingCount); }

    // Weekly Metrics getters and setters
    public long getWeeklyUsageCount() { return weeklyUsageCount; }
    public void setWeeklyUsageCount(long weeklyUsageCount) { this.weeklyUsageCount = Math.max(0L, weeklyUsageCount); }
    
    public long getWeeklyLikes() { return weeklyLikes; }
    public void setWeeklyLikes(long weeklyLikes) { this.weeklyLikes = Math.max(0L, weeklyLikes); }
    
    public long getWeeklyFavorites() { return weeklyFavorites; }
    public void setWeeklyFavorites(long weeklyFavorites) { this.weeklyFavorites = Math.max(0L, weeklyFavorites); }
    
    public long getWeeklyViewCount() { return weeklyViewCount; }
    public void setWeeklyViewCount(long weeklyViewCount) { this.weeklyViewCount = Math.max(0L, weeklyViewCount); }
    
    public long getWeeklySharedCount() { return weeklySharedCount; }
    public void setWeeklySharedCount(long weeklySharedCount) { this.weeklySharedCount = Math.max(0L, weeklySharedCount); }
    
    public long getWeeklyDownloadCount() { return weeklyDownloadCount; }
    public void setWeeklyDownloadCount(long weeklyDownloadCount) { this.weeklyDownloadCount = Math.max(0L, weeklyDownloadCount); }
    
    public long getWeeklyReportCount() { return weeklyReportCount; }
    public void setWeeklyReportCount(long weeklyReportCount) { this.weeklyReportCount = Math.max(0L, weeklyReportCount); }
    
    public Date getWeeklyScoreLastReset() { return weeklyScoreLastReset; }
    public void setWeeklyScoreLastReset(Date weeklyScoreLastReset) { this.weeklyScoreLastReset = weeklyScoreLastReset; }

    // AI Metadata getters and setters
    public boolean isAIGenerated() { return isAIGenerated; }
    public void setIsAIGenerated(boolean isAIGenerated) { this.isAIGenerated = isAIGenerated; }
    
    public String getAiPrompt() { return aiPrompt; }
    public void setAiPrompt(String aiPrompt) { this.aiPrompt = aiPrompt != null ? aiPrompt : ""; }
    
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel != null ? aiModel : ""; }
    
    public String getAiStyle() { return aiStyle; }
    public void setAiStyle(String aiStyle) { this.aiStyle = aiStyle != null ? aiStyle : ""; }
    
    public String getAiGenerationStage() { return aiGenerationStage; }
    public void setAiGenerationStage(String aiGenerationStage) { 
        this.aiGenerationStage = aiGenerationStage != null ? aiGenerationStage : "initial"; 
    }
    
    public String getGenerationMetadata() { return generationMetadata; }
    public void setGenerationMetadata(String generationMetadata) { this.generationMetadata = generationMetadata; }

    // Categorization & Tags getters and setters
    public String getFestivalTag() { return festivalTag; }
    public void setFestivalTag(String festivalTag) { this.festivalTag = festivalTag != null ? festivalTag : ""; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public List<String> getStyleTags() { return styleTags; }
    public void setStyleTags(List<String> styleTags) { this.styleTags = styleTags; }
    
    public List<String> getSearchKeywords() { return searchKeywords; }
    public void setSearchKeywords(List<String> searchKeywords) { this.searchKeywords = searchKeywords; }
    
    public String getVariationOf() { return variationOf; }
    public void setVariationOf(String variationOf) { this.variationOf = variationOf; }
    
    public List<String> getRelatedTemplates() { return relatedTemplates; }
    public void setRelatedTemplates(List<String> relatedTemplates) { this.relatedTemplates = relatedTemplates; }

    // Visibility & Ranking getters and setters
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType != null ? templateType : "html"; }
    
    public double getVisibilityScore() { return visibilityScore; }
    public void setVisibilityScore(double visibilityScore) { this.visibilityScore = Math.max(0.0, visibilityScore); }
    
    public double getBoostPriority() { return boostPriority; }
    public void setBoostPriority(double boostPriority) { this.boostPriority = Math.max(0.0, boostPriority); }
    
    public Date getLastBoostedAt() { return lastBoostedAt; }
    public void setLastBoostedAt(Date lastBoostedAt) { this.lastBoostedAt = lastBoostedAt; }
    
    public List<String> getIgnoredByUsers() { return ignoredByUsers; }
    public void setIgnoredByUsers(List<String> ignoredByUsers) { this.ignoredByUsers = ignoredByUsers; }

    // Customization Options getters and setters
    public String getCustomizationOptions() { return customizationOptions; }
    public void setCustomizationOptions(String customizationOptions) { this.customizationOptions = customizationOptions; }

    // Legacy & Compatibility getters and setters
    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getExperimentTag() { return experimentTag; }
    public void setExperimentTag(String experimentTag) { this.experimentTag = experimentTag != null ? experimentTag : ""; }
    
    public String getPerformanceLog() { return performanceLog; }
    public void setPerformanceLog(String performanceLog) { this.performanceLog = performanceLog; }

    // Getters and setters for overlay templates
    public String getOverlayHtmlTemplate() {
        return overlayHtmlTemplate != null ? overlayHtmlTemplate : "";
    }

    public void setOverlayHtmlTemplate(String overlayHtmlTemplate) {
        this.overlayHtmlTemplate = overlayHtmlTemplate;
    }

    public String getOverlayCssTemplate() {
        return overlayCssTemplate != null ? overlayCssTemplate : "";
    }

    public void setOverlayCssTemplate(String overlayCssTemplate) {
        this.overlayCssTemplate = overlayCssTemplate;
    }

    public String getOverlayJsTemplate() {
        return overlayJsTemplate != null ? overlayJsTemplate : "";
    }

    public void setOverlayJsTemplate(String overlayJsTemplate) {
        this.overlayJsTemplate = overlayJsTemplate;
    }

    public boolean hasValidOverlayTemplates() {
        return !TextUtils.isEmpty(overlayHtmlTemplate) && !TextUtils.isEmpty(overlayCssTemplate);
    }

    // Computed fields (virtual fields from server)
    public double getTrendingScore() {
        return (usageCount * 3 + likeCount * 2 + favoriteCount * 2 + viewCount + shareCount + downloadCount - reportCount * 5);
    }
    
    public double getWeeklyTrendingScore() {
        return (weeklyUsageCount * 3 + weeklyLikes * 2 + weeklyFavorites * 2 + weeklyViewCount + weeklySharedCount + weeklyDownloadCount - weeklyReportCount * 5);
    }
    
    /**
     * Get the display name for the template creator
     * @return Creator name or fallback to "eventwish" if not available
     */
    public String getCreatorDisplayName() {
        if (creatorName != null && !creatorName.trim().isEmpty()) {
            return creatorName.trim();
        }
        return "eventwish";
    }
    
    /**
     * Check if template has creator information
     * @return true if creator UID or ID is available
     */
    public boolean hasCreatorInfo() {
        return (creatorUid != null && !creatorUid.trim().isEmpty()) || 
               (creatorId != null && !creatorId.trim().isEmpty());
    }
    
    /**
     * Get the primary creator identifier (UID preferred, then ID)
     * @return Creator UID if available, otherwise creator ID
     */
    public String getPrimaryCreatorId() {
        if (creatorUid != null && !creatorUid.trim().isEmpty()) {
            return creatorUid;
        }
        return creatorId;
    }

    // Alias methods for backward compatibility
    public String getName() { return getTitle(); }
    public void setName(String name) { setTitle(name); }
    
    public String getCategory() { return getCategoryId(); }
    public void setCategory(String category) { setCategoryId(category); }
    
    public String getThumbnailUrl() { return getPreviewUrl(); }
    public void setThumbnailUrl(String url) { setPreviewUrl(url); }
    
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

               Objects.equals(id, template.id) &&
               Objects.equals(title, template.title) &&
               Objects.equals(categoryId, template.categoryId) &&
               Objects.equals(previewUrl, template.previewUrl) &&
               Objects.equals(lastUpdated, template.lastUpdated) &&
               Objects.equals(htmlContent, template.htmlContent) &&
               Objects.equals(cssContent, template.cssContent) &&
               Objects.equals(jsContent, template.jsContent) &&
               Objects.equals(createdAt, template.createdAt) &&
               Objects.equals(creatorId, template.creatorId) &&
               Objects.equals(creatorName, template.creatorName) &&
               Objects.equals(creatorProfilePhoto, template.creatorProfilePhoto) &&
               Objects.equals(generatedByUser, template.generatedByUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, categoryId, previewUrl, likeCount, favoriteCount, shareCount,
                          isLiked, isFavorited, lastUpdated, likeChanged, favoriteChanged,
                          htmlContent, cssContent, jsContent, createdAt,
                          creatorId, creatorName, creatorProfilePhoto, generatedByUser);
    }

    // Template Type Helper Methods
    
    /**
     * Get template type as enum with proper validation
     * @return TemplateType enum value
     */
    public TemplateType getTemplateTypeEnum() {
        return TemplateType.fromString(this.templateType);
    }
    
    /**
     * Set template type using enum
     * @param templateType TemplateType enum value
     */
    public void setTemplateTypeEnum(TemplateType templateType) {
        this.templateType = templateType != null ? templateType.getValue() : TemplateType.HTML.getValue();
    }
    
    /**
     * Check if template supports interactive editing
     * @return true if template can be edited interactively
     */
    public boolean supportsEditing() {
        return getTemplateTypeEnum().supportsEditing();
    }
    
    /**
     * Check if template requires a media player for rendering
     * @return true if template needs media player
     */
    public boolean requiresMediaPlayer() {
        return getTemplateTypeEnum().requiresMediaPlayer();
    }
    
    /**
     * Check if template is image-based
     * @return true if template is primarily image-based
     */
    public boolean isImageBased() {
        return getTemplateTypeEnum().isImageBased();
    }
    
    /**
     * Get appropriate MIME type for template content
     * @return MIME type string for rendering
     */
    public String getMimeType() {
        return getTemplateTypeEnum().getMimeType();
    }
    
    /**
     * Get the primary content URL based on template type
     * @return URL string for the main content
     */
    public String getPrimaryContentUrl() {
        TemplateType type = getTemplateTypeEnum();
        switch (type) {
            case VIDEO:
                return getVideoUrl() != null ? getVideoUrl() : getPreviewUrl();
            case IMAGE:
                return getImageUrl() != null ? getImageUrl() : getPreviewUrl();
            case HTML:
            default:
                return getPreviewUrl();
        }
    }
    
    /**
     * Check if template has valid content for its type
     * @return true if template has appropriate content
     */
    public boolean hasValidContent() {
        TemplateType type = getTemplateTypeEnum();
        switch (type) {
            case HTML:
                return getHtmlContent() != null && !getHtmlContent().trim().isEmpty();
            case VIDEO:
                return getVideoUrl() != null && !getVideoUrl().trim().isEmpty();
            case IMAGE:
                return (getImageUrl() != null && !getImageUrl().trim().isEmpty()) ||
                       (getPreviewUrl() != null && !getPreviewUrl().trim().isEmpty());
            default:
                return getPreviewUrl() != null && !getPreviewUrl().trim().isEmpty();
        }
    }
    
    /**
     * Get template description based on type and content
     * @return descriptive string for the template
     */
    public String getTypeDescription() {
        TemplateType type = getTemplateTypeEnum();
        String baseDescription = type.getDescription();
        
        if (isAIGenerated()) {
            baseDescription += " (AI Generated)";
        }
        if (isPremium()) {
            baseDescription += " (Premium)";
        }
        if (isFeatured()) {
            baseDescription += " (Featured)";
        }
        
        return baseDescription;
    }
    
    /**
     * Check if template can be rendered in the current context
     * @return true if template can be displayed
     */
    public boolean canRender() {
        return hasValidContent() && 
               getModerationStatus() != null && 
               getModerationStatus().equals("approved") &&
               isStatus(); // Template is active
    }
    
    /**
     * Get interaction capabilities for this template type
     * @return array of supported InteractionType values
     */
    public InteractionType[] getSupportedInteractions() {
        TemplateType type = getTemplateTypeEnum();
        switch (type) {
            case HTML:
                return new InteractionType[]{
                    InteractionType.VIEW, 
                    InteractionType.EDIT, 
                    InteractionType.PREVIEW, 
                    InteractionType.SHARE
                };
            case VIDEO:
                return new InteractionType[]{
                    InteractionType.VIEW, 
                    InteractionType.PREVIEW, 
                    InteractionType.SHARE
                };
            case IMAGE:
            default:
                return new InteractionType[]{
                    InteractionType.VIEW, 
                    InteractionType.PREVIEW, 
                    InteractionType.SHARE
                };
        }
    }
    
    /**
     * Check if specific interaction is supported
     * @param interaction InteractionType to check
     * @return true if interaction is supported
     */
    public boolean supportsInteraction(InteractionType interaction) {
        InteractionType[] supported = getSupportedInteractions();
        for (InteractionType supportedType : supported) {
            if (supportedType == interaction) {
                return true;
            }
        }
        return false;
    }
}
