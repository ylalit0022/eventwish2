package com.ds.eventwish.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.Timestamp;
import com.google.gson.annotations.SerializedName;

public class User {
    // Core Identity Fields
    private String uid; // Firebase UID
    private String phoneNumber;
    private String deviceId;
    private String deviceModel;
    private String deviceName;
    private String appVersion;
    private String osVersion;
    private long loginTimestamp;
    private String displayName;
    private String email;
    private String profilePhoto;
    
    // Legacy fields (keeping for backward compatibility)
    private int coins;
    private boolean isUnlocked;
    private long unlockExpiry;
    
    // Activity & Status (new fields from backend)
    private long lastOnline;
    private long lastActive;
    private Date lastInactivityNotification;
    private Date created;
    private boolean isBlocked = false;
    private BlockInfo blockInfo;
    
    // Subscription & Payment (enhanced from backend)
    private Subscription subscription;
    private SubscriptionOffer subscriptionOffer;
    private List<SubscriptionHistory> subscriptionHistory = new ArrayList<>();
    private Date lastSubscriptionEndedAt;
    private int monthsWithoutPayment = 0;
    private boolean adsAllowed = true;
    
    // AI Usage (new from backend)
    private AIUsage aiUsage;
    
    // Active sessions (enhanced)
    private Map<String, DeviceSession> activeSessions = new HashMap<>();
    
    // User preferences (enhanced)
    private String preferredTheme = "light";
    private String preferredLanguage = "en";
    private String timezone = "Asia/Kolkata";
    private Date muteNotificationsUntil;
    private PushPreferences pushPreferences = new PushPreferences();
    private List<String> topicSubscriptions = new ArrayList<>();
    private List<FcmToken> fcmTokens = new ArrayList<>();
    
    // Referral info (enhanced)
    private Referral referredBy = new Referral();
    private String referralCode;
    
    // Template interactions (enhanced)
    private List<String> recentTemplatesUsed = new ArrayList<>();
    private List<String> favorites = new ArrayList<>();
    private List<String> likes = new ArrayList<>();
    private String lastActiveTemplate;
    private String lastActionOnTemplate; // VIEW, LIKE, FAV, SHARE, UNLIKE, UNFAV
    
    // Engagement tracking (enhanced)
    private List<EngagementLog> engagementLog = new ArrayList<>();
    private List<TemplateAffinity> templateAffinity = new ArrayList<>();
    private List<CategoryVisit> categories = new ArrayList<>();
    private List<IgnoredTemplate> ignoredTemplates = new ArrayList<>();
    private Map<String, Date> viewedTemplatesMap = new HashMap<>();
    private List<Draft> drafts = new ArrayList<>();
    
    // Feed (new from backend)
    private List<CachedHomeFeed> cachedHomeFeed = new ArrayList<>();
    private Date homeFeedLastGeneratedAt;

    /**
     * Default constructor
     */
    public User() {
        // Required empty constructor
        this.lastActive = System.currentTimeMillis();
        this.lastOnline = System.currentTimeMillis();
        this.created = new Date();
    }

    public User(String phoneNumber) {
        this();
        this.phoneNumber = phoneNumber;
        this.coins = 0;
        this.isUnlocked = false;
        this.unlockExpiry = 0;
    }

    // Nested class for Category Visit (new from backend)
    public static class CategoryVisit {
        private String category;
        private Date visitDate;
        private int visitCount = 1;
        private String source = "direct"; // "direct" or "template"
        
        public CategoryVisit() {}
        
        public CategoryVisit(String category, String source) {
            this.category = category;
            this.visitDate = new Date();
            this.source = source;
        }
        
        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Date getVisitDate() { return visitDate; }
        public void setVisitDate(Date visitDate) { this.visitDate = visitDate; }
        public int getVisitCount() { return visitCount; }
        public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    // Nested class for Block Info (new from backend)
    public static class BlockInfo {
        private String blockedBy; // Admin UID who blocked the user
        private String reason;
        private Date blockedAt;
        private Date blockExpiresAt; // null means indefinite block
        private String notes;
        
        public BlockInfo() {}
        
