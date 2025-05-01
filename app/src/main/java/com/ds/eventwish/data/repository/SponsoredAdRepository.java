package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.model.response.SponsoredAdResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.DeviceUtils;
import com.ds.eventwish.EventWishApplication;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing sponsored ads from the API
 */
public class SponsoredAdRepository {
    private static final String TAG = "SponsoredAdRepository";
    private final ApiService apiService;
    private final MutableLiveData<List<SponsoredAd>> sponsoredAdsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final String deviceId;
    
    // Cache expiration
    private long lastFetchTime = 0;
    private static final long CACHE_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(15); // 15 minutes
    
    public SponsoredAdRepository() {
        this.apiService = ApiClient.getClient();
        this.deviceId = DeviceUtils.getDeviceId(EventWishApplication.getInstance());
        Log.d(TAG, "Initialized with deviceId: " + (deviceId != null ? deviceId : "null"));
    }
    
    /**
     * Callback interface for sponsored ad operations
     */
    public interface SponsoredAdCallback {
        /**
         * Called when the operation is successful
         * @param ads List of sponsored ads (may be empty)
         */
        void onSuccess(@NonNull List<SponsoredAd> ads);
        
        /**
         * Called when the operation fails
         * @param message Error message
         */
        void onError(@NonNull String message);
    }
    
    /**
     * Get all active sponsored ads from the server
     * @return LiveData of sponsored ads list
     */
    public LiveData<List<SponsoredAd>> getSponsoredAds() {
        return getSponsoredAdsForLocation(null);
    }
    
    /**
     * Get sponsored ads for a specific location
     * @param location The location filter, or null for all locations
     * @return LiveData of sponsored ads list
     */
    public LiveData<List<SponsoredAd>> getSponsoredAdsForLocation(String location) {
        // Check if we need to refresh
        boolean shouldRefresh = System.currentTimeMillis() - lastFetchTime > CACHE_EXPIRATION_MS;
        
        if (shouldRefresh || sponsoredAdsLiveData.getValue() == null || sponsoredAdsLiveData.getValue().isEmpty()) {
            fetchSponsoredAds(location);
        }
        
        return sponsoredAdsLiveData;
    }
    
