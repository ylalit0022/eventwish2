package com.ds.eventwish.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.ads.AdConstants;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.repository.AdMobRepository;
import com.ds.eventwish.utils.DeviceUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for AdMob functionality
 * This class serves as a facade for interacting with AdMob
 */
public class AdMobManager {
    private static final String TAG = "AdMobManager";
    
    private static AdMobManager instance;
    private final Context context;
    private final AdMobRepository repository;
    private final SharedPreferences preferences;
    
    private final MutableLiveData<Boolean> isInitializedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isEnabledLiveData = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    
    private boolean isInitialized = false;
    private boolean isInitializing = false;
    
    /**
     * Private constructor
     * @param context Application context
     */
    private AdMobManager(Context context) {
        this.context = context.getApplicationContext();
        this.repository = AdMobRepository.getInstance(context);
        this.preferences = context.getSharedPreferences(
                AdConstants.Preferences.PREF_FILE, Context.MODE_PRIVATE);
        
        // Check if ads are enabled
        boolean adsEnabled = preferences.getBoolean(AdConstants.Preferences.AD_FEATURE_ENABLED, true);
        isEnabledLiveData.setValue(adsEnabled);
    }
    
    /**
     * Initialize AdMobManager singleton
     * @param context Application context
     */
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AdMobManager(context);
        }
    }
    
    /**
     * Get singleton instance
     * @return AdMobManager instance
     */
    public static synchronized AdMobManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AdMobManager must be initialized first. Call init(Context)");
        }
        return instance;
    }
    
    /**
     * Initialize AdMob SDK
     * This is separate from init() to allow lazy loading of Google Mobile Ads SDK
     */
    public void initialize() {
        if (isInitialized || isInitializing) {
            Log.d(TAG, "AdMob already initialized or initializing");
            return;
        }
        
        isInitializing = true;
        Log.d(TAG, "Initializing AdMob");
        
        // Check if ads are enabled
        if (!isEnabled()) {
            Log.d(TAG, "Ads are disabled. Skipping initialization.");
            isInitializing = false;
            return;
        }
        
        try {
            // In a real implementation, this would initialize the Google Mobile Ads SDK
            // For now, we're just setting up our own infrastructure
            
            // Fetch initial ad units
            repository.fetchAdUnits(null);
            
            isInitialized = true;
            isInitializing = false;
            isInitializedLiveData.setValue(true);
            Log.d(TAG, "AdMob initialized successfully");
        } catch (Exception e) {
            isInitializing = false;
            errorLiveData.setValue("Failed to initialize AdMob: " + e.getMessage());
            Log.e(TAG, "Failed to initialize AdMob", e);
        }
    }
    
    /**
     * Check if AdMob is initialized
     * @return True if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Get initialization state LiveData
     * @return LiveData containing initialization state
     */
    public LiveData<Boolean> getInitializationState() {
        return isInitializedLiveData;
    }
    
    /**
     * Check if ads are enabled
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        Boolean enabled = isEnabledLiveData.getValue();
        return enabled != null && enabled;
    }
    
    /**
     * Get enabled state LiveData
     * @return LiveData containing enabled state
     */
    public LiveData<Boolean> getEnabledState() {
        return isEnabledLiveData;
    }
    
    /**
     * Enable or disable ads
     * @param enabled True to enable ads, false to disable
     */
    public void setEnabled(boolean enabled) {
        isEnabledLiveData.setValue(enabled);
        preferences.edit().putBoolean(AdConstants.Preferences.AD_FEATURE_ENABLED, enabled).apply();
        repository.setFeatureEnabled(enabled);
    }
    
    /**
     * Set test mode
     * @param enabled True to enable test mode, false otherwise
     */
    public void setTestMode(boolean enabled) {
        preferences.edit().putBoolean(AdConstants.Preferences.TEST_MODE_ENABLED, enabled).apply();
        repository.setTestMode(enabled);
    }
    
    /**
     * Check if test mode is enabled
     * @return True if test mode is enabled, false otherwise
     */
    public boolean isTestModeEnabled() {
        return repository.isTestModeEnabled();
    }
    
    /**
     * Get available ad units
     * @return LiveData containing list of available ad units
     */
    public LiveData<List<AdUnit>> getAdUnits() {
        // Initialize AdMob if not already initialized
        if (!isInitialized && !isInitializing) {
            initialize();
        }
        return repository.getAdUnits();
    }
    
    /**
     * Fetch ad units for a specific type
     * @param adType Ad type to filter by, or null for all types
     */
    public void fetchAdUnits(String adType) {
        // Initialize AdMob if not already initialized
        if (!isInitialized && !isInitializing) {
            initialize();
        }
        
        if (!isEnabled()) {
            Log.d(TAG, "Ads are disabled. Skipping fetch.");
            return;
        }
        
        repository.fetchAdUnits(adType);
    }
    
    /**
     * Get error LiveData
     * @return LiveData containing error messages
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }
    
    /**
     * Get loading state LiveData
     * @return LiveData containing loading state
     */
    public LiveData<Boolean> isLoading() {
        return repository.isLoading();
    }
    
    /**
     * Track ad impression
     * @param adUnitId Ad unit ID
     */
    public void trackImpression(String adUnitId) {
        if (!isEnabled()) {
            Log.d(TAG, "Ads are disabled. Skipping impression tracking.");
            return;
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", System.currentTimeMillis());
        context.put("deviceInfo", DeviceUtils.getDetailedDeviceInfo(this.context));
        
        repository.recordImpression(adUnitId, context, null);
    }
    
    /**
     * Track ad click
     * @param adUnitId Ad unit ID
     */
    public void trackClick(String adUnitId) {
        if (!isEnabled()) {
            Log.d(TAG, "Ads are disabled. Skipping click tracking.");
            return;
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", System.currentTimeMillis());
        context.put("deviceInfo", DeviceUtils.getDetailedDeviceInfo(this.context));
        
        repository.recordClick(adUnitId, context, null);
    }
    
    /**
     * Process reward for rewarded ad
     * @param adUnitId Ad unit ID
     * @param callback Callback for reward processing
     */
    public void processReward(String adUnitId, retrofit2.Callback<com.google.gson.JsonObject> callback) {
        if (!isEnabled()) {
            Log.d(TAG, "Ads are disabled. Skipping reward processing.");
            if (callback != null) {
                callback.onFailure(null, new Exception("Ads are disabled"));
            }
            return;
        }
        
        repository.processReward(adUnitId, callback);
    }
    
    /**
     * Track engagement with an ad
     * @param adUnitId Ad unit ID
     * @param actionType Type of action (click, view, etc.)
     * @param duration Duration of engagement in milliseconds
     */
    public void trackEngagement(String adUnitId, String actionType, int duration) {
        // Initialize AdMob if not already initialized
        if (!isInitialized && !isInitializing) {
            initialize();
        }
        
        if (!isEnabled()) {
            Log.d(TAG, "Ads are disabled. Skipping engagement tracking.");
            return;
        }
        
        Map<String, Object> engagementData = new HashMap<>();
        engagementData.put("adUnitId", adUnitId);
        engagementData.put("actionType", actionType);
        engagementData.put("duration", duration);
        engagementData.put("timestamp", System.currentTimeMillis());
        engagementData.put("testMode", isTestModeEnabled());
        
        // Use the fallback method that handles missing endpoints
        repository.recordEngagementWithFallback(engagementData);
        
        // Try to sync any stored engagement data
        repository.syncStoredEngagementData();
    }
    
    /**
     * Get ad unit ID for a given ad type
     * @param adType Ad type (banner, interstitial, etc.)
     * @return Ad unit ID or test ID if in test mode
     */
    public String getAdUnitId(String adType) {
        // Return test ad unit ID if in test mode
        if (isTestModeEnabled()) {
            return getTestAdUnitId(adType);
        }
        
        // Get cached ad units
        List<AdUnit> adUnits = repository.getAdUnits().getValue();
        if (adUnits != null) {
            // Find first matching ad unit
            for (AdUnit adUnit : adUnits) {
                if (adUnit.getAdType().equals(adType) && adUnit.isStatus() && adUnit.isCanShow()) {
                    return adUnit.getAdUnitCode();
                }
            }
        }
        
        // Fall back to test ID if no matching ad unit found
        return getTestAdUnitId(adType);
    }
    
    /**
     * Get test ad unit ID for a given ad type
     * @param adType Ad type (banner, interstitial, etc.)
     * @return Test ad unit ID
     */
    private String getTestAdUnitId(String adType) {
        if (adType == null) {
            return AdConstants.TestAdUnits.BANNER_TEST_ID;
        }
        
        switch (adType) {
            case AdConstants.AdType.BANNER:
                return AdConstants.TestAdUnits.BANNER_TEST_ID;
            case AdConstants.AdType.INTERSTITIAL:
                return AdConstants.TestAdUnits.INTERSTITIAL_TEST_ID;
            case AdConstants.AdType.REWARDED:
                return AdConstants.TestAdUnits.REWARDED_TEST_ID;
            case AdConstants.AdType.NATIVE:
                return AdConstants.TestAdUnits.NATIVE_ADVANCED_TEST_ID;
            case AdConstants.AdType.APP_OPEN:
                return AdConstants.TestAdUnits.APP_OPEN_TEST_ID;
            default:
                return AdConstants.TestAdUnits.BANNER_TEST_ID;
        }
    }
    
    /**
     * Clear cached ad data
     */
    public void clearCache() {
        repository.clearCache();
    }
} 