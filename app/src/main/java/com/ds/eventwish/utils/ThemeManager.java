package com.ds.eventwish.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ThemeManager handles dynamic theme switching using Firebase Remote Config
 * Supports festival themes, seasonal themes, and brand campaign themes
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_CURRENT_THEME = "current_theme";
    private static final String KEY_LAST_THEME_CHECK = "last_theme_check";
    
    // Remote Config keys
    private static final String RC_THEME_VARIANT = "theme_variant";
    private static final String RC_THEME_AUTO_SWITCH = "theme_auto_switch";
    private static final String RC_THEME_SCHEDULE = "theme_schedule";
    private static final String RC_THEME_COLORS = "theme_colors";
    
    // Theme variants
    public static final String THEME_DEFAULT = "default";
    public static final String THEME_CHRISTMAS = "christmas";
    public static final String THEME_DIWALI = "diwali";
    public static final String THEME_NEW_YEAR = "new_year";
    public static final String THEME_VALENTINE = "valentine";
    public static final String THEME_SPRING = "spring";
    public static final String THEME_SUMMER = "summer";
    public static final String THEME_FALL = "fall";
    public static final String THEME_WINTER = "winter";
    
    private static ThemeManager instance;
    private Context context;
    private FirebaseRemoteConfig remoteConfig;
    private SharedPreferences prefs;
    private String currentTheme = THEME_DEFAULT;
    
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.currentTheme = prefs.getString(KEY_CURRENT_THEME, THEME_DEFAULT);
        initializeRemoteConfig();
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize Firebase Remote Config with default values
     */
    public void initializeRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        
        // Set default values
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(RC_THEME_VARIANT, THEME_DEFAULT);
        defaults.put(RC_THEME_AUTO_SWITCH, true);
        defaults.put(RC_THEME_SCHEDULE, "{}");
        defaults.put(RC_THEME_COLORS, "{}");
        
        remoteConfig.setDefaultsAsync(defaults);
        
        // Configure settings for development/testing
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour for production, 0 for testing
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
    }
    
    /**
     * Fetch and apply theme from Remote Config
     */
    public void fetchAndApplyTheme(ThemeUpdateCallback callback) {
        Log.d(TAG, "Fetching theme configuration from Remote Config");
        
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Remote Config fetch successful");
                        
                        boolean autoSwitch = remoteConfig.getBoolean(RC_THEME_AUTO_SWITCH);
                        if (autoSwitch) {
                            String scheduledTheme = getScheduledTheme();
                            if (scheduledTheme != null) {
                                applyTheme(scheduledTheme, callback);
                                return;
                            }
                        }
                        
                        String remoteTheme = remoteConfig.getString(RC_THEME_VARIANT);
                        applyTheme(remoteTheme, callback);
                        
                    } else {
                        Log.e(TAG, "Remote Config fetch failed", task.getException());
                        if (callback != null) {
                            callback.onThemeUpdateFailed("Failed to fetch theme configuration");
                        }
                    }
                });
    }
    
    /**
     * Check if there's a scheduled theme for current date
     */
    private String getScheduledTheme() {
        try {
            String scheduleJson = remoteConfig.getString(RC_THEME_SCHEDULE);
            if (scheduleJson.isEmpty() || scheduleJson.equals("{}")) {
                return null;
            }
            
            JsonObject schedule = JsonParser.parseString(scheduleJson).getAsJsonObject();
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            for (String themeName : schedule.keySet()) {
                JsonObject themeSchedule = schedule.getAsJsonObject(themeName);
                String startDate = themeSchedule.get("start").getAsString();
                String endDate = themeSchedule.get("end").getAsString();
                String theme = themeSchedule.get("theme").getAsString();
                
                if (isDateInRange(currentDate, startDate, endDate)) {
                    Log.d(TAG, "Found scheduled theme: " + theme + " for date: " + currentDate);
                    return theme;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing theme schedule", e);
        }
        
        return null;
    }
    
    /**
     * Check if current date is within the specified range
     */
    private boolean isDateInRange(String current, String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = sdf.parse(current);
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);
            
            return currentDate != null && startDate != null && endDate != null &&
                   !currentDate.before(startDate) && !currentDate.after(endDate);
        } catch (Exception e) {
            Log.e(TAG, "Error comparing dates", e);
            return false;
        }
    }
    
    /**
     * Apply the specified theme
     */
    public void applyTheme(String theme, ThemeUpdateCallback callback) {
        if (theme == null || theme.isEmpty()) {
            theme = THEME_DEFAULT;
        }
        
        Log.d(TAG, "Applying theme: " + theme);
        
        // Validate theme
        if (!isValidTheme(theme)) {
            Log.w(TAG, "Invalid theme: " + theme + ", falling back to default");
            theme = THEME_DEFAULT;
        }
        
        // Check if theme actually changed
        if (theme.equals(currentTheme)) {
            Log.d(TAG, "Theme unchanged: " + theme);
            if (callback != null) {
                callback.onThemeUpdateCompleted(theme, false);
            }
            return;
        }
        
        // Update current theme
        String previousTheme = currentTheme;
        currentTheme = theme;
        
        // Save to preferences
        prefs.edit()
                .putString(KEY_CURRENT_THEME, theme)
                .putLong(KEY_LAST_THEME_CHECK, System.currentTimeMillis())
                .apply();
        
        Log.d(TAG, "Theme changed from " + previousTheme + " to " + theme);
        
        if (callback != null) {
            callback.onThemeUpdateCompleted(theme, true);
        }
    }
    
    /**
     * Get current active theme
     */
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Check if theme is valid
     */
    private boolean isValidTheme(String theme) {
        return theme.equals(THEME_DEFAULT) || 
               theme.equals(THEME_CHRISTMAS) || 
               theme.equals(THEME_DIWALI) || 
               theme.equals(THEME_NEW_YEAR) || 
               theme.equals(THEME_VALENTINE) ||
               theme.equals(THEME_SPRING) || 
               theme.equals(THEME_SUMMER) || 
               theme.equals(THEME_FALL) || 
               theme.equals(THEME_WINTER);
    }
    
    /**
     * Get theme resource ID for the current theme
     */
    public int getThemeResourceId() {
        switch (currentTheme) {
            case THEME_CHRISTMAS:
                return com.ds.eventwish.R.style.Theme_EventWish_Christmas;
            case THEME_DIWALI:
                return com.ds.eventwish.R.style.Theme_EventWish_Diwali;
            case THEME_NEW_YEAR:
                return com.ds.eventwish.R.style.Theme_EventWish_NewYear;
            case THEME_VALENTINE:
                return com.ds.eventwish.R.style.Theme_EventWish_Valentine;
            case THEME_SPRING:
                return com.ds.eventwish.R.style.Theme_EventWish_Spring;
            case THEME_SUMMER:
                return com.ds.eventwish.R.style.Theme_EventWish_Summer;
            case THEME_FALL:
                return com.ds.eventwish.R.style.Theme_EventWish_Fall;
            case THEME_WINTER:
                return com.ds.eventwish.R.style.Theme_EventWish_Winter;
            default:
                return com.ds.eventwish.R.style.Theme_EventWish;
        }
    }
    
    /**
     * Apply theme to activity
     */
    public void applyThemeToActivity(Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            activity.setTheme(getThemeResourceId());
        }
    }
    
    /**
     * Force theme refresh and recreate activity if needed
     */
    public void refreshTheme(Activity activity, ThemeUpdateCallback callback) {
        fetchAndApplyTheme(new ThemeUpdateCallback() {
            @Override
            public void onThemeUpdateCompleted(String theme, boolean changed) {
                if (changed && activity != null && !activity.isFinishing()) {
                    Log.d(TAG, "Recreating activity for theme change");
                    activity.recreate();
                }
                if (callback != null) {
                    callback.onThemeUpdateCompleted(theme, changed);
                }
            }
            
            @Override
            public void onThemeUpdateFailed(String error) {
                if (callback != null) {
                    callback.onThemeUpdateFailed(error);
                }
            }
        });
    }
    
    /**
     * Get time since last theme check
     */
    public long getTimeSinceLastCheck() {
        long lastCheck = prefs.getLong(KEY_LAST_THEME_CHECK, 0);
        return System.currentTimeMillis() - lastCheck;
    }
    
    /**
     * Check if theme check is needed (every 6 hours)
     */
    public boolean isThemeCheckNeeded() {
        return getTimeSinceLastCheck() > (6 * 60 * 60 * 1000); // 6 hours
    }
    
    /**
     * Callback interface for theme updates
     */
    public interface ThemeUpdateCallback {
        void onThemeUpdateCompleted(String theme, boolean changed);
        void onThemeUpdateFailed(String error);
    }
} 