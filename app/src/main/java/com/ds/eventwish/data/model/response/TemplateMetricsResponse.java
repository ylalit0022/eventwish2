package com.ds.eventwish.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Response model for Template metrics and analytics operations
 * Used for engagement tracking, performance analytics, and trending data
 */
public class TemplateMetricsResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("templateId")
    private String templateId;
    
    @SerializedName("period")
    private String period;
    
    @SerializedName("timestamp")
    private Date timestamp;
    
    // Engagement Metrics
    @SerializedName("engagementMetrics")
    private EngagementMetrics engagementMetrics;
    
    // Performance Data
    @SerializedName("performanceData")
    private PerformanceData performanceData;
    
    // Trending Information
    @SerializedName("trendingData")
    private TrendingData trendingData;
    
    // Analytics Summary
    @SerializedName("analyticsSummary")
    private Map<String, Object> analyticsSummary;
    
    // Export Information
    @SerializedName("exportInfo")
    private ExportInfo exportInfo;
    
    // Nested Classes for structured data
    public static class EngagementMetrics {
        @SerializedName("usageCount")
        private int usageCount;
        
        @SerializedName("viewCount")
        private int viewCount;
        
        @SerializedName("downloadCount")
        private int downloadCount;
        
        @SerializedName("shareCount")
        private int shareCount;
        
        @SerializedName("likeCount")
        private int likeCount;
        
        @SerializedName("favoriteCount")
        private int favoriteCount;
        
        @SerializedName("reportCount")
        private int reportCount;
        
        @SerializedName("rating")
        private float rating;
        
        @SerializedName("ratingCount")
        private int ratingCount;
        
        @SerializedName("engagementRate")
        private float engagementRate;
        
        @SerializedName("conversionRate")
        private float conversionRate;
        
        // Getters and Setters
        public int getUsageCount() { return usageCount; }
        public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
        
        public int getViewCount() { return viewCount; }
        public void setViewCount(int viewCount) { this.viewCount = viewCount; }
        
        public int getDownloadCount() { return downloadCount; }
        public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
        
        public int getShareCount() { return shareCount; }
        public void setShareCount(int shareCount) { this.shareCount = shareCount; }
        
        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
        
        public int getFavoriteCount() { return favoriteCount; }
        public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
        
        public int getReportCount() { return reportCount; }
        public void setReportCount(int reportCount) { this.reportCount = reportCount; }
        
        public float getRating() { return rating; }
        public void setRating(float rating) { this.rating = rating; }
        
        public int getRatingCount() { return ratingCount; }
        public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
        
        public float getEngagementRate() { return engagementRate; }
        public void setEngagementRate(float engagementRate) { this.engagementRate = engagementRate; }
        
        public float getConversionRate() { return conversionRate; }
        public void setConversionRate(float conversionRate) { this.conversionRate = conversionRate; }
    }
    
    public static class PerformanceData {
        @SerializedName("loadTime")
        private long loadTime;
        
        @SerializedName("renderTime")
        private long renderTime;
        
        @SerializedName("errorRate")
        private float errorRate;
        
        @SerializedName("successRate")
        private float successRate;
        
        @SerializedName("averageSessionDuration")
        private long averageSessionDuration;
        
        @SerializedName("bounceRate")
        private float bounceRate;
        
        @SerializedName("performanceScore")
        private float performanceScore;
        
        // Getters and Setters
        public long getLoadTime() { return loadTime; }
        public void setLoadTime(long loadTime) { this.loadTime = loadTime; }
        
        public long getRenderTime() { return renderTime; }
        public void setRenderTime(long renderTime) { this.renderTime = renderTime; }
        
        public float getErrorRate() { return errorRate; }
        public void setErrorRate(float errorRate) { this.errorRate = errorRate; }
        
        public float getSuccessRate() { return successRate; }
        public void setSuccessRate(float successRate) { this.successRate = successRate; }
        
        public long getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(long averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }
        
        public float getBounceRate() { return bounceRate; }
        public void setBounceRate(float bounceRate) { this.bounceRate = bounceRate; }
        
        public float getPerformanceScore() { return performanceScore; }
        public void setPerformanceScore(float performanceScore) { this.performanceScore = performanceScore; }
    }
    
    public static class TrendingData {
        @SerializedName("trendingScore")
        private float trendingScore;
        
        @SerializedName("weeklyTrendingScore")
        private float weeklyTrendingScore;
        
        @SerializedName("trendDirection")
        private String trendDirection; // "up", "down", "stable"
        
        @SerializedName("rankingPosition")
        private int rankingPosition;
        
        @SerializedName("categoryRanking")
        private int categoryRanking;
        
        @SerializedName("velocityScore")
        private float velocityScore;
        
        @SerializedName("momentumScore")
        private float momentumScore;
        
        // Getters and Setters
        public float getTrendingScore() { return trendingScore; }
        public void setTrendingScore(float trendingScore) { this.trendingScore = trendingScore; }
        
        public float getWeeklyTrendingScore() { return weeklyTrendingScore; }
        public void setWeeklyTrendingScore(float weeklyTrendingScore) { this.weeklyTrendingScore = weeklyTrendingScore; }
        
        public String getTrendDirection() { return trendDirection; }
        public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
        
        public int getRankingPosition() { return rankingPosition; }
        public void setRankingPosition(int rankingPosition) { this.rankingPosition = rankingPosition; }
        
        public int getCategoryRanking() { return categoryRanking; }
        public void setCategoryRanking(int categoryRanking) { this.categoryRanking = categoryRanking; }
        
        public float getVelocityScore() { return velocityScore; }
        public void setVelocityScore(float velocityScore) { this.velocityScore = velocityScore; }
        
        public float getMomentumScore() { return momentumScore; }
        public void setMomentumScore(float momentumScore) { this.momentumScore = momentumScore; }
    }
    
    public static class ExportInfo {
        @SerializedName("format")
        private String format;
        
        @SerializedName("downloadUrl")
        private String downloadUrl;
        
        @SerializedName("fileSize")
        private long fileSize;
        
        @SerializedName("expiresAt")
        private Date expiresAt;
        
        @SerializedName("recordCount")
        private int recordCount;
        
        // Getters and Setters
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public Date getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
        
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    }
    
    // Main class getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    public EngagementMetrics getEngagementMetrics() { return engagementMetrics; }
    public void setEngagementMetrics(EngagementMetrics engagementMetrics) { this.engagementMetrics = engagementMetrics; }
    
    public PerformanceData getPerformanceData() { return performanceData; }
    public void setPerformanceData(PerformanceData performanceData) { this.performanceData = performanceData; }
    
    public TrendingData getTrendingData() { return trendingData; }
    public void setTrendingData(TrendingData trendingData) { this.trendingData = trendingData; }
    
    public Map<String, Object> getAnalyticsSummary() { return analyticsSummary; }
    public void setAnalyticsSummary(Map<String, Object> analyticsSummary) { this.analyticsSummary = analyticsSummary; }
    
    public ExportInfo getExportInfo() { return exportInfo; }
    public void setExportInfo(ExportInfo exportInfo) { this.exportInfo = exportInfo; }
} 