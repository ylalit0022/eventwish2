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
              " with deviceId: " + (deviceId != null ? deviceId.substring(0, 6) + "..." : "null"));
        
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
        
        apiService.recordSponsoredAdImpression(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Impression recorded for ad: " + adId);
                    callback.onSuccess(new ArrayList<>());
                } else {
                    String errorMsg = "Error recording impression: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                String errorMsg = "Network error recording impression: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
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
        
        apiService.recordSponsoredAdClick(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Click recorded for ad: " + adId);
                    callback.onSuccess(new ArrayList<>());
                } else {
                    String errorMsg = "Error recording click: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                String errorMsg = "Network error recording click: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
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