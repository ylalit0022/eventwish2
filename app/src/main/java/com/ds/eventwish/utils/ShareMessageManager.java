package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager class for handling share messages from Firebase Remote Config
 * with caching and placeholder replacement
 */
public class ShareMessageManager {
    private static final String TAG = "ShareMessageManager";
    
    // Remote Config keys
    private static final String KEY_SHARE_MESSAGE = "share_message";
    private static final String KEY_SHARE_MESSAGE_WHATSAPP = "share_message_whatsapp";
    private static final String KEY_SHARE_MESSAGE_FACEBOOK = "share_message_facebook";
    private static final String KEY_SHARE_MESSAGE_TWITTER = "share_message_twitter";
    private static final String KEY_SHARE_MESSAGE_INSTAGRAM = "share_message_instagram";
    private static final String KEY_SHARE_MESSAGE_EMAIL = "share_message_email";
    private static final String KEY_SHARE_MESSAGE_SMS = "share_message_sms";
    
    // Cache constants
    private static final String PREF_NAME = "share_message_cache";
    private static final String KEY_CACHE_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours
    
    // Placeholder patterns
    private static final String PLACEHOLDER_SENDER_NAME = "{{sender_name}}";
    private static final String PLACEHOLDER_RECIPIENT_NAME = "{{recipient_name}}";
    private static final String PLACEHOLDER_SHARE_URL = "{{share_url}}";
    
    // Singleton instance
    private static ShareMessageManager instance;
    
    private final Context context;
    private final FirebaseRemoteConfig remoteConfig;
    private final SharedPreferences sharedPreferences;
    
    /**
     * Private constructor to enforce singleton pattern
     * @param context Application context
     */
    private ShareMessageManager(Context context) {
        this.context = context.getApplicationContext();
        this.remoteConfig = FirebaseRemoteConfig.getInstance();
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Set default values for share messages
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(KEY_SHARE_MESSAGE, context.getString(R.string.share_wish_text));
        defaults.put(KEY_SHARE_MESSAGE_WHATSAPP, context.getString(R.string.share_wish_text_whatsapp));
        defaults.put(KEY_SHARE_MESSAGE_FACEBOOK, context.getString(R.string.share_wish_text_facebook));
        defaults.put(KEY_SHARE_MESSAGE_TWITTER, context.getString(R.string.share_wish_text_twitter));
        defaults.put(KEY_SHARE_MESSAGE_INSTAGRAM, context.getString(R.string.share_wish_text_instagram));
        defaults.put(KEY_SHARE_MESSAGE_EMAIL, context.getString(R.string.share_wish_text_email));
        defaults.put(KEY_SHARE_MESSAGE_SMS, context.getString(R.string.share_wish_text_sms));
        
        remoteConfig.setDefaultsAsync(defaults);
        
        Log.d(TAG, "ShareMessageManager initialized");
    }
    