        // Getters and setters
        public String getBlockedBy() { return blockedBy; }
        public void setBlockedBy(String blockedBy) { this.blockedBy = blockedBy; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Date getBlockedAt() { return blockedAt; }
        public void setBlockedAt(Date blockedAt) { this.blockedAt = blockedAt; }
        public Date getBlockExpiresAt() { return blockExpiresAt; }
        public void setBlockExpiresAt(Date blockExpiresAt) { this.blockExpiresAt = blockExpiresAt; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // Nested class for FCM Token (new from backend)
    public static class FcmToken {
        private String token;
        private String platform = "android"; // "android", "ios", "web"
        private List<String> subscribedTopics = new ArrayList<>();
        private Date updatedAt;
        
        public FcmToken() {}
        
        public FcmToken(String token, String platform) {
            this.token = token;
            this.platform = platform;
            this.updatedAt = new Date();
        }
        
        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public List<String> getSubscribedTopics() { return subscribedTopics; }
        public void setSubscribedTopics(List<String> subscribedTopics) { this.subscribedTopics = subscribedTopics; }
        public Date getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    }

    // Enhanced Subscription class (from backend)
    public static class Subscription {
        private boolean isActive;
        private String plan; // MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY
        private String planLevel; // BASIC, PREMIUM, PRO
        private Date startedAt;
        private Date expiresAt;
        private SubscriptionFeatures features = new SubscriptionFeatures();
        private String appliedPricingRuleId;
        private double basePrice = 0;
        private double finalPrice = 0;
        private String currency = "INR";
        
        public static class SubscriptionFeatures {
            private boolean allowNameEdit = false;
            private boolean allowPhotoEdit = false;
            private boolean allowAdFree = false;
            private boolean allowDraftSave = false;
            private boolean allowAnalytics = false;
            private boolean allowMusic = false;
            private boolean allowThemeCustomization = false;
            private boolean allowPrioritySupport = false;
            
            // Getters and setters
            public boolean isAllowNameEdit() { return allowNameEdit; }
            public void setAllowNameEdit(boolean allowNameEdit) { this.allowNameEdit = allowNameEdit; }
            public boolean isAllowPhotoEdit() { return allowPhotoEdit; }
            public void setAllowPhotoEdit(boolean allowPhotoEdit) { this.allowPhotoEdit = allowPhotoEdit; }
            public boolean isAllowAdFree() { return allowAdFree; }
            public void setAllowAdFree(boolean allowAdFree) { this.allowAdFree = allowAdFree; }
            public boolean isAllowDraftSave() { return allowDraftSave; }
            public void setAllowDraftSave(boolean allowDraftSave) { this.allowDraftSave = allowDraftSave; }
            public boolean isAllowAnalytics() { return allowAnalytics; }
            public void setAllowAnalytics(boolean allowAnalytics) { this.allowAnalytics = allowAnalytics; }
            public boolean isAllowMusic() { return allowMusic; }
            public void setAllowMusic(boolean allowMusic) { this.allowMusic = allowMusic; }
            public boolean isAllowThemeCustomization() { return allowThemeCustomization; }
            public void setAllowThemeCustomization(boolean allowThemeCustomization) { this.allowThemeCustomization = allowThemeCustomization; }
            public boolean isAllowPrioritySupport() { return allowPrioritySupport; }
            public void setAllowPrioritySupport(boolean allowPrioritySupport) { this.allowPrioritySupport = allowPrioritySupport; }
        }
        
        // Existing getters and setters plus new ones
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
        public String getPlanLevel() { return planLevel; }
        public void setPlanLevel(String planLevel) { this.planLevel = planLevel; }
        public Date getStartedAt() { return startedAt; }
        public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
        public Date getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
        public SubscriptionFeatures getFeatures() { return features; }
        public void setFeatures(SubscriptionFeatures features) { this.features = features; }
        public String getAppliedPricingRuleId() { return appliedPricingRuleId; }
        public void setAppliedPricingRuleId(String appliedPricingRuleId) { this.appliedPricingRuleId = appliedPricingRuleId; }
        public double getBasePrice() { return basePrice; }
        public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
        public double getFinalPrice() { return finalPrice; }
        public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    // Subscription Offer (new from backend)
    public static class SubscriptionOffer {
        private String planAssigned; // BASIC, PREMIUM, PRO
        private double originalPrice = 0;
        private double discountedPrice = 0;
        private double discountPercentage = 0;
        private Date offerEndsAt;
        private Date assignedAt;
        private String experimentTag;
        
        // Getters and setters
        public String getPlanAssigned() { return planAssigned; }
        public void setPlanAssigned(String planAssigned) { this.planAssigned = planAssigned; }
        public double getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }
        public double getDiscountedPrice() { return discountedPrice; }
        public void setDiscountedPrice(double discountedPrice) { this.discountedPrice = discountedPrice; }
        public double getDiscountPercentage() { return discountPercentage; }
        public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }
        public Date getOfferEndsAt() { return offerEndsAt; }
        public void setOfferEndsAt(Date offerEndsAt) { this.offerEndsAt = offerEndsAt; }
        public Date getAssignedAt() { return assignedAt; }
        public void setAssignedAt(Date assignedAt) { this.assignedAt = assignedAt; }
        public String getExperimentTag() { return experimentTag; }
        public void setExperimentTag(String experimentTag) { this.experimentTag = experimentTag; }
    }

    // Subscription History (new from backend)
    public static class SubscriptionHistory {
        private String plan; // BASIC, PREMIUM, PRO
        private Date startedAt;
        private Date endedAt;
        
        // Getters and setters
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
        public Date getStartedAt() { return startedAt; }
        public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
        public Date getEndedAt() { return endedAt; }
        public void setEndedAt(Date endedAt) { this.endedAt = endedAt; }
    }

    // AI Usage (new from backend)
    public static class AIUsage {
        private int monthlyGenerationCount = 0;
        private Date lastGenerationAt;
        private int quota = 5;
        private List<String> recentPrompts = new ArrayList<>();
        private List<String> stylePreferences = new ArrayList<>();
        
        // Getters and setters
        public int getMonthlyGenerationCount() { return monthlyGenerationCount; }
        public void setMonthlyGenerationCount(int monthlyGenerationCount) { this.monthlyGenerationCount = monthlyGenerationCount; }
        public Date getLastGenerationAt() { return lastGenerationAt; }
        public void setLastGenerationAt(Date lastGenerationAt) { this.lastGenerationAt = lastGenerationAt; }
        public int getQuota() { return quota; }
        public void setQuota(int quota) { this.quota = quota; }
        public List<String> getRecentPrompts() { return recentPrompts; }
        public void setRecentPrompts(List<String> recentPrompts) { this.recentPrompts = recentPrompts; }
        public List<String> getStylePreferences() { return stylePreferences; }
        public void setStylePreferences(List<String> stylePreferences) { this.stylePreferences = stylePreferences; }
    }

    // Template Affinity (new from backend)
    public static class TemplateAffinity {
        private String tag;
        private double score = 1;
        
        public TemplateAffinity() {}
        
        public TemplateAffinity(String tag, double score) {
            this.tag = tag;
            this.score = score;
        }
        
        // Getters and setters
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }

    // Ignored Template (new from backend)
    public static class IgnoredTemplate {
        private String templateId;
        private int views = 1;
        private Date lastViewed;
        private double lastIgnoredScore = 1;
        
        // Getters and setters
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public int getViews() { return views; }
        public void setViews(int views) { this.views = views; }
        public Date getLastViewed() { return lastViewed; }
        public void setLastViewed(Date lastViewed) { this.lastViewed = lastViewed; }
        public double getLastIgnoredScore() { return lastIgnoredScore; }
        public void setLastIgnoredScore(double lastIgnoredScore) { this.lastIgnoredScore = lastIgnoredScore; }
    }

    // Draft (new from backend)
    public static class Draft {
        private String templateId;
        private String html;
        private Date lastUpdated;
        
        // Getters and setters
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }
        public Date getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    // Cached Home Feed (new from backend)
    public static class CachedHomeFeed {
        private String type;
        private List<String> templateIds = new ArrayList<>();
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<String> getTemplateIds() { return templateIds; }
        public void setTemplateIds(List<String> templateIds) { this.templateIds = templateIds; }
    }
    
    // Nested class for push preferences (existing, keeping as is)
    public static class PushPreferences {
        @SerializedName("allowFestivalPush")
        private boolean allowFestivalPush = true;
        
        @SerializedName("allowPersonalPush")
        private boolean allowPersonalPush = true;
        
        public boolean isAllowFestivalPush() {
            return allowFestivalPush;
        }
        
        public void setAllowFestivalPush(boolean allowFestivalPush) {
            this.allowFestivalPush = allowFestivalPush;
        }
        
        public boolean isAllowPersonalPush() {
            return allowPersonalPush;
        }
        
        public void setAllowPersonalPush(boolean allowPersonalPush) {
            this.allowPersonalPush = allowPersonalPush;
        }
    }
    
    // Nested class for referral (existing, keeping as is)
    public static class Referral {
        private String referredBy;
        private String referralCode;
        
        public String getReferredBy() {
            return referredBy;
        }
        
        public void setReferredBy(String referredBy) {
            this.referredBy = referredBy;
        }
        
        public String getReferralCode() {
            return referralCode;
        }
        
        public void setReferralCode(String referralCode) {
            this.referralCode = referralCode;
        }
    }
    
    // Nested class for engagement log (existing, keeping as is)
    public static class EngagementLog {
        private String action; // SHARE, VIEW, LIKE, FAV, UNLIKE, UNFAV
        private String templateId;
        private Date timestamp;
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getTemplateId() {
            return templateId;
        }
        
        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }
        
        public Date getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    // Enhanced DeviceSession class
    public static class DeviceSession {
        private String deviceId;
        private String deviceModel;
        private String deviceName;
        private String appVersion;
        private String osVersion;
        private long loginTimestamp;
        private long lastActiveTimestamp;
        private boolean isCurrentDevice;
        
        public DeviceSession() {
            // Required empty constructor
        }
        
        public DeviceSession(String deviceId, String deviceModel, String deviceName, 
                           String appVersion, String osVersion) {
            this.deviceId = deviceId;
            this.deviceModel = deviceModel;
            this.deviceName = deviceName;
            this.appVersion = appVersion;
            this.osVersion = osVersion;
            this.loginTimestamp = System.currentTimeMillis();
            this.lastActiveTimestamp = System.currentTimeMillis();
            this.isCurrentDevice = false;
        }
        
        public DeviceSession(String deviceId, String deviceModel, String deviceName, 
                           String appVersion, String osVersion, Date loginTimestamp, 
                           Date lastActiveTimestamp, boolean isCurrentDevice) {
        this.deviceId = deviceId;
            this.deviceModel = deviceModel;
            this.deviceName = deviceName;
            this.appVersion = appVersion;
            this.osVersion = osVersion;
            this.loginTimestamp = loginTimestamp != null ? loginTimestamp.getTime() : System.currentTimeMillis();
            this.lastActiveTimestamp = lastActiveTimestamp != null ? lastActiveTimestamp.getTime() : System.currentTimeMillis();
            this.isCurrentDevice = isCurrentDevice;
        }
        
        // All existing getters and setters remain the same
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getDeviceModel() { return deviceModel; }
        public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public long getLoginTimestamp() { return loginTimestamp; }
        public void setLoginTimestamp(long loginTimestamp) { this.loginTimestamp = loginTimestamp; }
        public void setLoginTimestamp(Date loginTimestamp) { this.loginTimestamp = loginTimestamp != null ? loginTimestamp.getTime() : System.currentTimeMillis(); }
        public long getLastActiveTimestamp() { return lastActiveTimestamp; }
        public void setLastActiveTimestamp(long lastActiveTimestamp) { this.lastActiveTimestamp = lastActiveTimestamp; }
        public void setLastActiveTimestamp(Date lastActiveTimestamp) { this.lastActiveTimestamp = lastActiveTimestamp != null ? lastActiveTimestamp.getTime() : System.currentTimeMillis(); }
        public boolean isCurrentDevice() { return isCurrentDevice; }
        public void setCurrentDevice(boolean currentDevice) { this.isCurrentDevice = currentDevice; }
    }
    
    // Core getters and setters (existing ones preserved)
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public long getLoginTimestamp() { return loginTimestamp; }
    public void setLoginTimestamp(long loginTimestamp) { this.loginTimestamp = loginTimestamp; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
    
    // Legacy fields (keeping for backward compatibility)
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    public long getUnlockExpiry() { return unlockExpiry; }
    public void setUnlockExpiry(long unlockExpiry) { this.unlockExpiry = unlockExpiry; }
    
    // Activity & Status getters and setters (new)
    public long getLastOnline() { return lastOnline; }
    public void setLastOnline(long lastOnline) { this.lastOnline = lastOnline; }
    public long getLastActive() { return lastActive; }
    public void setLastActive(long lastActive) { this.lastActive = lastActive; }
    public Date getLastInactivityNotification() { return lastInactivityNotification; }
    public void setLastInactivityNotification(Date lastInactivityNotification) { this.lastInactivityNotification = lastInactivityNotification; }
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
    public BlockInfo getBlockInfo() { return blockInfo; }
    public void setBlockInfo(BlockInfo blockInfo) { this.blockInfo = blockInfo; }
    
    // Enhanced subscription getters and setters
    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }
    public SubscriptionOffer getSubscriptionOffer() { return subscriptionOffer; }
    public void setSubscriptionOffer(SubscriptionOffer subscriptionOffer) { this.subscriptionOffer = subscriptionOffer; }
    public List<SubscriptionHistory> getSubscriptionHistory() { return subscriptionHistory; }
    public void setSubscriptionHistory(List<SubscriptionHistory> subscriptionHistory) { this.subscriptionHistory = subscriptionHistory; }
    public Date getLastSubscriptionEndedAt() { return lastSubscriptionEndedAt; }
    public void setLastSubscriptionEndedAt(Date lastSubscriptionEndedAt) { this.lastSubscriptionEndedAt = lastSubscriptionEndedAt; }
    public int getMonthsWithoutPayment() { return monthsWithoutPayment; }
    public void setMonthsWithoutPayment(int monthsWithoutPayment) { this.monthsWithoutPayment = monthsWithoutPayment; }
    public boolean isAdsAllowed() { return adsAllowed; }
    public void setAdsAllowed(boolean adsAllowed) { this.adsAllowed = adsAllowed; }
    
    // AI Usage getters and setters (new)
    public AIUsage getAiUsage() { return aiUsage; }
    public void setAiUsage(AIUsage aiUsage) { this.aiUsage = aiUsage; }
    
    // Device sessions (existing)
    public Map<String, DeviceSession> getActiveSessions() { return activeSessions; }
    public void setActiveSessions(Map<String, DeviceSession> activeSessions) { this.activeSessions = activeSessions; }
    public void addDeviceSession(String deviceId, DeviceSession session) {
        if (this.activeSessions == null) {
            this.activeSessions = new HashMap<>();
        }
        this.activeSessions.put(deviceId, session);
    }
    public DeviceSession getDeviceSession(String deviceId) {
        if (this.activeSessions == null) {
            return null;
        }
        return this.activeSessions.get(deviceId);
    }
    public void removeDeviceSession(String deviceId) {
        if (this.activeSessions != null) {
            this.activeSessions.remove(deviceId);
        }
    }
    
    // User preferences (existing and new)
    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Date getMuteNotificationsUntil() { return muteNotificationsUntil; }
    public void setMuteNotificationsUntil(Date muteNotificationsUntil) { this.muteNotificationsUntil = muteNotificationsUntil; }
    public PushPreferences getPushPreferences() { return pushPreferences; }
    public void setPushPreferences(PushPreferences pushPreferences) { this.pushPreferences = pushPreferences; }
    public List<String> getTopicSubscriptions() { return topicSubscriptions; }
    public void setTopicSubscriptions(List<String> topicSubscriptions) { this.topicSubscriptions = topicSubscriptions; }
    public List<FcmToken> getFcmTokens() { return fcmTokens; }
    public void setFcmTokens(List<FcmToken> fcmTokens) { this.fcmTokens = fcmTokens; }
    
    // Referral (existing)
    public Referral getReferredBy() { return referredBy; }
    public void setReferredBy(Referral referredBy) { this.referredBy = referredBy; }
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    
    // Template interactions (existing)
    public List<String> getRecentTemplatesUsed() { return recentTemplatesUsed; }
    public void setRecentTemplatesUsed(List<String> recentTemplatesUsed) { this.recentTemplatesUsed = recentTemplatesUsed; }
    public List<String> getFavorites() { return favorites; }
    public void setFavorites(List<String> favorites) { this.favorites = favorites; }
    public List<String> getLikes() { return likes; }
    public void setLikes(List<String> likes) { this.likes = likes; }
    public String getLastActiveTemplate() { return lastActiveTemplate; }
    public void setLastActiveTemplate(String lastActiveTemplate) { this.lastActiveTemplate = lastActiveTemplate; }
    public String getLastActionOnTemplate() { return lastActionOnTemplate; }
    public void setLastActionOnTemplate(String lastActionOnTemplate) { this.lastActionOnTemplate = lastActionOnTemplate; }
    
    // Enhanced engagement tracking getters and setters
    public List<EngagementLog> getEngagementLog() { return engagementLog; }
    public void setEngagementLog(List<EngagementLog> engagementLog) { this.engagementLog = engagementLog; }
    public List<TemplateAffinity> getTemplateAffinity() { return templateAffinity; }
    public void setTemplateAffinity(List<TemplateAffinity> templateAffinity) { this.templateAffinity = templateAffinity; }
    public List<CategoryVisit> getCategories() { return categories; }
    public void setCategories(List<CategoryVisit> categories) { this.categories = categories; }
    public List<IgnoredTemplate> getIgnoredTemplates() { return ignoredTemplates; }
    public void setIgnoredTemplates(List<IgnoredTemplate> ignoredTemplates) { this.ignoredTemplates = ignoredTemplates; }
    public Map<String, Date> getViewedTemplatesMap() { return viewedTemplatesMap; }
    public void setViewedTemplatesMap(Map<String, Date> viewedTemplatesMap) { this.viewedTemplatesMap = viewedTemplatesMap; }
    public List<Draft> getDrafts() { return drafts; }
    public void setDrafts(List<Draft> drafts) { this.drafts = drafts; }
    
    // Feed getters and setters (new)
    public List<CachedHomeFeed> getCachedHomeFeed() { return cachedHomeFeed; }
    public void setCachedHomeFeed(List<CachedHomeFeed> cachedHomeFeed) { this.cachedHomeFeed = cachedHomeFeed; }
    public Date getHomeFeedLastGeneratedAt() { return homeFeedLastGeneratedAt; }
    public void setHomeFeedLastGeneratedAt(Date homeFeedLastGeneratedAt) { this.homeFeedLastGeneratedAt = homeFeedLastGeneratedAt; }
    
    // Utility methods (existing ones preserved)
    public void addFavorite(String templateId) {
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        if (!favorites.contains(templateId)) {
            favorites.add(templateId);
        }
    }
    
    public void removeFavorite(String templateId) {
        if (favorites != null) {
            favorites.remove(templateId);
        }
    }
    
    public boolean isFavorite(String templateId) {
        return favorites != null && favorites.contains(templateId);
    }
    
    public void addLike(String templateId) {
        if (likes == null) {
            likes = new ArrayList<>();
        }
        if (!likes.contains(templateId)) {
            likes.add(templateId);
        }
    }
    
    public void removeLike(String templateId) {
        if (likes != null) {
            likes.remove(templateId);
        }
    }
    
    public boolean isLiked(String templateId) {
        return likes != null && likes.contains(templateId);
    }
    
    public void recordTemplateView(String templateId) {
        if (recentTemplatesUsed == null) {
            recentTemplatesUsed = new ArrayList<>();
        }
        
        // Remove if already exists
        recentTemplatesUsed.remove(templateId);
        
        // Add to beginning
        recentTemplatesUsed.add(0, templateId);
        
        // Keep only 10 most recent
        if (recentTemplatesUsed.size() > 10) {
            recentTemplatesUsed = recentTemplatesUsed.subList(0, 10);
        }
        
        // Update last active template and action
        lastActiveTemplate = templateId;
        lastActionOnTemplate = "VIEW";
        
        // Add to engagement log
        if (engagementLog == null) {
            engagementLog = new ArrayList<>();
        }
        
        EngagementLog log = new EngagementLog();
        log.setAction("VIEW");
        log.setTemplateId(templateId);
        log.setTimestamp(new Date());
        engagementLog.add(log);
    }
    
    public void recordTemplateShare(String templateId) {
        // Update last active template and action
        lastActiveTemplate = templateId;
        lastActionOnTemplate = "SHARE";
        
        // Add to engagement log
        if (engagementLog == null) {
            engagementLog = new ArrayList<>();
        }
        
        EngagementLog log = new EngagementLog();
        log.setAction("SHARE");
        log.setTemplateId(templateId);
        log.setTimestamp(new Date());
        engagementLog.add(log);
    }

    // New utility methods based on backend functionality
    public void visitCategory(String categoryName, String source) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        
        // Check if category already exists
        CategoryVisit existingCategory = null;
        for (CategoryVisit category : categories) {
            if (category.getCategory().toLowerCase().equals(categoryName.toLowerCase())) {
                existingCategory = category;
                break;
            }
        }
        
        if (existingCategory != null) {
            // Update existing category
            existingCategory.setVisitDate(new Date());
            existingCategory.setVisitCount(existingCategory.getVisitCount() + 1);
            existingCategory.setSource(source);
        } else {
            // Add new category
            categories.add(new CategoryVisit(categoryName, source));
        }
        
        // Update last active time
        this.lastActive = System.currentTimeMillis();
        this.lastOnline = System.currentTimeMillis();
    }
    
