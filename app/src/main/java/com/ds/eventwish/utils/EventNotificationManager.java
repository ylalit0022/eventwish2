package com.ds.eventwish.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.EventNotificationConfig;
import com.ds.eventwish.data.model.RemoteConfigWrapper;
import com.ds.eventwish.receivers.EventNotificationReceiver;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manager class for handling Firebase Remote Config and local push notifications
 */
public class EventNotificationManager {
    private static final String TAG = "EventNotificationMgr";
    
    // Lock object for thread safety
    private static final Object LOCK = new Object();
    
    // Remote Config keys
    private static final String KEY_EVENT_CONFIG = "event_push";
    private static final String KEY_EVENTS_CONFIG = "events";
    
    // SharedPreferences keys
    private static final String PREF_NAME = "event_notification_prefs";
    private static final String KEY_CONFIG_JSON = "config_json";
    private static final String KEY_EVENTS_JSON = "events_json";
    private static final String KEY_LAST_FETCH_TIME = "last_fetch_time";
    private static final String KEY_LAST_NOTIFICATION_DATE = "last_notification_date";
    private static final String KEY_LAST_NOTIFICATION_DATE_PREFIX = "last_notification_date_";
    
    // Notification channel ID
    private static final String CHANNEL_ID = "event_notifications";
    
    // Notification ID base
    private static final int NOTIFICATION_ID_BASE = 1001;
    
    // Request codes for PendingIntents
    private static final int REQUEST_CODE_DAILY_CHECK = 2001;
    
    // Minimum fetch interval (24 hours)
    private static final long MINIMUM_FETCH_INTERVAL = TimeUnit.HOURS.toMillis(24);
    
    // Default notification time (9:00 AM)
    private static final int DEFAULT_NOTIFICATION_HOUR = 9;
    private static final int DEFAULT_NOTIFICATION_MINUTE = 0;
    
    // Singleton instance
    private static EventNotificationManager instance;
    
    // Context
    private final Context context;
    
    // Firebase Remote Config
    private final FirebaseRemoteConfig remoteConfig;
    
    // Gson for JSON parsing
    private final Gson gson;
    
    // SharedPreferences
    private final SharedPreferences prefs;
    