    /**
     * Get singleton instance
     * @param context Application context
     * @return ShareMessageManager instance
     */
    public static synchronized ShareMessageManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ShareMessageManager(context);
        }
        return instance;
    }
    
    /**
     * Get share message for a specific platform with placeholders replaced
     * @param senderName Sender's name
     * @param recipientName Recipient's name
     * @param shareUrl Share URL
     * @param platform Platform identifier (whatsapp, facebook, etc.)
     * @return Processed share message
     */
    public String getShareMessage(String senderName, String recipientName, String shareUrl, String platform) {
        // First check if we have a valid cached message
        String cachedMessage = getCachedMessage(platform);
        if (cachedMessage != null) {
            Log.d(TAG, "Using cached share message for platform: " + platform);
            return processPlaceholders(cachedMessage, senderName, recipientName, shareUrl);
        }
        
        // If no valid cache, use the message from Remote Config
        String configKey = getConfigKeyForPlatform(platform);
        String message = remoteConfig.getString(configKey);
        
        // If message is empty, use default message
        if (message.isEmpty()) {
            message = getDefaultMessageForPlatform(platform);
            Log.d(TAG, "Using default share message for platform: " + platform);
        } else {
            Log.d(TAG, "Using remote config share message for platform: " + platform);
            // Cache the message for future use
            saveToCache(platform, message);
        }
        
        return processPlaceholders(message, senderName, recipientName, shareUrl);
    }
    
    /**
     * Get share message with placeholders replaced (uses default platform)
     * @param senderName Sender's name
     * @param recipientName Recipient's name
     * @param shareUrl Share URL
     * @return Processed share message
     */
    public String getShareMessage(String senderName, String recipientName, String shareUrl) {
        return getShareMessage(senderName, recipientName, shareUrl, null);
    }
    
    /**
     * Fetch latest share messages from Remote Config
     * @return Task for the operation
     */
    public Task<Boolean> fetchAndActivate() {
        Log.d(TAG, "Fetching share messages from Remote Config");
        return remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Config params updated: " + updated);
                        
                        if (updated) {
                            // Clear cache if config was updated
                            clearCache();
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch remote config", task.getException());
                    }
                });
    }
    
    /**
     * Process placeholders in the message
     * @param message Message with placeholders
     * @param senderName Sender's name
     * @param recipientName Recipient's name
     * @param shareUrl Share URL
     * @return Processed message
     */
    private String processPlaceholders(String message, String senderName, String recipientName, String shareUrl) {
        if (message == null) return "";
        
        // Replace placeholders with actual values
        String processed = message;
        
        if (senderName != null && !senderName.isEmpty()) {
            processed = processed.replace(PLACEHOLDER_SENDER_NAME, senderName);
        }
        
        if (recipientName != null && !recipientName.isEmpty()) {
            processed = processed.replace(PLACEHOLDER_RECIPIENT_NAME, recipientName);
        }
        
        if (shareUrl != null && !shareUrl.isEmpty()) {
            processed = processed.replace(PLACEHOLDER_SHARE_URL, shareUrl);
        }
        
        return processed;
    }
    
    /**
     * Get cached message if valid
     * @param platform Platform identifier
     * @return Cached message or null if not valid
     */
    @Nullable
    private String getCachedMessage(String platform) {
        // Check if cache is valid
        if (!isCacheValid()) {
            Log.d(TAG, "Cache expired, clearing");
            clearCache();
            return null;
        }
        
        // Get cached message for platform
        String key = getCacheKeyForPlatform(platform);
        return sharedPreferences.getString(key, null);
    }
    
    /**
     * Check if cache is still valid
     * @return True if cache is valid
     */
    private boolean isCacheValid() {
        long timestamp = sharedPreferences.getLong(KEY_CACHE_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        
        // Cache is valid if less than CACHE_DURATION_MS has passed
        return (currentTime - timestamp) < CACHE_DURATION_MS;
    }
    
    /**
     * Save message to cache
     * @param platform Platform identifier
     * @param message Message to cache
     */
    private void saveToCache(String platform, String message) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // Save the message
        String key = getCacheKeyForPlatform(platform);
        editor.putString(key, message);
        
        // Update timestamp if not already set
        if (!sharedPreferences.contains(KEY_CACHE_TIMESTAMP)) {
            editor.putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis());
        }
        
        editor.apply();
        Log.d(TAG, "Saved message to cache for platform: " + platform);
    }
    
    /**
     * Clear all cached messages
     */
    public void clearCache() {
        sharedPreferences.edit().clear().apply();
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Get Remote Config key for platform
     * @param platform Platform identifier
     * @return Config key
     */
    private String getConfigKeyForPlatform(String platform) {
        if (platform == null) return KEY_SHARE_MESSAGE;
        
        switch (platform.toLowerCase()) {
            case "whatsapp":
                return KEY_SHARE_MESSAGE_WHATSAPP;
            case "facebook":
                return KEY_SHARE_MESSAGE_FACEBOOK;
            case "twitter":
                return KEY_SHARE_MESSAGE_TWITTER;
            case "instagram":
                return KEY_SHARE_MESSAGE_INSTAGRAM;
            case "email":
                return KEY_SHARE_MESSAGE_EMAIL;
            case "sms":
                return KEY_SHARE_MESSAGE_SMS;
            default:
                return KEY_SHARE_MESSAGE;
        }
    }
    
    /**
     * Get cache key for platform
     * @param platform Platform identifier
     * @return Cache key
     */
    private String getCacheKeyForPlatform(String platform) {
        if (platform == null) return "message_default";
        return "message_" + platform.toLowerCase();
    }
    
    /**
     * Get default message for platform from resources
     * @param platform Platform identifier
     * @return Default message
     */
    private String getDefaultMessageForPlatform(String platform) {
        if (platform == null) return context.getString(R.string.share_wish_text);
        
        switch (platform.toLowerCase()) {
            case "whatsapp":
                return context.getString(R.string.share_wish_text_whatsapp);
            case "facebook":
                return context.getString(R.string.share_wish_text_facebook);
            case "twitter":
                return context.getString(R.string.share_wish_text_twitter);
            case "instagram":
                return context.getString(R.string.share_wish_text_instagram);
            case "email":
                return context.getString(R.string.share_wish_text_email);
            case "sms":
                return context.getString(R.string.share_wish_text_sms);
            default:
                return context.getString(R.string.share_wish_text);
        }
    }
} 