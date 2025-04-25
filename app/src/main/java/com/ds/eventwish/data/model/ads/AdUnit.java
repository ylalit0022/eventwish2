package com.ds.eventwish.data.model.ads;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Model class representing an ad unit configuration from the server.
 * Matches the MongoDB schema defined in AdMob.js
 */
public class AdUnit {
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

    @SerializedName("targetSegments")
    private List<String> targetSegments;

    @SerializedName("targetingPriority")
    private int targetingPriority;

    @SerializedName("parameters")
    private Map<String, String> parameters;

    // Analytics fields
    @SerializedName("impressions")
    private int impressions;

    @SerializedName("clicks")
    private int clicks;

    @SerializedName("ctr")
    private double ctr;

    @SerializedName("revenue")
    private double revenue;

    @SerializedName("impressionData")
    private List<ImpressionData> impressionData;

    @SerializedName("clickData")
    private List<ClickData> clickData;

    @SerializedName("revenueData")
    private List<RevenueData> revenueData;

    @SerializedName("segmentPerformance")
    private Map<String, SegmentMetrics> segmentPerformance;

    @SerializedName("displaySettings")
    private DisplaySettings displaySettings;

    @SerializedName("deviceSettings")
    private Map<String, DeviceData> deviceSettings;

    // Response fields for ad availability
    @SerializedName("canShow")
    private boolean canShow;

    @SerializedName("reason")
    private String reason;

    @SerializedName("nextAvailable")
    private Date nextAvailable;

    public static class ImpressionData {
        @SerializedName("timestamp")
        private Date timestamp;

        @SerializedName("context")
        private Map<String, Object> context;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }

    public static class ClickData {
        @SerializedName("timestamp")
        private Date timestamp;

        @SerializedName("context")
        private Map<String, Object> context;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }

    public static class RevenueData {
        @SerializedName("timestamp")
        private Date timestamp;

        @SerializedName("amount")
        private double amount;

        @SerializedName("currency")
        private String currency;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

    public static class SegmentMetrics {
        @SerializedName("impressions")
        private int impressions;

        @SerializedName("clicks")
        private int clicks;

        @SerializedName("ctr")
        private double ctr;

        @SerializedName("revenue")
        private double revenue;

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
    }

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

    public static class DeviceData {
        @SerializedName("lastShown")
        private Date lastShown;

        @SerializedName("impressionsToday")
        private int impressionsToday;

        @SerializedName("cooldownUntil")
        private Date cooldownUntil;

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
    }

    // Constructor
    public AdUnit(String adName, String adType, String adUnitCode, boolean status) {
        this.adName = adName;
        this.adType = adType;
        this.adUnitCode = adUnitCode;
        this.status = status;
    }

    // Getters and Setters
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

    public List<ImpressionData> getImpressionData() {
        return impressionData;
    }

    public void setImpressionData(List<ImpressionData> impressionData) {
        this.impressionData = impressionData;
    }

    public List<ClickData> getClickData() {
        return clickData;
    }

    public void setClickData(List<ClickData> clickData) {
        this.clickData = clickData;
    }

    public List<RevenueData> getRevenueData() {
        return revenueData;
    }

    public void setRevenueData(List<RevenueData> revenueData) {
        this.revenueData = revenueData;
    }

    public Map<String, SegmentMetrics> getSegmentPerformance() {
        return segmentPerformance;
    }

    public void setSegmentPerformance(Map<String, SegmentMetrics> segmentPerformance) {
        this.segmentPerformance = segmentPerformance;
    }

    public DisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    public void setDisplaySettings(DisplaySettings displaySettings) {
        this.displaySettings = displaySettings;
    }

    public Map<String, DeviceData> getDeviceSettings() {
        return deviceSettings;
    }

    public void setDeviceSettings(Map<String, DeviceData> deviceSettings) {
        this.deviceSettings = deviceSettings;
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

    public Date getNextAvailable() {
        return nextAvailable;
    }

    public void setNextAvailable(Date nextAvailable) {
        this.nextAvailable = nextAvailable;
    }

    @Override
    public String toString() {
        return "AdUnit{" +
                "adName='" + adName + '\'' +
                ", adType='" + adType + '\'' +
                ", adUnitCode='" + adUnitCode + '\'' +
                ", status=" + status +
                ", targetingPriority=" + targetingPriority +
                ", impressions=" + impressions +
                ", clicks=" + clicks +
                ", ctr=" + ctr +
                ", revenue=" + revenue +
                ", canShow=" + canShow +
                (reason != null ? ", reason='" + reason + '\'' : "") +
                (nextAvailable != null ? ", nextAvailable='" + nextAvailable + '\'' : "") +
                '}';
    }
} 