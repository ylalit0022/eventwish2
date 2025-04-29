package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.CustomKeysAndValues;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Manager class for Firebase Crashlytics integration
 */
public class FirebaseCrashManager {
    private static final String TAG = "FirebaseCrashManager";
    
    private static FirebaseCrashlytics crashlytics;
    private static boolean isInitialized = false;
    
    /**
     * Initialize Firebase Crashlytics
     * @param context Application context
     */
    public static void init(@NonNull Context context) {
        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            
            // Enable Crashlytics data collection
            crashlytics.setCrashlyticsCollectionEnabled(true);
            
            // Set default keys
            setDefaultKeys();
            
            isInitialized = true;
            Log.d(TAG, "Firebase Crashlytics initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Crashlytics", e);
            isInitialized = false;
        }
    }
    
    /**
     * Set default keys for crash reports
     */
    private static void setDefaultKeys() {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            // Set device information and app version as custom keys
            CustomKeysAndValues keysAndValues = new CustomKeysAndValues.Builder()
                .putString("device_brand", DeviceUtils.getDeviceBrand())
                .putString("device_model", DeviceUtils.getDeviceModel())
                .putString("os_version", DeviceUtils.getAndroidVersion())
                .putString("app_version", DeviceUtils.getAppVersionName())
                .putInt("app_version_code", DeviceUtils.getAppVersionCode())
                .build();
            
            crashlytics.setCustomKeys(keysAndValues);
            
            Log.d(TAG, "Default crash keys set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set default crash keys", e);
        }
    }
    
    /**
     * Set user identifier for crash reports
     * @param userId User ID
     */
    public static void setUserId(@Nullable String userId) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        if (userId != null && !userId.isEmpty()) {
            crashlytics.setUserId(userId);
            Log.d(TAG, "User ID set for crash reports: " + userId);
        }
    }
    
    /**
     * Set a custom key for crash reports
     * @param key Key name
     * @param value String value
     */
    public static void setCustomKey(@NonNull String key, @Nullable String value) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            if (key != null && !key.isEmpty()) {
                crashlytics.setCustomKey(key, value != null ? value : "null");
                Log.d(TAG, "Custom key set: " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key: " + key, e);
        }
    }
    
    /**
     * Set a custom key for crash reports
     * @param key Key name
     * @param value Boolean value
     */
    public static void setCustomKey(@NonNull String key, boolean value) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            if (key != null && !key.isEmpty()) {
                crashlytics.setCustomKey(key, value);
                Log.d(TAG, "Custom key set: " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key: " + key, e);
        }
    }
    
    /**
     * Set a custom key for crash reports
     * @param key Key name
     * @param value Integer value
     */
    public static void setCustomKey(@NonNull String key, int value) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            if (key != null && !key.isEmpty()) {
                crashlytics.setCustomKey(key, value);
                Log.d(TAG, "Custom key set: " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key: " + key, e);
        }
    }
    
    /**
     * Set a custom key for crash reports
     * @param key Key name
     * @param value Long value
     */
    public static void setCustomKey(@NonNull String key, long value) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            if (key != null && !key.isEmpty()) {
                crashlytics.setCustomKey(key, value);
                Log.d(TAG, "Custom key set: " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key: " + key, e);
        }
    }
    
    /**
     * Set a custom key for crash reports
     * @param key Key name
     * @param value Float value
     */
    public static void setCustomKey(@NonNull String key, float value) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            if (key != null && !key.isEmpty()) {
                crashlytics.setCustomKey(key, value);
                Log.d(TAG, "Custom key set: " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key: " + key, e);
        }
    }
    
    /**
     * Set a custom key for crash reports
     * @param key Key name
     * @param value Double value
     */
    public static void setCustomKey(@NonNull String key, double value) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            if (key != null && !key.isEmpty()) {
                crashlytics.setCustomKey(key, value);
                Log.d(TAG, "Custom key set: " + key + " = " + value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key: " + key, e);
        }
    }
    
    /**
     * Log a non-fatal exception to Crashlytics
     * @param throwable The exception to log
     */
    public static void logException(@NonNull Throwable throwable) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            crashlytics.recordException(throwable);
            Log.d(TAG, "Exception logged to Crashlytics: " + throwable.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to log exception to Crashlytics", e);
        }
    }
    
    /**
     * Log a message to Crashlytics
     * @param message The message to log
     */
    public static void log(@NonNull String message) {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        try {
            crashlytics.log(message);
            Log.d(TAG, "Message logged to Crashlytics: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log message to Crashlytics", e);
        }
    }
    
    /**
     * Create a test crash to verify Crashlytics setup
     * Should only be used in debug builds
     */
    public static void testCrash() {
        if (!isInitialized || crashlytics == null) {
            Log.e(TAG, "Crashlytics not initialized. Call init() first.");
            return;
        }
        
        Log.w(TAG, "Forcing a test crash");
        throw new RuntimeException("Test Crash from FirebaseCrashManager");
    }
} 