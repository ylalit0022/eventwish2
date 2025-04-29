package com.ds.eventwish.data.repository;

import android.util.Log;

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
    
    public SponsoredAdRepository() {
        this.apiService = ApiClient.getClient();
    }
    
    /**
     * Get all active sponsored ads from the server
     * @return LiveData of sponsored ads list
     */
    public LiveData<List<SponsoredAd>> getSponsoredAds() {
        loadingLiveData.setValue(true);
        
        apiService.getSponsoredAds().enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    SponsoredAdResponse adResponse = response.body();
                    if (adResponse.isSuccess() && adResponse.getAds() != null) {
                        sponsoredAdsLiveData.postValue(adResponse.getAds());
                        Log.d(TAG, "Loaded " + adResponse.getAds().size() + " sponsored ads");
                    } else {
                        errorLiveData.postValue(adResponse.getMessage() != null ? 
                                adResponse.getMessage() : "No ads available");
                        Log.w(TAG, "Error loading sponsored ads: " + adResponse.getMessage());
                    }
                } else {
                    errorLiveData.postValue("Error fetching sponsored ads: " + 
                            (response.errorBody() != null ? response.code() : "Unknown error"));
                    Log.e(TAG, "Error fetching sponsored ads: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error fetching sponsored ads", t);
            }
        });
        
        return sponsoredAdsLiveData;
    }
    
    /**
     * Record an impression for a sponsored ad
     * @param adId The ID of the ad that was viewed
     */
    public void recordImpression(String adId) {
        String deviceId = DeviceUtils.getDeviceId(EventWishApplication.getAppContext());
        if (deviceId == null || adId == null) {
            Log.e(TAG, "Cannot record impression - missing deviceId or adId");
            return;
        }
        
        apiService.recordSponsoredAdImpression(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully recorded impression for ad: " + adId);
                } else {
                    Log.e(TAG, "Failed to record impression for ad: " + adId + ", code: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error recording impression for ad: " + adId, t);
            }
        });
    }
    
    /**
     * Record a click for a sponsored ad
     * @param adId The ID of the ad that was clicked
     */
    public void recordClick(String adId) {
        String deviceId = DeviceUtils.getDeviceId(EventWishApplication.getAppContext());
        if (deviceId == null || adId == null) {
            Log.e(TAG, "Cannot record click - missing deviceId or adId");
            return;
        }
        
        apiService.recordSponsoredAdClick(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully recorded click for ad: " + adId);
                } else {
                    Log.e(TAG, "Failed to record click for ad: " + adId + ", code: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error recording click for ad: " + adId, t);
            }
        });
    }
    
    /**
     * Get the loading state LiveData
     * @return LiveData of loading state
     */
    public LiveData<Boolean> getLoadingState() {
        return loadingLiveData;
    }
    
    /**
     * Get the error LiveData
     * @return LiveData of error messages
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }
    
    /**
     * Get sponsored ads for a specific location
     * @param location The location identifier (e.g., "home_bottom")
     * @return LiveData containing filtered ads list
     */
    public LiveData<List<SponsoredAd>> getAdsByLocation(String location) {
        MutableLiveData<List<SponsoredAd>> filteredAds = new MutableLiveData<>(new ArrayList<>());
        
        sponsoredAdsLiveData.observeForever(ads -> {
            if (ads != null) {
                List<SponsoredAd> filtered = new ArrayList<>();
                for (SponsoredAd ad : ads) {
                    if (location.equals(ad.getLocation()) && ad.isStatus()) {
                        filtered.add(ad);
                    }
                }
                filteredAds.postValue(filtered);
                Log.d(TAG, "Filtered " + filtered.size() + " ads for location: " + location);
            }
        });
        
        return filteredAds;
    }
} 