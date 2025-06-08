package com.ds.eventwish.data.converter;

import android.util.Log;

import com.ds.eventwish.data.model.NotificationPreference;
import com.ds.eventwish.data.model.UserPreferences;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Converter class to handle conversion between Firestore documents and UserPreferences objects
 */
public class UserPreferencesConverter {
    private static final String TAG = "UserPrefsConverter";

    /**
     * Convert a Firestore document to a UserPreferences object
     */
    public static Task<UserPreferences> fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return Tasks.forResult(new UserPreferences()); // Return default preferences
        }

        try {
            UserPreferences prefs = new UserPreferences();
            prefs.setUserId(doc.getId());
            
            // Basic preferences
            prefs.setName(doc.getString("name"));
            prefs.setFcmToken(doc.getString("fcm_token"));
            prefs.setLastUpdated(doc.getTimestamp("updated_at"));
            
            // Notification preferences
            Map<String, Object> notifPrefs = doc.get("notification_preferences", Map.class);
            if (notifPrefs != null) {
                Map<String, NotificationPreference> notificationPreferences = new HashMap<>();
                
                for (Map.Entry<String, Object> entry : notifPrefs.entrySet()) {
                    String type = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> prefData = (Map<String, Object>) entry.getValue();
                    
                    NotificationPreference notificationPreference = new NotificationPreference(type);
                    notificationPreference.setEnabled(Boolean.TRUE.equals(prefData.get("enabled")));
                    notificationPreference.setSoundEnabled(Boolean.TRUE.equals(prefData.get("sound_enabled")));
                    notificationPreference.setVibrationEnabled(Boolean.TRUE.equals(prefData.get("vibration_enabled")));
                    
                    Object startHour = prefData.get("quiet_hours_start");
                    if (startHour instanceof Long) {
                        notificationPreference.setQuietHoursStart(((Long) startHour).intValue());
                    }
                    
                    Object endHour = prefData.get("quiet_hours_end");
                    if (endHour instanceof Long) {
                        notificationPreference.setQuietHoursEnd(((Long) endHour).intValue());
                    }
                    
                    notificationPreferences.put(type, notificationPreference);
                }
                
                prefs.setNotificationPreferences(notificationPreferences);
            } else {
                prefs.setNotificationPreferences(new HashMap<>()); // Default empty preferences
            }
            
            // Template interactions
            Map<String, Boolean> favoriteTemplates = doc.get("favorite_templates", Map.class);
            if (favoriteTemplates != null) {
                prefs.setFavoriteTemplates(favoriteTemplates);
            }
            
            Map<String, Boolean> likedTemplates = doc.get("liked_templates", Map.class);
            if (likedTemplates != null) {
                prefs.setLikedTemplates(likedTemplates);
            }
            
            return Tasks.forResult(prefs);
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to UserPreferences", e);
            return Tasks.forException(new Exception("Error converting document to UserPreferences", e));
        }
    }

    /**
     * Convert a UserPreferences object to a Firestore document map
     */
    public static Map<String, Object> toDocument(UserPreferences prefs) {
        Map<String, Object> data = new HashMap<>();
        
        if (prefs.getName() != null) {
            data.put("name", prefs.getName());
        }
        
        if (prefs.getFcmToken() != null) {
            data.put("fcm_token", prefs.getFcmToken());
        }
        
        Map<String, NotificationPreference> notificationPreferences = prefs.getNotificationPreferences();
        if (notificationPreferences != null) {
            Map<String, Object> notifPrefsData = new HashMap<>();
            
            for (Map.Entry<String, NotificationPreference> entry : notificationPreferences.entrySet()) {
                NotificationPreference pref = entry.getValue();
                Map<String, Object> prefData = new HashMap<>();
                
                prefData.put("enabled", pref.isEnabled());
                prefData.put("sound_enabled", pref.isSoundEnabled());
                prefData.put("vibration_enabled", pref.isVibrationEnabled());
                prefData.put("quiet_hours_start", pref.getQuietHoursStart());
                prefData.put("quiet_hours_end", pref.getQuietHoursEnd());
                
                notifPrefsData.put(entry.getKey(), prefData);
            }
            
            data.put("notification_preferences", notifPrefsData);
        }
        
        data.put("favorite_templates", prefs.getFavoriteTemplates());
        data.put("liked_templates", prefs.getLikedTemplates());
        
        return data;
    }
} 