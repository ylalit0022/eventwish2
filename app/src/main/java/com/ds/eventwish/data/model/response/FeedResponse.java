package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.Template;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model for multi-section feed API
 * Used for personalized feed with dynamic sections
 * Handles both direct response and wrapped response formats
 */
public class FeedResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private FeedData data;
    
    @SerializedName("sections")
    private List<FeedSection> sections;
    
    @SerializedName("metadata")
    private FeedMetadata metadata;
    
    @SerializedName("timestamp")
    private String timestamp;

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<FeedSection> getSections() { 
        // Handle both direct response and wrapped response formats
        if (sections != null) {
            return sections;
        } else if (data != null && data.getSections() != null) {
            return data.getSections();
        } else {
            return new ArrayList<>();
        }
    }
    public FeedMetadata getMetadata() { 
        // Handle both direct response and wrapped response formats
        if (metadata != null) {
            return metadata;
        } else if (data != null) {
            return data.getMetadata();
        } else {
            return null;
        }
    }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setSections(List<FeedSection> sections) { this.sections = sections; }
    public void setMetadata(FeedMetadata metadata) { this.metadata = metadata; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    /**
     * Feed Data - represents the actual feed data when wrapped in API response
     */
    public static class FeedData {
        @SerializedName("sections")
        private List<FeedSection> sections;
        
        @SerializedName("metadata")
        private FeedMetadata metadata;
        
        @SerializedName("timestamp")
        private String timestamp;

        // Getters
        public List<FeedSection> getSections() { return sections != null ? sections : new ArrayList<>(); }
        public FeedMetadata getMetadata() { return metadata; }
        public String getTimestamp() { return timestamp; }

        // Setters
        public void setSections(List<FeedSection> sections) { this.sections = sections; }
        public void setMetadata(FeedMetadata metadata) { this.metadata = metadata; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Feed Section - represents one section in the multi-section feed
     */
    public static class FeedSection {
        @SerializedName("type")
        private String type; // "personalized", "trending", "fresh", "category", "serendipity"
        
        @SerializedName("title")
        private String title; // Display title for the section
        
        @SerializedName("category")
        private String category; // For category sections
        
        @SerializedName("templates")
        private List<Template> templates;
        
        @SerializedName("maxItems")
        private int maxItems;
        
        @SerializedName("hasMore")
        private boolean hasMore;

        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public List<Template> getTemplates() { return templates != null ? templates : new ArrayList<>(); }
        public int getMaxItems() { return maxItems; }
        public boolean isHasMore() { return hasMore; }

        // Setters
        public void setType(String type) { this.type = type; }
        public void setTitle(String title) { this.title = title; }
        public void setCategory(String category) { this.category = category; }
        public void setTemplates(List<Template> templates) { this.templates = templates; }
        public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
        public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    }

    /**
     * Feed Metadata - contains feed-level information
     */
    public static class FeedMetadata {
        @SerializedName("userId")
        private String userId;
        
        @SerializedName("totalTemplates")
        private int totalTemplates;
        
        @SerializedName("cacheHit")
        private boolean cacheHit;
        
        @SerializedName("generationTime")
        private long generationTime;
        
        @SerializedName("version")
        private String version;

        // Getters
        public String getUserId() { return userId; }
        public int getTotalTemplates() { return totalTemplates; }
        public boolean isCacheHit() { return cacheHit; }
        public long getGenerationTime() { return generationTime; }
        public String getVersion() { return version; }

        // Setters
        public void setUserId(String userId) { this.userId = userId; }
        public void setTotalTemplates(int totalTemplates) { this.totalTemplates = totalTemplates; }
        public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
        public void setGenerationTime(long generationTime) { this.generationTime = generationTime; }
        public void setVersion(String version) { this.version = version; }
    }

    /**
     * Helper method to get all templates from all sections as a flat list
     * @return Combined list of all templates from all sections
     */
    public List<Template> getAllTemplates() {
        List<Template> allTemplates = new ArrayList<>();
        if (sections != null) {
            for (FeedSection section : sections) {
                if (section.getTemplates() != null) {
                    allTemplates.addAll(section.getTemplates());
                }
            }
        }
        return allTemplates;
    }

    /**
     * Helper method to get templates from a specific section type
     * @param sectionType The type of section to get templates from
     * @return List of templates from the specified section type
     */
    public List<Template> getTemplatesFromSection(String sectionType) {
        if (sections != null) {
            for (FeedSection section : sections) {
                if (sectionType.equals(section.getType())) {
                    return section.getTemplates();
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Helper method to get a specific section by type
     * @param sectionType The type of section to get
     * @return The section or null if not found
     */
    public FeedSection getSection(String sectionType) {
        if (sections != null) {
            for (FeedSection section : sections) {
                if (sectionType.equals(section.getType())) {
                    return section;
                }
            }
        }
        return null;
    }
} 