package com.ds.eventwish.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.db.Converters;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity(tableName = "ad_units")
@TypeConverters(Converters.class)
public class AdUnitEntity {
    @PrimaryKey
    @NonNull
    private String id;
    
    private String adName;
    private String adType;
    private String adUnitCode;
    private int status;  // Using int for boolean (0/1)
    private Map<String, Object> targetingCriteria;
    private List<String> targetSegments;
    private int targetingPriority;
    private Map<String, String> parameters;
    
    // Analytics fields
    private int impressions;
    private int clicks;
    private double ctr;
    private double revenue;

    // Display settings
    private int maxImpressionsPerDay;
    private int minIntervalBetweenAds;
    private int cooldownPeriod;

    // Device specific fields
    private boolean canShow;
    private String reason;
    private String nextAvailable;
    private Date lastShown;
    private int impressionsToday;
    private Date cooldownUntil;

    // Timestamps
    private Date createdAt;
    private Date updatedAt;

    // Default constructor for Room
    public AdUnitEntity() {
    }

    // Constructor with required fields
    @Ignore
    public AdUnitEntity(@NonNull String id, String adName, String adType, String adUnitCode, 
                       int status, int targetingPriority) {
        this.id = id;
        this.adName = adName;
        this.adType = adType;
        this.adUnitCode = adUnitCode;
        this.status = status;
        this.targetingPriority = targetingPriority;
        this.canShow = true;
        this.impressionsToday = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getAdUnitCode() {
        return adUnitCode;
    }

    public void setAdUnitCode(String adUnitCode) {
        this.adUnitCode = adUnitCode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, Object> getTargetingCriteria() {
        return targetingCriteria;
    }

    public void setTargetingCriteria(Map<String, Object> targetingCriteria) {
        this.targetingCriteria = targetingCriteria;
    }

    public List<String> getTargetSegments() {
        return targetSegments;
    }

    public void setTargetSegments(List<String> targetSegments) {
        this.targetSegments = targetSegments;
    }

    public int getTargetingPriority() {
        return targetingPriority;
    }

    public void setTargetingPriority(int targetingPriority) {
        this.targetingPriority = targetingPriority;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public int getImpressions() {
        return impressions;
    }

    public void setImpressions(int impressions) {
        this.impressions = impressions;
    }

    public int getClicks() {
        return clicks;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public double getCtr() {
        return ctr;
    }

    public void setCtr(double ctr) {
        this.ctr = ctr;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public int getMaxImpressionsPerDay() {
        return maxImpressionsPerDay;
    }

    public void setMaxImpressionsPerDay(int maxImpressionsPerDay) {
        this.maxImpressionsPerDay = maxImpressionsPerDay;
    }

    public int getMinIntervalBetweenAds() {
        return minIntervalBetweenAds;
    }

    public void setMinIntervalBetweenAds(int minIntervalBetweenAds) {
        this.minIntervalBetweenAds = minIntervalBetweenAds;
    }

    public int getCooldownPeriod() {
        return cooldownPeriod;
    }

    public void setCooldownPeriod(int cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
    }

    public boolean isCanShow() {
        return canShow;
    }

    public void setCanShow(boolean canShow) {
        this.canShow = canShow;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNextAvailable() {
        return nextAvailable;
    }

    public void setNextAvailable(String nextAvailable) {
        this.nextAvailable = nextAvailable;
    }

    public Date getLastShown() {
        return lastShown;
    }

    public void setLastShown(Date lastShown) {
        this.lastShown = lastShown;
    }

    public int getImpressionsToday() {
        return impressionsToday;
    }

    public void setImpressionsToday(int impressionsToday) {
        this.impressionsToday = impressionsToday;
    }

    public Date getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(Date cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAdUnitId() {
        return adUnitCode;
    }

    public void setAdUnitId(String adUnitId) {
        this.adUnitCode = adUnitId;
    }

    public void setPriority(int priority) {
        this.targetingPriority = priority;
    }

    @Override
    public String toString() {
        return "AdUnitEntity{" +
                "id='" + id + '\'' +
                ", adName='" + adName + '\'' +
                ", adType='" + adType + '\'' +
                ", adUnitCode='" + adUnitCode + '\'' +
                ", status=" + status +
                ", targetingPriority=" + targetingPriority +
                ", impressions=" + impressions +
                ", clicks=" + clicks +
                ", ctr=" + ctr +
                ", revenue=" + revenue +
                ", canShow=" + canShow +
                ", impressionsToday=" + impressionsToday +
                (reason != null ? ", reason='" + reason + '\'' : "") +
                (nextAvailable != null ? ", nextAvailable='" + nextAvailable + '\'' : "") +
                (lastShown != null ? ", lastShown=" + lastShown : "") +
                (cooldownUntil != null ? ", cooldownUntil=" + cooldownUntil : "") +
                '}';
    }
} 