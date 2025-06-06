package com.ds.eventwish.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.BuildConfig;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.UUID;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.content.pm.PackageInfo;

/**
 * Utility class for tracking analytics events using Firebase Analytics
 */
public class AnalyticsUtils {
    private static final String TAG = "AnalyticsUtils";
    private static final String ANALYTICS_PREFS = "analytics_prefs";
    private static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
    
    // Debug flag - set to true for detailed logging
    private static boolean isDebugMode = BuildConfig.DEBUG;
    
    // Event keys
    public static final String EVENT_TEMPLATE_VIEW = "template_view";
    public static final String EVENT_SHARED_WISH_VIEW = "shared_wish_view";
    public static final String EVENT_VIEWER_ACTIVE = "viewer_active";
    public static final String EVENT_VIEWER_INACTIVE = "viewer_inactive";
    public static final String EVENT_APP_OPEN = "app_open";
    public static final String EVENT_USER_REGISTER = "user_register";
    public static final String EVENT_TEMPLATE_CREATE = "template_create";
    public static final String EVENT_TEMPLATE_SHARE = "template_share";
    public static final String EVENT_SEARCH = "search";
    public static final String EVENT_CONTENT_LOAD_ERROR = "content_load_error";
    public static final String EVENT_CATEGORY_CLICK = "category_click";
    public static final String EVENT_SHARE_BUTTON_CLICK = "share_button_click";
    public static final String EVENT_SOCIAL_SHARE = "social_share";
    public static final String EVENT_DEVICE_INFO = "device_info";
    public static final String EVENT_SPONSORED_AD_IMPRESSION = "sponsored_ad_impression";
    public static final String EVENT_SPONSORED_AD_CLICK = "sponsored_ad_click";
    public static final String EVENT_UPDATE_CHECK = "update_check";
    public static final String EVENT_UPDATE_AVAILABLE = "update_available";
    public static final String EVENT_UPDATE_PROMPT_SHOWN = "update_prompt_shown";
    public static final String EVENT_UPDATE_PROMPT_ACTION = "update_prompt_action";
    public static final String EVENT_UPDATE_DIALOG_SHOWN = "update_dialog_shown";
    
    // Param keys
    public static final String PARAM_TEMPLATE_ID = "template_id";
    public static final String PARAM_SHORT_CODE = "short_code";
    public static final String PARAM_SENDER_NAME = "sender_name";
    public static final String PARAM_RECIPIENT_NAME = "recipient_name";
    public static final String PARAM_SESSION_ID = "session_id";
    public static final String PARAM_VIEW_DURATION = "view_duration";
    public static final String PARAM_CATEGORY = "category";
    public static final String PARAM_SEARCH_TERM = "search_term";
    public static final String PARAM_ERROR_MESSAGE = "error_message";
    public static final String PARAM_ERROR_CODE = "error_code";
    public static final String PARAM_SCREEN_NAME = "screen_name";
    public static final String PARAM_LOAD_TIME = "load_time";
    public static final String PARAM_TEMPLATE_TITLE = "template_title";
    public static final String PARAM_BUTTON_NAME = "button_name";
    public static final String PARAM_FRAGMENT_NAME = "fragment_name";
    public static final String PARAM_PLATFORM = "platform";
    public static final String PARAM_SUCCESS = "successful";
    public static final String PARAM_DEVICE_NAME = "device_name";
    public static final String PARAM_OS_VERSION = "os_version";
    public static final String PARAM_SCREEN_WIDTH_PX = "screen_width_px";
    public static final String PARAM_SCREEN_HEIGHT_PX = "screen_height_px";
    public static final String PARAM_SCREEN_DENSITY = "screen_density";
    public static final String PARAM_SCREEN_SIZE_DP = "screen_size_dp";
    public static final String PARAM_TIMESTAMP = "timestamp";
    public static final String PARAM_AD_ID = "ad_id";
    public static final String PARAM_AD_TITLE = "ad_title";
    public static final String PARAM_AD_LOCATION = "ad_location";
    public static final String PARAM_CURRENT_VERSION_CODE = "current_version_code";
    public static final String PARAM_LATEST_VERSION_CODE = "latest_version_code";
    public static final String PARAM_CURRENT_VERSION_NAME = "current_version_name";
    public static final String PARAM_LATEST_VERSION_NAME = "latest_version_name";
    public static final String PARAM_IS_FORCE_UPDATE = "is_force_update";
    public static final String PARAM_UPDATE_SOURCE = "update_source";
    public static final String PARAM_VERSION_NAME = "version_name";
    public static final String PARAM_ACTION = "action";
    
