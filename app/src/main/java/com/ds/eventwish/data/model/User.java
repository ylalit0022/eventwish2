package com.ds.eventwish.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.Timestamp;
import com.google.gson.annotations.SerializedName;

public class User {
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
    private int coins;
    private long lastActive;
    private boolean isUnlocked;
    private long unlockExpiry;
    
    // Active sessions
    private Map<String, DeviceSession> activeSessions = new HashMap<>();
    
    // Subscription details
    private Subscription subscription;
    private boolean adsAllowed = true;
    
    // User preferences
    private String preferredTheme = "light";
    private String preferredLanguage = "en";
    private String timezone = "Asia/Kolkata";
    private Date muteNotificationsUntil;
    private PushPreferences pushPreferences = new PushPreferences();
    private List<String> topicSubscriptions = new ArrayList<>();
    
    // Referral info
    private Referral referredBy = new Referral();
    private String referralCode;
    
    // Template interactions
    private List<String> recentTemplatesUsed = new ArrayList<>();
    private List<String> favorites = new ArrayList<>();
    private List<String> likes = new ArrayList<>();
    private String lastActiveTemplate;
    private String lastActionOnTemplate; // VIEW, LIKE, FAV, SHARE
    
    // Engagement tracking
    private List<EngagementLog> engagementLog = new ArrayList<>();

    /**
     * Default constructor
     */
    public User() {
        // Required empty constructor
        this.lastActive = System.currentTimeMillis();
    }

    public User(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.coins = 0;
        this.lastActive = System.currentTimeMillis();
        this.isUnlocked = false;
        this.unlockExpiry = 0;
    }

    // Nested class for subscription
    public static class Subscription {
        private boolean isActive;
        private String plan; // MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY
        private Date startedAt;
        private Date expiresAt;
        
        public boolean isActive() {
            return isActive;
        }
        
        public void setActive(boolean active) {
            isActive = active;
        }
        
        public String getPlan() {
            return plan;
        }
        
        public void setPlan(String plan) {
            this.plan = plan;
        }
        
        public Date getStartedAt() {
            return startedAt;
        }
        
        public void setStartedAt(Date startedAt) {
            this.startedAt = startedAt;
        }
        
        public Date getExpiresAt() {
            return expiresAt;
        }
        
        public void setExpiresAt(Date expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
    
    // Nested class for push preferences
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
    
    // Nested class for referral
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
    
    // Nested class for engagement log
    public static class EngagementLog {
        private String action; // SHARE, VIEW, LIKE, FAV
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
    
    // Nested class for device session
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
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
        
        public String getDeviceModel() {
            return deviceModel;
        }
        
        public void setDeviceModel(String deviceModel) {
            this.deviceModel = deviceModel;
        }
        
        public String getDeviceName() {
            return deviceName;
        }
        
        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }
        
        public String getAppVersion() {
            return appVersion;
        }
        
        public void setAppVersion(String appVersion) {
            this.appVersion = appVersion;
        }
        
        public String getOsVersion() {
            return osVersion;
        }
        
        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }
        
        public long getLoginTimestamp() {
            return loginTimestamp;
        }
        
        public void setLoginTimestamp(long loginTimestamp) {
            this.loginTimestamp = loginTimestamp;
        }
        
        public void setLoginTimestamp(Date loginTimestamp) {
            this.loginTimestamp = loginTimestamp != null ? loginTimestamp.getTime() : System.currentTimeMillis();
        }
        
        public long getLastActiveTimestamp() {
            return lastActiveTimestamp;
        }
        
        public void setLastActiveTimestamp(long lastActiveTimestamp) {
            this.lastActiveTimestamp = lastActiveTimestamp;
        }
        
        public void setLastActiveTimestamp(Date lastActiveTimestamp) {
            this.lastActiveTimestamp = lastActiveTimestamp != null ? lastActiveTimestamp.getTime() : System.currentTimeMillis();
        }
        
        public boolean isCurrentDevice() {
            return isCurrentDevice;
        }
        
        public void setCurrentDevice(boolean currentDevice) {
            this.isCurrentDevice = currentDevice;
        }
    }
    
    // Getters and setters
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceModel() {
        return deviceModel;
    }
    
    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
    
    public long getLoginTimestamp() {
        return loginTimestamp;
    }
    
    public void setLoginTimestamp(long loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
    }
    
    public Map<String, DeviceSession> getActiveSessions() {
        return activeSessions;
    }
    
    public void setActiveSessions(Map<String, DeviceSession> activeSessions) {
        this.activeSessions = activeSessions;
    }
    
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
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getProfilePhoto() {
        return profilePhoto;
    }
    
    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
    
