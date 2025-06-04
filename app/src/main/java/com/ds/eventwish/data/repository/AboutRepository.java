package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.About;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AboutRepository extends BaseRepository {
    private static final String TAG = "AboutRepository";
    private final ApiService apiService;
    private final MutableLiveData<About> aboutLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    private boolean isFetching = false;

    public AboutRepository(Context context) {
        super();
        apiService = ApiClient.getClient();
        aboutLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
    }

    public LiveData<About> getAbout() {
        if (aboutLiveData.getValue() == null) {
            fetchAbout(false);
        }
        return aboutLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public void fetchAbout() {
        fetchAbout(false);
    }

    public void fetchAbout(boolean forceRefresh) {
        // If already fetching, don't start another request unless forced
        if (isFetching && !forceRefresh) {
            return;
        }

        if (!isNetworkAvailable()) {
            errorLiveData.postValue("No internet connection");
            return;
        }

        isFetching = true;
        loadingLiveData.postValue(true);
        errorLiveData.postValue(null); // Clear previous errors

        Log.d(TAG, "Fetching about content" + (forceRefresh ? " (forced refresh)" : ""));

        apiService.getAbout().enqueue(new Callback<About>() {
            @Override
            public void onResponse(Call<About> call, Response<About> response) {
                isFetching = false;
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    aboutLiveData.postValue(response.body());
                    Log.d(TAG, "About content fetched successfully");
                } else {
                    handleApiError(response, errorLiveData);
                }
            }

            @Override
            public void onFailure(Call<About> call, Throwable t) {
                isFetching = false;
                loadingLiveData.postValue(false);
                String errorMessage = "Error fetching about content: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                errorLiveData.postValue(errorMessage);
            }
        });
    }
} 