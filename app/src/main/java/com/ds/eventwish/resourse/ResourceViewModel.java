package com.ds.eventwish.resourse;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.response.BaseResponse;
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
    private Call<BaseResponse<WishResponse>> currentCall;
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
        if (shortCode == null || shortCode.isEmpty()) {
            error.setValue("Invalid wish code");
            Log.e(TAG, "loadWish: Cannot load wish with null or empty shortCode");
            return;
        }

        // Trim any whitespace or unnecessary characters
        shortCode = shortCode.trim();
        
        // Check if shortCode has any URL encoding or special characters
        if (shortCode.contains("%")) {
            try {
                shortCode = java.net.URLDecoder.decode(shortCode, "UTF-8");
                Log.d(TAG, "loadWish: Decoded URL-encoded shortCode: " + shortCode);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding shortCode", e);
            }
        }
        
        // Remove any path prefixes that might have been included
        if (shortCode.startsWith("wish/")) {
            shortCode = shortCode.substring(5);
            Log.d(TAG, "loadWish: Removed 'wish/' prefix from shortCode: " + shortCode);
        }
        
        Log.d(TAG, "loadWish: Final shortCode to use for API call: " + shortCode);

        if (currentCall != null) {
            currentCall.cancel();
        }

        isLoading.setValue(true);
        Log.d(TAG, "Loading wish with shortCode: " + shortCode);

        // Store the final shortCode for retry logic
        final String finalShortCode = shortCode;

        currentCall = apiService.getSharedWish(shortCode);
        currentCall.enqueue(new Callback<BaseResponse<WishResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<WishResponse>> call, Response<BaseResponse<WishResponse>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<WishResponse> baseResponse = response.body();
                    Log.d(TAG, "API Response received - success: " + baseResponse.isSuccess() + 
                          ", message: " + baseResponse.getMessage());
                    
                    if (baseResponse.isSuccess() && baseResponse.getData() != null) {
                        WishResponse wishResponse = baseResponse.getData();
                        if (wishResponse.getTemplate() != null) {
                            Log.d(TAG, "Wish loaded successfully with template id: " + 
                                  (wishResponse.getTemplate().getId() != null ? wishResponse.getTemplate().getId() : "null"));
                            wish.setValue(wishResponse);
                        } else {
                            Log.e(TAG, "Template is null in wish response");
                            error.setValue("Invalid wish template");
                        }
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? 
                            baseResponse.getMessage() : "Failed to load wish";
                        Log.e(TAG, "Error in response: " + errorMsg);
                        
                        // If the error message indicates the wish wasn't found, provide a better error message
                        if (errorMsg.toLowerCase().contains("not found") || 
                            errorMsg.toLowerCase().contains("invalid") || 
                            errorMsg.toLowerCase().contains("does not exist")) {
                            error.setValue("Shared wish not found. The link may be expired or invalid.");
                        } else {
                            error.setValue(errorMsg);
                        }
                    }
                } else {
                    handleErrorResponse(response, finalShortCode);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<WishResponse>> call, Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Request was cancelled");
                    return;
                }

                isLoading.setValue(false);
                handleFailure(t, finalShortCode);
            }
        });
    }

    private void handleErrorResponse(Response<BaseResponse<WishResponse>> response, String shortCode) {
        String errorMsg;
        try {
            if (response.errorBody() != null) {
                errorMsg = response.errorBody().string();
            } else {
                errorMsg = "Error " + response.code() + ": " + response.message();
            }
        } catch (IOException e) {
            errorMsg = "Error reading error response";
        }
        
        Log.e(TAG, "Error response for shortCode [" + shortCode + "]: " + errorMsg + ", HTTP code: " + response.code());
        
        // Provide more user-friendly error messages based on HTTP status codes
        if (response.code() == 404) {
            error.setValue("Shared wish not found. The link may be expired or invalid.");
        } else if (response.code() == 403) {
            error.setValue("You don't have permission to view this wish.");
        } else if (response.code() >= 500) {
            error.setValue("Server error. Please try again later.");
        } else {
            // For other errors, use the error message from the response if possible
            if (errorMsg.contains("not found") || errorMsg.toLowerCase().contains("invalid")) {
                error.setValue("Shared wish not found. The link may be expired or invalid.");
            } else {
                error.setValue("Error loading wish: " + response.code());
            }
        }
    }

    private void handleFailure(Throwable t, String shortCode) {
        String errorMessage;
        if (t instanceof SocketTimeoutException) {
            errorMessage = "Connection timed out. Please check your internet connection and try again.";
        } else if (t instanceof IOException) {
            errorMessage = "Network error. Please check your internet connection and try again.";
        } else {
            errorMessage = "An error occurred. Please try again.";
        }
        
        Log.e(TAG, "Request failed for shortCode [" + shortCode + "]: " + errorMessage, t);
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