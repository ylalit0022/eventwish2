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
    
    @PropertyName("fcm_token")
    private String fcmToken;
    
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
    }

    public UserPreferences(String userId, String name) {
        this();
        this.userId = userId;
        this.name = name;
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

    @Nullable
    @PropertyName("fcm_token")
    public String getFcmToken() {
        return fcmToken;
    }

    @PropertyName("fcm_token")
    public void setFcmToken(@Nullable String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @NonNull
    @PropertyName("notification_preferences")
    public Map<String, NotificationPreference> getNotificationPreferences() {
        return notificationPreferences != null ? notificationPreferences : new HashMap<>();
    }

    @PropertyName("notification_preferences")
    public void setNotificationPreferences(@Nullable Map<String, NotificationPreference> preferences) {
        this.notificationPreferences = preferences != null ? preferences : new HashMap<>();
    }

    @NonNull
    @PropertyName("favorite_templates")
    public Map<String, Boolean> getFavoriteTemplates() {
        return favoriteTemplates != null ? favoriteTemplates : new HashMap<>();
    }

    @PropertyName("favorite_templates")
    public void setFavoriteTemplates(@Nullable Map<String, Boolean> favorites) {
        this.favoriteTemplates = favorites != null ? favorites : new HashMap<>();
    }

    @NonNull
    @PropertyName("liked_templates")
    public Map<String, Boolean> getLikedTemplates() {
        return likedTemplates != null ? likedTemplates : new HashMap<>();
    }

    @PropertyName("liked_templates")
    public void setLikedTemplates(@Nullable Map<String, Boolean> likes) {
        this.likedTemplates = likes != null ? likes : new HashMap<>();
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

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 