    public void addFcmToken(String token, String platform) {
        if (fcmTokens == null) {
            fcmTokens = new ArrayList<>();
        }
        
        // Check if token already exists
        FcmToken existingToken = null;
        for (FcmToken fcmToken : fcmTokens) {
            if (fcmToken.getToken().equals(token)) {
                existingToken = fcmToken;
                break;
            }
        }
        
        if (existingToken != null) {
            // Update existing token
            existingToken.setPlatform(platform);
            existingToken.setUpdatedAt(new Date());
        } else {
            // Add new token
            fcmTokens.add(new FcmToken(token, platform));
        }
    }
    
    public void removeFcmToken(String token) {
        if (fcmTokens != null) {
            fcmTokens.removeIf(fcmToken -> fcmToken.getToken().equals(token));
        }
    }
    
    public void updateTemplateAffinity(String tag, double score) {
        if (templateAffinity == null) {
            templateAffinity = new ArrayList<>();
        }
        
        // Check if tag already exists
        TemplateAffinity existingAffinity = null;
        for (TemplateAffinity affinity : templateAffinity) {
            if (affinity.getTag().equals(tag)) {
                existingAffinity = affinity;
                break;
            }
        }
        
        if (existingAffinity != null) {
            // Update existing affinity
            existingAffinity.setScore(existingAffinity.getScore() + score);
        } else {
            // Add new affinity
            templateAffinity.add(new TemplateAffinity(tag, score));
        }
    }
    
