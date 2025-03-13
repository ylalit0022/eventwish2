package com.ds.eventwish.resourse;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.net.SocketTimeoutException;
import java.io.IOException;

public class ResourceViewModel extends ViewModel {
    private static final String TAG = "ResourceViewModel";
    private final ApiService apiService;
    private final MutableLiveData<WishResponse> wish = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private Call<WishResponse> currentCall;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    public ResourceViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<WishResponse> getWish() {
        return wish;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public void loadWish(String shortCode) {
        if (currentCall != null) {
            currentCall.cancel();
        }

        isLoading.setValue(true);
        error.setValue(null);
        
        Log.d(TAG, "Loading wish with shortCode: " + shortCode + ", attempt: " + (retryCount + 1));

        currentCall = apiService.getSharedWish(shortCode);
        currentCall.enqueue(new Callback<WishResponse>() {
            @Override
            public void onResponse(Call<WishResponse> call, Response<WishResponse> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Wish loaded successfully");
                    wish.setValue(response.body());
                    isLoading.setValue(false);
                    retryCount = 0;
                } else {
                    handleError(new IOException("Server returned error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<WishResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                handleError(t);
            }
        });
    }

    private void handleError(Throwable t) {
        Log.e(TAG, "Error loading wish", t);
        
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            Log.d(TAG, "Retrying... Attempt " + retryCount + " of " + MAX_RETRIES);
            
            // Get the URL that was called
            String url = currentCall.request().url().toString();
            Log.d(TAG, "Failed URL: " + url);
            
            // Extract the shortCode from the URL more safely
            String[] pathSegments = currentCall.request().url().pathSegments().toArray(new String[0]);
            if (pathSegments.length > 0) {
                // Get the last path segment which should be the shortCode
                String shortCode = pathSegments[pathSegments.length - 1];
                
                // Don't retry if the shortCode is "wish" - this indicates a problem
                if ("wish".equals(shortCode)) {
                    Log.e(TAG, "Invalid shortCode 'wish' detected. Aborting retry.");
                    isLoading.setValue(false);
                    error.setValue("Invalid wish code. Please try again.");
                    retryCount = MAX_RETRIES; // Stop retrying
                    return;
                }
                
                Log.d(TAG, "Retrying with shortCode: " + shortCode);
                loadWish(shortCode);
            } else {
                // If we can't extract the shortCode, stop retrying
                Log.e(TAG, "Could not extract shortCode from URL. Aborting retry.");
                isLoading.setValue(false);
                error.setValue("Could not load wish. Please try again.");
                retryCount = MAX_RETRIES; // Stop retrying
            }
            return;
        }

        isLoading.setValue(false);
        retryCount = 0;

        String errorMessage;
        if (t instanceof SocketTimeoutException) {
            errorMessage = "Connection timed out. Please check your internet connection and try again.";
        } else if (t instanceof IOException) {
            errorMessage = "Network error. Please check your internet connection and try again.";
        } else {
            errorMessage = "An error occurred. Please try again.";
        }
        
        error.setValue(errorMessage);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentCall != null) {
            currentCall.cancel();
        }
    }
}