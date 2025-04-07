package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.model.ads.AdConstants;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.model.response.AdMobResponse;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.util.SecureTokenManager;
import com.ds.eventwish.utils.DeviceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for AdMob operations
 */
public class AdMobRepository {
    private static final String TAG = "AdMobRepository";
    
    private static AdMobRepository instance;
    private Context context;
    private ApiService apiService;
    private SharedPreferences preferences;
    private Gson gson;
    private SecureTokenManager secureTokenManager;
    private DeviceUtils deviceUtils;
    
    private final MutableLiveData<List<AdUnit>> adUnitsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, AdMobResponse.AdStatus>> adStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    
    private long lastRequestTime = 0;
    private int retryCount = 0;
    private boolean isTestMode = false;
    private boolean isInitialized = false;

    /**
     * Get the singleton instance of AdMobRepository
     * @param context Context
     * @return AdMobRepository instance
     */
    public static synchronized AdMobRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AdMobRepository(context);
        }
        return instance;
    }

    /**
     * Constructor
     * @param context Context
     */
    private AdMobRepository(Context context) {
        try {
            this.context = context.getApplicationContext();
            
            // Initialize the API client
            try {
                apiService = ApiClient.getClient();
            } catch (Exception e) {
                Log.w(TAG, "Error getting API client: " + e.getMessage());
                apiService = null;
            }
            
            // Get SecureTokenManager instance if available
            try {
                secureTokenManager = SecureTokenManager.getInstance();
            } catch (IllegalStateException e) {
                Log.w(TAG, "SecureTokenManager not initialized yet: " + e.getMessage());
                // Initialize with default values, will be updated when SecureTokenManager is ready
                secureTokenManager = null;
            }
            
            // Get DeviceUtils instance if available
            try {
                deviceUtils = DeviceUtils.getInstance();
            } catch (Exception e) {
                Log.w(TAG, "DeviceUtils not initialized yet: " + e.getMessage());
                deviceUtils = null;
            }
            
            // Initialize cache
            initializeCache();
            
            isInitialized = true;
            Log.d(TAG, "AdMobRepository initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdMobRepository", e);
            isInitialized = false;
        }
    }

    /**
     * Set test mode
     * @param enabled True to enable test mode, false to disable
     */
    public void setTestMode(boolean enabled) {
        isTestMode = enabled;
        preferences.edit().putBoolean(AdConstants.Preferences.TEST_MODE_ENABLED, enabled).apply();
    }

    /**
     * Check if test mode is enabled
     * @return True if test mode is enabled, false otherwise
     */
    public boolean isTestModeEnabled() {
        return isTestMode;
    }

    /**
     * Get ad units LiveData
     * @return LiveData containing ad units
     */
    public LiveData<List<AdUnit>> getAdUnits() {
        return adUnitsLiveData;
    }

    /**
     * Get ad status LiveData
     * @return LiveData containing ad status
     */
    public LiveData<Map<String, AdMobResponse.AdStatus>> getAdStatus() {
        return adStatusLiveData;
    }

    /**
     * Get loading state LiveData
     * @return LiveData containing loading state
     */
    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }

    /**
     * Get error LiveData
     * @return LiveData containing error messages
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }

    /**
     * Fetch ad units from the API
     * @param adType Ad type to filter by, or null for all types
     */
    public void fetchAdUnits(String adType) {
        if (System.currentTimeMillis() - lastRequestTime < AdConstants.RequestSettings.MIN_REQUEST_INTERVAL) {
            Log.d(TAG, "Throttling request, too soon since last request");
            return;
        }
        
        isLoadingLiveData.setValue(true);
        lastRequestTime = System.currentTimeMillis();
        
        Map<String, String> headers = prepareHeaders();
        Call<AdMobResponse> call = apiService.getAdUnits(headers, adType);
        
        call.enqueue(new Callback<AdMobResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdMobResponse> call, @NonNull Response<AdMobResponse> response) {
                isLoadingLiveData.setValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    AdMobResponse.AdMobData data = response.body().getData();
                    if (data != null) {
                        List<AdUnit> adUnits = data.getAdUnits();
                        if (adUnits != null) {
                            adUnitsLiveData.setValue(adUnits);
                            // Cache the ad units
                            cacheAdUnits(adUnits);
                            Log.d(TAG, "Successfully fetched " + adUnits.size() + " ad units");
                        } else {
                            adUnitsLiveData.setValue(Collections.emptyList());
                            Log.d(TAG, "Successfully fetched ad units, but list is null");
                        }
                    } else {
                        adUnitsLiveData.setValue(Collections.emptyList());
                        Log.d(TAG, "Successfully fetched ad units, but data is null");
                    }
                    
                    // Reset retry count on success
                    retryCount = 0;
                } else {
                    errorLiveData.setValue("Failed to fetch ad units: " + response.message());
                    Log.e(TAG, "Failed to fetch ad units: " + response.code() + " " + response.message());
                    
                    // Handle retry if needed
                    handleRetry(adType);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdMobResponse> call, @NonNull Throwable t) {
                isLoadingLiveData.setValue(false);
                errorLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error fetching ad units", t);
                
                // Handle retry if needed
                handleRetry(adType);
            }
        });
    }

    /**
     * Fetch ad status from the API
     * @param adType Ad type to filter by, or null for all types
     */
    public void fetchAdStatus(String adType) {
        if (System.currentTimeMillis() - lastRequestTime < AdConstants.RequestSettings.MIN_REQUEST_INTERVAL) {
            Log.d(TAG, "Throttling request, too soon since last request");
            return;
        }
        
        isLoadingLiveData.setValue(true);
        lastRequestTime = System.currentTimeMillis();
        
        Map<String, String> headers = prepareHeaders();
        Call<AdMobResponse> call = apiService.getAdStatus(headers, adType);
        
        call.enqueue(new Callback<AdMobResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdMobResponse> call, @NonNull Response<AdMobResponse> response) {
                isLoadingLiveData.setValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    AdMobResponse.AdMobData data = response.body().getData();
                    if (data != null) {
                        Map<String, AdMobResponse.AdStatus> adStatus = data.getAdStatus();
                        if (adStatus != null) {
                            adStatusLiveData.setValue(adStatus);
                            Log.d(TAG, "Successfully fetched ad status for " + adStatus.size() + " ad units");
                        } else {
                            adStatusLiveData.setValue(Collections.emptyMap());
                            Log.d(TAG, "Successfully fetched ad status, but map is null");
                        }
                    } else {
                        adStatusLiveData.setValue(Collections.emptyMap());
                        Log.d(TAG, "Successfully fetched ad status, but data is null");
                    }
                    
                    // Reset retry count on success
                    retryCount = 0;
                } else {
                    errorLiveData.setValue("Failed to fetch ad status: " + response.message());
                    Log.e(TAG, "Failed to fetch ad status: " + response.code() + " " + response.message());
                    
                    // Handle retry if needed
                    handleRetry(adType);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdMobResponse> call, @NonNull Throwable t) {
                isLoadingLiveData.setValue(false);
                errorLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error fetching ad status", t);
                
                // Handle retry if needed
                handleRetry(adType);
            }
        });
    }

    /**
     * Record impression
     * @param adUnitId Ad unit ID
     * @param context Context data
     * @param callback Response callback
     */
    public void recordImpression(String adUnitId, Map<String, Object> context, Callback<JsonObject> callback) {
        if (apiService == null) {
            Log.e(TAG, "Cannot record impression: API service not initialized");
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("API service not initialized"));
            }
            return;
        }
        
        try {
            // Create request body
            Map<String, Object> body = new HashMap<>();
            body.put("adUnitId", adUnitId);
            body.put("context", context);
            
            // Add to request queue
            JsonObject requestBody = new JsonObject();
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                if (entry.getValue() instanceof String) {
                    requestBody.addProperty(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Number) {
                    requestBody.addProperty(entry.getKey(), (Number) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    requestBody.addProperty(entry.getKey(), (Boolean) entry.getValue());
                } else if (entry.getValue() instanceof Map) {
                    JsonObject contextObject = new JsonObject();
                    Map<String, Object> contextMap = (Map<String, Object>) entry.getValue();
                    for (Map.Entry<String, Object> contextEntry : contextMap.entrySet()) {
                        if (contextEntry.getValue() instanceof String) {
                            contextObject.addProperty(contextEntry.getKey(), (String) contextEntry.getValue());
                        } else if (contextEntry.getValue() instanceof Number) {
                            contextObject.addProperty(contextEntry.getKey(), (Number) contextEntry.getValue());
                        } else if (contextEntry.getValue() instanceof Boolean) {
                            contextObject.addProperty(contextEntry.getKey(), (Boolean) contextEntry.getValue());
                        }
                    }
                    requestBody.add(entry.getKey(), contextObject);
                }
            }
            
            // Make API call
            Map<String, String> headers = prepareHeaders();
            Call<JsonObject> call = apiService.recordImpression(headers, requestBody);
            call.enqueue(callback);
        } catch (Exception e) {
            Log.e(TAG, "Error recording impression", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    /**
     * Record click
     * @param adUnitId Ad unit ID
     * @param context Context data
     * @param callback Response callback
     */
    public void recordClick(String adUnitId, Map<String, Object> context, Callback<JsonObject> callback) {
        if (apiService == null) {
            Log.e(TAG, "Cannot record click: API service not initialized");
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("API service not initialized"));
            }
            return;
        }
        
        try {
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("adUnitId", adUnitId);
            
            JsonObject contextObj = new JsonObject();
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getValue() instanceof String) {
                    contextObj.addProperty(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Number) {
                    contextObj.addProperty(entry.getKey(), (Number) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    contextObj.addProperty(entry.getKey(), (Boolean) entry.getValue());
                }
            }
            requestBody.add("context", contextObj);
            
            // Make API call
            Map<String, String> headers = prepareHeaders();
            Call<JsonObject> call = apiService.recordClick(headers, requestBody);
            call.enqueue(callback);
        } catch (Exception e) {
            Log.e(TAG, "Error recording click", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    /**
     * Process reward
     * @param adUnitId Ad unit ID
     * @param callback Response callback
     */
    public void processReward(String adUnitId, Callback<JsonObject> callback) {
        if (apiService == null) {
            Log.e(TAG, "Cannot process reward: API service not initialized");
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("API service not initialized"));
            }
            return;
        }
        
        try {
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("adUnitId", adUnitId);
            requestBody.addProperty("timestamp", System.currentTimeMillis());
            
            // Make API call
            Map<String, String> headers = prepareHeaders();
            Call<JsonObject> call = apiService.processReward(headers, requestBody);
            call.enqueue(callback);
        } catch (Exception e) {
            Log.e(TAG, "Error processing reward", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    /**
     * Track user engagement
     * @param adUnitId Ad unit ID
     * @param type Engagement type
     * @param duration Duration in seconds
     * @param callback Response callback
     */
    public void trackEngagement(String adUnitId, String type, int duration, Callback<JsonObject> callback) {
        if (apiService == null) {
            Log.e(TAG, "Cannot track engagement: API service not initialized");
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("API service not initialized"));
            }
            return;
        }
        
        try {
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("adUnitId", adUnitId);
            requestBody.addProperty("type", type);
            requestBody.addProperty("duration", duration);
            requestBody.addProperty("timestamp", System.currentTimeMillis());
            
            // Make API call
            Map<String, String> headers = prepareHeaders();
            Call<JsonObject> call = apiService.trackEngagement(headers, requestBody);
            call.enqueue(callback);
        } catch (Exception e) {
            Log.e(TAG, "Error tracking engagement", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    /**
     * Check if AdMob feature is enabled
     * @return True if enabled, false otherwise
     */
    public boolean isFeatureEnabled() {
        return preferences.getBoolean(AdConstants.Preferences.AD_FEATURE_ENABLED, true);
    }

    /**
     * Enable or disable AdMob feature
     * @param enabled True to enable, false to disable
     */
    public void setFeatureEnabled(boolean enabled) {
        preferences.edit().putBoolean(AdConstants.Preferences.AD_FEATURE_ENABLED, enabled).apply();
    }

    /**
     * Prepare headers for API requests
     * @return Map of headers
     */
    private Map<String, String> prepareHeaders() {
        Map<String, String> headers = new HashMap<>();
        
        // Add device ID
        String deviceId = deviceUtils != null ? deviceUtils.getDeviceId() : null;
        if (deviceId == null) {
            deviceId = preferences.getString("device_id", null);
        }
        
        if (deviceId != null) {
            headers.put(AdConstants.Headers.DEVICE_ID, deviceId);
            Log.d(TAG, "Using device ID: " + deviceId);
        } else {
            Log.w(TAG, "No device ID available for headers");
        }
        
        // Add API key
        String apiKey = null;
        if (secureTokenManager != null) {
            apiKey = secureTokenManager.getApiKey();
            if (apiKey != null) {
                headers.put(AdConstants.Headers.API_KEY, apiKey);
                Log.d(TAG, "Using API key: " + apiKey);
            } else {
                Log.w(TAG, "No API key available from SecureTokenManager");
            }
        } else {
            Log.w(TAG, "SecureTokenManager is null, cannot get API key");
        }
        
        // Add app signature - use the verified signature that works with the server
        headers.put(AdConstants.Headers.APP_SIGNATURE, AdConstants.Signature.APP_SIGNATURE);
        Log.d(TAG, "Using app signature: " + AdConstants.Signature.APP_SIGNATURE);
        
        return headers;
    }

    /**
     * Load cached ad units from SharedPreferences
     */
    private void loadCachedAdUnits() {
        String cachedJson = preferences.getString(AdConstants.Preferences.CACHED_AD_UNITS, null);
        if (cachedJson != null) {
            try {
                Type listType = new TypeToken<ArrayList<AdUnit>>(){}.getType();
                List<AdUnit> cachedAdUnits = gson.fromJson(cachedJson, listType);
                if (cachedAdUnits != null && !cachedAdUnits.isEmpty()) {
                    adUnitsLiveData.setValue(cachedAdUnits);
                    Log.d(TAG, "Loaded " + cachedAdUnits.size() + " cached ad units");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading cached ad units", e);
            }
        }
    }

    /**
     * Cache ad units in SharedPreferences
     * @param adUnits Ad units to cache
     */
    private void cacheAdUnits(List<AdUnit> adUnits) {
        if (adUnits != null && !adUnits.isEmpty()) {
            String json = gson.toJson(adUnits);
            preferences.edit().putString(AdConstants.Preferences.CACHED_AD_UNITS, json).apply();
            preferences.edit().putLong(AdConstants.Preferences.LAST_AD_FETCH_TIME, System.currentTimeMillis()).apply();
            Log.d(TAG, "Cached " + adUnits.size() + " ad units");
        }
    }

    /**
     * Handle retry logic for API calls
     * @param adType Ad type for the retry
     */
    private void handleRetry(String adType) {
        if (retryCount < AdConstants.RequestSettings.MAX_RETRY_COUNT) {
            retryCount++;
            long delay = AdConstants.RequestSettings.RETRY_BACKOFF_MS * (long) Math.pow(2, retryCount - 1);
            Log.d(TAG, "Retrying in " + delay + "ms (attempt " + retryCount + " of " + AdConstants.RequestSettings.MAX_RETRY_COUNT + ")");
            
            // Use a Handler to retry after delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                fetchAdUnits(adType);
            }, delay);
        } else {
            retryCount = 0;
            Log.e(TAG, "Maximum retry attempts reached");
        }
    }

    /**
     * Get the API service for testing
     * @return ApiService
     */
    public ApiService getApiService() {
        return apiService;
    }

    /**
     * Clear all cached data
     */
    public void clearCache() {
        preferences.edit()
            .remove(AdConstants.Preferences.CACHED_AD_UNITS)
            .remove(AdConstants.Preferences.LAST_AD_FETCH_TIME)
            .apply();
        Log.d(TAG, "Cleared ad cache");
    }

    /**
     * Initialize cache
     */
    private void initializeCache() {
        try {
            // Initialize Gson
            gson = new Gson();
            
            // Initialize SharedPreferences
            preferences = context.getSharedPreferences(AdConstants.Preferences.PREF_FILE, Context.MODE_PRIVATE);
            
            // Enable test mode by default in debug mode
            isTestMode = preferences.getBoolean(AdConstants.Preferences.TEST_MODE_ENABLED, true);
            
            // Load cached ad units
            loadCachedAdUnits();
            
            Log.d(TAG, "Cache initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing cache", e);
        }
    }

    /**
     * Fetch ad units with fallback for signature errors
     * @param adType Ad type to filter by, or null for all types
     */
    public void fetchAdUnitsWithFallback(String adType) {
        isLoadingLiveData.setValue(true);
        lastRequestTime = System.currentTimeMillis();
        
        Map<String, String> headers = prepareHeaders();
        Call<AdMobResponse> call = apiService.getAdUnits(headers, adType);
        
        call.enqueue(new Callback<AdMobResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdMobResponse> call, @NonNull Response<AdMobResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Process successful response
                    processAdUnitsResponse(response);
                } else {
                    // API error
                    isLoadingLiveData.setValue(false);
                    errorLiveData.setValue("Error: " + response.code());
                    Log.e(TAG, "API error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<AdMobResponse> call, @NonNull Throwable t) {
                isLoadingLiveData.setValue(false);
                errorLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }
    
    /**
     * Process successful ad units response
     * @param response API response
     */
    private void processAdUnitsResponse(Response<AdMobResponse> response) {
        isLoadingLiveData.setValue(false);
        
        if (response.body() != null) {
            AdMobResponse.AdMobData data = response.body().getData();
            if (data != null) {
                List<AdUnit> adUnits = data.getAdUnits();
                if (adUnits != null) {
                    adUnitsLiveData.setValue(adUnits);
                    // Cache the ad units
                    cacheAdUnits(adUnits);
                    Log.d(TAG, "Successfully fetched " + adUnits.size() + " ad units");
                } else {
                    adUnitsLiveData.setValue(Collections.emptyList());
                    Log.d(TAG, "Successfully fetched ad units, but list is null");
                }
            } else {
                adUnitsLiveData.setValue(Collections.emptyList());
                Log.d(TAG, "API response success but data is null");
            }
        }
    }

    /**
     * Get the preferences for testing purposes
     * @return SharedPreferences
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }

    /**
     * Record user engagement with fallback
     * This method tries to send engagement data to the server
     * If the server endpoint is missing, it stores the data locally
     * 
     * @param engagementData Engagement data to record
     */
    public void recordEngagementWithFallback(Map<String, Object> engagementData) {
        if (apiService == null) {
            Log.e(TAG, "Cannot record engagement: API service not initialized");
            storeEngagementDataLocally(engagementData);
            return;
        }
        
        // Add timestamp if not present
        if (!engagementData.containsKey("timestamp")) {
            engagementData.put("timestamp", System.currentTimeMillis());
        }
        
        // Try to first use the admob/engagement endpoint (which exists)
        Map<String, String> headers = prepareHeaders();
        JsonObject jsonBody = new JsonObject();
        
        for (Map.Entry<String, Object> entry : engagementData.entrySet()) {
            if (entry.getValue() instanceof String) {
                jsonBody.addProperty(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Number) {
                jsonBody.addProperty(entry.getKey(), (Number) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                jsonBody.addProperty(entry.getKey(), (Boolean) entry.getValue());
            } else {
                try {
                    jsonBody.addProperty(entry.getKey(), gson.toJson(entry.getValue()));
                } catch (Exception e) {
                    Log.w(TAG, "Failed to serialize value for " + entry.getKey(), e);
                }
            }
        }
        
        apiService.trackEngagement(headers, jsonBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully recorded engagement: " + gson.toJson(engagementData));
                } else {
                    Log.w(TAG, "Failed to record engagement: " + response.code() + " - " + response.message());
                    storeEngagementDataLocally(engagementData);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error recording engagement", t);
                storeEngagementDataLocally(engagementData);
            }
        });
    }
    
    /**
     * Store engagement data locally
     * @param engagementData Engagement data to store
     */
    private void storeEngagementDataLocally(Map<String, Object> engagementData) {
        try {
            // Get existing stored data
            String storedJson = preferences.getString(AdConstants.Preferences.STORED_ENGAGEMENT_DATA, "[]");
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> storedData = gson.fromJson(storedJson, type);
            
            if (storedData == null) {
                storedData = new ArrayList<>();
            }
            
            // Add new data
            storedData.add(engagementData);
            
            // Limit storage size (keep last 100 records)
            if (storedData.size() > 100) {
                storedData = storedData.subList(storedData.size() - 100, storedData.size());
            }
            
            // Save back to preferences
            String updatedJson = gson.toJson(storedData);
            preferences.edit().putString(AdConstants.Preferences.STORED_ENGAGEMENT_DATA, updatedJson).apply();
            
            Log.d(TAG, "Stored engagement data locally (total: " + storedData.size() + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error storing engagement data locally", e);
        }
    }
    
    /**
     * Try to sync stored engagement data
     * This should be called periodically to attempt to send cached data
     */
    public void syncStoredEngagementData() {
        if (apiService == null) {
            Log.e(TAG, "Cannot sync engagement data: API service not initialized");
            return;
        }
        
        try {
            // Get stored data
            String storedJson = preferences.getString(AdConstants.Preferences.STORED_ENGAGEMENT_DATA, "[]");
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> storedData = gson.fromJson(storedJson, type);
            
            if (storedData == null || storedData.isEmpty()) {
                Log.d(TAG, "No stored engagement data to sync");
                return;
            }
            
            Log.d(TAG, "Attempting to sync " + storedData.size() + " stored engagement records");
            
            // Prepare batch request
            JsonObject batchRequest = new JsonObject();
            JsonArray records = new JsonArray();
            
            for (Map<String, Object> record : storedData) {
                JsonObject jsonRecord = new JsonObject();
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        jsonRecord.addProperty(entry.getKey(), (String) entry.getValue());
                    } else if (entry.getValue() instanceof Number) {
                        jsonRecord.addProperty(entry.getKey(), (Number) entry.getValue());
                    } else if (entry.getValue() instanceof Boolean) {
                        jsonRecord.addProperty(entry.getKey(), (Boolean) entry.getValue());
                    } else {
                        try {
                            jsonRecord.addProperty(entry.getKey(), gson.toJson(entry.getValue()));
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to serialize value for " + entry.getKey(), e);
                        }
                    }
                }
                records.add(jsonRecord);
            }
            
            batchRequest.add("records", records);
            
            // Try to send first to admob/engagement as batch
            Map<String, String> headers = prepareHeaders();
            apiService.trackEngagement(headers, batchRequest).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Successfully synced " + storedData.size() + " engagement records");
                        // Clear stored data on success
                        preferences.edit().putString(AdConstants.Preferences.STORED_ENGAGEMENT_DATA, "[]").apply();
                    } else {
                        Log.w(TAG, "Failed to sync engagement data: " + response.code() + " - " + response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Log.e(TAG, "Network error syncing engagement data", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error syncing stored engagement data", e);
        }
    }
} 