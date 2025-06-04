package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Model class for sponsored ads from custom backend API
 */
public class SponsoredAd {
    @SerializedName("_id")
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
    
    @SerializedName("click_count")
    private int clickCount;
    
    @SerializedName("impression_count")
    private int impressionCount;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
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
                     int clickCount, int impressionCount, String title, String description) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.redirectUrl = redirectUrl;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.priority = priority;
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
    public int getClickCount() { return clickCount; }
    public int getImpressionCount() { return impressionCount; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public void setStatus(boolean status) { this.status = status; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public void setLocation(String location) { this.location = location; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
    public void setImpressionCount(int impressionCount) { this.impressionCount = impressionCount; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return "SponsoredAd{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", priority=" + priority +
                ", clicks=" + clickCount +
                ", impressions=" + impressionCount +
                '}';
    }
} 