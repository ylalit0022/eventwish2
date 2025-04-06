package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.entity.SharedWishEntity;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.util.AppExecutors;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing shared wishes (wishes shared with other users)
 */
public class SharedWishRepository {
    private static final String TAG = "SharedWishRepository";
    private static SharedWishRepository instance;
    
    private final ApiService apiService;
    private final AppExecutors executors;
    private final Context context;

    private SharedWishRepository(Context context) {
        this.context = context.getApplicationContext();
        apiService = ApiClient.getClient();
        executors = AppExecutors.getInstance();
    }

    public static SharedWishRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (SharedWishRepository.class) {
                if (instance == null) {
                    instance = new SharedWishRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Create a shared wish that can be accessed via a shortened URL
     * @param templateId Template ID
     * @param customizedHtml Customized HTML content
     * @param customizationData Additional customization data
     * @return LiveData with result
     */
    public LiveData<Result<SharedWishEntity>> createSharedWish(String templateId, String customizedHtml, Map<String, Object> customizationData) {
        MutableLiveData<Result<SharedWishEntity>> result = new MutableLiveData<>();
        result.setValue(Result.loading());

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("templateId", templateId);
        requestBody.put("customizedHtml", customizedHtml);
        requestBody.put("customizationData", customizationData);

        // Make API call
        apiService.createSharedWish(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject responseBody = response.body();
                        String shortCode = responseBody.get("shortCode").getAsString();
                        String shareUrl = responseBody.get("shareUrl").getAsString();
                        
                        // Create entity
                        SharedWishEntity sharedWish = new SharedWishEntity(
                                shortCode,
                                templateId,
                                customizedHtml,
                                shareUrl,
                                System.currentTimeMillis()
                        );
                        
                        result.setValue(Result.success(sharedWish));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        result.setValue(Result.error("Error creating shared wish: " + e.getMessage()));
                    }
                } else {
                    Log.e(TAG, "Error response: " + response.code());
                    result.setValue(Result.error("Error creating shared wish. Server returned: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error", t);
                result.setValue(Result.error("Network error: " + t.getMessage()));
            }
        });

        return result;
    }

    /**
     * Retrieve a shared wish by its short code
     * @param shortCode Short code
     * @return LiveData with result
     */
    public LiveData<Result<SharedWishEntity>> getSharedWish(String shortCode) {
        MutableLiveData<Result<SharedWishEntity>> result = new MutableLiveData<>();
        result.setValue(Result.loading());

        apiService.getSharedWishJsonByShortCode(shortCode).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject responseBody = response.body();
                        String templateId = responseBody.get("templateId").getAsString();
                        String customizedHtml = responseBody.get("customizedHtml").getAsString();
                        String shareUrl = responseBody.get("shareUrl").getAsString();
                        
                        // Create entity
                        SharedWishEntity sharedWish = new SharedWishEntity(
                                shortCode,
                                templateId,
                                customizedHtml,
                                shareUrl,
                                System.currentTimeMillis()
                        );
                        
                        result.setValue(Result.success(sharedWish));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        result.setValue(Result.error("Error retrieving shared wish: " + e.getMessage()));
                    }
                } else {
                    Log.e(TAG, "Error response: " + response.code());
                    result.setValue(Result.error("Error retrieving shared wish. Server returned: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error", t);
                result.setValue(Result.error("Network error: " + t.getMessage()));
            }
        });

        return result;
    }

    /**
     * Generic class for representing operation results
     */
    public static class Result<T> {
        public enum Status {
            SUCCESS,
            ERROR,
            LOADING
        }

        private final Status status;
        private final T data;
        private final String message;

        private Result(Status status, T data, String message) {
            this.status = status;
            this.data = data;
            this.message = message;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(Status.SUCCESS, data, null);
        }

        public static <T> Result<T> error(String msg) {
            return new Result<>(Status.ERROR, null, msg);
        }

        public static <T> Result<T> loading() {
            return new Result<>(Status.LOADING, null, null);
        }

        public Status getStatus() {
            return status;
        }

        public T getData() {
            return data;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public boolean isLoading() {
            return status == Status.LOADING;
        }

        public boolean isError() {
            return status == Status.ERROR;
        }
    }
} 