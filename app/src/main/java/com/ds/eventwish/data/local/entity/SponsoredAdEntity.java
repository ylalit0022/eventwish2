package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.local.converters.DateConverter;
import com.ds.eventwish.data.model.SponsoredAd;

import java.util.Date;

/**
 * Room database entity for sponsored ads.
 * Includes caching information like insertion time and cache expiration.
 */
@Entity(
    tableName = "sponsored_ads",
    indices = {
        @Index(value = {"location"})
    }
)
@TypeConverters({DateConverter.class})
public class SponsoredAdEntity {
    
    @PrimaryKey
    @NonNull
    private String id;
    
    private String imageUrl;
    private String redirectUrl;
    private boolean status;
    private Date startDate;
    private Date endDate;
    private String location;
    private int priority;
    private int clickCount;
    private int impressionCount;
    private String title;
    private String description;
    
    // Cache-specific fields
    private long insertedAt; // Timestamp when this entity was inserted into the cache
    private long expiresAt;  // Timestamp when this cache entry should expire
    private long lastImpressionTime; // Timestamp of the last impression tracking
    
    /**
     * Default constructor required by Room
     */
    public SponsoredAdEntity() {
        // Required empty constructor
    }
    
    /**
     * Create entity from model with cache information
     * @param ad SponsoredAd model
     * @param cacheDurationMs How long the cache should be valid (in milliseconds)
     */
    public SponsoredAdEntity(SponsoredAd ad, long cacheDurationMs) {
        this.id = ad.getId();
        this.imageUrl = ad.getImageUrl();
        this.redirectUrl = ad.getRedirectUrl();
        this.status = ad.isStatus();
        this.startDate = ad.getStartDate();
        this.endDate = ad.getEndDate();
        this.location = ad.getLocation();
        this.priority = ad.getPriority();
        this.clickCount = ad.getClickCount();
        this.impressionCount = ad.getImpressionCount();
        this.title = ad.getTitle();
        this.description = ad.getDescription();
        
        // Set cache metadata
        this.insertedAt = System.currentTimeMillis();
        this.expiresAt = this.insertedAt + cacheDurationMs;
    }
    
    /**
     * Convert entity to model
     * @return SponsoredAd model
     */
    public SponsoredAd toModel() {
        return new SponsoredAd(
            id,
            imageUrl,
            redirectUrl,
            status,
            startDate,
            endDate, 
            location,
            priority,
            clickCount,
            impressionCount,
            title,
            description
        );
    }
    
    /**
     * Check if this cache entry is still valid
     * @return true if the entry hasn't expired, false otherwise
     */
    public boolean isValid() {
        return System.currentTimeMillis() < expiresAt;
    }
    
    // Getters and setters
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    public boolean isStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public Date getStartDate() {
        return startDate;
    }
    
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public int getClickCount() {
        return clickCount;
    }
    
    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }
    
    public int getImpressionCount() {
        return impressionCount;
    }
    
    public void setImpressionCount(int impressionCount) {
        this.impressionCount = impressionCount;
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
    
    public long getInsertedAt() {
        return insertedAt;
    }
    
    public void setInsertedAt(long insertedAt) {
        this.insertedAt = insertedAt;
    }
    
    /**
     * Get the time this entity was last fetched from the network (same as insertedAt)
     * @return Timestamp in milliseconds when this ad was last fetched
     */
    public long getLastFetchTime() {
        return insertedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    /**
     * Get the timestamp of the last impression tracking
     * @return Timestamp in milliseconds when the last impression was tracked
     */
    public long getLastImpressionTime() {
        return lastImpressionTime;
    }
    
    /**
     * Set the timestamp of the last impression tracking
     * @param lastImpressionTime Timestamp in milliseconds
     */
    public void setLastImpressionTime(long lastImpressionTime) {
        this.lastImpressionTime = lastImpressionTime;
    }
} 