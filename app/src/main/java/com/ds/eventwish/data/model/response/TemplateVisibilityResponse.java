package com.ds.eventwish.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Response model for Template visibility operations
 * Used for ranking, boosting, visibility management, and type management
 */
public class TemplateVisibilityResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("templateId")
    private String templateId;
    
    @SerializedName("timestamp")
    private Date timestamp;
    
    // Visibility Score Data
    @SerializedName("visibilityData")
    private VisibilityData visibilityData;
    
    // Ranking Information
    @SerializedName("rankingData")
    private RankingData rankingData;
    
    // Boost Information
    @SerializedName("boostData")
    private BoostData boostData;
    
    // Type Management
    @SerializedName("typeData")
    private TypeData typeData;
    
    // Block/Ignore Information
    @SerializedName("blockData")
    private BlockData blockData;
    
    // Export Information
    @SerializedName("exportInfo")
    private ExportInfo exportInfo;
    
    // Nested Classes for structured data
    public static class VisibilityData {
        @SerializedName("visibilityScore")
        private float visibilityScore;
        
        @SerializedName("factors")
        private Map<String, Float> factors;
        
        @SerializedName("recommendations")
        private List<String> recommendations;
        
        @SerializedName("currentStatus")
        private String currentStatus; // "visible", "hidden", "restricted"
        
        @SerializedName("lastCalculated")
        private Date lastCalculated;
        
        @SerializedName("nextUpdate")
        private Date nextUpdate;
        
        @SerializedName("historicalScores")
        private List<ScoreHistory> historicalScores;
        
        public static class ScoreHistory {
            @SerializedName("date")
            private Date date;
            
            @SerializedName("score")
            private float score;
            
            @SerializedName("reason")
            private String reason;
            
            // Getters and Setters
            public Date getDate() { return date; }
            public void setDate(Date date) { this.date = date; }
            
            public float getScore() { return score; }
            public void setScore(float score) { this.score = score; }
            
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
        
        // Getters and Setters for VisibilityData
        public float getVisibilityScore() { return visibilityScore; }
        public void setVisibilityScore(float visibilityScore) { this.visibilityScore = visibilityScore; }
        
        public Map<String, Float> getFactors() { return factors; }
        public void setFactors(Map<String, Float> factors) { this.factors = factors; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        
        public Date getLastCalculated() { return lastCalculated; }
        public void setLastCalculated(Date lastCalculated) { this.lastCalculated = lastCalculated; }
        
        public Date getNextUpdate() { return nextUpdate; }
        public void setNextUpdate(Date nextUpdate) { this.nextUpdate = nextUpdate; }
        
        public List<ScoreHistory> getHistoricalScores() { return historicalScores; }
        public void setHistoricalScores(List<ScoreHistory> historicalScores) { this.historicalScores = historicalScores; }
    }
    
    public static class RankingData {
        @SerializedName("globalRanking")
        private int globalRanking;
        
        @SerializedName("categoryRanking")
        private int categoryRanking;
        
        @SerializedName("trendingRanking")
        private int trendingRanking;
        
        @SerializedName("rankingHistory")
        private List<RankingHistory> rankingHistory;
        
        @SerializedName("competitorAnalysis")
        private Map<String, Object> competitorAnalysis;
        
        @SerializedName("rankingFactors")
        private Map<String, Float> rankingFactors;
        
        public static class RankingHistory {
            @SerializedName("date")
            private Date date;
            
            @SerializedName("globalRank")
            private int globalRank;
            
            @SerializedName("categoryRank")
            private int categoryRank;
            
            @SerializedName("change")
            private int change;
            
            // Getters and Setters
            public Date getDate() { return date; }
            public void setDate(Date date) { this.date = date; }
            
            public int getGlobalRank() { return globalRank; }
            public void setGlobalRank(int globalRank) { this.globalRank = globalRank; }
            
            public int getCategoryRank() { return categoryRank; }
            public void setCategoryRank(int categoryRank) { this.categoryRank = categoryRank; }
            
            public int getChange() { return change; }
            public void setChange(int change) { this.change = change; }
        }
        
        // Getters and Setters for RankingData
        public int getGlobalRanking() { return globalRanking; }
        public void setGlobalRanking(int globalRanking) { this.globalRanking = globalRanking; }
        
        public int getCategoryRanking() { return categoryRanking; }
        public void setCategoryRanking(int categoryRanking) { this.categoryRanking = categoryRanking; }
        
        public int getTrendingRanking() { return trendingRanking; }
        public void setTrendingRanking(int trendingRanking) { this.trendingRanking = trendingRanking; }
        
        public List<RankingHistory> getRankingHistory() { return rankingHistory; }
        public void setRankingHistory(List<RankingHistory> rankingHistory) { this.rankingHistory = rankingHistory; }
        
        public Map<String, Object> getCompetitorAnalysis() { return competitorAnalysis; }
        public void setCompetitorAnalysis(Map<String, Object> competitorAnalysis) { this.competitorAnalysis = competitorAnalysis; }
        
        public Map<String, Float> getRankingFactors() { return rankingFactors; }
        public void setRankingFactors(Map<String, Float> rankingFactors) { this.rankingFactors = rankingFactors; }
    }
    
    public static class BoostData {
        @SerializedName("isCurrentlyBoosted")
        private boolean isCurrentlyBoosted;
        
        @SerializedName("boostPriority")
        private int boostPriority;
        
        @SerializedName("lastBoostedAt")
        private Date lastBoostedAt;
        
        @SerializedName("boostExpiresAt")
        private Date boostExpiresAt;
        
        @SerializedName("boostHistory")
        private List<BoostHistory> boostHistory;
        
        @SerializedName("boostEffectiveness")
        private float boostEffectiveness;
        
        @SerializedName("recommendedBoostDuration")
        private long recommendedBoostDuration;
        
        public static class BoostHistory {
            @SerializedName("boostedAt")
            private Date boostedAt;
            
            @SerializedName("duration")
            private long duration;
            
            @SerializedName("priority")
            private int priority;
            
            @SerializedName("effectiveness")
            private float effectiveness;
            
            @SerializedName("reason")
            private String reason;
            
            // Getters and Setters
            public Date getBoostedAt() { return boostedAt; }
            public void setBoostedAt(Date boostedAt) { this.boostedAt = boostedAt; }
            
            public long getDuration() { return duration; }
            public void setDuration(long duration) { this.duration = duration; }
            
            public int getPriority() { return priority; }
            public void setPriority(int priority) { this.priority = priority; }
            
            public float getEffectiveness() { return effectiveness; }
            public void setEffectiveness(float effectiveness) { this.effectiveness = effectiveness; }
            
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
        
        // Getters and Setters for BoostData
        public boolean isCurrentlyBoosted() { return isCurrentlyBoosted; }
        public void setCurrentlyBoosted(boolean currentlyBoosted) { isCurrentlyBoosted = currentlyBoosted; }
        
        public int getBoostPriority() { return boostPriority; }
        public void setBoostPriority(int boostPriority) { this.boostPriority = boostPriority; }
        
        public Date getLastBoostedAt() { return lastBoostedAt; }
        public void setLastBoostedAt(Date lastBoostedAt) { this.lastBoostedAt = lastBoostedAt; }
        
        public Date getBoostExpiresAt() { return boostExpiresAt; }
        public void setBoostExpiresAt(Date boostExpiresAt) { this.boostExpiresAt = boostExpiresAt; }
        
        public List<BoostHistory> getBoostHistory() { return boostHistory; }
        public void setBoostHistory(List<BoostHistory> boostHistory) { this.boostHistory = boostHistory; }
        
        public float getBoostEffectiveness() { return boostEffectiveness; }
        public void setBoostEffectiveness(float boostEffectiveness) { this.boostEffectiveness = boostEffectiveness; }
        
        public long getRecommendedBoostDuration() { return recommendedBoostDuration; }
        public void setRecommendedBoostDuration(long recommendedBoostDuration) { this.recommendedBoostDuration = recommendedBoostDuration; }
    }
    
    public static class TypeData {
        @SerializedName("templateType")
        private String templateType;
        
        @SerializedName("availableTypes")
        private List<String> availableTypes;
        
        @SerializedName("typeHistory")
        private List<TypeChange> typeHistory;
        
        @SerializedName("typeRecommendations")
        private Map<String, Float> typeRecommendations;
        
        public static class TypeChange {
            @SerializedName("changedAt")
            private Date changedAt;
            
            @SerializedName("fromType")
            private String fromType;
            
            @SerializedName("toType")
            private String toType;
            
            @SerializedName("reason")
            private String reason;
            
            // Getters and Setters
            public Date getChangedAt() { return changedAt; }
            public void setChangedAt(Date changedAt) { this.changedAt = changedAt; }
            
            public String getFromType() { return fromType; }
            public void setFromType(String fromType) { this.fromType = fromType; }
            
            public String getToType() { return toType; }
            public void setToType(String toType) { this.toType = toType; }
            
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
        
        // Getters and Setters for TypeData
        public String getTemplateType() { return templateType; }
        public void setTemplateType(String templateType) { this.templateType = templateType; }
        
        public List<String> getAvailableTypes() { return availableTypes; }
        public void setAvailableTypes(List<String> availableTypes) { this.availableTypes = availableTypes; }
        
        public List<TypeChange> getTypeHistory() { return typeHistory; }
        public void setTypeHistory(List<TypeChange> typeHistory) { this.typeHistory = typeHistory; }
        
        public Map<String, Float> getTypeRecommendations() { return typeRecommendations; }
        public void setTypeRecommendations(Map<String, Float> typeRecommendations) { this.typeRecommendations = typeRecommendations; }
    }
    
    public static class BlockData {
        @SerializedName("isBlocked")
        private boolean isBlocked;
        
        @SerializedName("blockReason")
        private String blockReason;
        
        @SerializedName("blockedAt")
        private Date blockedAt;
        
        @SerializedName("blockedBy")
        private String blockedBy;
        
        @SerializedName("ignoredByUsers")
        private List<String> ignoredByUsers;
        
        @SerializedName("ignoreCount")
        private int ignoreCount;
        
        @SerializedName("blockHistory")
        private List<BlockHistory> blockHistory;
        
        public static class BlockHistory {
            @SerializedName("action")
            private String action; // "block", "unblock", "ignore", "unignore"
            
            @SerializedName("timestamp")
            private Date timestamp;
            
            @SerializedName("reason")
            private String reason;
            
            @SerializedName("userId")
            private String userId;
            
            // Getters and Setters
            public String getAction() { return action; }
            public void setAction(String action) { this.action = action; }
            
            public Date getTimestamp() { return timestamp; }
            public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
            
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
            
            public String getUserId() { return userId; }
            public void setUserId(String userId) { this.userId = userId; }
        }
        
        // Getters and Setters for BlockData
        public boolean isBlocked() { return isBlocked; }
        public void setBlocked(boolean blocked) { isBlocked = blocked; }
        
        public String getBlockReason() { return blockReason; }
        public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
        
        public Date getBlockedAt() { return blockedAt; }
        public void setBlockedAt(Date blockedAt) { this.blockedAt = blockedAt; }
        
        public String getBlockedBy() { return blockedBy; }
        public void setBlockedBy(String blockedBy) { this.blockedBy = blockedBy; }
        
        public List<String> getIgnoredByUsers() { return ignoredByUsers; }
        public void setIgnoredByUsers(List<String> ignoredByUsers) { this.ignoredByUsers = ignoredByUsers; }
        
        public int getIgnoreCount() { return ignoreCount; }
        public void setIgnoreCount(int ignoreCount) { this.ignoreCount = ignoreCount; }
        
        public List<BlockHistory> getBlockHistory() { return blockHistory; }
        public void setBlockHistory(List<BlockHistory> blockHistory) { this.blockHistory = blockHistory; }
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
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    public VisibilityData getVisibilityData() { return visibilityData; }
    public void setVisibilityData(VisibilityData visibilityData) { this.visibilityData = visibilityData; }
    
    public RankingData getRankingData() { return rankingData; }
    public void setRankingData(RankingData rankingData) { this.rankingData = rankingData; }
    
    public BoostData getBoostData() { return boostData; }
    public void setBoostData(BoostData boostData) { this.boostData = boostData; }
    
    public TypeData getTypeData() { return typeData; }
    public void setTypeData(TypeData typeData) { this.typeData = typeData; }
    
    public BlockData getBlockData() { return blockData; }
    public void setBlockData(BlockData blockData) { this.blockData = blockData; }
    
    public ExportInfo getExportInfo() { return exportInfo; }
    public void setExportInfo(ExportInfo exportInfo) { this.exportInfo = exportInfo; }
} 