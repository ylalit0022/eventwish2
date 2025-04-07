package com.ds.eventwish.data.model.ads;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Model representing an AdMob ad unit
 */
public class AdUnit {
    @SerializedName("id")
    private String id;
    
    @SerializedName("adName")
    private String adName;
    
    @SerializedName("adUnitCode")
    private String adUnitCode;
    
    @SerializedName("adType")
    private String adType;
    
    @SerializedName("status")
    private boolean status;
    
    @SerializedName("targetingCriteria")
    private Map<String, Object> targetingCriteria;
    
    @SerializedName("targetingPriority")
    private int targetingPriority;
    
    @SerializedName("parameters")
    private Map<String, String> parameters;
    
    @SerializedName("displaySettings")
    private DisplaySettings displaySettings;
    
    @SerializedName("canShow")
    private boolean canShow;
    
    @SerializedName("reason")
    private String reason;
    
    @SerializedName("nextAvailable")
    private String nextAvailable;

    /**
     * Display settings for an ad unit
     */
    public static class DisplaySettings {
        @SerializedName("maxImpressionsPerDay")
        private int maxImpressionsPerDay;
        
        @SerializedName("minIntervalBetweenAds")
        private int minIntervalBetweenAds;
        
        @SerializedName("cooldownPeriod")
        private int cooldownPeriod;

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
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getAdUnitCode() {
        return adUnitCode;
    }

    public void setAdUnitCode(String adUnitCode) {
        this.adUnitCode = adUnitCode;
    }

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Map<String, Object> getTargetingCriteria() {
        return targetingCriteria;
    }

    public void setTargetingCriteria(Map<String, Object> targetingCriteria) {
        this.targetingCriteria = targetingCriteria;
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

    public DisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    public void setDisplaySettings(DisplaySettings displaySettings) {
        this.displaySettings = displaySettings;
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
} 