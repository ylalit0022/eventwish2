package com.ds.eventwish.data.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Model class for event notification configuration from Firebase Remote Config
 */
public class EventNotificationConfig {
    private static final String TAG = "EventNotificationConfig";
    private static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    static {
        ISO_8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("body")
    private String body;

    @SerializedName("status")
    private boolean status;

    @SerializedName("startdate")
    private String startDate;

    @SerializedName("enddate")
    private String endDate;

    @SerializedName("deeplinksupport")
    private String deepLinkSupport;

    @SerializedName("showTime")
    private String showTime; // Format: "HH:mm" (24-hour format)

    // Required empty constructor for Gson
    public EventNotificationConfig() {}

    public EventNotificationConfig(String id, String title, String body, boolean status, String startDate, String endDate, String deepLinkSupport) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.deepLinkSupport = deepLinkSupport;
        this.showTime = "08:00"; // Default to 8:00 AM if not specified
    }

    public EventNotificationConfig(String id, String title, String body, boolean status, String startDate, String endDate, String deepLinkSupport, String showTime) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.deepLinkSupport = deepLinkSupport;
        this.showTime = showTime != null && !showTime.isEmpty() ? showTime : "08:00"; // Default to 8:00 AM if not specified
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    public void setBody(@Nullable String body) {
        this.body = body;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Nullable
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable String startDate) {
        this.startDate = startDate;
    }

    @Nullable
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(@Nullable String endDate) {
        this.endDate = endDate;
    }

    @Nullable
    public String getDeepLinkSupport() {
        return deepLinkSupport;
    }

    public void setDeepLinkSupport(@Nullable String deepLinkSupport) {
        this.deepLinkSupport = deepLinkSupport;
    }

    public String getShowTime() {
        return showTime != null ? showTime : "08:00"; // Default to 8:00 AM if not specified
    }

    public void setShowTime(String showTime) {
        this.showTime = showTime;
    }

    /**
     * Get notification hour from showTime
     * @return Hour in 24-hour format (0-23)
     */
    public int getNotificationHour() {
        try {
            String[] timeParts = getShowTime().split(":");
            return Integer.parseInt(timeParts[0]);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification hour", e);
            return 8; // Default to 8 AM
        }
    }

    /**
     * Get notification minute from showTime
     * @return Minute (0-59)
     */
    public int getNotificationMinute() {
        try {
            String[] timeParts = getShowTime().split(":");
            return Integer.parseInt(timeParts[1]);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification minute", e);
            return 0; // Default to 0 minutes
        }
    }

    /**
     * Parse the start date string to a Date object
     * @return Date object or null if parsing fails
     */
    @Nullable
    public Date getStartDateAsDate() {
        if (startDate == null || startDate.isEmpty()) {
            return null;
        }
        try {
            synchronized (ISO_8601_FORMAT) {
                return ISO_8601_FORMAT.parse(startDate);
            }
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Parse the end date string to a Date object
     * @return Date object or null if parsing fails
     */
    @Nullable
    public Date getEndDateAsDate() {
        if (endDate == null || endDate.isEmpty()) {
            return null;
        }
        try {
            synchronized (ISO_8601_FORMAT) {
                return ISO_8601_FORMAT.parse(endDate);
            }
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Check if the event is currently active based on the current date
     * @return true if the event is active, false otherwise
     */
    public boolean isEventActive() {
        if (!status) {
            return false;
        }

        Date now = new Date();
        Date start = getStartDateAsDate();
        Date end = getEndDateAsDate();

        if (start == null || end == null) {
            return false;
        }

        return now.after(start) && now.before(end);
    }

    /**
     * Calculate the number of days left until the end date
     * @return number of days left, or -1 if calculation fails
     */
    public int getDaysLeft() {
        Date now = new Date();
        Date end = getEndDateAsDate();

        if (end == null) {
            return -1;
        }

        // Calculate days left (convert milliseconds to days)
        long diffInMillis = end.getTime() - now.getTime();
        if (diffInMillis <= 0) {
            return 0; // Event has ended
        }

        return (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1; // +1 to include the current day
    }

    /**
     * Get the countdown text based on days left
     * @return countdown text (e.g., "3 days left", "2 days left", "Today")
     */
    @NonNull
    public String getCountdownText() {
        int daysLeft = getDaysLeft();
        if (daysLeft <= 0) {
            return "Today";
        } else if (daysLeft == 1) {
            return "Today";
        } else {
            return daysLeft + " days left";
        }
    }

    /**
     * Get the body text with countdown placeholder replaced
     * @return body text with countdown placeholder replaced
     */
    @NonNull
    public String getBodyWithCountdown() {
        if (body == null) {
            return "";
        }
        return body.replace("{{countdown}}", getCountdownText());
    }

    /**
     * Check if the notification should be shown today
     * @return true if the notification should be shown today, false otherwise
     */
    public boolean shouldShowNotificationToday() {
        if (!isEventActive()) {
            return false;
        }

        int daysLeft = getDaysLeft();
        // Show notification if 3, 2, or 1 day left
        return daysLeft <= 3 && daysLeft >= 0;
    }

    @Override
    public String toString() {
        return "EventNotificationConfig{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", status=" + status +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", deepLinkSupport='" + deepLinkSupport + '\'' +
                ", showTime='" + showTime + '\'' +
                ", daysLeft=" + getDaysLeft() +
                ", countdownText='" + getCountdownText() + '\'' +
                '}';
    }
} 