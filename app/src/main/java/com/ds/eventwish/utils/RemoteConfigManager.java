package com.ds.eventwish.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.BuildConfig;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for Firebase Remote Config operations
 * Handles app update checking during development and production
 */
public class RemoteConfigManager {
    private static final String TAG = "RemoteConfigManager";
    
    // Remote Config keys
    public static final String KEY_LATEST_VERSION_CODE = "latest_version_code";
    public static final String KEY_LATEST_VERSION_NAME = "latest_version_name";
    public static final String KEY_UPDATE_MESSAGE = "update_message";
    public static final String KEY_FORCE_UPDATE = "force_update";
    public static final String KEY_UPDATE_URL = "update_url";
    
    // Default values
    private static final Map<String, Object> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put(KEY_LATEST_VERSION_CODE, BuildConfig.VERSION_CODE);
        DEFAULTS.put(KEY_LATEST_VERSION_NAME, BuildConfig.VERSION_NAME);
        DEFAULTS.put(KEY_UPDATE_MESSAGE, "A new version of the app is available.");
        DEFAULTS.put(KEY_FORCE_UPDATE, false);
        DEFAULTS.put(KEY_UPDATE_URL, "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
    }
    
    // Cache expiration
    private static final long CACHE_EXPIRATION_SECONDS = 3600; // 1 hour
    
    // Singleton instance
    private static RemoteConfigManager instance;
    
    private final FirebaseRemoteConfig remoteConfig;
    private final Context context;
    
    // LiveData for update availability
    private final MutableLiveData<Boolean> isUpdateAvailable = new MutableLiveData<>(false);
    
    /**
     * Interface for update callbacks
     */
    public interface UpdateCheckCallback {
        void onUpdateAvailable(boolean isForceUpdate, String versionName, String updateMessage);
        void onUpdateNotAvailable();
        void onError(Exception exception);
    }
    
    private RemoteConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.remoteConfig = FirebaseRemoteConfig.getInstance();
        
