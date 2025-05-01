package com.ds.eventwish.data.repository;

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
        loadingLiveData.setValue(true);
        
        apiService.getSponsoredAds().enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    SponsoredAdResponse adResponse = response.body();
                    Log.d(TAG, "Sponsored ads API response: " + adResponse.toString());
                    
                    if (adResponse.isSuccess() && adResponse.getAds() != null) {
                        sponsoredAdsLiveData.postValue(adResponse.getAds());
                        Log.d(TAG, "Loaded " + adResponse.getAds().size() + " sponsored ads");
                    } else {
                        errorLiveData.postValue(adResponse.getMessage() != null ? 
                                adResponse.getMessage() : "No ads available");
                        Log.w(TAG, "Error loading sponsored ads: " + 
                               (adResponse.getError() != null ? adResponse.getError() : adResponse.getMessage()));
                    }
                } else {
                    String errorMsg = "Error fetching sponsored ads: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                            Log.e(TAG, "Error response body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                    
                    errorLiveData.postValue(errorMsg);
                    Log.e(TAG, errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                String errorMsg = "Network error: " + t.getMessage();
                errorLiveData.postValue(errorMsg);
                Log.e(TAG, "Network error fetching sponsored ads", t);
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Request headers: " + call.request().headers());
            }
        });
        
        return sponsoredAdsLiveData;
    }
    
    /**
     * Get all active sponsored ads from the server with callback
     * @param callback Callback for handling response
     */
    public void getSponsoredAds(SponsoredAdCallback callback) {
        loadingLiveData.setValue(true);
        
        apiService.getSponsoredAds().enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    SponsoredAdResponse adResponse = response.body();
                    Log.d(TAG, "Sponsored ads API response: " + adResponse.toString());
                    
                    if (adResponse.isSuccess() && adResponse.getAds() != null) {
                        List<SponsoredAd> ads = adResponse.getAds();
                        sponsoredAdsLiveData.postValue(ads);
                        callback.onSuccess(ads);
                        Log.d(TAG, "Loaded " + ads.size() + " sponsored ads");
                    } else {
                        String errorMsg = adResponse.getMessage() != null ? 
                                adResponse.getMessage() : "No ads available";
                        errorLiveData.postValue(errorMsg);
                        callback.onError(errorMsg);
                        Log.w(TAG, "Error loading sponsored ads: " + 
                               (adResponse.getError() != null ? adResponse.getError() : adResponse.getMessage()));
                    }
                } else {
                    String errorMsg = "Error fetching sponsored ads: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                            Log.e(TAG, "Error response body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                    
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
                Log.e(TAG, "Network error fetching sponsored ads", t);
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Request headers: " + call.request().headers());
            }
        });
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
        
        Log.d(TAG, "Recording impression for ad: " + adId + " with deviceId: " + deviceId);
        apiService.recordSponsoredAdImpression(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully recorded impression for ad: " + adId + 
                          ", response: " + response.body().toString());
                } else {
                    String errorMsg = "Failed to record impression for ad: " + adId + ", code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing impression error response", e);
                    }
                    Log.e(TAG, errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error recording impression for ad: " + adId, t);
                Log.e(TAG, "Impression request URL: " + call.request().url());
            }
        });
    }
    
    /**
     * Record an impression for a sponsored ad with callback
     * @param adId The ID of the ad that was viewed
     * @param callback Callback for handling response
     */
    public void recordAdImpression(String adId, SponsoredAdCallback callback) {
        String deviceId = DeviceUtils.getDeviceId(EventWishApplication.getAppContext());
        if (deviceId == null || adId == null) {
            String errorMsg = "Cannot record impression - missing deviceId or adId";
            Log.e(TAG, errorMsg);
            callback.onError(errorMsg);
            return;
        }
        
        Log.d(TAG, "Recording impression for ad: " + adId + " with deviceId: " + deviceId);
        apiService.recordSponsoredAdImpression(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully recorded impression for ad: " + adId + 
                          ", response: " + response.body().toString());
                    callback.onSuccess(new ArrayList<>()); // Empty list for success
                } else {
                    String errorMsg = "Failed to record impression for ad: " + adId + ", code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing impression error response", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                String errorMsg = "Network error recording impression for ad: " + adId + " - " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                Log.e(TAG, "Impression request URL: " + call.request().url());
                callback.onError(errorMsg);
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
        
        Log.d(TAG, "Recording click for ad: " + adId + " with deviceId: " + deviceId);
        apiService.recordSponsoredAdClick(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully recorded click for ad: " + adId + 
                          ", response: " + response.body().toString());
                } else {
                    String errorMsg = "Failed to record click for ad: " + adId + ", code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing click error response", e);
                    }
                    Log.e(TAG, errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error recording click for ad: " + adId, t);
                Log.e(TAG, "Click request URL: " + call.request().url());
            }
        });
    }
    
    /**
     * Record a click for a sponsored ad with callback
     * @param adId The ID of the ad that was clicked
     * @param callback Callback for handling response
     */
    public void recordAdClick(String adId, SponsoredAdCallback callback) {
        String deviceId = DeviceUtils.getDeviceId(EventWishApplication.getAppContext());
        if (deviceId == null || adId == null) {
            String errorMsg = "Cannot record click - missing deviceId or adId";
            Log.e(TAG, errorMsg);
            callback.onError(errorMsg);
            return;
        }
        
        Log.d(TAG, "Recording click for ad: " + adId + " with deviceId: " + deviceId);
        apiService.recordSponsoredAdClick(adId, deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully recorded click for ad: " + adId + 
                          ", response: " + response.body().toString());
                    callback.onSuccess(new ArrayList<>()); // Empty list for success
                } else {
                    String errorMsg = "Failed to record click for ad: " + adId + ", code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing click error response", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                String errorMsg = "Network error recording click for ad: " + adId + " - " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                Log.e(TAG, "Click request URL: " + call.request().url());
                callback.onError(errorMsg);
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
                Log.d(TAG, "Filtering ads for location '" + location + "' from " + ads.size() + " total ads");
                List<SponsoredAd> filtered = new ArrayList<>();
                
                // Log all available locations for debugging
                StringBuilder availableLocations = new StringBuilder("Available ad locations: ");
                for (SponsoredAd ad : ads) {
                    availableLocations.append("'").append(ad.getLocation()).append("'");
                    if (ads.indexOf(ad) < ads.size() - 1) {
                        availableLocations.append(", ");
                    }
                }
                Log.d(TAG, availableLocations.toString());
                
                // Try different matching strategies if no exact match is found
                boolean foundExactMatch = false;
                
                // First pass: try exact match
                for (SponsoredAd ad : ads) {
                    boolean exactMatch = location.equals(ad.getLocation()) && ad.isStatus();
                    if (exactMatch) {
                        foundExactMatch = true;
                        filtered.add(ad);
                        Log.d(TAG, "✓ Added ad to filtered list (exact match): " + ad.getId());
                    }
                }
                
                // If no exact match, try case-insensitive match
                if (!foundExactMatch) {
                    Log.d(TAG, "No exact location match found. Trying case-insensitive match...");
                    for (SponsoredAd ad : ads) {
                        boolean caseInsensitiveMatch = location.equalsIgnoreCase(ad.getLocation()) && ad.isStatus();
                        if (caseInsensitiveMatch) {
                            filtered.add(ad);
                            Log.d(TAG, "✓ Added ad to filtered list (case-insensitive match): " + ad.getId());
                        }
                    }
                }
                
                // If still no match, check for partial match (e.g., "home" in "home_bottom")
                if (filtered.isEmpty()) {
                    Log.d(TAG, "No case-insensitive match found. Trying partial match...");
                    for (SponsoredAd ad : ads) {
                        String adLocation = ad.getLocation().toLowerCase();
                        String requestedLocation = location.toLowerCase();
                        
                        boolean partialMatch = (adLocation.contains(requestedLocation) || 
                                               requestedLocation.contains(adLocation)) && 
                                               ad.isStatus();
                        
                        if (partialMatch) {
                            filtered.add(ad);
                            Log.d(TAG, "✓ Added ad to filtered list (partial match): " + ad.getId() + 
                                  ", ad location: '" + ad.getLocation() + "', requested: '" + location + "'");
                        }
                    }
                }
                
                // As a last resort, if no ads match any criteria, take the first active ad
                if (filtered.isEmpty() && !ads.isEmpty()) {
                    Log.d(TAG, "No matching ads found. Taking first active ad as fallback...");
                    for (SponsoredAd ad : ads) {
                        if (ad.isStatus()) {
                            filtered.add(ad);
                            Log.d(TAG, "✓ Added ad to filtered list (fallback): " + ad.getId() + 
                                  ", using location: '" + ad.getLocation() + "' instead of '" + location + "'");
                            break; // Just take the first one
                        }
                    }
                }
                
                filteredAds.postValue(filtered);
                Log.d(TAG, "Filtered " + filtered.size() + " ads for location: " + location);
            } else {
                Log.d(TAG, "No ads available to filter for location: " + location);
                filteredAds.postValue(new ArrayList<>());
            }
        });
        
        return filteredAds;
    }
} 