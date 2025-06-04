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
import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.data.model.response.BaseResponse;
import androidx.annotation.NonNull;
import com.ds.eventwish.R;

public class AboutRepository extends BaseRepository {
    private static final String TAG = "AboutRepository";
    private final ApiService apiService;
    private final MutableLiveData<About> aboutLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    private boolean isFetching = false;
    private final Context context;

    public AboutRepository(Context context) {
        super();
        apiService = ApiClient.getClient();
        aboutLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
        this.context = context;
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

        apiService.getAbout().enqueue(new Callback<BaseResponse<About>>() {
            @Override
            public void onResponse(Call<BaseResponse<About>> call, Response<BaseResponse<About>> response) {
                isFetching = false;
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    aboutLiveData.postValue(response.body().getData());
                    Log.d(TAG, "About content fetched successfully");
                } else {
                    handleApiError(response, errorLiveData);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<About>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching about content: " + t.getMessage(), t);
                
                // Create fallback content
                About fallbackAbout = new About();
                fallbackAbout.setTitle("About EventWish");
                fallbackAbout.setHtmlCode(
                    "<html><body style='padding: 16px; font-family: sans-serif;'>" +
                    "<h2>About EventWish</h2>" +
                    "<p>EventWish is your go-to app for creating beautiful wishes for all occasions.</p>" +
                    "<p>With EventWish, you can:</p>" +
                    "<ul>" +
                    "<li>Create personalized wishes for birthdays, anniversaries, and special occasions</li>" +
                    "<li>Choose from a variety of templates and designs</li>" +
                    "<li>Share your wishes on social media platforms</li>" +
                    "<li>Set reminders for upcoming events</li>" +
                    "</ul>" +
                    "<p>Thank you for using EventWish!</p>" +
                    "</body></html>"
                );
                
                // Post fallback content to LiveData
                aboutLiveData.postValue(fallbackAbout);
                
                // Post error message indicating offline content is being used
                errorLiveData.postValue(context.getString(R.string.using_offline_content));
                
                // Update loading state
                loadingLiveData.postValue(false);
            }
        });
    }
} 