        // Configure Remote Config
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : CACHE_EXPIRATION_SECONDS)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        
        // Set default values
        remoteConfig.setDefaultsAsync(DEFAULTS);
        
        Log.d(TAG, "RemoteConfigManager initialized with defaults: " + DEFAULTS);
    }
    
    /**
     * Get singleton instance
     * @param context Application context
     * @return RemoteConfigManager instance
     */
    public static synchronized RemoteConfigManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new RemoteConfigManager(context);
        }
        return instance;
    }
    
    /**
     * Fetch and activate remote config values
     * @return Task for the operation
     */
    public Task<Boolean> fetchAndActivate() {
        Log.d(TAG, "Fetching remote config values");
        return remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Config params updated: " + updated);
                        
                        // Log all values after fetch
                        Log.d(TAG, "Remote Config values after fetch:");
                        Log.d(TAG, "- Latest version code: " + remoteConfig.getLong(KEY_LATEST_VERSION_CODE));
                        Log.d(TAG, "- Latest version name: " + remoteConfig.getString(KEY_LATEST_VERSION_NAME));
                        Log.d(TAG, "- Update message: " + remoteConfig.getString(KEY_UPDATE_MESSAGE));
                        Log.d(TAG, "- Force update: " + remoteConfig.getBoolean(KEY_FORCE_UPDATE));
                        Log.d(TAG, "- Update URL: " + remoteConfig.getString(KEY_UPDATE_URL));
                        
                        // Check for updates after fetching
                        checkForUpdates(null);
                    } else {
                        Log.e(TAG, "Failed to fetch remote config", task.getException());
                    }
                });
    }
    
    /**
     * Check if an update is available
     * @param callback Callback for update check result
     */
    public void checkForUpdates(UpdateCheckCallback callback) {
        try {
            // Get current app version
            int currentVersionCode = BuildConfig.VERSION_CODE;
            String currentVersionName = BuildConfig.VERSION_NAME;
            
            // Get latest version from Remote Config
            long latestVersionCode = remoteConfig.getLong(KEY_LATEST_VERSION_CODE);
            String latestVersionName = remoteConfig.getString(KEY_LATEST_VERSION_NAME);
            String updateMessage = remoteConfig.getString(KEY_UPDATE_MESSAGE);
            boolean forceUpdate = remoteConfig.getBoolean(KEY_FORCE_UPDATE);
            String updateUrl = remoteConfig.getString(KEY_UPDATE_URL);
            
            Log.d(TAG, "Remote Config values:");
            Log.d(TAG, "- Current version: " + currentVersionCode + " (" + currentVersionName + ")");
            Log.d(TAG, "- Latest version: " + latestVersionCode + " (" + latestVersionName + ")");
            Log.d(TAG, "- Update message: " + updateMessage);
            Log.d(TAG, "- Force update: " + forceUpdate);
            Log.d(TAG, "- Update URL: " + updateUrl);
            
            // Track the update check event
            trackUpdateCheck(currentVersionCode, latestVersionCode);
            
            // Check if update is available
            boolean updateAvailable = latestVersionCode > currentVersionCode;
            Log.d(TAG, "Update available: " + updateAvailable);
            isUpdateAvailable.postValue(updateAvailable);
            
            if (updateAvailable) {
                Log.d(TAG, "Update available: " + latestVersionName);
                
                // Track update available impression
                trackUpdateAvailableImpression(latestVersionCode, latestVersionName, forceUpdate);
                
                if (callback != null) {
                    Log.d(TAG, "Calling onUpdateAvailable callback");
                    callback.onUpdateAvailable(forceUpdate, latestVersionName, updateMessage);
                } else {
                    Log.d(TAG, "Callback is null, not showing update dialog");
                }
            } else {
                Log.d(TAG, "No update available");
                if (callback != null) {
                    callback.onUpdateNotAvailable();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Track update check event
     */
    private void trackUpdateCheck(long currentVersion, long latestVersion) {
        Bundle params = new Bundle();
        params.putLong("current_version_code", currentVersion);
        params.putLong("latest_version_code", latestVersion);
        params.putString("current_version_name", BuildConfig.VERSION_NAME);
        params.putString("latest_version_name", remoteConfig.getString(KEY_LATEST_VERSION_NAME));
        params.putBoolean("is_update_available", latestVersion > currentVersion);
        
        AnalyticsUtils.getInstance().trackEvent("update_check", params);
    }
    
    /**
     * Track update available impression
     */
    private void trackUpdateAvailableImpression(long latestVersionCode, String latestVersionName, boolean isForceUpdate) {
        Bundle params = new Bundle();
        params.putLong("current_version_code", BuildConfig.VERSION_CODE);
        params.putLong("latest_version_code", latestVersionCode);
        params.putString("current_version_name", BuildConfig.VERSION_NAME);
        params.putString("latest_version_name", latestVersionName);
        params.putBoolean("is_force_update", isForceUpdate);
        
        AnalyticsUtils.getInstance().trackEvent("update_available_impression", params);
    }
    
    /**
     * Track update prompt action
     */
    public void trackUpdatePromptAction(String action, String latestVersionName) {
        Bundle params = new Bundle();
        params.putString("action", action); // "accepted", "declined", "deferred"
        params.putLong("current_version_code", BuildConfig.VERSION_CODE);
        params.putString("current_version_name", BuildConfig.VERSION_NAME);
        params.putString("latest_version_name", latestVersionName);
        
        AnalyticsUtils.getInstance().trackEvent("update_prompt_action", params);
    }
    
    /**
     * Get the latest version name
     * @return Latest version name from Remote Config
     */
    public String getLatestVersionName() {
        return remoteConfig.getString(KEY_LATEST_VERSION_NAME);
    }
    
    /**
     * Get the update message
     * @return Update message from Remote Config
     */
    public String getUpdateMessage() {
        return remoteConfig.getString(KEY_UPDATE_MESSAGE);
    }
    
    /**
     * Get the update URL
     * @return Update URL from Remote Config
     */
    public String getUpdateUrl() {
        return remoteConfig.getString(KEY_UPDATE_URL);
    }
    
    /**
     * Check if force update is required
     * @return True if force update is required
     */
    public boolean isForceUpdate() {
        return remoteConfig.getBoolean(KEY_FORCE_UPDATE);
    }
    
    /**
     * Get update availability LiveData
     * @return LiveData for update availability
     */
    public LiveData<Boolean> getIsUpdateAvailable() {
        return isUpdateAvailable;
    }
    
    /**
     * Verify that Firebase Remote Config is properly initialized
     * This method will log the current values and can be called for debugging
     */
    public void verifyRemoteConfigSetup() {
        Log.d(TAG, "Verifying Remote Config setup...");
        
        try {
            // Log default values
            Log.d(TAG, "Default values:");
            for (Map.Entry<String, Object> entry : DEFAULTS.entrySet()) {
                Log.d(TAG, "- " + entry.getKey() + ": " + entry.getValue());
            }
            
            // Log current values
            Log.d(TAG, "Current values:");
            Log.d(TAG, "- " + KEY_LATEST_VERSION_CODE + ": " + remoteConfig.getLong(KEY_LATEST_VERSION_CODE));
            Log.d(TAG, "- " + KEY_LATEST_VERSION_NAME + ": " + remoteConfig.getString(KEY_LATEST_VERSION_NAME));
            Log.d(TAG, "- " + KEY_UPDATE_MESSAGE + ": " + remoteConfig.getString(KEY_UPDATE_MESSAGE));
            Log.d(TAG, "- " + KEY_FORCE_UPDATE + ": " + remoteConfig.getBoolean(KEY_FORCE_UPDATE));
            Log.d(TAG, "- " + KEY_UPDATE_URL + ": " + remoteConfig.getString(KEY_UPDATE_URL));
            
            // Verify if values match defaults or have been updated
            boolean allValuesAreDefaults = true;
            for (Map.Entry<String, Object> entry : DEFAULTS.entrySet()) {
                String key = entry.getKey();
                Object defaultValue = entry.getValue();
                
                if (defaultValue instanceof Long) {
                    long currentValue = remoteConfig.getLong(key);
                    long defaultLong = (Long) defaultValue;
                    if (currentValue != defaultLong) {
                        allValuesAreDefaults = false;
                        Log.d(TAG, "Value for " + key + " has been updated from default " + 
                              defaultLong + " to " + currentValue);
                    }
                } else if (defaultValue instanceof String) {
                    String currentValue = remoteConfig.getString(key);
                    String defaultString = (String) defaultValue;
                    if (!currentValue.equals(defaultString)) {
                        allValuesAreDefaults = false;
                        Log.d(TAG, "Value for " + key + " has been updated from default '" + 
                              defaultString + "' to '" + currentValue + "'");
                    }
                } else if (defaultValue instanceof Boolean) {
                    boolean currentValue = remoteConfig.getBoolean(key);
                    boolean defaultBool = (Boolean) defaultValue;
                    if (currentValue != defaultBool) {
                        allValuesAreDefaults = false;
                        Log.d(TAG, "Value for " + key + " has been updated from default " + 
                              defaultBool + " to " + currentValue);
                    }
                }
            }
            
            if (allValuesAreDefaults) {
                Log.w(TAG, "All Remote Config values are still defaults! Make sure you've configured values in the Firebase Console.");
            } else {
                Log.d(TAG, "Remote Config has updated values from the server.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying Remote Config setup", e);
        }
    }
    
    /**
     * Force fetch the latest values from Firebase Remote Config with zero cache expiration
     * This is useful for debugging and testing
     * @return Task for the operation
     */
    public Task<Boolean> forceFetchAndActivate() {
        Log.d(TAG, "Force fetching remote config values with zero cache expiration");
        
        // Set minimum fetch interval to 0 to always fetch from the server
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        
        return remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Force fetch successful. Config params updated: " + updated);
                        
                        // Log all values after fetch
                        Log.d(TAG, "Remote Config values after force fetch:");
                        Log.d(TAG, "- Latest version code: " + remoteConfig.getLong(KEY_LATEST_VERSION_CODE));
                        Log.d(TAG, "- Latest version name: " + remoteConfig.getString(KEY_LATEST_VERSION_NAME));
                        Log.d(TAG, "- Update message: " + remoteConfig.getString(KEY_UPDATE_MESSAGE));
                        Log.d(TAG, "- Force update: " + remoteConfig.getBoolean(KEY_FORCE_UPDATE));
                        Log.d(TAG, "- Update URL: " + remoteConfig.getString(KEY_UPDATE_URL));
                        
                        // Check for updates after fetching
                        checkForUpdates(null);
                    } else {
                        Log.e(TAG, "Failed to force fetch remote config", task.getException());
                    }
                });
    }
} 