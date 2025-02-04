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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedWishViewModel extends ViewModel {
    private static final String TAG = "SharedWishViewModel";
    private final ApiService apiService;
    private WishResponse wishResponse;
    private final MutableLiveData<WishResponse> sharedWish = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public SharedWishViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<WishResponse> getSharedWish() {
        return sharedWish;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadSharedWish(String shortCode) {
        Log.d(TAG, "Loading wish: " + shortCode);

        apiService.getSharedWish(shortCode).enqueue(new Callback<WishResponse>() {
            @Override
            public void onResponse(Call<WishResponse> call, Response<WishResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    wishResponse = response.body();
                    Log.d(TAG, "Wish loaded: " + new Gson().toJson(wishResponse));
                Log.d(TAG, "Template: " + (wishResponse.getTemplate() != null ? 
                    wishResponse.getTemplate().getId() : "null"));
                    sharedWish.setValue(wishResponse);

                } else {
                    String errorMessage = "Failed to load wish: " + response.code();
                    Log.e(TAG, errorMessage);
                    error.setValue("Failed to load wish");
                }
            }

            @Override
            public void onFailure(Call<WishResponse> call, Throwable t) {
                String errorMessage = "Network error: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                error.setValue(t.getMessage());
            }
        });
    }
}
