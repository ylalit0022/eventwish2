package com.ds.eventwish.ads;

import android.content.Context;
import android.util.Log;

import com.ds.eventwish.R;
import com.ds.eventwish.data.remote.ApiService;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for handling AdMob ad unit fetching directly from server
 * No caching is implemented - all requests go directly to the server
 */
public class AdMobRepository {
    private static final String TAG = "AdMobRepository";
    
    private final Context context;
    private final ApiService apiService;
    private final String apiKey = "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915";
    private final String appSignature = "app_sig_1";
    
    public interface AdUnitCallback {
        void onSuccess(JsonObject response);
        void onError(String error);
    }
    
    public AdMobRepository(Context context, ApiService apiService) {
        this.context = context.getApplicationContext();
        this.apiService = apiService;
    }

    /**
     * Fetch ad unit by type directly from server
     * @param adType Type of ad (app_open, banner, interstitial, rewarded)
     * @param callback Callback to handle response
     */
    public void fetchAdUnit(String adType, AdUnitCallback callback) {
        Log.d(TAG, context.getString(R.string.debug_request_start, adType));
        
        // Get device ID - in real app, this should be properly generated and stored
        String deviceId = "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e";
        
        // Log request details
        Log.d(TAG, context.getString(R.string.log_admob_headers, apiKey, appSignature, deviceId));
        
        apiService.getAdUnits(adType, apiKey, appSignature, deviceId)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Log.d(TAG, context.getString(R.string.debug_response_code, response.code()));
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String rawResponse = response.body().toString();
                        Log.d(TAG, context.getString(R.string.debug_raw_response, rawResponse));
                        callback.onSuccess(response.body());
                    } else if (response.code() == 503) {
                        String error = context.getString(R.string.log_admob_server_error);
                        Log.e(TAG, error);
                        callback.onError(error);
                    } else {
                        String error = context.getString(R.string.log_admob_response_error, 
                            response.code() + " " + response.message());
                        Log.e(TAG, error);
                        callback.onError(error);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    String error = context.getString(R.string.log_admob_network_error, t.getMessage());
                    Log.e(TAG, error, t);
                    callback.onError(error);
            }
        });
    }
    
    /**
     * Fetch all ad units directly from server
     * @param callback Callback to handle response
     */
    public void fetchAllAdUnits(AdUnitCallback callback) {
        Log.d(TAG, "Fetching all ad units");
        
        // Get device ID - in real app, this should be properly generated and stored
        String deviceId = "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e";
        
        // Log request details
        Log.d(TAG, context.getString(R.string.log_admob_headers, apiKey, appSignature, deviceId));
        
        apiService.getAllAdUnits(apiKey, appSignature, deviceId)
            .enqueue(new Callback<JsonObject>() {
            @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Log.d(TAG, context.getString(R.string.debug_response_code, response.code()));
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String rawResponse = response.body().toString();
                        Log.d(TAG, context.getString(R.string.debug_raw_response, rawResponse));
                        callback.onSuccess(response.body());
                    } else if (response.code() == 503) {
                        String error = context.getString(R.string.log_admob_server_error);
                        Log.e(TAG, error);
                        callback.onError(error);
                } else {
                        String error = context.getString(R.string.log_admob_response_error, 
                            response.code() + " " + response.message());
                        Log.e(TAG, error);
                        callback.onError(error);
                }
            }
            
            @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    String error = context.getString(R.string.log_admob_network_error, t.getMessage());
                    Log.e(TAG, error, t);
                    callback.onError(error);
                }
            });
    }
} 