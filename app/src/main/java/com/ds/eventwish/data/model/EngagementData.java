package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import java.util.Date;
import java.util.UUID;

/**
 * Entity class to store user engagement data for better recommendations
 */
@Entity(tableName = "engagement_data")
public class EngagementData {
    
    // Engagement types
    public static final int TYPE_CATEGORY_VISIT = 1;
    public static final int TYPE_TEMPLATE_VIEW = 2;
    public static final int TYPE_TEMPLATE_USE = 3;
    public static final int TYPE_EXPLICIT_LIKE = 4;
    public static final int TYPE_EXPLICIT_DISLIKE = 5;
    
    // Engagement sources
    public static final String SOURCE_DIRECT = "direct";           // Direct selection
    public static final String SOURCE_RECOMMENDATION = "rec";      // From recommendation
    public static final String SOURCE_SEARCH = "search";           // From search
    public static final String SOURCE_HISTORY = "history";         // From history
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    
    @ColumnInfo(name = "type")
    private int type;
    
    @ColumnInfo(name = "template_id")
    private String templateId;
    
    @ColumnInfo(name = "category")
    private String category;
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    @ColumnInfo(name = "duration_ms")
    private long durationMs;
    
    @ColumnInfo(name = "engagement_score")
    private int engagementScore;
    
    @ColumnInfo(name = "source")
    private String source;
    
    @ColumnInfo(name = "synced")
    private boolean synced;
    
    /**
     * Default constructor required by Room
     */
    public EngagementData() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.synced = false;
    }
    
    /**
     * Constructor for category visit
     * @param category Category name
     * @param source Source of engagement
     */
    @Ignore
    public EngagementData(String category, String source) {
        this();
        this.type = TYPE_CATEGORY_VISIT;
        this.category = category;
        this.source = source;
        this.engagementScore = 1; // Default score
    }
    
    /**
     * Constructor for template view/use
     * @param type Type of engagement (TYPE_TEMPLATE_VIEW or TYPE_TEMPLATE_USE)
     * @param templateId Template ID
     * @param category Category ID
     * @param source Source of engagement
     */
    @Ignore
    public EngagementData(int type, String templateId, String category, String source) {
        this();
        this.type = type;
        this.templateId = templateId;
        this.category = category;
        this.source = source;
        
        // Default engagement score based on type
        if (type == TYPE_TEMPLATE_USE) {
            this.engagementScore = 5; // Higher score for template usage
        } else {
            this.engagementScore = 2; // Lower score for template viewing
        }
    }
    
    /**
     * Detailed constructor for template engagement with duration and score
     * @param type Type of engagement
     * @param templateId Template ID
     * @param category Category ID
     * @param durationMs Engagement duration in milliseconds
     * @param engagementScore Score indicating engagement level
     * @param source Source of engagement
     */
    @Ignore
    public EngagementData(int type, String templateId, String category, 
                          long durationMs, int engagementScore, String source) {
        this();
        this.type = type;
        this.templateId = templateId;
        this.category = category;
        this.durationMs = durationMs;
        this.engagementScore = engagementScore;
        this.source = source;
    }
    
    // Getters and setters
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public int getEngagementScore() {
        return engagementScore;
    }
    
    public void setEngagementScore(int engagementScore) {
        this.engagementScore = engagementScore;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public boolean isSynced() {
        return synced;
    }
    
    public void setSynced(boolean synced) {
        this.synced = synced;
    }
    
    /**
     * Get relative weight of this engagement for recommendations
     * Factors in recency, type, and explicit engagement score
     * @return Calculated weight between 0.0 and 1.0
     */
    public float getRecommendationWeight() {
        // Base score from engagement score (0-5 scale)
        float baseScore = engagementScore / 5.0f;
        
        // Type multiplier (category visits worth less than template use)
        float typeMultiplier = 1.0f;
        switch (type) {
            case TYPE_CATEGORY_VISIT:
                typeMultiplier = 0.7f;
                break;
            case TYPE_TEMPLATE_VIEW:
                typeMultiplier = 1.0f;
                break;
            case TYPE_TEMPLATE_USE:
                typeMultiplier = 1.5f;
                break;
            case TYPE_EXPLICIT_LIKE:
                typeMultiplier = 2.0f;
                break;
            case TYPE_EXPLICIT_DISLIKE:
                typeMultiplier = -1.0f; // Negative score for dislikes
                break;
        }
        
        // Recency decay - events older than 30 days have reduced impact
        long now = System.currentTimeMillis();
        long ageMs = now - timestamp;
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
        float recencyFactor = ageMs < thirtyDaysMs ? 1.0f : (float)(0.5f + (0.5f * thirtyDaysMs / ageMs));
        
        return baseScore * typeMultiplier * recencyFactor;
    }
} 