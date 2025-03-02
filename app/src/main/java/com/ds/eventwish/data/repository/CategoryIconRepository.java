package com.ds.eventwish.data.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import java.util.List;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryIconRepository {
    private static final String TAG = "CategoryIconRepository";
    private static CategoryIconRepository instance;
    private final ApiService apiService;
    private final MutableLiveData<List<CategoryIcon>> categoryIcons = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private CategoryIconRepository() {
        apiService = ApiClient.getClient();
    }

    public static synchronized CategoryIconRepository getInstance() {
        if (instance == null) {
            instance = new CategoryIconRepository();
        }
        return instance;
    }

    public LiveData<List<CategoryIcon>> getCategoryIcons() {
        return categoryIcons;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void loadCategoryIcons() {
        Log.d(TAG, "Starting to load category icons...");
        Log.d(TAG, "Current loading state: " + loading.getValue());
        loading.setValue(true);
        Log.d(TAG, "Updated loading state to: true");

        Call<List<CategoryIcon>> call = apiService.getCategoryIcons();
        Log.d(TAG, "Preparing API call to: " + call.request().url());

        call.enqueue(new Callback<List<CategoryIcon>>() {
            @Override
            public void onResponse(Call<List<CategoryIcon>> call, Response<List<CategoryIcon>> response) {
                Log.d(TAG, "Received API response");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());
                Log.d(TAG, "Called URL: " + call.request().url());

                loading.setValue(false);
                Log.d(TAG, "Updated loading state to: false");

                if (response.isSuccessful() && response.body() != null) {
                    List<CategoryIcon> icons = response.body();
                    Log.i(TAG, "Successfully loaded category icons");
                    
                    for (CategoryIcon icon : icons) {
                        Log.d(TAG, String.format("Icon - Category: %s, URL: %s", 
                            icon.getCategory(), icon.getCategoryIcon()));
                    }
                    categoryIcons.setValue(icons);
                    Log.d(TAG, "Updated categoryIcons LiveData with icon");
                } else {
                    String errorMsg = String.format("Failed to load category icons. Response code: %d, Message: %s", 
                        response.code(), response.message());
                    Log.e(TAG, errorMsg);
                    error.setValue("Failed to load category icons");
                    Log.d(TAG, "Updated error LiveData with failure message");
                }
            }

            @Override
            public void onFailure(Call<List<CategoryIcon>> call, Throwable t) {
                Log.e(TAG, "API call failed");
                Log.e(TAG, "Failed URL: " + call.request().url());
                Log.e(TAG, "Error details: " + t.getMessage(), t);

                loading.setValue(false);
                Log.d(TAG, "Updated loading state to: false");

                error.setValue(t.getMessage());
                Log.d(TAG, "Updated error LiveData with error message: " + t.getMessage());
            }
        });

        Log.d(TAG, "Category icons request has been queued");
    }
}