    /**
     * Get the singleton instance
     * @param context Application context
     * @return EventNotificationManager instance
     */
    public static synchronized EventNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventNotificationManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private EventNotificationManager(Context context) {
        this.context = context;
        this.remoteConfig = FirebaseRemoteConfig.getInstance();
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize Firebase Remote Config
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(TimeUnit.HOURS.toSeconds(12)) // Allow more frequent fetches during development
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        
        // Set default values
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        
        // Create notification channel for Android O and above
        createNotificationChannel();
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_events),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(context.getString(R.string.notification_channel_events_desc));
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            }
        }
    }
    
    /**
     * Initialize the EventNotificationManager
     * @param context Application context
     */
    public static void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize EventNotificationManager with null context");
            return;
        }
        
        synchronized (LOCK) {
            if (instance == null) {
                instance = new EventNotificationManager(context.getApplicationContext());
                Log.d(TAG, "EventNotificationManager initialized");
                
                // Schedule daily notification check
                instance.scheduleDailyCheck();
            }
        }
    }
    
    /**
     * Fetch remote config and schedule notification check
     */
    public void initialize() {
        // Fetch remote config if needed
        fetchRemoteConfigIfNeeded();
        
        // Schedule daily notification check
        scheduleDailyCheck();
    }
    
    /**
     * Fetch remote config if it hasn't been fetched in the last 24 hours
     */
    public void fetchRemoteConfigIfNeeded() {
        long lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastFetchTime >= MINIMUM_FETCH_INTERVAL) {
            Log.d(TAG, "Fetching remote config (last fetch: " + (currentTime - lastFetchTime) / 1000 / 60 + " minutes ago)");
            
            remoteConfig.fetchAndActivate()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Remote config fetched and activated successfully");
                            
                            // Save fetch time
                            prefs.edit().putLong(KEY_LAST_FETCH_TIME, currentTime).apply();
                            
                            // Save config JSON
                            saveConfigJson();
                            
                            // Check if notification should be shown
                            checkAndShowNotification();
                        } else {
                            Log.e(TAG, "Failed to fetch remote config", task.getException());
                        }
                    });
        } else {
            Log.d(TAG, "Using cached remote config (last fetch: " + (currentTime - lastFetchTime) / 1000 / 60 + " minutes ago)");
        }
    }
    
    /**
     * Force fetch remote config
     * @return Task to monitor the fetch operation
     */
    public Task<Boolean> forceFetchRemoteConfig() {
        Log.d(TAG, "Force fetching remote config started");
        
        // Log the current values before fetching
        String currentValue = remoteConfig.getString(KEY_EVENT_CONFIG);
        String currentEventsValue = remoteConfig.getString(KEY_EVENTS_CONFIG);
        Log.d(TAG, "Current remote config value before fetch: " + (currentValue.isEmpty() ? "EMPTY" : currentValue));
        Log.d(TAG, "Current events config value before fetch: " + (currentEventsValue.isEmpty() ? "EMPTY" : currentEventsValue));
        
        return remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Remote config fetched and activated successfully");
                        
                        // Check if the values actually changed
                        String newValue = remoteConfig.getString(KEY_EVENT_CONFIG);
                        String newEventsValue = remoteConfig.getString(KEY_EVENTS_CONFIG);
                        Log.d(TAG, "Remote config value after fetch: " + (newValue.isEmpty() ? "EMPTY" : newValue));
                        Log.d(TAG, "Events config value after fetch: " + (newEventsValue.isEmpty() ? "EMPTY" : newEventsValue));
                        Log.d(TAG, "Event value changed: " + !currentValue.equals(newValue));
                        Log.d(TAG, "Events value changed: " + !currentEventsValue.equals(newEventsValue));
                        
                        // Save fetch time
                        prefs.edit().putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis()).apply();
                        Log.d(TAG, "Updated last fetch time in preferences");
                        
                        // Save config JSON
                        saveConfigJson();
                        
                        // Check if notification should be shown
                        checkAndShowNotification();
                    } else {
                        Log.e(TAG, "Failed to fetch remote config", task.getException());
                    }
                });
    }
    
    /**
     * Save config JSON to SharedPreferences
     */
    private void saveConfigJson() {
        // Get the main config JSON
        String mainJson = remoteConfig.getString(KEY_EVENT_CONFIG);
        Log.d(TAG, "saveConfigJson: Attempting to save config JSON");
        
        if (mainJson != null && !mainJson.isEmpty()) {
            // Save the full JSON
            prefs.edit().putString(KEY_CONFIG_JSON, mainJson).apply();
            Log.d(TAG, "saveConfigJson: Successfully saved config JSON: " + mainJson);
            
            // Try to parse the main JSON to extract events array
            try {
                JsonObject jsonObject = JsonParser.parseString(mainJson).getAsJsonObject();
                
                // Check if it contains an events array
                if (jsonObject.has("events") && jsonObject.get("events").isJsonArray()) {
                    String eventsJson = jsonObject.get("events").toString();
                    prefs.edit().putString(KEY_EVENTS_JSON, eventsJson).apply();
                    Log.d(TAG, "saveConfigJson: Successfully extracted and saved events JSON: " + eventsJson);
                    
                    // Log the number of events
                    JsonArray eventsArray = jsonObject.getAsJsonArray("events");
                    Log.d(TAG, "saveConfigJson: Found " + eventsArray.size() + " events in the JSON");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing main JSON to extract events", e);
            }
        } else {
            Log.w(TAG, "saveConfigJson: No valid JSON to save, value is " + (mainJson == null ? "null" : "empty"));
        }
        
        // Also check for separate events parameter (fallback)
        String eventsJson = remoteConfig.getString(KEY_EVENTS_CONFIG);
        if (eventsJson != null && !eventsJson.isEmpty()) {
            prefs.edit().putString(KEY_EVENTS_JSON, eventsJson).apply();
            Log.d(TAG, "saveConfigJson: Successfully saved separate events JSON: " + eventsJson);
            
            // Log the number of events parsed for debugging
            try {
                // Try to parse as a direct array
                Type listType = new TypeToken<List<EventNotificationConfig>>(){}.getType();
                List<EventNotificationConfig> configList = gson.fromJson(eventsJson, listType);
                if (configList != null) {
                    Log.d(TAG, "saveConfigJson: Found " + configList.size() + " events in separate events parameter");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing separate events JSON during save", e);
            }
        }
    }
    
    /**
     * Get the event notification config
     * @return EventNotificationConfig or null if not available
     */
    public EventNotificationConfig getEventConfig() {
        String json = prefs.getString(KEY_CONFIG_JSON, null);
        if (json == null || json.isEmpty()) {
            json = remoteConfig.getString(KEY_EVENT_CONFIG);
            if (json == null || json.isEmpty()) {
                return null;
            }
        }
        
        try {
            // Try to parse as a JSON object first
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            
            // Check if it contains an event_push object
            if (jsonObject.has("event_push")) {
                String eventPushJson = jsonObject.get("event_push").toString();
                EventNotificationConfig config = gson.fromJson(eventPushJson, EventNotificationConfig.class);
                if (config != null) {
                    Log.d(TAG, "Parsed event_push from nested JSON structure");
                    return config;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse as nested structure, trying direct parse", e);
        }
        
        // Fallback to direct parsing
        return getEventConfigFromJson(json);
    }
    
    /**
     * Schedule the daily notification check
     */
    public void scheduleDailyCheck() {
        Log.d(TAG, "Scheduling daily notification check");
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }
        
        // Get the notification config
        EventNotificationConfig config = getEventConfig();
        
        // Create a calendar for the notification time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        
        // Set the hour and minute from the config, or default to 8:00 AM
        int hour = config != null ? config.getNotificationHour() : 8;
        int minute = config != null ? config.getNotificationMinute() : 0;
        
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Create the pending intent
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction(EventNotificationReceiver.ACTION_CHECK_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_DAILY_CHECK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
        
        Log.d(TAG, "Daily notification check scheduled for " + hour + ":" + 
                (minute < 10 ? "0" + minute : minute) + " (tomorrow: " + 
                (calendar.getTimeInMillis() > System.currentTimeMillis() + 12 * 60 * 60 * 1000) + ")");
    }
    
    /**
     * Get all event notification configs
     * @return List of EventNotificationConfig or empty list if not available
     */
    public List<EventNotificationConfig> getAllEventConfigs() {
        List<EventNotificationConfig> events = new ArrayList<>();
        // Track event IDs to avoid duplicates
        Set<String> processedEventIds = new HashSet<>();
        
        // First, add the single event if available
        EventNotificationConfig singleEvent = getEventConfig();
        if (singleEvent != null) {
            // If the single event doesn't have an ID, give it one
            if (singleEvent.getId() == null || singleEvent.getId().isEmpty()) {
                singleEvent.setId("default_event");
            }
            events.add(singleEvent);
            processedEventIds.add(singleEvent.getId());
            Log.d(TAG, "getAllEventConfigs: Added single event with ID: " + singleEvent.getId());
        } else {
            Log.d(TAG, "getAllEventConfigs: No single event available");
        }
        
        // Try to get events from the main JSON first
        String mainJson = prefs.getString(KEY_CONFIG_JSON, null);
        if (mainJson == null || mainJson.isEmpty()) {
            mainJson = remoteConfig.getString(KEY_EVENT_CONFIG);
        }
        
        if (mainJson != null && !mainJson.isEmpty()) {
            try {
                JsonObject jsonObject = JsonParser.parseString(mainJson).getAsJsonObject();
                
                // Check if it contains an events array
                if (jsonObject.has("events") && jsonObject.get("events").isJsonArray()) {
                    JsonArray eventsArray = jsonObject.getAsJsonArray("events");
                    for (int i = 0; i < eventsArray.size(); i++) {
                        try {
                            EventNotificationConfig event = gson.fromJson(eventsArray.get(i), EventNotificationConfig.class);
                            if (event != null && event.getId() != null && !processedEventIds.contains(event.getId())) {
                                events.add(event);
                                processedEventIds.add(event.getId());
                                Log.d(TAG, "getAllEventConfigs: Added event from nested JSON: " + 
                                        event.getId() + ", title: " + event.getTitle());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event at index " + i, e);
                        }
                    }
                    Log.d(TAG, "getAllEventConfigs: Added events from nested JSON");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing main JSON to extract events array", e);
            }
        }
        
        // Then, try to get events from the separate events array (fallback)
        // Only use this if we didn't find events in the main JSON
        if (processedEventIds.size() <= 1) { // Only single event or no events found so far
            String eventsJson = prefs.getString(KEY_EVENTS_JSON, null);
            if (eventsJson == null || eventsJson.isEmpty()) {
                eventsJson = remoteConfig.getString(KEY_EVENTS_CONFIG);
                Log.d(TAG, "getAllEventConfigs: Using remote config events JSON: " + (eventsJson != null && !eventsJson.isEmpty() ? "available" : "not available"));
            } else {
                Log.d(TAG, "getAllEventConfigs: Using cached events JSON: " + (eventsJson != null && !eventsJson.isEmpty() ? "available" : "not available"));
            }
            
            if (eventsJson != null && !eventsJson.isEmpty()) {
                try {
                    // Try to parse as a direct array
                    Type listType = new TypeToken<List<EventNotificationConfig>>(){}.getType();
                    List<EventNotificationConfig> configList = gson.fromJson(eventsJson, listType);
                    if (configList != null && !configList.isEmpty()) {
                        int addedCount = 0;
                        for (EventNotificationConfig event : configList) {
                            if (event != null && event.getId() != null && !processedEventIds.contains(event.getId())) {
                                events.add(event);
                                processedEventIds.add(event.getId());
                                addedCount++;
                                Log.d(TAG, "getAllEventConfigs: Event from separate parameter: " + 
                                        event.getId() + ", title: " + event.getTitle());
                            }
                        }
                        if (addedCount > 0) {
                            Log.d(TAG, "getAllEventConfigs: Added " + addedCount + " events from separate events parameter");
                        }
                    } else {
                        Log.d(TAG, "getAllEventConfigs: No events found in separate events parameter or array is null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing events JSON", e);
                }
            } else {
                Log.d(TAG, "getAllEventConfigs: No separate events JSON available");
            }
        }
        
        Log.d(TAG, "getAllEventConfigs: Total events found: " + events.size());
        return events;
    }
    
    /**
     * Check if notification should be shown and show it if needed for all events
     */
    public void checkAndShowNotification() {
        List<EventNotificationConfig> events = getAllEventConfigs();
        
        // Log the raw events JSON for debugging
        String eventsJson = prefs.getString(KEY_EVENTS_JSON, null);
        if (eventsJson == null || eventsJson.isEmpty()) {
            eventsJson = remoteConfig.getString(KEY_EVENTS_CONFIG);
        }
        Log.d(TAG, "Raw events JSON: " + (eventsJson != null ? eventsJson : "null"));
        
        if (events.isEmpty()) {
            Log.d(TAG, "No events available");
            return;
        }
        
        Log.d(TAG, "Checking " + events.size() + " events for notifications");
        
        for (EventNotificationConfig event : events) {
            checkAndShowNotificationForEvent(event);
        }
    }
    
    /**
     * Check if notification should be shown and show it if needed for a specific event
     * @param event The event to check
     */
    private void checkAndShowNotificationForEvent(EventNotificationConfig event) {
        if (event == null) {
            Log.e(TAG, "Event is null");
            return;
        }
        
        String eventId = event.getId() != null ? event.getId() : "default_event";
        Log.d(TAG, "Checking if notification should be shown for event " + eventId + ": " + event);
        
        // Check if the event is active
        if (!event.isEventActive()) {
            Log.d(TAG, "Event " + eventId + " is not active");
            return;
        }
        
        // Check if notification should be shown today
        boolean shouldShow = event.shouldShowNotificationToday();
        Log.d(TAG, "Should show notification for event " + eventId + "? " + shouldShow);
        
        if (shouldShow) {
            // Check if notification has already been shown today
            Calendar today = Calendar.getInstance();
            int todayKey = today.get(Calendar.YEAR) * 10000 + (today.get(Calendar.MONTH) + 1) * 100 + today.get(Calendar.DAY_OF_MONTH);
            int lastNotificationDate = prefs.getInt(KEY_LAST_NOTIFICATION_DATE_PREFIX + eventId, 0);
            
            Log.d(TAG, "Today key: " + todayKey + ", Last notification date: " + lastNotificationDate);
            
            if (lastNotificationDate != todayKey) {
                // Show notification
                Log.d(TAG, "Showing notification for event " + eventId);
                showNotification(event);
                
                // Save notification date
                prefs.edit().putInt(KEY_LAST_NOTIFICATION_DATE_PREFIX + eventId, todayKey).apply();
                
                Log.d(TAG, "Notification shown for event " + eventId + " today (days left: " + event.getDaysLeft() + ")");
            } else {
                Log.d(TAG, "Notification already shown for event " + eventId + " today");
            }
        } else {
            Log.d(TAG, "Notification should not be shown for event " + eventId + " today (days left: " + event.getDaysLeft() + ")");
        }
    }
    
    /**
     * Show notification
     * @param config Event notification config
     */
    private void showNotification(@NonNull EventNotificationConfig config) {
        String title = config.getTitle();
        String body = config.getBodyWithCountdown();
        String deepLink = config.getDeepLinkSupport();
        
        Log.d(TAG, "Attempting to show notification: " + title + " - " + body);
        
        if (title == null || title.isEmpty() || body == null || body.isEmpty()) {
            Log.e(TAG, "Invalid notification content");
            return;
        }
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted");
                return;
            } else {
                Log.d(TAG, "Notification permission is granted");
            }
        }
        
        // Create intent for notification tap
        Intent intent;
        if (deepLink != null && !deepLink.isEmpty()) {
            // Special handling for festival notification deep link
            if (deepLink.equals("eventwish://open/festival_notification")) {
                Log.d(TAG, "Creating intent for FestivalNotificationFragment");
                intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (intent != null) {
                    // Add data to indicate we want to open the festival notification fragment
                    intent.setData(Uri.parse(deepLink));
                    intent.putExtra("open_fragment", "festival_notification");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            } else {
                // Handle other deep links normally
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink));
            }
        } else {
            intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        }
        
        if (intent == null) {
            Log.e(TAG, "Failed to create intent for notification");
            return;
        }
        
        // Generate a unique notification ID based on the event ID
        int notificationId = NOTIFICATION_ID_BASE;
        if (config.getId() != null && !config.getId().isEmpty()) {
            notificationId = NOTIFICATION_ID_BASE + Math.abs(config.getId().hashCode() % 1000);
        }
        
        Log.d(TAG, "Using notification ID: " + notificationId);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        Log.d(TAG, "Notification builder created");
        
        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            try {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification shown with ID " + notificationId + ": " + title + " - " + body);
            } catch (Exception e) {
                Log.e(TAG, "Error showing notification", e);
            }
        } else {
            Log.e(TAG, "NotificationManager is null");
        }
    }
    
    /**
     * Get the raw config JSON from Firebase Remote Config
     * @return Raw JSON string or null if not available
     */
    public String getRawConfigJson() {
        String json = remoteConfig.getString(KEY_EVENT_CONFIG);
        if (json == null || json.isEmpty()) {
            json = prefs.getString(KEY_CONFIG_JSON, null);
        }
        return json;
    }
    
    /**
     * Parse JSON string to EventNotificationConfig
     * @param json JSON string
     * @return EventNotificationConfig or null if parsing fails
     */
    private EventNotificationConfig getEventConfigFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as a wrapper first (for proper JSON structure)
            RemoteConfigWrapper wrapper = gson.fromJson(json, RemoteConfigWrapper.class);
            if (wrapper != null && wrapper.getEventPush() != null) {
                Log.d(TAG, "Parsed config from wrapper JSON structure");
                return wrapper.getEventPush();
            }
            
            // If that fails, try to parse directly as EventNotificationConfig
            EventNotificationConfig config = gson.fromJson(json, EventNotificationConfig.class);
            if (config != null && (config.getTitle() != null || config.getBody() != null)) {
                Log.d(TAG, "Parsed config from direct JSON structure");
                return config;
            }
            
            Log.w(TAG, "Failed to parse JSON as either wrapper or direct structure: " + json);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse event config JSON", e);
            return null;
        }
    }
    
    /**
     * Force check notifications immediately
     */
    public void forceCheckNotifications() {
        Log.d(TAG, "Force checking notifications immediately");
        checkAndShowNotification();
    }
} 