package com.ds.eventwish.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Response model for Template AI operations
 * Used for AI generation, metadata management, and user AI interactions
 */
public class TemplateAIResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("templateId")
    private String templateId;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("timestamp")
    private Date timestamp;
    
    // AI Generation Data
    @SerializedName("generationData")
    private AIGenerationData generationData;
    
    // AI Metadata
    @SerializedName("aiMetadata")
    private AIMetadata aiMetadata;
    
    // User AI Usage
    @SerializedName("userAIUsage")
    private UserAIUsage userAIUsage;
    
    // AI Analytics
    @SerializedName("aiAnalytics")
    private Map<String, Object> aiAnalytics;
    
    // Generation Queue Info
    @SerializedName("queueInfo")
    private GenerationQueueInfo queueInfo;
    
    // Nested Classes for structured data
    public static class AIGenerationData {
        @SerializedName("prompt")
        private String prompt;
        
        @SerializedName("model")
        private String model;
        
        @SerializedName("style")
        private String style;
        
        @SerializedName("stage")
        private String stage;
        
        @SerializedName("version")
        private String version;
        
        @SerializedName("parameters")
        private Map<String, Object> parameters;
        
        @SerializedName("generatedContent")
        private GeneratedContent generatedContent;
        
        @SerializedName("quality")
        private float quality;
        
        @SerializedName("processingTime")
        private long processingTime;
        
        @SerializedName("confidence")
        private float confidence;
        
        @SerializedName("alternatives")
        private List<GeneratedContent> alternatives;
        
        // Nested class for generated content
        public static class GeneratedContent {
            @SerializedName("htmlContent")
            private String htmlContent;
            
            @SerializedName("cssContent")
            private String cssContent;
            
            @SerializedName("jsContent")
            private String jsContent;
            
            @SerializedName("previewUrl")
            private String previewUrl;
            
            @SerializedName("title")
            private String title;
            
            @SerializedName("description")
            private String description;
            
            @SerializedName("tags")
            private List<String> tags;
            
            // Getters and Setters
            public String getHtmlContent() { return htmlContent; }
            public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
            
            public String getCssContent() { return cssContent; }
            public void setCssContent(String cssContent) { this.cssContent = cssContent; }
            
            public String getJsContent() { return jsContent; }
            public void setJsContent(String jsContent) { this.jsContent = jsContent; }
            
            public String getPreviewUrl() { return previewUrl; }
            public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
            
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public List<String> getTags() { return tags; }
            public void setTags(List<String> tags) { this.tags = tags; }
        }
        
        // Getters and Setters for AIGenerationData
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
        
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        
        public GeneratedContent getGeneratedContent() { return generatedContent; }
        public void setGeneratedContent(GeneratedContent generatedContent) { this.generatedContent = generatedContent; }
        
        public float getQuality() { return quality; }
        public void setQuality(float quality) { this.quality = quality; }
        
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        
        public List<GeneratedContent> getAlternatives() { return alternatives; }
        public void setAlternatives(List<GeneratedContent> alternatives) { this.alternatives = alternatives; }
    }
    
    public static class AIMetadata {
        @SerializedName("isAIGenerated")
        private boolean isAIGenerated;
        
        @SerializedName("aiProvider")
        private String aiProvider;
        
        @SerializedName("trainingData")
        private String trainingData;
        
        @SerializedName("bias")
        private String bias;
        
        @SerializedName("limitations")
        private List<String> limitations;
        
        @SerializedName("capabilities")
        private List<String> capabilities;
        
        @SerializedName("lastUpdated")
        private Date lastUpdated;
        
        @SerializedName("accuracy")
        private float accuracy;
        
        @SerializedName("reliability")
        private float reliability;
        
        // Getters and Setters
        public boolean isAIGenerated() { return isAIGenerated; }
        public void setAIGenerated(boolean AIGenerated) { isAIGenerated = AIGenerated; }
        
        public String getAiProvider() { return aiProvider; }
        public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
        
        public String getTrainingData() { return trainingData; }
        public void setTrainingData(String trainingData) { this.trainingData = trainingData; }
        
        public String getBias() { return bias; }
        public void setBias(String bias) { this.bias = bias; }
        
        public List<String> getLimitations() { return limitations; }
        public void setLimitations(List<String> limitations) { this.limitations = limitations; }
        
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
        
        public Date getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public float getAccuracy() { return accuracy; }
        public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
        
        public float getReliability() { return reliability; }
        public void setReliability(float reliability) { this.reliability = reliability; }
    }
    
    public static class UserAIUsage {
        @SerializedName("totalGenerations")
        private int totalGenerations;
        
        @SerializedName("monthlyGenerations")
        private int monthlyGenerations;
        
        @SerializedName("dailyGenerations")
        private int dailyGenerations;
        
        @SerializedName("remainingCredits")
        private int remainingCredits;
        
        @SerializedName("subscriptionTier")
        private String subscriptionTier;
        
        @SerializedName("lastGenerationAt")
        private Date lastGenerationAt;
        
        @SerializedName("averageQuality")
        private float averageQuality;
        
        @SerializedName("preferredModels")
        private List<String> preferredModels;
        
        @SerializedName("usageHistory")
        private List<UsageRecord> usageHistory;
        
        public static class UsageRecord {
            @SerializedName("date")
            private Date date;
            
            @SerializedName("count")
            private int count;
            
            @SerializedName("model")
            private String model;
            
            @SerializedName("quality")
            private float quality;
            
            // Getters and Setters
            public Date getDate() { return date; }
            public void setDate(Date date) { this.date = date; }
            
            public int getCount() { return count; }
            public void setCount(int count) { this.count = count; }
            
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            
            public float getQuality() { return quality; }
            public void setQuality(float quality) { this.quality = quality; }
        }
        
        // Getters and Setters for UserAIUsage
        public int getTotalGenerations() { return totalGenerations; }
        public void setTotalGenerations(int totalGenerations) { this.totalGenerations = totalGenerations; }
        
        public int getMonthlyGenerations() { return monthlyGenerations; }
        public void setMonthlyGenerations(int monthlyGenerations) { this.monthlyGenerations = monthlyGenerations; }
        
        public int getDailyGenerations() { return dailyGenerations; }
        public void setDailyGenerations(int dailyGenerations) { this.dailyGenerations = dailyGenerations; }
        
        public int getRemainingCredits() { return remainingCredits; }
        public void setRemainingCredits(int remainingCredits) { this.remainingCredits = remainingCredits; }
        
        public String getSubscriptionTier() { return subscriptionTier; }
        public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }
        
        public Date getLastGenerationAt() { return lastGenerationAt; }
        public void setLastGenerationAt(Date lastGenerationAt) { this.lastGenerationAt = lastGenerationAt; }
        
        public float getAverageQuality() { return averageQuality; }
        public void setAverageQuality(float averageQuality) { this.averageQuality = averageQuality; }
        
        public List<String> getPreferredModels() { return preferredModels; }
        public void setPreferredModels(List<String> preferredModels) { this.preferredModels = preferredModels; }
        
        public List<UsageRecord> getUsageHistory() { return usageHistory; }
        public void setUsageHistory(List<UsageRecord> usageHistory) { this.usageHistory = usageHistory; }
    }
    
    public static class GenerationQueueInfo {
        @SerializedName("queuePosition")
        private int queuePosition;
        
        @SerializedName("estimatedWaitTime")
        private long estimatedWaitTime;
        
        @SerializedName("queueId")
        private String queueId;
        
        @SerializedName("priority")
        private String priority;
        
        @SerializedName("status")
        private String status; // "queued", "processing", "completed", "failed"
        
        @SerializedName("progress")
        private float progress; // 0.0 to 1.0
        
        // Getters and Setters
        public int getQueuePosition() { return queuePosition; }
        public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }
        
        public long getEstimatedWaitTime() { return estimatedWaitTime; }
        public void setEstimatedWaitTime(long estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }
        
        public String getQueueId() { return queueId; }
        public void setQueueId(String queueId) { this.queueId = queueId; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public float getProgress() { return progress; }
        public void setProgress(float progress) { this.progress = progress; }
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
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    public AIGenerationData getGenerationData() { return generationData; }
    public void setGenerationData(AIGenerationData generationData) { this.generationData = generationData; }
    
    public AIMetadata getAiMetadata() { return aiMetadata; }
    public void setAiMetadata(AIMetadata aiMetadata) { this.aiMetadata = aiMetadata; }
    
    public UserAIUsage getUserAIUsage() { return userAIUsage; }
    public void setUserAIUsage(UserAIUsage userAIUsage) { this.userAIUsage = userAIUsage; }
    
    public Map<String, Object> getAiAnalytics() { return aiAnalytics; }
    public void setAiAnalytics(Map<String, Object> aiAnalytics) { this.aiAnalytics = aiAnalytics; }
    
    public GenerationQueueInfo getQueueInfo() { return queueInfo; }
    public void setQueueInfo(GenerationQueueInfo queueInfo) { this.queueInfo = queueInfo; }
} 