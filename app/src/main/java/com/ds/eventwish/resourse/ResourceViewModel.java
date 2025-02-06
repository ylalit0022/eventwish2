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
            loadWish(currentCall.request().url().pathSegments().get(1)); // Get shortCode from URL
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