    public void saveDraft(String templateId, String html) {
        if (drafts == null) {
            drafts = new ArrayList<>();
        }
        
        // Check if draft already exists
        Draft existingDraft = null;
        for (Draft draft : drafts) {
            if (draft.getTemplateId().equals(templateId)) {
                existingDraft = draft;
                break;
            }
        }
        
        if (existingDraft != null) {
            // Update existing draft
            existingDraft.setHtml(html);
            existingDraft.setLastUpdated(new Date());
        } else {
            // Add new draft
            Draft newDraft = new Draft();
            newDraft.setTemplateId(templateId);
            newDraft.setHtml(html);
            newDraft.setLastUpdated(new Date());
            drafts.add(newDraft);
        }
    }
    
    public void deleteDraft(String templateId) {
        if (drafts != null) {
            drafts.removeIf(draft -> draft.getTemplateId().equals(templateId));
        }
    }
    
    public boolean isCurrentlyBlocked() {
        if (!isBlocked) return false;
        
        // If block has an expiration time and that time has passed, user is no longer blocked
        if (blockInfo != null && blockInfo.getBlockExpiresAt() != null && 
            new Date().after(blockInfo.getBlockExpiresAt())) {
            // Auto-unblock
            isBlocked = false;
            return false;
        }
        
        return true;
    }
}