    public int getCoins() {
        return coins;
    }
    
    public void setCoins(int coins) {
        this.coins = coins;
    }
    
    public long getLastActive() {
        return lastActive;
    }
    
    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }
    
    public boolean isUnlocked() {
        return isUnlocked;
    }
    
    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
    
    public long getUnlockExpiry() {
        return unlockExpiry;
    }
    
    public void setUnlockExpiry(long unlockExpiry) {
        this.unlockExpiry = unlockExpiry;
    }
    
    public Subscription getSubscription() {
        return subscription;
    }
    
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
    
    public boolean isAdsAllowed() {
        return adsAllowed;
    }
    
    public void setAdsAllowed(boolean adsAllowed) {
        this.adsAllowed = adsAllowed;
    }
    
    public String getPreferredTheme() {
        return preferredTheme;
    }
    
    public void setPreferredTheme(String preferredTheme) {
        this.preferredTheme = preferredTheme;
    }
    
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
    
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public Date getMuteNotificationsUntil() {
        return muteNotificationsUntil;
    }
    
    public void setMuteNotificationsUntil(Date muteNotificationsUntil) {
        this.muteNotificationsUntil = muteNotificationsUntil;
    }
    
    public PushPreferences getPushPreferences() {
        return pushPreferences;
    }
    
    public void setPushPreferences(PushPreferences pushPreferences) {
        this.pushPreferences = pushPreferences;
    }
    
    public List<String> getTopicSubscriptions() {
        return topicSubscriptions;
    }
    
    public void setTopicSubscriptions(List<String> topicSubscriptions) {
        this.topicSubscriptions = topicSubscriptions;
    }
    
    public Referral getReferredBy() {
        return referredBy;
    }
    
    public void setReferredBy(Referral referredBy) {
        this.referredBy = referredBy;
    }
    
    public String getReferralCode() {
        return referralCode;
    }
    
    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }
    
    public List<String> getRecentTemplatesUsed() {
        return recentTemplatesUsed;
    }
    
    public void setRecentTemplatesUsed(List<String> recentTemplatesUsed) {
        this.recentTemplatesUsed = recentTemplatesUsed;
    }
    
    public List<String> getFavorites() {
        return favorites;
    }
    
    public void setFavorites(List<String> favorites) {
        this.favorites = favorites;
    }
    
    public List<String> getLikes() {
        return likes;
    }
    
    public void setLikes(List<String> likes) {
        this.likes = likes;
    }
    
    public String getLastActiveTemplate() {
        return lastActiveTemplate;
    }
    
    public void setLastActiveTemplate(String lastActiveTemplate) {
        this.lastActiveTemplate = lastActiveTemplate;
    }
    
    public String getLastActionOnTemplate() {
        return lastActionOnTemplate;
    }
    
    public void setLastActionOnTemplate(String lastActionOnTemplate) {
        this.lastActionOnTemplate = lastActionOnTemplate;
    }
    
    public List<EngagementLog> getEngagementLog() {
        return engagementLog;
    }
    
    public void setEngagementLog(List<EngagementLog> engagementLog) {
        this.engagementLog = engagementLog;
    }
    
    /**
     * Add a template to favorites
     * @param templateId ID of the template to favorite
     */
    public void addFavorite(String templateId) {
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        
        if (!favorites.contains(templateId)) {
            favorites.add(templateId);
        }
    }
    
    /**
     * Remove a template from favorites
     * @param templateId ID of the template to unfavorite
     */
    public void removeFavorite(String templateId) {
        if (favorites != null) {
            favorites.remove(templateId);
        }
    }
    
    /**
     * Check if a template is favorited
     * @param templateId ID of the template to check
     * @return true if template is favorited, false otherwise
     */
    public boolean isFavorite(String templateId) {
        return favorites != null && favorites.contains(templateId);
    }
    
    /**
     * Add a template to likes
     * @param templateId ID of the template to like
     */
    public void addLike(String templateId) {
        if (likes == null) {
            likes = new ArrayList<>();
        }
        
        if (!likes.contains(templateId)) {
            likes.add(templateId);
        }
    }
    
    /**
     * Remove a template from likes
     * @param templateId ID of the template to unlike
     */
    public void removeLike(String templateId) {
        if (likes != null) {
            likes.remove(templateId);
        }
    }
    
    /**
     * Check if a template is liked
     * @param templateId ID of the template to check
     * @return true if template is liked, false otherwise
     */
    public boolean isLiked(String templateId) {
        return likes != null && likes.contains(templateId);
    }
    
    /**
     * Record template view in recent templates
     * @param templateId ID of the template viewed
     */
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
    
    /**
     * Record template share
     * @param templateId ID of the template shared
     */
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
}
