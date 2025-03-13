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
            return;
        }

        if (currentCall != null) {
            currentCall.cancel();
        }

        isLoading.setValue(true);
        Log.d(TAG, "Loading wish with shortCode: " + shortCode);

        currentCall = apiService.getSharedWish(shortCode);
        currentCall.enqueue(new Callback<BaseResponse<WishResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<WishResponse>> call, Response<BaseResponse<WishResponse>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<WishResponse> baseResponse = response.body();
                    if (baseResponse.isSuccess() && baseResponse.getData() != null) {
                        WishResponse wishResponse = baseResponse.getData();
                        if (wishResponse.getTemplate() != null) {
                            Log.d(TAG, "Wish loaded successfully with template");
                            wish.setValue(wishResponse);
                        } else {
                            Log.e(TAG, "Template is null in wish response");
                            error.setValue("Invalid wish template");
                        }
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? 
                            baseResponse.getMessage() : "Failed to load wish";
                        Log.e(TAG, "Error in response: " + errorMsg);
                        error.setValue(errorMsg);
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<WishResponse>> call, Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Request was cancelled");
                    return;
                }

                isLoading.setValue(false);
                handleFailure(t);
            }
        });
    }

    private void handleErrorResponse(Response<BaseResponse<WishResponse>> response) {
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
        Log.e(TAG, "Error response: " + errorMsg);
        error.setValue(errorMsg);
    }

    private void handleFailure(Throwable t) {
        String errorMessage;
        if (t instanceof SocketTimeoutException) {
            errorMessage = "Connection timed out. Please check your internet connection and try again.";
        } else if (t instanceof IOException) {
            errorMessage = "Network error. Please check your internet connection and try again.";
        } else {
            errorMessage = "An error occurred. Please try again.";
        }
        
        Log.e(TAG, "Request failed: " + errorMessage, t);
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