    // Update source values
    public static final String UPDATE_SOURCE_PLAY_STORE = "play_store";
    public static final String UPDATE_SOURCE_REMOTE_CONFIG = "remote_config";
    public static final String UPDATE_SOURCE_MANUAL = "manual_check";
    
    // Update action values
    public static final String UPDATE_ACTION_ACCEPTED = "accepted";
    public static final String UPDATE_ACTION_DECLINED = "declined";
    public static final String UPDATE_ACTION_DEFERRED = "deferred";
    
    private static String sessionId;
    private static FirebaseAnalytics firebaseAnalytics;
    private static boolean analyticsEnabled = true;
    
    // Analytics tracking statistics
    private static long sessionStartTime = System.currentTimeMillis();
    private static int eventCounter = 0;
    
    // Singleton instance
    private static AnalyticsUtils instance;
    
    /**
     * Get the singleton instance of AnalyticsUtils
     * @return AnalyticsUtils instance
     */
    public static AnalyticsUtils getInstance() {
        if (instance == null) {
            instance = new AnalyticsUtils();
        }
        return instance;
    }
    
    /**
     * Initialize the analytics system
     * @param context Application context
     */
    public static void init(@NonNull Context context) {
        // Initialize Firebase Analytics
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
            
            // Explicitly enable analytics collection
            firebaseAnalytics.setAnalyticsCollectionEnabled(true);
            
            // Generate a unique session ID
            sessionId = generateSessionId();
            logDebug("Analytics initialized with session ID: " + sessionId);
            
            // Set session ID as user property
            firebaseAnalytics.setUserProperty("session_id", sessionId);

            // Ensure consistent device ID to prevent duplicate device counts
            String persistentDeviceId = getPersistentDeviceId(context);
            firebaseAnalytics.setUserId(persistentDeviceId);
            logDebug("Using persistent device ID: " + persistentDeviceId);
            
            // Log initialization success event (important for verifying connectivity)
            Bundle initParams = new Bundle();
            initParams.putString("app_version", BuildConfig.VERSION_NAME);
            initParams.putString("device_model", Build.MODEL);
            firebaseAnalytics.logEvent("analytics_initialized", initParams);
            
            // Track device info
            trackDeviceInfo(context);
            
            // Log app open event
            trackAppOpen();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Analytics", e);
        }
    }
    
    /**
     * Get or create a persistent device ID for consistent analytics tracking
     * @param context Application context
     * @return Persistent device ID
     */
    private static String getPersistentDeviceId(Context context) {
        try {
            // Use SharedPreferences to store a persistent device ID
            android.content.SharedPreferences prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE);
            String deviceId = prefs.getString("device_id", null);
            
            if (deviceId == null) {
                // Generate a new device ID if not found
                deviceId = "device_" + UUID.randomUUID().toString();
                prefs.edit().putString("device_id", deviceId).apply();
                Log.d(TAG, "Created new persistent device ID: " + deviceId);
            } else {
                Log.d(TAG, "Using existing persistent device ID: " + deviceId);
            }
            
            return deviceId;
        } catch (Exception e) {
            Log.e(TAG, "Error getting persistent device ID", e);
            // Fallback to a temporarily generated ID
            return "tmp_" + UUID.randomUUID().toString();
        }
    }
    
    /**
     * Track detailed device information
     * @param context Application context
     */
    public static void trackDeviceInfo(Context context) {
        if (firebaseAnalytics == null || context == null) {
            Log.e(TAG, "Analytics not initialized or context is null. Call init() first.");
            return;
        }
        
        try {
            Bundle params = new Bundle();
            
            // Device model name
            params.putString(PARAM_DEVICE_NAME, Build.MODEL);
            
            // Device manufacturer
            params.putString("device_manufacturer", Build.MANUFACTURER);
            
            // OS version
            params.putString(PARAM_OS_VERSION, Build.VERSION.RELEASE);
            params.putInt("os_sdk_int", Build.VERSION.SDK_INT);
            
            // Device ID for debugging
            String deviceId = getPersistentDeviceId(context);
            params.putString("persistent_device_id", deviceId);
            
            // Is debug build
            params.putBoolean("is_debug_build", BuildConfig.DEBUG);
            
            // Screen dimensions
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenWidthPx = displayMetrics.widthPixels;
            int screenHeightPx = displayMetrics.heightPixels;
            float density = displayMetrics.density;
            
            params.putInt(PARAM_SCREEN_WIDTH_PX, screenWidthPx);
            params.putInt(PARAM_SCREEN_HEIGHT_PX, screenHeightPx);
            params.putFloat(PARAM_SCREEN_DENSITY, density);
            params.putString(PARAM_SCREEN_SIZE_DP, 
                    Math.round(screenWidthPx/density) + "x" + Math.round(screenHeightPx/density));
            
            // App version info
            params.putString("app_version", BuildConfig.VERSION_NAME);
            params.putInt("app_version_code", BuildConfig.VERSION_CODE);
            
            // Session info
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
            
            // Log the event
            firebaseAnalytics.logEvent(EVENT_DEVICE_INFO, params);
            logDebug("Tracked device info: " + Build.MODEL + ", " +  
                    Build.VERSION.RELEASE + ", " + 
                    Math.round(screenWidthPx/density) + "x" + Math.round(screenHeightPx/density) + "dp" +
                    ", Device ID: " + deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Error tracking device info", e);
        }
    }
    
    /**
     * Set debug mode for detailed logging
     * @param debug True to enable detailed logging
     */
    public static void setDebugMode(boolean debug) {
        isDebugMode = debug;
        logDebug("Analytics debug mode " + (debug ? "enabled" : "disabled"));
    }
    
    /**
     * Enable or disable analytics collection
     * @param enabled true to enable, false to disable
     */
    public static void setAnalyticsEnabled(boolean enabled) {
        analyticsEnabled = enabled;
        if (firebaseAnalytics != null) {
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
            Log.i(TAG, "Analytics collection " + (enabled ? "enabled" : "disabled"));
            
            // Log the change in analytics status
            Bundle params = new Bundle();
            params.putBoolean("analytics_enabled", enabled);
            firebaseAnalytics.logEvent("analytics_status_changed", params);
        }
    }
    
    /**
     * Check if analytics is enabled
     * @return true if analytics is enabled, false otherwise
     */
    public static boolean isAnalyticsEnabled() {
        return firebaseAnalytics != null && analyticsEnabled;
    }
    
    /**
     * Internal debug logging helper
     * @param message Log message
     */
    private static void logDebug(String message) {
        if (isDebugMode) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Verify Firebase Analytics configuration is properly set up
     * @param context Application context
     */
    public static void verifyConfiguration(Context context) {
        try {
            logDebug("Verifying Firebase Analytics configuration...");
            
            // Check if Firebase Analytics is properly initialized
            if (firebaseAnalytics == null) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context);
                if (firebaseAnalytics == null) {
                    Log.e(TAG, "Firebase Analytics instance is null, configuration may be incorrect");
                    return;
                }
            }
            
            // Check analytics collection status
            boolean isAnalyticsEnabled = true; // Default assumption
            try {
                // This is a reflection-based check since there's no direct API to query this
                java.lang.reflect.Method method = firebaseAnalytics.getClass().getDeclaredMethod("isAnalyticsCollectionEnabled");
                method.setAccessible(true);
                isAnalyticsEnabled = (boolean) method.invoke(firebaseAnalytics);
            } catch (Exception e) {
                // If we can't check, just assume it's enabled
                Log.w(TAG, "Could not verify analytics collection status: " + e.getMessage());
            }
            
            logDebug("Analytics collection enabled: " + isAnalyticsEnabled);
            logDebug("Firebase Analytics configuration verification complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying Firebase Analytics configuration", e);
        }
    }
    
    /**
     * Track app open event
     */
    public static void trackAppOpen() {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not enabled or not initialized. Call init() first.");
            return;
        }
        
        try {
            Bundle params = new Bundle();
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
            
            // Add device information
            params.putString(PARAM_DEVICE_NAME, Build.MODEL);
            params.putString(PARAM_OS_VERSION, Build.VERSION.RELEASE);
            params.putString("app_version", BuildConfig.VERSION_NAME);
            params.putInt("app_version_code", BuildConfig.VERSION_CODE);
            
            // Track app open event
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, params);
            logDebug("Tracked app open event with detailed parameters");
        } catch (Exception e) {
            Log.e(TAG, "Error tracking app open event", e);
        }
    }
    
    /**
     * Track template view event
     * @param templateId ID of the viewed template
     */
    public static void trackTemplateView(String templateId) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not enabled or not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_TEMPLATE_ID, templateId);
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
        
        firebaseAnalytics.logEvent(EVENT_TEMPLATE_VIEW, params);
        logDebug("Tracked template view: " + templateId);
    }
    
    /**
     * Track template view event with detailed information
     * @param templateId ID of the viewed template
     * @param templateTitle Title of the template
     * @param category Category of the template
     */
    public static void trackTemplateView(String templateId, String templateTitle, @Nullable String category) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_TEMPLATE_ID, templateId);
        
        if (templateTitle != null && !templateTitle.isEmpty()) {
            params.putString(PARAM_TEMPLATE_TITLE, templateTitle);
        }
        
        if (category != null && !category.isEmpty()) {
            params.putString(PARAM_CATEGORY, category);
        }
        
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
        
        firebaseAnalytics.logEvent(EVENT_TEMPLATE_VIEW, params);
        logDebug("Tracked detailed template view: " + templateId + 
                 (templateTitle != null ? ", " + templateTitle : "") + 
                 (category != null ? ", category: " + category : ""));
    }
    
    /**
     * Track category click event
     * @param categoryName Name of the clicked category
     */
    public static void trackCategoryClick(String categoryName) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_CATEGORY, categoryName);
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
        
        firebaseAnalytics.logEvent(EVENT_CATEGORY_CLICK, params);
        logDebug("Tracked category click: " + categoryName);
    }
    
    /**
     * Track share button click event
     * @param buttonName Name of the share button
     * @param fragmentName Name of the fragment where button was clicked
     * @param templateId ID of the related template (optional)
     */
    public static void trackShareButtonClick(String buttonName, String fragmentName, @Nullable String templateId) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_BUTTON_NAME, buttonName);
        params.putString(PARAM_FRAGMENT_NAME, fragmentName);
        
        if (templateId != null && !templateId.isEmpty()) {
            params.putString(PARAM_TEMPLATE_ID, templateId);
        }
        
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
        
        firebaseAnalytics.logEvent(EVENT_SHARE_BUTTON_CLICK, params);
        logDebug("Tracked share button click: " + buttonName + " in " + fragmentName + 
                 (templateId != null ? " for template " + templateId : ""));
    }
    
    /**
     * Track social media platform share
     * @param platform Name of the social media platform
     * @param templateId ID of the shared template
     * @param successful Whether the share was successful
     */
    public static void trackSocialShare(String platform, String templateId, boolean successful) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_PLATFORM, platform);
        params.putString(PARAM_TEMPLATE_ID, templateId);
        params.putBoolean(PARAM_SUCCESS, successful);
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
        
        firebaseAnalytics.logEvent(EVENT_SOCIAL_SHARE, params);
        logDebug("Tracked social share: " + platform + " for template " + templateId + 
                 " - " + (successful ? "successful" : "failed"));
    }

    /**
     * Track shared wish view event
     * @param shortCode Shortcode of the shared wish
     * @param senderName Name of the sender (if available)
     * @param recipientName Name of the recipient (if available)
     */
    public static void trackSharedWishView(String shortCode, @Nullable String senderName, @Nullable String recipientName) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            Log.e(TAG, "Cannot track shared wish view: Analytics disabled or not initialized");
            return;
        }
        
        try {
            Bundle params = new Bundle();
            params.putString(PARAM_SHORT_CODE, shortCode);
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
            
            if (senderName != null && !senderName.isEmpty()) {
                params.putString(PARAM_SENDER_NAME, senderName);
            }
            
            if (recipientName != null && !recipientName.isEmpty()) {
                params.putString(PARAM_RECIPIENT_NAME, recipientName);
            }
            
            // Log the event with standard event name
            firebaseAnalytics.logEvent(EVENT_SHARED_WISH_VIEW, params);
            
            // ALSO log as a screen view to ensure it appears in proper reports
            Bundle screenParams = new Bundle();
            screenParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "SharedWish_" + shortCode);
            screenParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SharedWishFragment");
            // Add other params for consistency
            screenParams.putString(PARAM_SHORT_CODE, shortCode);
            screenParams.putString(PARAM_SESSION_ID, sessionId);
            if (senderName != null) screenParams.putString(PARAM_SENDER_NAME, senderName);
            if (recipientName != null) screenParams.putString(PARAM_RECIPIENT_NAME, recipientName);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenParams);
            
            // ENHANCED DEBUGGING: Print to console with special formatting for visibility
            Log.d(TAG, "=======================");
            Log.d(TAG, "SHARED WISH VIEW TRACKED");
            Log.d(TAG, "Short code: " + shortCode);
            if (senderName != null) Log.d(TAG, "Sender: " + senderName);
            if (recipientName != null) Log.d(TAG, "Recipient: " + recipientName);
            Log.d(TAG, "Session ID: " + sessionId);
            Log.d(TAG, "=======================");
            
            // Force dispatch events immediately to ensure they're sent to Firebase
            forceDispatchEvents();
        } catch (Exception e) {
            Log.e(TAG, "Error tracking shared wish view", e);
        }
    }
    
    /**
     * Track active viewer
     * @param pageIdentifier An identifier for the page (template ID or short code)
     */
    public static void trackViewerActive(String pageIdentifier) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString("page_id", pageIdentifier);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        firebaseAnalytics.logEvent(EVENT_VIEWER_ACTIVE, params);
        Log.d(TAG, "Tracked viewer active: " + pageIdentifier);
    }
    
    /**
     * Track inactive viewer
     * @param pageIdentifier An identifier for the page (template ID or short code)
     * @param durationSeconds Duration in seconds that the viewer was active
     */
    public static void trackViewerInactive(String pageIdentifier, long durationSeconds) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString("page_id", pageIdentifier);
        params.putString(PARAM_SESSION_ID, sessionId);
        params.putLong(PARAM_VIEW_DURATION, durationSeconds);
        
        firebaseAnalytics.logEvent(EVENT_VIEWER_INACTIVE, params);
        Log.d(TAG, "Tracked viewer inactive: " + pageIdentifier + ", duration: " + durationSeconds + "s");
    }
    
    /**
     * Track template creation event
     * @param templateId ID of the created template
     * @param category Category of the template
     */
    public static void trackTemplateCreate(String templateId, @Nullable String category) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_TEMPLATE_ID, templateId);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        if (category != null && !category.isEmpty()) {
            params.putString(PARAM_CATEGORY, category);
        }
        
        firebaseAnalytics.logEvent(EVENT_TEMPLATE_CREATE, params);
        Log.d(TAG, "Tracked template create: " + templateId);
    }
    
    /**
     * Track template share event
     * @param templateId ID of the shared template
     * @param recipientName Name of the recipient (if available)
     */
    public static void trackTemplateShare(String templateId, @Nullable String recipientName) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_TEMPLATE_ID, templateId);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        if (recipientName != null && !recipientName.isEmpty()) {
            params.putString(PARAM_RECIPIENT_NAME, recipientName);
        }
        
        firebaseAnalytics.logEvent(EVENT_TEMPLATE_SHARE, params);
        Log.d(TAG, "Tracked template share: " + templateId);
    }
    
    /**
     * Track search event
     * @param searchTerm The search term used
     * @param resultCount Number of results found
     */
    public static void trackSearch(String searchTerm, int resultCount) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_SEARCH_TERM, searchTerm);
        params.putInt("result_count", resultCount);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        firebaseAnalytics.logEvent(EVENT_SEARCH, params);
        Log.d(TAG, "Tracked search: " + searchTerm + ", results: " + resultCount);
    }
    
    /**
     * Track content load error
     * @param errorMessage Error message
     * @param errorCode Error code (if available)
     */
    public static void trackContentLoadError(String errorMessage, @Nullable String errorCode) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        Bundle params = new Bundle();
        params.putString(PARAM_ERROR_MESSAGE, errorMessage);
        params.putString(PARAM_SESSION_ID, sessionId);
        
        if (errorCode != null && !errorCode.isEmpty()) {
            params.putString(PARAM_ERROR_CODE, errorCode);
        }
        
        firebaseAnalytics.logEvent(EVENT_CONTENT_LOAD_ERROR, params);
        Log.d(TAG, "Tracked content load error: " + errorMessage);
    }
    
    /**
     * Track screen view event with extra debugging
     * @param screenName Name of the screen
     * @param screenClass Class name of the screen (optional)
     */
    public static void trackScreenView(String screenName, @Nullable String screenClass) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        try {
            // Create detailed screen view parameters
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass != null ? screenClass : "");
            
            // Add additional context
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong("timestamp", System.currentTimeMillis());
            
            // For fragments, add fragment_type parameter
            if (screenClass != null && screenClass.contains("Fragment")) {
                params.putString("fragment_type", "Fragment");
                params.putString("navigation_path", screenName);
                
                // Log detailed fragment navigation for debugging
                Log.d(TAG, "Fragment Screen View: " + screenName);
            }
            
            // Log standard Firebase screen view event
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
            
            // ENHANCED DEBUGGING: Also log a custom event to ensure visibility in Firebase
            Bundle debugParams = new Bundle();
            debugParams.putString("debug_screen_name", screenName);
            debugParams.putString("debug_screen_class", screenClass != null ? screenClass : "");
            debugParams.putString("session_id", sessionId);
            debugParams.putLong("timestamp", System.currentTimeMillis());
            firebaseAnalytics.logEvent("debug_screen_view", debugParams);
            
            // Get the application context to use for current screen tracking
            Context context = null;
            try {
                context = com.ds.eventwish.EventWishApplication.getAppContext();
            } catch (Exception e) {
                Log.e(TAG, "Could not get application context for screen tracking", e);
            }
            
            // Also set the current screen - this helps with accurate screen tracking
            try {
                // If we have an activity context, use it
                if (context instanceof android.app.Activity) {
                    firebaseAnalytics.setCurrentScreen((android.app.Activity) context, screenName, screenClass);
                    Log.d(TAG, "Set current screen with activity context: " + screenName);
                } else {
                    // Note: passing null as the activity is not recommended but still works
                    // Firebase may deprecate this in the future
                    firebaseAnalytics.setCurrentScreen(null, screenName, screenClass);
                    Log.d(TAG, "Set current screen with null activity (not recommended): " + screenName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting current screen: " + e.getMessage());
            }
            
            // ENHANCED DEBUGGING: Print to console with special formatting for visibility
            Log.d(TAG, "=======================");
            Log.d(TAG, "SCREEN VIEW TRACKED: " + screenName);
            Log.d(TAG, "SCREEN CLASS: " + (screenClass != null ? screenClass : "unknown"));
            Log.d(TAG, "SESSION ID: " + sessionId);
            Log.d(TAG, "=======================");
            
            // Force dispatch events immediately in debug mode
            if (BuildConfig.DEBUG) {
                forceDispatchEvents();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking screen view", e);
        }
    }
    
    /**
     * Track screen view event with activity context
     * @param activity The activity context
     * @param screenName Name of the screen
     * @param screenClass Class name of the screen (optional)
     */
    public static void trackScreenView(android.app.Activity activity, String screenName, @Nullable String screenClass) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        try {
            // Create detailed screen view parameters
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass != null ? screenClass : "");
            
            // Add additional context
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong("timestamp", System.currentTimeMillis());
            
            // For fragments, add fragment_type parameter
            if (screenClass != null && screenClass.contains("Fragment")) {
                params.putString("fragment_type", "Fragment");
                params.putString("navigation_path", screenName);
            }
            
            // Log standard Firebase screen view event
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
            
            // Set the current screen using the provided activity context
            // This is the recommended way to track screens in Firebase
            firebaseAnalytics.setCurrentScreen(activity, screenName, screenClass);
            
            Log.d(TAG, "Tracked screen view with activity context: " + screenName + 
                 " (" + (screenClass != null ? screenClass : "unknown") + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error tracking screen view with activity context", e);
        }
    }
    
    /**
     * Set the user ID for analytics tracking
     * @param userId Unique identifier for the user
     */
    public static void setUserId(@NonNull String userId) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot set user ID: value is null or empty");
            return;
        }
        
        try {
            // Set user ID in Firebase Analytics
            firebaseAnalytics.setUserId(userId);
            
            // Add debug log to verify user ID is being set
            Log.d(TAG, "==========================");
            Log.d(TAG, "USER ID SET IN ANALYTICS: " + userId);
            Log.d(TAG, "Make sure this appears in Firebase console");
            Log.d(TAG, "==========================");
            
            // Log an event to ensure data is being sent to Firebase
            Bundle params = new Bundle();
            params.putString("user_id", userId);
            params.putLong("timestamp", System.currentTimeMillis());
            firebaseAnalytics.logEvent("user_identified", params);
        } catch (Exception e) {
            Log.e(TAG, "Error setting user ID for analytics", e);
        }
    }
    
    /**
     * Set user properties for analytics segmentation
     * @param userId Optional user ID (can be null)
     */
    public static void setUserProperties(@Nullable String userId) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        if (userId != null && !userId.isEmpty()) {
            firebaseAnalytics.setUserId(userId);
            Log.d(TAG, "Set user ID: " + userId);
        }
    }
    
    /**
     * Set a custom user property
     * @param key Property key
     * @param value Property value
     */
    public static void setUserProperty(String key, String value) {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return;
        }
        
        if (key != null && !key.isEmpty() && value != null) {
            firebaseAnalytics.setUserProperty(key, value);
            Log.d(TAG, "Set user property: " + key + " = " + value);
        }
    }
    
    /**
     * Generate a unique session ID
     * @return A unique session ID
     */
    private static String generateSessionId() {
        return String.valueOf(System.currentTimeMillis()) + "-" + 
               Math.round(Math.random() * 100000);
    }
    
    /**
     * Force immediate dispatch of any pending analytics events
     * This can help with testing and debugging analytics
     * @return true if dispatch succeeds, false otherwise
     */
    public static boolean forceDispatchEvents() {
        if (firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not initialized. Call init() first.");
            return false;
        }
        
        try {
            // Use reflection to access internal method as Firebase doesn't expose this directly
            Method dispatchMethod = firebaseAnalytics.getClass().getDeclaredMethod("dispatchEvent");
            dispatchMethod.setAccessible(true);
            dispatchMethod.invoke(firebaseAnalytics);
            Log.d(TAG, "Forced dispatch of pending analytics events");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error forcing dispatch of analytics events", e);
            return false;
        }
    }
    
    /**
     * Checks if analytics collection is enabled
     * @param context The context
     * @return true if analytics is enabled, false otherwise
     */
    public static boolean isAnalyticsEnabled(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null in isAnalyticsEnabled");
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(ANALYTICS_ENABLED_KEY, true);
    }

    /**
     * Gets a summary of diagnostic information for debugging
     * @param context The context
     * @return A string containing diagnostic information
     */
    public static String getDiagnosticSummary(Context context) {
        if (context == null) {
            return "Error: Context is null";
        }

        StringBuilder summary = new StringBuilder();
        
        // Device Information
        summary.append("Device Information:\n");
        summary.append("- Model: ").append(Build.MODEL).append("\n");
        summary.append("- Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        summary.append("- Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        summary.append("- SDK Level: ").append(Build.VERSION.SDK_INT).append("\n\n");
        
        // Screen Information
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            summary.append("Screen Information:\n");
            summary.append("- Width: ").append(metrics.widthPixels).append("px\n");
            summary.append("- Height: ").append(metrics.heightPixels).append("px\n");
            summary.append("- Density: ").append(metrics.density).append("\n\n");
        }
        
        // Firebase Analytics Info
        try {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
            summary.append("Firebase Analytics:\n");
            
            // Safe check for analytics collection status
            boolean isCollectionEnabled = analyticsEnabled;
            try {
                // Try to use reflection to access the method
                java.lang.reflect.Method method = firebaseAnalytics.getClass().getDeclaredMethod("isAnalyticsCollectionEnabled");
                method.setAccessible(true);
                isCollectionEnabled = (boolean) method.invoke(firebaseAnalytics);
            } catch (Exception e) {
                // Fall back to our internal flag if method is not available
                Log.w(TAG, "Could not check analytics collection status: " + e.getMessage());
            }
            
            summary.append("- Collection Enabled: ").append(isCollectionEnabled).append("\n");
            summary.append("- App Instance ID: ").append(firebaseAnalytics.getAppInstanceId()).append("\n\n");
        } catch (Exception e) {
            summary.append("Firebase Analytics: Error accessing instance - ").append(e.getMessage()).append("\n\n");
        }
        
        // App Information
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            summary.append("App Information:\n");
            summary.append("- Version: ").append(pInfo.versionName).append("\n");
            summary.append("- Version Code: ").append(pInfo.versionCode).append("\n");
            summary.append("- First Install: ").append(new Date(pInfo.firstInstallTime)).append("\n");
            summary.append("- Last Update: ").append(new Date(pInfo.lastUpdateTime)).append("\n\n");
        } catch (PackageManager.NameNotFoundException e) {
            summary.append("App Information: Error - ").append(e.getMessage()).append("\n\n");
        }
        
        // Analytics Events Count (if we have a counter)
        summary.append("Analytics Events:\n");
        summary.append("- Session Start Time: ").append(new Date(sessionStartTime)).append("\n");
        summary.append("- Events This Session: ").append(eventCounter).append("\n\n");
        
        return summary.toString();
    }
    
    /**
     * Track a custom event
     * @param eventName Name of the event
     * @param params Parameters for the event
     */
    public void trackEvent(String eventName, Bundle params) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            Log.e(TAG, "Analytics not enabled or not initialized. Call init() first.");
            return;
        }
        
        try {
            firebaseAnalytics.logEvent(eventName, params);
            logDebug("Tracked custom event: " + eventName);
        } catch (Exception e) {
            Log.e(TAG, "Error tracking custom event", e);
        }
    }
    
    /**
     * Enable or disable analytics collection with context
     * @param context The application context
     * @param enabled true to enable, false to disable
     */
    public static void setAnalyticsEnabled(Context context, boolean enabled) {
        analyticsEnabled = enabled;
        
        // Save the setting to SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(ANALYTICS_ENABLED_KEY, enabled).apply();
        
        if (firebaseAnalytics != null) {
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
            Log.i(TAG, "Analytics collection " + (enabled ? "enabled" : "disabled"));
            
            // Log the change in analytics status
            Bundle params = new Bundle();
            params.putBoolean("analytics_enabled", enabled);
            firebaseAnalytics.logEvent("analytics_status_changed", params);
        }
    }

    /**
     * Track sponsored ad impression
     * @param adId The ID of the ad that was displayed
     * @param adTitle The title of the ad
     * @param location The location where the ad was displayed
     */
    public static void trackAdImpression(String adId, String adTitle, String location) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            logDebug("Analytics disabled or not initialized. Skipping ad impression tracking.");
            return;
        }
        
        try {
            Bundle params = new Bundle();
            params.putString(PARAM_AD_ID, adId);
            params.putString(PARAM_AD_TITLE, adTitle);
            params.putString(PARAM_AD_LOCATION, location);
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
            
            firebaseAnalytics.logEvent(EVENT_SPONSORED_AD_IMPRESSION, params);
            logDebug("Tracked ad impression: " + adTitle + " (ID: " + adId + ") at " + location);
            eventCounter++;
        } catch (Exception e) {
            Log.e(TAG, "Error tracking ad impression", e);
        }
    }
    
    /**
     * Track sponsored ad click
     * @param adId The ID of the ad that was clicked
     * @param adTitle The title of the ad
     * @param location The location where the ad was displayed
     */
    public static void trackAdClick(String adId, String adTitle, String location) {
        if (!analyticsEnabled || firebaseAnalytics == null) {
            logDebug("Analytics disabled or not initialized. Skipping ad click tracking.");
            return;
        }
        
        try {
            Bundle params = new Bundle();
            params.putString(PARAM_AD_ID, adId);
            params.putString(PARAM_AD_TITLE, adTitle);
            params.putString(PARAM_AD_LOCATION, location);
            params.putString(PARAM_SESSION_ID, sessionId);
            params.putLong(PARAM_TIMESTAMP, System.currentTimeMillis());
            
            firebaseAnalytics.logEvent(EVENT_SPONSORED_AD_CLICK, params);
            logDebug("Tracked ad click: " + adTitle + " (ID: " + adId + ") at " + location);
            eventCounter++;
        } catch (Exception e) {
            Log.e(TAG, "Error tracking ad click", e);
        }
    }
} 