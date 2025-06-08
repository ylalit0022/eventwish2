package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.PropertyName;

/**
 * Model class for notification preferences
 */
public class NotificationPreference {
    public static final String TYPE_TEMPLATE_UPDATES = "template_updates";
    public static final String TYPE_FESTIVAL_REMINDERS = "festival_reminders";
    public static final String TYPE_FAVORITE_NOTIFICATIONS = "favorite_notifications";
    public static final String TYPE_LIKE_NOTIFICATIONS = "like_notifications";

    @PropertyName("type")
    private String type;

    @PropertyName("enabled")
    private boolean enabled;

    @PropertyName("sound_enabled")
    private boolean soundEnabled;

    @PropertyName("vibration_enabled")
    private boolean vibrationEnabled;

    @PropertyName("quiet_hours_start")
    private Integer quietHoursStart;

    @PropertyName("quiet_hours_end")
    private Integer quietHoursEnd;

    // Required empty constructor for Firestore
    public NotificationPreference() {}

    public NotificationPreference(String type) {
        this.type = type;
        this.enabled = true;
        this.soundEnabled = true;
        this.vibrationEnabled = true;
    }

    @NonNull
    public String getType() {
        return type != null ? type : "";
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PropertyName("sound_enabled")
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    @PropertyName("sound_enabled")
    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    @PropertyName("vibration_enabled")
    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    @PropertyName("vibration_enabled")
    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    @Nullable
    @PropertyName("quiet_hours_start")
    public Integer getQuietHoursStart() {
        return quietHoursStart;
    }

    @PropertyName("quiet_hours_start")
    public void setQuietHoursStart(@Nullable Integer quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    @Nullable
    @PropertyName("quiet_hours_end")
    public Integer getQuietHoursEnd() {
        return quietHoursEnd;
    }

    @PropertyName("quiet_hours_end")
    public void setQuietHoursEnd(@Nullable Integer quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    /**
     * Check if notification should be shown based on quiet hours
     */
    public boolean shouldShowNotification(int currentHour) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return true;
        }

        if (quietHoursStart <= quietHoursEnd) {
            // Normal case: quiet hours within same day
            return currentHour < quietHoursStart || currentHour >= quietHoursEnd;
        } else {
            // Overnight case: quiet hours span midnight
            return currentHour >= quietHoursEnd && currentHour < quietHoursStart;
        }
    }

    /**
     * Create default notification preferences for a given type
     */
    public static NotificationPreference createDefault(String type) {
        NotificationPreference preference = new NotificationPreference(type);
        
        // Set type-specific defaults
        switch (type) {
            case TYPE_TEMPLATE_UPDATES:
                preference.setQuietHoursStart(22); // 10 PM
                preference.setQuietHoursEnd(8);    // 8 AM
                break;
            case TYPE_FESTIVAL_REMINDERS:
                preference.setQuietHoursStart(23); // 11 PM
                preference.setQuietHoursEnd(7);    // 7 AM
                break;
            case TYPE_FAVORITE_NOTIFICATIONS:
            case TYPE_LIKE_NOTIFICATIONS:
                preference.setQuietHoursStart(null);
                preference.setQuietHoursEnd(null);
                break;
        }
        
        return preference;
    }
} 