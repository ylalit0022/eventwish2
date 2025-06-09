package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for user preferences including notification settings and template interactions
 */
public class UserPreferences {
    @DocumentId
    private String userId;
    
    @PropertyName("name")
    private String name;
    
    @PropertyName("email")
    private String email;
    
    @PropertyName("fcm_token")
    private String fcmToken;
    
    @PropertyName("is_online")
    private boolean isOnline;
    
    @PropertyName("last_login")
    private Timestamp lastLogin;
    
    @PropertyName("notification_preferences")
    private Map<String, NotificationPreference> notificationPreferences;
    
    @PropertyName("favorite_templates")
    private Map<String, Boolean> favoriteTemplates;
    
    @PropertyName("liked_templates")
    private Map<String, Boolean> likedTemplates;
    
    @PropertyName("last_updated")
    private Timestamp lastUpdated;

    // Required empty constructor for Firestore
    public UserPreferences() {
        notificationPreferences = new HashMap<>();
        favoriteTemplates = new HashMap<>();
        likedTemplates = new HashMap<>();
        this.lastUpdated = Timestamp.now();
        this.lastLogin = Timestamp.now();
        this.isOnline = false;
    }

    public UserPreferences(String userId, String name, String email) {
        this();
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    @NonNull
    public String getUserId() {
        return userId != null ? userId : "";
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Nullable
    @PropertyName("fcm_token")
    public String getFcmToken() {
        return fcmToken;
    }

    @PropertyName("fcm_token")
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @PropertyName("is_online")
    public boolean isOnline() {
        return isOnline;
    }

    @PropertyName("is_online")
    public void setOnline(boolean online) {
        isOnline = online;
    }

    @PropertyName("last_login")
    public Timestamp getLastLogin() {
        return lastLogin;
    }

    @PropertyName("last_login")
    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    @PropertyName("notification_preferences")
    public Map<String, NotificationPreference> getNotificationPreferences() {
        return notificationPreferences;
    }

    @PropertyName("notification_preferences")
    public void setNotificationPreferences(Map<String, NotificationPreference> notificationPreferences) {
        this.notificationPreferences = notificationPreferences != null ? notificationPreferences : new HashMap<>();
    }

    @PropertyName("favorite_templates")
    public Map<String, Boolean> getFavoriteTemplates() {
        return favoriteTemplates;
    }

    @PropertyName("favorite_templates")
    public void setFavoriteTemplates(Map<String, Boolean> favoriteTemplates) {
        this.favoriteTemplates = favoriteTemplates != null ? favoriteTemplates : new HashMap<>();
    }

    @PropertyName("liked_templates")
    public Map<String, Boolean> getLikedTemplates() {
        return likedTemplates;
    }

    @PropertyName("liked_templates")
    public void setLikedTemplates(Map<String, Boolean> likedTemplates) {
        this.likedTemplates = likedTemplates != null ? likedTemplates : new HashMap<>();
    }

    @PropertyName("last_updated")
    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    @PropertyName("last_updated")
    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void addFavoriteTemplate(String templateId) {
        if (favoriteTemplates == null) {
            favoriteTemplates = new HashMap<>();
        }
        favoriteTemplates.put(templateId, true);
        this.lastUpdated = Timestamp.now();
    }

    public void removeFavoriteTemplate(String templateId) {
        if (favoriteTemplates != null) {
            favoriteTemplates.remove(templateId);
            this.lastUpdated = Timestamp.now();
        }
    }

    public boolean isTemplateFavorited(String templateId) {
        return favoriteTemplates != null && Boolean.TRUE.equals(favoriteTemplates.get(templateId));
    }

    public void addLikedTemplate(String templateId) {
        if (likedTemplates == null) {
            likedTemplates = new HashMap<>();
        }
        likedTemplates.put(templateId, true);
        this.lastUpdated = Timestamp.now();
    }

    public void removeLikedTemplate(String templateId) {
        if (likedTemplates != null) {
            likedTemplates.remove(templateId);
            this.lastUpdated = Timestamp.now();
        }
    }

    public boolean isTemplateLiked(String templateId) {
        return likedTemplates != null && Boolean.TRUE.equals(likedTemplates.get(templateId));
    }

    public void setNotificationPreference(String type, NotificationPreference preference) {
        if (notificationPreferences == null) {
            notificationPreferences = new HashMap<>();
        }
        notificationPreferences.put(type, preference);
        this.lastUpdated = Timestamp.now();
    }

    @Nullable
    public NotificationPreference getNotificationPreference(String type) {
        return notificationPreferences != null ? notificationPreferences.get(type) : null;
    }
} 