    /**
     * Fetch sponsored ads from the server
     * @param location The location filter, or null for all locations
     */
    private void fetchSponsoredAds(String location) {
        loadingLiveData.setValue(true);
        
        Log.d(TAG, "Fetching sponsored ads for location: " + (location != null ? location : "all") + 
              " with deviceId: " + (deviceId != null ? deviceId.substring(0, Math.min(6, deviceId.length())) + "..." : "null"));
        
        apiService.getSponsoredAds(location, deviceId).enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                loadingLiveData.postValue(false);
                lastFetchTime = System.currentTimeMillis();
                
                if (response.isSuccessful() && response.body() != null) {
                    SponsoredAdResponse adResponse = response.body();
                    
                    if (adResponse.isSuccess() && adResponse.getAds() != null) {
                        sponsoredAdsLiveData.postValue(adResponse.getAds());
                        Log.d(TAG, "Loaded " + adResponse.getAds().size() + " sponsored ads for location: " + 
                              (location != null ? location : "all"));
                        
                        // Log frequency cap and metrics for each ad
                        for (SponsoredAd ad : adResponse.getAds()) {
                            Log.d(TAG, "Ad: " + ad.getId() + ", Title: " + ad.getTitle() +
                                  ", Frequency Cap: " + ad.getFrequencyCap() +
                                  ", Daily Frequency Cap: " + ad.getDailyFrequencyCap());
                            
                            if (ad.getMetrics() != null) {
                                Log.d(TAG, "Ad Metrics - Device Impressions: " + ad.getMetrics().getDeviceImpressions() +
                                      ", Daily Impressions: " + ad.getMetrics().getDeviceDailyImpressions() +
                                      ", Frequency Capped: " + ad.getMetrics().isFrequencyCapped() +
                                      ", Daily Frequency Capped: " + ad.getMetrics().isDailyFrequencyCapped());
                            }
                        }
                    } else {
                        errorLiveData.postValue(adResponse.getMessage() != null ? 
                                adResponse.getMessage() : "No ads available");
                        Log.w(TAG, "Error loading sponsored ads: " + 
                               (adResponse.getError() != null ? adResponse.getError() : adResponse.getMessage()));
                        
                        // Keep any existing ads rather than clearing them
                        if (sponsoredAdsLiveData.getValue() == null || sponsoredAdsLiveData.getValue().isEmpty()) {
                            sponsoredAdsLiveData.postValue(new ArrayList<>());
                        }
                    }
                } else {
                    String errorMsg = "Server error: " + (response.code() == 404 ? "Endpoint not found" : 
                                    "Status " + response.code());
                    errorLiveData.postValue(errorMsg);
                    Log.e(TAG, errorMsg);
                    
                    // Keep any existing ads rather than clearing them
                    if (sponsoredAdsLiveData.getValue() == null || sponsoredAdsLiveData.getValue().isEmpty()) {
                        sponsoredAdsLiveData.postValue(new ArrayList<>());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                String errorMsg = "Network error: " + t.getMessage();
                errorLiveData.postValue(errorMsg);
                Log.e(TAG, errorMsg, t);
                
                // Keep any existing ads rather than clearing them
                if (sponsoredAdsLiveData.getValue() == null || sponsoredAdsLiveData.getValue().isEmpty()) {
                    sponsoredAdsLiveData.postValue(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * Get sponsored ads with callback instead of LiveData
     * @param callback Callback for success or error
     */
    public void getSponsoredAdsWithCallback(SponsoredAdCallback callback) {
        getSponsoredAdsWithCallback(null, callback);
    }
    
    /**
     * Get sponsored ads for a specific location with callback
     * @param location The location filter, or null for all locations
     * @param callback Callback for success or error
     */
    public void getSponsoredAdsWithCallback(String location, SponsoredAdCallback callback) {
        loadingLiveData.setValue(true);
        
        apiService.getSponsoredAds(location, deviceId).enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                loadingLiveData.postValue(false);
                lastFetchTime = System.currentTimeMillis();
                
                if (response.isSuccessful() && response.body() != null) {
                    SponsoredAdResponse adResponse = response.body();
                    
                    if (adResponse.isSuccess() && adResponse.getAds() != null) {
                        List<SponsoredAd> ads = adResponse.getAds();
                        sponsoredAdsLiveData.postValue(ads);
                        callback.onSuccess(ads);
                        Log.d(TAG, "Loaded " + ads.size() + " sponsored ads for location: " + 
                              (location != null ? location : "all"));
                    } else {
                        String errorMsg = adResponse.getMessage() != null ? 
                                adResponse.getMessage() : "No ads available";
                        errorLiveData.postValue(errorMsg);
                        callback.onError(errorMsg);
                        Log.w(TAG, "Error loading sponsored ads: " + 
                               (adResponse.getError() != null ? adResponse.getError() : adResponse.getMessage()));
                    }
                } else {
                    String errorMsg = "Server error: " + (response.code() == 404 ? "Endpoint not found" : 
                                    "Status " + response.code());
                    errorLiveData.postValue(errorMsg);
                    callback.onError(errorMsg);
                    Log.e(TAG, errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                String errorMsg = "Network error: " + t.getMessage();
                errorLiveData.postValue(errorMsg);
                callback.onError(errorMsg);
                Log.e(TAG, errorMsg, t);
            }
        });
    }
    
    /**
     * Record ad impression
     * @param adId The ID of the ad
     * @param callback Callback for success or error
     */
    public void recordAdImpression(String adId, SponsoredAdCallback callback) {
        if (adId == null || adId.isEmpty()) {
            callback.onError("Invalid ad ID");
            return;
        }
        
        Log.d(TAG, "⭐ Recording impression for ad: " + adId + " with deviceId: " + 
              (deviceId != null ? deviceId.substring(0, Math.min(6, deviceId.length())) + "..." : "null"));
        
        apiService.recordSponsoredAdImpression(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    
                    // Log detailed response information
                    Log.d(TAG, "✅ Impression recorded successfully for ad: " + adId);
                    Log.d(TAG, "URL: " + call.request().url());
                    Log.d(TAG, "Response: " + responseBody);
                    
                    try {
                        // Extract tracking metrics with improved parsing
                        StringBuilder trackingInfo = new StringBuilder();
                        trackingInfo.append("📊 Impression Tracking Info for ad ").append(adId).append(":\n");
                        
                        // Impression counts (total, device, daily)
                        int impressionCount = getIntField(responseBody, "impression_count", "impressionCount", 0);
                        int deviceImpressions = getIntField(responseBody, "device_impressions", "deviceImpressions", 0);
                        int dailyCount = getIntField(responseBody, "daily_count", "dailyCount", 0);
                        
                        trackingInfo.append("  • Total Impressions: ").append(impressionCount).append("\n");
                        trackingInfo.append("  • Device Impressions: ").append(deviceImpressions).append("\n");
                        trackingInfo.append("  • Today's Impressions: ").append(dailyCount).append("\n");
                        
                        // Was this impression actually counted?
                        boolean wasCounted = getBooleanField(responseBody, "was_counted", "wasCounted", true);
                        trackingInfo.append("  • Impression Counted: ").append(wasCounted ? "YES ✓" : "NO ✗").append("\n");
                        
                        // Capping information
                        boolean isCapped = getBooleanField(responseBody, "is_capped", "isCapped", false);
                        boolean isDailyCapped = getBooleanField(responseBody, "is_daily_capped", "isDailyCapped", false);
                        int frequencyCap = getIntField(responseBody, "frequency_cap", "frequencyCap", 0);
                        int dailyFrequencyCap = getIntField(responseBody, "daily_frequency_cap", "dailyFrequencyCap", 0);
                        
                        trackingInfo.append("  • Frequency Cap: ").append(frequencyCap > 0 ? frequencyCap : "Unlimited").append("\n");
                        trackingInfo.append("  • Daily Cap: ").append(dailyFrequencyCap > 0 ? dailyFrequencyCap : "Unlimited").append("\n");
                        trackingInfo.append("  • Is Capped: ").append(isCapped ? "YES ⚠️" : "NO").append("\n");
                        trackingInfo.append("  • Is Daily Capped: ").append(isDailyCapped ? "YES ⚠️" : "NO").append("\n");
                        
                        // Calculate remaining impressions
                        if (frequencyCap > 0) {
                            int remaining = Math.max(0, frequencyCap - deviceImpressions);
                            trackingInfo.append("  • Remaining Total Impressions: ").append(remaining).append("\n");
                        }
                        
                        if (dailyFrequencyCap > 0) {
                            int remaining = Math.max(0, dailyFrequencyCap - dailyCount);
                            trackingInfo.append("  • Remaining Daily Impressions: ").append(remaining).append("\n");
                        }
                        
                        Log.i(TAG, trackingInfo.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing impression response fields: " + e.getMessage(), e);
                    }
                    
                    callback.onSuccess(new ArrayList<>());
                } else {
                    String errorMsg = "Error recording impression: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    Log.e(TAG, "URL: " + call.request().url());
                    
                    // Try to extract error details
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error response: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                String errorMsg = "Network error recording impression: " + t.getMessage();
                Log.e(TAG, "❌ " + errorMsg);
                Log.e(TAG, "URL: " + call.request().url());
                Log.e(TAG, "Error details", t);
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Record ad click
     * @param adId The ID of the ad
     * @param callback Callback for success or error
     */
    public void recordAdClick(String adId, SponsoredAdCallback callback) {
        if (adId == null || adId.isEmpty()) {
            callback.onError("Invalid ad ID");
            return;
        }
        
        Log.d(TAG, "⭐ Recording click for ad: " + adId + " with deviceId: " + 
              (deviceId != null ? deviceId.substring(0, Math.min(6, deviceId.length())) + "..." : "null"));
        
        apiService.recordSponsoredAdClick(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    
                    // Log detailed response information
                    Log.d(TAG, "✅ Click recorded successfully for ad: " + adId);
                    Log.d(TAG, "URL: " + call.request().url());
                    Log.d(TAG, "Response: " + responseBody);
                    
                    try {
                        // Extract tracking metrics with improved parsing
                        StringBuilder trackingInfo = new StringBuilder();
                        trackingInfo.append("📊 Click Tracking Info for ad ").append(adId).append(":\n");
                        
                        // Click counts (total, device)
                        int clickCount = getIntField(responseBody, "click_count", "clickCount", 0);
                        int deviceClicks = getIntField(responseBody, "device_clicks", "deviceClicks", 0);
                        int delta = getIntField(responseBody, "delta", "delta", 1);
                        
                        trackingInfo.append("  • Total Clicks: ").append(clickCount).append("\n");
                        trackingInfo.append("  • Device Clicks: ").append(deviceClicks).append("\n");
                        trackingInfo.append("  • Change: ").append(delta > 0 ? "+" + delta : delta).append("\n");
                        
                        // Reference impression data
                        int impressionCount = getIntField(responseBody, "impression_count", "impressionCount", 0);
                        trackingInfo.append("  • Total Impressions: ").append(impressionCount).append("\n");
                        
                        // Engagement metrics
                        float ctr = getFloatField(responseBody, "ctr", "ctr", 0);
                        float deviceCtr = getFloatField(responseBody, "device_ctr", "deviceCtr", 0);
                        
                        trackingInfo.append("  • Overall CTR: ").append(String.format("%.2f%%", ctr)).append("\n");
                        trackingInfo.append("  • Device CTR: ").append(String.format("%.2f%%", deviceCtr)).append("\n");
                        
                        Log.i(TAG, trackingInfo.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing click response fields: " + e.getMessage(), e);
                    }
                    
                    callback.onSuccess(new ArrayList<>());
                } else {
                    String errorMsg = "Error recording click: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    Log.e(TAG, "URL: " + call.request().url());
                    
                    // Try to extract error details
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error response: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                String errorMsg = "Network error recording click: " + t.getMessage();
                Log.e(TAG, "❌ " + errorMsg);
                Log.e(TAG, "URL: " + call.request().url());
                Log.e(TAG, "Error details", t);
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Helper method to safely extract int fields from response with fallback to camelCase
     */
    private int getIntField(JsonObject json, String snakeCaseKey, String camelCaseKey, int defaultValue) {
        try {
            if (json.has(snakeCaseKey) && !json.get(snakeCaseKey).isJsonNull()) {
                return json.get(snakeCaseKey).getAsInt();
            } else if (json.has(camelCaseKey) && !json.get(camelCaseKey).isJsonNull()) {
                return json.get(camelCaseKey).getAsInt();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse int field " + snakeCaseKey + "/" + camelCaseKey + ": " + e.getMessage());
        }
        return defaultValue;
    }
    
    /**
     * Helper method to safely extract boolean fields from response with fallback to camelCase
     */
    private boolean getBooleanField(JsonObject json, String snakeCaseKey, String camelCaseKey, boolean defaultValue) {
        try {
            if (json.has(snakeCaseKey) && !json.get(snakeCaseKey).isJsonNull()) {
                return json.get(snakeCaseKey).getAsBoolean();
            } else if (json.has(camelCaseKey) && !json.get(camelCaseKey).isJsonNull()) {
                return json.get(camelCaseKey).getAsBoolean();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse boolean field " + snakeCaseKey + "/" + camelCaseKey + ": " + e.getMessage());
        }
        return defaultValue;
    }
    
    /**
     * Helper method to safely extract float fields from response with fallback to camelCase
     */
    private float getFloatField(JsonObject json, String snakeCaseKey, String camelCaseKey, float defaultValue) {
        try {
            if (json.has(snakeCaseKey) && !json.get(snakeCaseKey).isJsonNull()) {
                return json.get(snakeCaseKey).getAsFloat();
            } else if (json.has(camelCaseKey) && !json.get(camelCaseKey).isJsonNull()) {
                return json.get(camelCaseKey).getAsFloat();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse float field " + snakeCaseKey + "/" + camelCaseKey + ": " + e.getMessage());
        }
        return defaultValue;
    }
    
    /**
     * Get loading state
     * @return LiveData of loading state
     */
    public LiveData<Boolean> getLoadingState() {
        return loadingLiveData;
    }
    
    /**
     * Get error state
     * @return LiveData of error message
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }
} 