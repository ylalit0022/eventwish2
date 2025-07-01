package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Model class for centralized notification configuration from Firebase Remote Config
 */
public class NotificationConfig {
    private static final String TAG = "NotificationConfig";

    @SerializedName("version")
    private String version;

    @SerializedName("lastUpdated")
    private String lastUpdated;

    @SerializedName("notificationTypes")
    private Map<String, NotificationType> notificationTypes;

    @SerializedName("globalSettings")
    private GlobalSettings globalSettings;

    // Required empty constructor for Gson
    public NotificationConfig() {}

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, NotificationType> getNotificationTypes() {
        return notificationTypes;
    }

    public void setNotificationTypes(Map<String, NotificationType> notificationTypes) {
        this.notificationTypes = notificationTypes;
    }

    public GlobalSettings getGlobalSettings() {
        return globalSettings;
    }

    public void setGlobalSettings(GlobalSettings globalSettings) {
        this.globalSettings = globalSettings;
    }

    /**
     * Get a specific notification type configuration
     * @param type The notification type key (e.g., "dailyReminder", "festivalAlert")
     * @return NotificationType configuration or null if not found
     */
    @Nullable
    public NotificationType getNotificationType(String type) {
        return notificationTypes != null ? notificationTypes.get(type) : null;
    }

    /**
     * Check if a notification type is enabled
     * @param type The notification type key
     * @return true if enabled, false otherwise
     */
    public boolean isNotificationTypeEnabled(String type) {
        NotificationType notificationType = getNotificationType(type);
        return notificationType != null && notificationType.isEnabled();
    }

    /**
     * Inner class for notification type configuration
     */
    public static class NotificationType {
        @SerializedName("enabled")
        private boolean enabled;

        @SerializedName("schedule")
        private Schedule schedule;

        @SerializedName("messages")
        private Messages messages;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Schedule getSchedule() {
            return schedule;
        }

        public void setSchedule(Schedule schedule) {
            this.schedule = schedule;
        }

        public Messages getMessages() {
            return messages;
        }

        public void setMessages(Messages messages) {
            this.messages = messages;
        }
    }

    /**
     * Inner class for schedule configuration
     */
    public static class Schedule {
        @SerializedName("type")
        private String type; // "daily", "dynamic", "inactivity", "expiry"

        @SerializedName("utcHour")
        private Integer utcHour;

        @SerializedName("utcMinute")
        private Integer utcMinute;

        @SerializedName("daysBeforeFestival")
        private List<Integer> daysBeforeFestival;

        @SerializedName("thresholdDays")
        private Integer thresholdDays;

        @SerializedName("daysBeforeExpiry")
        private List<Integer> daysBeforeExpiry;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getUtcHour() {
            return utcHour;
        }

        public void setUtcHour(Integer utcHour) {
            this.utcHour = utcHour;
        }

        public Integer getUtcMinute() {
            return utcMinute;
        }

        public void setUtcMinute(Integer utcMinute) {
            this.utcMinute = utcMinute;
        }

        public List<Integer> getDaysBeforeFestival() {
            return daysBeforeFestival;
        }

        public void setDaysBeforeFestival(List<Integer> daysBeforeFestival) {
            this.daysBeforeFestival = daysBeforeFestival;
        }

        public Integer getThresholdDays() {
            return thresholdDays;
        }

        public void setThresholdDays(Integer thresholdDays) {
            this.thresholdDays = thresholdDays;
        }

        public List<Integer> getDaysBeforeExpiry() {
            return daysBeforeExpiry;
        }

        public void setDaysBeforeExpiry(List<Integer> daysBeforeExpiry) {
            this.daysBeforeExpiry = daysBeforeExpiry;
        }
    }

    /**
     * Inner class for message configuration
     */
    public static class Messages {
        @SerializedName("title")
        private String title;

        @SerializedName("body")
        private String body;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    /**
     * Inner class for global settings
     */
    public static class GlobalSettings {
        @SerializedName("maxNotificationsPerDay")
        private int maxNotificationsPerDay;

        @SerializedName("quietHoursStart")
        private int quietHoursStart;

        @SerializedName("quietHoursEnd")
        private int quietHoursEnd;

        @SerializedName("batchingIntervalMinutes")
        private int batchingIntervalMinutes;

        public int getMaxNotificationsPerDay() {
            return maxNotificationsPerDay;
        }

        public void setMaxNotificationsPerDay(int maxNotificationsPerDay) {
            this.maxNotificationsPerDay = maxNotificationsPerDay;
        }

        public int getQuietHoursStart() {
            return quietHoursStart;
        }

        public void setQuietHoursStart(int quietHoursStart) {
            this.quietHoursStart = quietHoursStart;
        }

        public int getQuietHoursEnd() {
            return quietHoursEnd;
        }

        public void setQuietHoursEnd(int quietHoursEnd) {
            this.quietHoursEnd = quietHoursEnd;
        }

        public int getBatchingIntervalMinutes() {
            return batchingIntervalMinutes;
        }

        public void setBatchingIntervalMinutes(int batchingIntervalMinutes) {
            this.batchingIntervalMinutes = batchingIntervalMinutes;
        }

        /**
         * Check if current time is within quiet hours
         * @param currentHour Current hour in 24-hour format (0-23)
         * @return true if within quiet hours, false otherwise
         */
        public boolean isQuietHours(int currentHour) {
            if (quietHoursStart == quietHoursEnd) {
                return false; // No quiet hours
            }
            
            if (quietHoursStart < quietHoursEnd) {
                // Normal case: e.g., 22:00 to 08:00 next day
                return currentHour >= quietHoursStart || currentHour < quietHoursEnd;
            } else {
                // Crosses midnight: e.g., 22:00 to 08:00
                return currentHour >= quietHoursStart && currentHour < quietHoursEnd;
            }
        }
    }

    // Notification type constants
    public static final String TYPE_DAILY_REMINDER = "dailyReminder";
    public static final String TYPE_FESTIVAL_ALERT = "festivalAlert";
    public static final String TYPE_INACTIVITY_NUDGE = "inactivityNudge";
    public static final String TYPE_SUBSCRIPTION_EXPIRY = "subscriptionExpiry";

    // Schedule type constants
    public static final String SCHEDULE_TYPE_DAILY = "daily";
    public static final String SCHEDULE_TYPE_DYNAMIC = "dynamic";
    public static final String SCHEDULE_TYPE_INACTIVITY = "inactivity";
    public static final String SCHEDULE_TYPE_EXPIRY = "expiry";

    @Override
    public String toString() {
        return "NotificationConfig{" +
                "version='" + version + '\'' +
                ", lastUpdated='" + lastUpdated + '\'' +
                ", notificationTypes=" + (notificationTypes != null ? notificationTypes.size() + " types" : "null") +
                ", globalSettings=" + globalSettings +
                '}';
    }
} 