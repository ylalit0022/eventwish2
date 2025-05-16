package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Model class for sponsored ads from custom backend API
 */
public class SponsoredAd {
    @SerializedName("id")
    private String id;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("redirect_url")
    private String redirectUrl;
    
    @SerializedName("status")
    private boolean status;
    
    @SerializedName("start_date")
    private Date startDate;
    
    @SerializedName("end_date")
    private Date endDate;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("priority")
    private int priority;
    
    @SerializedName("frequency_cap")
    private int frequencyCap;
    
    @SerializedName("daily_frequency_cap")
    private int dailyFrequencyCap;
    
    @SerializedName("click_count")
    private int clickCount;
    
    @SerializedName("impression_count")
    private int impressionCount;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    // Internal fields for client-side use
    private transient float weightedScore;
    
    @SerializedName("metrics")
    private AdMetrics metrics;
    
    /**
     * Inner class for ad metrics data
     */
    public static class AdMetrics {
        @SerializedName("device_impressions")
        private int deviceImpressions;
        
        @SerializedName("device_daily_impressions")
        private int deviceDailyImpressions;
        
        @SerializedName("remaining_impressions")
        private Integer remainingImpressions;
        
        @SerializedName("remaining_daily_impressions")
        private Integer remainingDailyImpressions;
        
        @SerializedName("is_frequency_capped")
        private boolean isFrequencyCapped;
        
        @SerializedName("is_daily_frequency_capped")
        private boolean isDailyFrequencyCapped;
        
        public int getDeviceImpressions() {
            return deviceImpressions;
        }
        
        public int getDeviceDailyImpressions() {
            return deviceDailyImpressions;
        }
        
        public Integer getRemainingImpressions() {
            return remainingImpressions;
        }
        
        public Integer getRemainingDailyImpressions() {
            return remainingDailyImpressions;
        }
        
        public boolean isFrequencyCapped() {
            return isFrequencyCapped;
        }
        
        public boolean isDailyFrequencyCapped() {
            return isDailyFrequencyCapped;
        }
        
        @Override
        public String toString() {
            return "AdMetrics{" +
                    "deviceImpressions=" + deviceImpressions +
                    ", deviceDailyImpressions=" + deviceDailyImpressions +
                    ", remainingImpressions=" + remainingImpressions +
                    ", remainingDailyImpressions=" + remainingDailyImpressions +
                    ", isFrequencyCapped=" + isFrequencyCapped +
                    ", isDailyFrequencyCapped=" + isDailyFrequencyCapped +
                    '}';
        }
    }
    
    /**
     * Default constructor
     */
    public SponsoredAd() {
        // Required empty constructor for GSON
    }
    
    /**
     * Full constructor
     */
    public SponsoredAd(String id, String imageUrl, String redirectUrl, boolean status,
                     Date startDate, Date endDate, String location, int priority,
                     int frequencyCap, int dailyFrequencyCap,
                     int clickCount, int impressionCount, String title, String description) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.redirectUrl = redirectUrl;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.priority = priority;
        this.frequencyCap = frequencyCap;
        this.dailyFrequencyCap = dailyFrequencyCap;
        this.clickCount = clickCount;
        this.impressionCount = impressionCount;
        this.title = title;
        this.description = description;
    }
    
    // Getters
    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getRedirectUrl() { return redirectUrl; }
    public boolean isStatus() { return status; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public String getLocation() { return location; }
    public int getPriority() { return priority; }
    public int getFrequencyCap() { return frequencyCap; }
    public int getDailyFrequencyCap() { return dailyFrequencyCap; }
    public int getClickCount() { return clickCount; }
    public int getImpressionCount() { return impressionCount; }
    public String getTitle() { return title != null ? title : "Sponsored"; }
    public String getDescription() { return description != null ? description : ""; }
    public AdMetrics getMetrics() { return metrics; }
    
    // Setters (used internally)
    public void setWeightedScore(float weightedScore) {
        this.weightedScore = weightedScore;
    }
    
    public float getWeightedScore() {
        return weightedScore;
    }
    
    /**
     * Check if the ad is capped for the current device
     * @return true if ad should not be shown due to frequency caps
     */
    public boolean isFrequencyCapped() {
        if (metrics == null) {
            return false;
        }
        return metrics.isFrequencyCapped() || metrics.isDailyFrequencyCapped();
    }
    
    @Override
    public String toString() {
        return "SponsoredAd{" +
                "id='" + id + '\'' +
                ", location='" + location + '\'' +
                ", priority=" + priority +
                ", title='" + title + '\'' +
                ", metrics=" + (metrics != null ? metrics.toString() : "null") +
                '}';
    }
} 