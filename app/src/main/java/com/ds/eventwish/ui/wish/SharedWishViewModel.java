package com.ds.eventwish.ui.wish;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedWishViewModel extends ViewModel {
    private final ApiService apiService;
    private final MutableLiveData<SharedWish> sharedWish = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public SharedWishViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<SharedWish> getSharedWish() {
        return sharedWish;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadSharedWish(String shortCode) {
        apiService.getSharedWish(shortCode).enqueue(new Callback<SharedWish>() {
            @Override
            public void onResponse(Call<SharedWish> call, Response<SharedWish> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sharedWish.setValue(response.body());
                } else {
                    error.setValue("Failed to load wish");
                }
            }

            @Override
            public void onFailure(Call<SharedWish> call, Throwable t) {
                error.setValue(t.getMessage());
            }
        });
    }
}
