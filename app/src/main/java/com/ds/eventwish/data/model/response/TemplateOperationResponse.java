package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.Template;
import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * General response model for Template operations
 * Used for CRUD operations, validation, content management, and monetization
 */
public class TemplateOperationResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("timestamp")
    private Date timestamp;
    
    // Template Data
    @SerializedName("template")
    private Template template;
    
    @SerializedName("templates")
    private List<Template> templates;
    
    // Operation Information
    @SerializedName("operation")
    private String operation; // "create", "update", "delete", "validate", etc.
    
    @SerializedName("operationId")
    private String operationId;
    
    // Validation Results
    @SerializedName("validationResults")
    private ValidationResults validationResults;
    
    // Content Information
    @SerializedName("contentInfo")
    private ContentInfo contentInfo;
    
    // Monetization Data
    @SerializedName("monetizationData")
    private MonetizationData monetizationData;
    
    // Pagination (for list operations)
    @SerializedName("pagination")
    private PaginationInfo pagination;
    
    // Additional metadata
    @SerializedName("metadata")
    private Map<String, Object> metadata;
    
    // Nested Classes for structured data
    public static class ValidationResults {
        @SerializedName("isValid")
        private boolean isValid;
        
        @SerializedName("errors")
        private List<ValidationError> errors;
        
        @SerializedName("warnings")
        private List<String> warnings;
        
        @SerializedName("suggestions")
        private List<String> suggestions;
        
        @SerializedName("score")
        private float score; // Overall validation score 0-100
        
        public static class ValidationError {
            @SerializedName("field")
            private String field;
            
            @SerializedName("code")
            private String code;
            
            @SerializedName("message")
            private String message;
            
            @SerializedName("severity")
            private String severity; // "error", "warning", "info"
            
            @SerializedName("value")
            private Object value;
            
            // Getters and Setters
            public String getField() { return field; }
            public void setField(String field) { this.field = field; }
            
            public String getCode() { return code; }
            public void setCode(String code) { this.code = code; }
            
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
            
            public String getSeverity() { return severity; }
            public void setSeverity(String severity) { this.severity = severity; }
            
            public Object getValue() { return value; }
            public void setValue(Object value) { this.value = value; }
        }
        
        // Getters and Setters for ValidationResults
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        
        public List<ValidationError> getErrors() { return errors; }
        public void setErrors(List<ValidationError> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        
        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }
    }
    
    public static class ContentInfo {
        @SerializedName("contentSize")
        private long contentSize;
        
        @SerializedName("htmlSize")
        private long htmlSize;
        
        @SerializedName("cssSize")
        private long cssSize;
        
        @SerializedName("jsSize")
        private long jsSize;
        
        @SerializedName("previewGenerated")
        private boolean previewGenerated;
        
        @SerializedName("previewUrl")
        private String previewUrl;
        
        @SerializedName("mediaFiles")
        private List<MediaFile> mediaFiles;
        
        @SerializedName("dependencies")
        private List<String> dependencies;
        
        @SerializedName("compatibility")
        private Map<String, Boolean> compatibility;
        
        public static class MediaFile {
            @SerializedName("type")
            private String type; // "image", "video", "audio", "font"
            
            @SerializedName("url")
            private String url;
            
            @SerializedName("size")
            private long size;
            
            @SerializedName("format")
            private String format;
            
            @SerializedName("dimensions")
            private String dimensions;
            
            // Getters and Setters
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            
            public long getSize() { return size; }
            public void setSize(long size) { this.size = size; }
            
            public String getFormat() { return format; }
            public void setFormat(String format) { this.format = format; }
            
            public String getDimensions() { return dimensions; }
            public void setDimensions(String dimensions) { this.dimensions = dimensions; }
        }
        
        // Getters and Setters for ContentInfo
        public long getContentSize() { return contentSize; }
        public void setContentSize(long contentSize) { this.contentSize = contentSize; }
        
        public long getHtmlSize() { return htmlSize; }
        public void setHtmlSize(long htmlSize) { this.htmlSize = htmlSize; }
        
        public long getCssSize() { return cssSize; }
        public void setCssSize(long cssSize) { this.cssSize = cssSize; }
        
        public long getJsSize() { return jsSize; }
        public void setJsSize(long jsSize) { this.jsSize = jsSize; }
        
        public boolean isPreviewGenerated() { return previewGenerated; }
        public void setPreviewGenerated(boolean previewGenerated) { this.previewGenerated = previewGenerated; }
        
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        
        public List<MediaFile> getMediaFiles() { return mediaFiles; }
        public void setMediaFiles(List<MediaFile> mediaFiles) { this.mediaFiles = mediaFiles; }
        
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        
        public Map<String, Boolean> getCompatibility() { return compatibility; }
        public void setCompatibility(Map<String, Boolean> compatibility) { this.compatibility = compatibility; }
    }
    
    public static class MonetizationData {
        @SerializedName("isPremium")
        private boolean isPremium;
        
        @SerializedName("price")
        private float price;
        
        @SerializedName("currency")
        private String currency;
        
        @SerializedName("pricingTier")
        private String pricingTier;
        
        @SerializedName("revenue")
        private float revenue;
        
        @SerializedName("purchaseCount")
        private int purchaseCount;
        
        @SerializedName("conversionRate")
        private float conversionRate;
        
        @SerializedName("pricingHistory")
        private List<PriceChange> pricingHistory;
        
        @SerializedName("revenueProjection")
        private Map<String, Float> revenueProjection;
        
        public static class PriceChange {
            @SerializedName("changedAt")
            private Date changedAt;
            
            @SerializedName("oldPrice")
            private float oldPrice;
            
            @SerializedName("newPrice")
            private float newPrice;
            
            @SerializedName("reason")
            private String reason;
            
            // Getters and Setters
            public Date getChangedAt() { return changedAt; }
            public void setChangedAt(Date changedAt) { this.changedAt = changedAt; }
            
            public float getOldPrice() { return oldPrice; }
            public void setOldPrice(float oldPrice) { this.oldPrice = oldPrice; }
            
            public float getNewPrice() { return newPrice; }
            public void setNewPrice(float newPrice) { this.newPrice = newPrice; }
            
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
        
        // Getters and Setters for MonetizationData
        public boolean isPremium() { return isPremium; }
        public void setPremium(boolean premium) { isPremium = premium; }
        
        public float getPrice() { return price; }
        public void setPrice(float price) { this.price = price; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getPricingTier() { return pricingTier; }
        public void setPricingTier(String pricingTier) { this.pricingTier = pricingTier; }
        
        public float getRevenue() { return revenue; }
        public void setRevenue(float revenue) { this.revenue = revenue; }
        
        public int getPurchaseCount() { return purchaseCount; }
        public void setPurchaseCount(int purchaseCount) { this.purchaseCount = purchaseCount; }
        
        public float getConversionRate() { return conversionRate; }
        public void setConversionRate(float conversionRate) { this.conversionRate = conversionRate; }
        
        public List<PriceChange> getPricingHistory() { return pricingHistory; }
        public void setPricingHistory(List<PriceChange> pricingHistory) { this.pricingHistory = pricingHistory; }
        
        public Map<String, Float> getRevenueProjection() { return revenueProjection; }
        public void setRevenueProjection(Map<String, Float> revenueProjection) { this.revenueProjection = revenueProjection; }
    }
    
    public static class PaginationInfo {
        @SerializedName("page")
        private int page;
        
        @SerializedName("size")
        private int size;
        
        @SerializedName("totalPages")
        private int totalPages;
        
        @SerializedName("totalElements")
        private long totalElements;
        
        @SerializedName("hasNext")
        private boolean hasNext;
        
        @SerializedName("hasPrevious")
        private boolean hasPrevious;
        
        @SerializedName("isFirst")
        private boolean isFirst;
        
        @SerializedName("isLast")
        private boolean isLast;
        
        // Getters and Setters
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
        
        public boolean isFirst() { return isFirst; }
        public void setFirst(boolean first) { isFirst = first; }
        
        public boolean isLast() { return isLast; }
        public void setLast(boolean last) { isLast = last; }
    }
    
    // Main class getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }
    
    public List<Template> getTemplates() { return templates; }
    public void setTemplates(List<Template> templates) { this.templates = templates; }
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    
    public ValidationResults getValidationResults() { return validationResults; }
    public void setValidationResults(ValidationResults validationResults) { this.validationResults = validationResults; }
    
    public ContentInfo getContentInfo() { return contentInfo; }
    public void setContentInfo(ContentInfo contentInfo) { this.contentInfo = contentInfo; }
    
    public MonetizationData getMonetizationData() { return monetizationData; }
    public void setMonetizationData(MonetizationData monetizationData) { this.monetizationData = monetizationData; }
    
    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
} 