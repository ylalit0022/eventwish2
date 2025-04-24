package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.AdUnitDao;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private AdUnitDao adUnitDao;
    private ExecutorService executor;
    
    private final MutableLiveData<List<AdUnit>> _adUnits = new MutableLiveData<>();
    private final MutableLiveData<Map<String, AdMobResponse.AdStatus>> _adStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    
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
                secureTokenManager = null;
            }
            
            // Get DeviceUtils instance if available
            try {
                deviceUtils = DeviceUtils.getInstance();
            } catch (Exception e) {
                Log.w(TAG, "DeviceUtils not initialized yet: " + e.getMessage());
                deviceUtils = null;
            }
            
            // Initialize database
            AppDatabase db = AppDatabase.getInstance(context);
            adUnitDao = db.adUnitDao();
            
            // Initialize executor for background operations
            executor = Executors.newSingleThreadExecutor();
            
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
        return _adUnits;
    }

    /**
     * Get ad status LiveData
     * @return LiveData containing ad status
     */
    public LiveData<Map<String, AdMobResponse.AdStatus>> getAdStatus() {
        return _adStatus;
    }

    /**
     * Get loading state LiveData
     * @return LiveData containing loading state
     */
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    /**
     * Get error LiveData
     * @return LiveData containing error messages
     */
    public LiveData<String> getError() {
        return _error;
    }

    /**
     * Fetch ad units from the API and store in database
     * @param adType Ad type to filter by, or null for all types
     */
    public void fetchAdUnits(String adType) {
        if (System.currentTimeMillis() - lastRequestTime < AdConstants.RequestSettings.MIN_REQUEST_INTERVAL) {
            Log.d(TAG, "Throttling request, too soon since last request");
            return;
        }
        
        _isLoading.setValue(true);
        lastRequestTime = System.currentTimeMillis();
        
        Map<String, String> headers = prepareHeaders();
        Call<AdMobResponse> call = apiService.getAdUnits(headers, adType);
        
        call.enqueue(new Callback<AdMobResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdMobResponse> call, @NonNull Response<AdMobResponse> response) {
                _isLoading.setValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    AdMobResponse.AdMobData data = response.body().getData();
                    if (data != null) {
                        List<AdUnit> adUnits = data.getAdUnits();
                        if (adUnits != null) {
                            // Convert to entities and save to database
                            List<AdUnitEntity> entities = new ArrayList<>();
                            for (AdUnit unit : adUnits) {
                                entities.add(AdUnitEntity.fromAdUnit(unit));
                            }
                            
                            executor.execute(() -> {
                                if (adType != null) {
                                    adUnitDao.refreshAdUnitsByType(adType, entities);
                                } else {
                                    adUnitDao.refreshAdUnits(entities);
                                }
                            });
                            
                            _adUnits.setValue(adUnits);
                            Log.d(TAG, "Successfully fetched and saved " + adUnits.size() + " ad units");
                        } else {
                            _adUnits.setValue(Collections.emptyList());
                            Log.d(TAG, "Successfully fetched ad units, but list is null");
                        }
                    } else {
                        String error = "Response successful but data is null";
                        Log.e(TAG, error);
                        updateError(error);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        String error = "Failed to fetch ad units: " + response.code() + " - " + errorBody;
                        Log.e(TAG, error);
                        updateError(error);
                        
                        // Load from database as fallback
                        loadFromDatabase(adType);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<AdMobResponse> call, @NonNull Throwable t) {
                _isLoading.setValue(false);
                String error = "Network error when fetching ad units: " + t.getMessage();
                Log.e(TAG, error, t);
                updateError(error);
                
                // Load from database as fallback
                loadFromDatabase(adType);
            }
        });
    }

    /**
     * Load ad units from database
     * @param adType Ad type to filter by, or null for all types
     */
    private void loadFromDatabase(String adType) {
        Log.d(TAG, "Loading ad units from database for type: " + adType);
        
        LiveData<List<AdUnitEntity>> source = adType != null ? 
            adUnitDao.getAdUnitsByType(adType) : 
            adUnitDao.getAllAdUnits();
            
        // Convert entities to models
        source.observeForever(entities -> {
            if (entities != null) {
                List<AdUnit> adUnits = new ArrayList<>();
                for (AdUnitEntity entity : entities) {
                    adUnits.add(entity.toAdUnit());
                }
                _adUnits.setValue(adUnits);
                Log.d(TAG, "Loaded " + adUnits.size() + " ad units from database");
            } else {
                _adUnits.setValue(Collections.emptyList());
                Log.d(TAG, "No ad units found in database");
            }
        });
    }

    /**
     * Get ad units by type from database
     * @param adType Ad type to filter by
     * @return LiveData containing ad units
     */
    public LiveData<List<AdUnit>> getAdUnitsByType(String adType) {
        MediatorLiveData<List<AdUnit>> result = new MediatorLiveData<>();
        
        result.addSource(adUnitDao.getAdUnitsByType(adType), entities -> {
            if (entities != null) {
                List<AdUnit> adUnits = new ArrayList<>();
                for (AdUnitEntity entity : entities) {
                    adUnits.add(entity.toAdUnit());
                }
                result.setValue(adUnits);
            } else {
                result.setValue(Collections.emptyList());
            }
        });
        
        return result;
    }

    /**
     * Get available ad units by type from database
     * @param adType Ad type to filter by
     * @return LiveData containing available ad units
     */
    public LiveData<List<AdUnit>> getAvailableAdUnitsByType(String adType) {
        MediatorLiveData<List<AdUnit>> result = new MediatorLiveData<>();
        
        result.addSource(adUnitDao.getAvailableAdUnitsByType(adType), entities -> {
            if (entities != null) {
                List<AdUnit> adUnits = new ArrayList<>();
                for (AdUnitEntity entity : entities) {
                    adUnits.add(entity.toAdUnit());
                }
                result.setValue(adUnits);
            } else {
                result.setValue(Collections.emptyList());
            }
        });
        
        return result;
    }

    /**
     * Get highest priority ad unit by type
     * @param adType Ad type to filter by
     * @return Highest priority ad unit or null if none found
     */
    public AdUnit getHighestPriorityAdUnit(String adType) {
        if (adType == null) {
            Log.e(TAG, "getHighestPriorityAdUnit: adType is null");
            return null;
        }

        try {
            return executor.submit(() -> {
                AdUnitEntity entity = adUnitDao.getHighestPriorityAdUnit(adType);
                return entity != null ? entity.toAdUnit() : null;
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Error getting highest priority ad unit for type " + adType, e);
            return null;
        }
    }

    /**
     * Update ad unit status in database
     * @param id Ad unit ID
     * @param canShow Whether the ad can be shown
     * @param reason Reason why the ad cannot be shown (if applicable)
     */
    public void updateAdUnitStatus(String id, boolean canShow, String reason) {
        executor.execute(() -> {
            adUnitDao.updateAdUnitStatus(id, canShow, reason);
        });
    }

    /**
     * Update next available time for ad unit
     * @param id Ad unit ID
     * @param nextAvailable Next available time (ISO 8601 format)
     */
    public void updateNextAvailable(String id, String nextAvailable) {
        executor.execute(() -> {
            adUnitDao.updateNextAvailable(id, nextAvailable);
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
        
        _isLoading.setValue(true);
        lastRequestTime = System.currentTimeMillis();
        
        Map<String, String> headers = prepareHeaders();
        Call<AdMobResponse> call = apiService.getAdStatus(headers, adType);
        
        call.enqueue(new Callback<AdMobResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdMobResponse> call, @NonNull Response<AdMobResponse> response) {
                _isLoading.setValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    AdMobResponse.AdMobData data = response.body().getData();
                    if (data != null) {
                        Map<String, AdMobResponse.AdStatus> adStatus = data.getAdStatus();
                        if (adStatus != null) {
                            updateAdStatus(adStatus);
                            Log.d(TAG, "Successfully fetched ad status for " + adStatus.size() + " ad units");
                        } else {
                            updateAdStatus(Collections.emptyMap());
                            Log.d(TAG, "Successfully fetched ad status, but map is null");
                        }
                    } else {
                        updateAdStatus(Collections.emptyMap());
                        Log.d(TAG, "Successfully fetched ad status, but data is null");
                    }
                    
                    // Reset retry count on success
                    retryCount = 0;
                } else {
                    updateError("Failed to fetch ad status: " + response.message());
                    Log.e(TAG, "Failed to fetch ad status: " + response.code() + " " + response.message());
                    
                    // Handle retry if needed
                    handleRetry(adType);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdMobResponse> call, @NonNull Throwable t) {
                _isLoading.setValue(false);
                updateError("Network error: " + t.getMessage());
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
     * @param callback Response callback (optional)
     */
    public void recordImpression(String adUnitId, Map<String, Object> context, Callback<JsonObject> callback) {
        if (apiService == null) {
            String error = "Cannot record impression: API service not initialized";
            Log.e(TAG, "❌ " + error);
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException(error));
            }
            return;
        }

        // Validate API key before making request
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            String error = "Cannot record impression: API key is missing";
            Log.e(TAG, "❌ " + error);
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException(error));
            }
            return;
        }

        // Prepare headers early so we can use them in the callback
        final Map<String, String> headers = prepareHeaders();
        if (!headers.containsKey(AdConstants.Headers.API_KEY)) {
            String error = "Cannot record impression: API key header is missing";
            Log.e(TAG, "❌ " + error);
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException(error));
            }
            return;
        }

        // Create empty callback if none provided
        final Callback<JsonObject> finalCallback = callback != null ? callback : new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Impression recorded successfully for ad unit: " + adUnitId);
                } else {
                    String errorMsg;
                    switch (response.code()) {
                        case 401:
                            String authHeader = headers.get(AdConstants.Headers.API_KEY);
                            errorMsg = "Authentication failed (401):\n" +
                                     "- API Key: " + (apiKey.isEmpty() ? "Missing" : "Present") + "\n" +
                                     "- Bearer Token: " + (authHeader != null && authHeader.startsWith("Bearer") ? "Present" : "Missing") + "\n" +
                                     "Please check API key configuration and format";
                            break;
                        case 403:
                            errorMsg = "Authorization failed (403):\n" +
                                     "- App Signature: " + (headers.get(AdConstants.Headers.APP_SIGNATURE) != null ? "Present" : "Missing") + "\n" +
                                     "Please verify app signature";
                            break;
                        case 404:
                            errorMsg = "Endpoint not found (404) - Please check server configuration";
                            break;
                        default:
                            errorMsg = "Failed to record impression: " + response.code();
                    }
                    Log.e(TAG, "❌ " + errorMsg);

                    // Try to get more details from error body
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "Error details: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "❌ Network error recording impression", t);
            }
        };
        
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
            
            // Log request details
            Log.d(TAG, "📤 Sending impression request for ad unit: " + adUnitId);
            Log.d(TAG, "🔑 Request headers: API Key present: " + (headers.containsKey(AdConstants.Headers.API_KEY)));
            
            // Make API call
            Call<JsonObject> call = apiService.recordImpression(headers, requestBody);
            call.enqueue(finalCallback);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error preparing impression request", e);
            finalCallback.onFailure(null, e);
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
        
        // Add API key first (most important)
        String apiKey = getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put(AdConstants.Headers.API_KEY, apiKey); // Remove Bearer prefix
            Log.d(TAG, "Using API key: " + apiKey);
        } else {
            Log.e(TAG, "❌ No API key available from SecureTokenManager");
        }
        
        // Add device ID
        String deviceId = deviceUtils != null ? deviceUtils.getDeviceId() : null;
        if (deviceId == null) {
            deviceId = preferences.getString("device_id", null);
        }
        
        if (deviceId != null && !deviceId.isEmpty()) {
            headers.put(AdConstants.Headers.DEVICE_ID, deviceId);
            Log.d(TAG, "Using device ID: " + deviceId);
        } else {
            Log.w(TAG, "⚠️ No device ID available");
        }
        
        // Add app signature
        String signature = AdConstants.Signature.APP_SIGNATURE;
        if (signature != null && !signature.isEmpty()) {
            headers.put(AdConstants.Headers.APP_SIGNATURE, signature);
            Log.d(TAG, "Using app signature: " + signature);
        }
        
        // Add content type
        headers.put("Content-Type", "application/json");
        
        // Log complete headers for debugging (mask sensitive data)
        StringBuilder headerLog = new StringBuilder("Request Headers:\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String value = entry.getValue();
            if (entry.getKey().equals(AdConstants.Headers.API_KEY)) {
                value = value.substring(0, Math.min(value.length(), 10)) + "...";
            }
            headerLog.append(entry.getKey()).append(": ").append(value).append("\n");
        }
        Log.d(TAG, headerLog.toString());
        
        return headers;
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
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
        Log.d(TAG, "=== Clearing AdMob Repository Cache ===");
        
        // Clear shared preferences
        preferences.edit()
            .remove(AdConstants.Preferences.CACHED_AD_UNITS)
            .remove(AdConstants.Preferences.LAST_AD_FETCH_TIME)
            .apply();
            
        // Clear database
        executor.execute(() -> {
            try {
                adUnitDao.deleteAllAdUnits();
                Log.d(TAG, "Successfully cleared ad units from database");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing ad units from database", e);
            }
        });
        
        // Clear live data
        _adUnits.setValue(Collections.emptyList());
        
        Log.d(TAG, "Ad cache cleared successfully");
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
     * Load cached ad units from SharedPreferences
     */
    private void loadCachedAdUnits() {
        String cachedJson = preferences.getString(AdConstants.Preferences.CACHED_AD_UNITS, null);
        if (cachedJson != null) {
            try {
                Type listType = new TypeToken<ArrayList<AdUnit>>(){}.getType();
                List<AdUnit> cachedAdUnits = gson.fromJson(cachedJson, listType);
                if (cachedAdUnits != null && !cachedAdUnits.isEmpty()) {
                    _adUnits.setValue(cachedAdUnits);
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
     * Fetch ad units with fallback for signature errors
     * @param adType Ad type to filter by, or null for all types
     */
    public void fetchAdUnitsWithFallback(String adType) {
        Log.d(TAG, "=== Fetching Ad Units with Fallback ===");
        Log.d(TAG, "Requested Ad Type: " + adType);

        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "No API key available from SecureTokenManager");
            // Load from database as fallback
            loadAdUnitsFromDatabase();
            return;
        }

        // Prepare headers
        Map<String, String> headers = prepareHeaders();
        
        // Start API request
        Log.d(TAG, "Starting API request for ad units");
        apiService.getAdUnits(headers, adType)
            .enqueue(new Callback<AdMobResponse>() {
                @Override
                public void onResponse(Call<AdMobResponse> call, Response<AdMobResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        AdMobResponse adMobResponse = response.body();
                        AdMobResponse.AdMobData data = adMobResponse.getData();
                        
                        if (data != null && data.getAdUnits() != null) {
                            Log.d(TAG, "API request successful, received " + data.getAdUnits().size() + " ad units");

                            // Save to database
                            if (!data.getAdUnits().isEmpty()) {
                                executor.execute(() -> {
                                    try {
                                        List<AdUnitEntity> entities = new ArrayList<>();
                                        for (AdUnit unit : data.getAdUnits()) {
                                            entities.add(AdUnitEntity.fromAdUnit(unit));
                                        }
                                        adUnitDao.insertAdUnits(entities);
                                        Log.d(TAG, "Successfully saved ad units to database");
                                        // Reload from database to update LiveData
                                        loadAdUnitsFromDatabase();
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error saving ad units to database", e);
                                    }
                                });
                            } else {
                                Log.w(TAG, "API response contained no ad units");
                                loadAdUnitsFromDatabase();
                            }
                        } else {
                            Log.w(TAG, "API response data or ad units is null");
                            loadAdUnitsFromDatabase();
                        }
                    } else {
                        Log.e(TAG, "API request failed with code: " + response.code());
                        loadAdUnitsFromDatabase();
                    }
                }

                @Override
                public void onFailure(Call<AdMobResponse> call, Throwable t) {
                    Log.e(TAG, "API request failed", t);
                    loadAdUnitsFromDatabase();
                }
            });
    }

    private void loadAdUnitsFromDatabase() {
        Log.d(TAG, "Loading ad units from database");
        
        // Execute database query on background thread
        executor.execute(() -> {
            try {
                // Get entities directly from DAO
                List<AdUnitEntity> entities = adUnitDao.getAdUnitsSync();
                
                if (entities != null) {
                    List<AdUnit> adUnits = new ArrayList<>();
                    for (AdUnitEntity entity : entities) {
                        adUnits.add(entity.toAdUnit());
                    }
                    
                    // Post value on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        _adUnits.setValue(adUnits);
                        Log.d(TAG, "Loaded " + adUnits.size() + " ad units from database");
                    });
                } else {
                    // Post empty list on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        _adUnits.setValue(new ArrayList<>());
                        Log.d(TAG, "No ad units found in database");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading ad units from database", e);
                // Post empty list on error
                new Handler(Looper.getMainLooper()).post(() -> {
                    _adUnits.setValue(new ArrayList<>());
                });
            }
        });
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

    /**
     * Get the AdUnitDao for direct database operations
     * @return AdUnitDao instance
     */
    public AdUnitDao getAdUnitDao() {
        return adUnitDao;
    }

    /**
     * Get API key from SecureTokenManager
     * @return API key or null if not available
     */
    public String getApiKey() {
        if (secureTokenManager != null) {
            String apiKey = secureTokenManager.getApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                return apiKey;
            }
            Log.w(TAG, "No API key available from SecureTokenManager");
        } else {
            Log.w(TAG, "SecureTokenManager is null");
        }
        return null;
    }

    private void saveAdUnits(List<AdUnitEntity> adUnits) {
        if (adUnits == null || adUnits.isEmpty()) {
            Log.w(TAG, "No ad units to save");
            return;
        }

        executor.execute(() -> {
            try {
                // Delete existing ad units and insert new ones in a transaction
                adUnitDao.deleteAllSync();
                adUnitDao.insertAdUnitsSync(adUnits);
                
                // Convert entities to models before posting to LiveData
                List<AdUnit> models = new ArrayList<>();
                for (AdUnitEntity entity : adUnits) {
                    models.add(entity.toAdUnit());
                }
                
                // Post the updated list to LiveData on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    _adUnits.postValue(models);
                    Log.d(TAG, "Successfully saved " + adUnits.size() + " ad units");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving ad units: " + e.getMessage());
                // Try to load existing ad units from database as fallback
                try {
                    List<AdUnitEntity> existingAdUnits = adUnitDao.getAdUnitsSync();
                    if (!existingAdUnits.isEmpty()) {
                        // Convert entities to models before posting
                        List<AdUnit> existingModels = new ArrayList<>();
                        for (AdUnitEntity entity : existingAdUnits) {
                            existingModels.add(entity.toAdUnit());
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            _adUnits.postValue(existingModels);
                            Log.d(TAG, "Loaded " + existingAdUnits.size() + " existing ad units as fallback");
                        });
                    }
                } catch (Exception dbError) {
                    Log.e(TAG, "Failed to load existing ad units: " + dbError.getMessage());
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        new Handler(Looper.getMainLooper()).post(() -> _isLoading.postValue(loading));
    }

    private void updateAdStatus(Map<String, AdMobResponse.AdStatus> status) {
        new Handler(Looper.getMainLooper()).post(() -> _adStatus.postValue(status));
    }

    private void updateError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> _error.postValue(error));
    }
} 