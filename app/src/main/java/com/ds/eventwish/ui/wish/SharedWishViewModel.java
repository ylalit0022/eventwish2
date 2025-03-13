package com.ds.eventwish.ui.wish;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedWishViewModel extends ViewModel {
    private static final String TAG = "SharedWishViewModel";
    private final ApiService apiService;
    private WishResponse wishResponse;
    private final MutableLiveData<WishResponse> sharedWish = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<JsonObject> analytics = new MutableLiveData<>();

    private Call<WishResponse> currentCall;
    private Call<JsonObject> analyticsCall;

    public SharedWishViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<WishResponse> getSharedWish() {
        return sharedWish;
    }

    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<JsonObject> getAnalytics() {
        return analytics;
    }

    public void loadSharedWish(String shortCode) {
        Log.d(TAG, "Loading wish: " + shortCode);

        // Cancel any ongoing call
        if (currentCall != null) {
            currentCall.cancel();
        }

        currentCall = apiService.getSharedWish(shortCode);
        currentCall.enqueue(new Callback<WishResponse>() {
            @Override
            public void onResponse(Call<WishResponse> call, Response<WishResponse> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    wishResponse = response.body();
                    Log.d(TAG, "Wish loaded: " + new Gson().toJson(wishResponse));
                    Log.d(TAG, "Template: " + (wishResponse.getTemplate() != null ? 
                        wishResponse.getTemplate().getId() : "null"));
                    sharedWish.setValue(wishResponse);
                    
                    // After loading the wish, fetch analytics
                    loadWishAnalytics(shortCode);
                } else {
                    String errorMessage = "Failed to load wish: " + response.code();
                    Log.e(TAG, errorMessage);
                    error.setValue("Failed to load wish");
                }
            }

            @Override
            public void onFailure(Call<WishResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                
                String errorMessage = "Network error: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                error.setValue(t.getMessage());
            }
        });
    }
    
    /**
     * Load analytics data for a shared wish
     * @param shortCode The short code of the wish
     */
    public void loadWishAnalytics(String shortCode) {
        Log.d(TAG, "Loading analytics for wish: " + shortCode);
        
        // Cancel any ongoing analytics call
        if (analyticsCall != null) {
            analyticsCall.cancel();
        }
        
        analyticsCall = apiService.getWishAnalytics(shortCode);
        analyticsCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (call.isCanceled()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject analyticsData = response.body();
                    Log.d(TAG, "Analytics loaded: " + analyticsData);
                    analytics.setValue(analyticsData);
                } else {
                    Log.e(TAG, "Failed to load analytics: " + response.code());
                    // Don't set error here to avoid disrupting the main flow
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (call.isCanceled()) return;
                
                Log.e(TAG, "Network error loading analytics: " + t.getMessage(), t);
                // Don't set error here to avoid disrupting the main flow
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentCall != null) {
            currentCall.cancel();
        }
        if (analyticsCall != null) {
            analyticsCall.cancel();
        }
    }
}
