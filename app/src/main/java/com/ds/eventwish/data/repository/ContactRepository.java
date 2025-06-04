package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.Contact;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactRepository extends BaseRepository {
    private static final String TAG = "ContactRepository";
    private final ApiService apiService;
    private final MutableLiveData<Contact> contactLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    private boolean isFetching = false;

    public ContactRepository(Context context) {
        super();
        apiService = ApiClient.getClient();
        contactLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
    }

    public LiveData<Contact> getContact() {
        if (contactLiveData.getValue() == null) {
            fetchContact(false);
        }
        return contactLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public void fetchContact() {
        fetchContact(false);
    }

    public void fetchContact(boolean forceRefresh) {
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

        Log.d(TAG, "Fetching contact content" + (forceRefresh ? " (forced refresh)" : ""));

        apiService.getContact().enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                isFetching = false;
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    contactLiveData.postValue(response.body());
                    Log.d(TAG, "Contact content fetched successfully");
                } else {
                    handleApiError(response, errorLiveData);
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                isFetching = false;
                loadingLiveData.postValue(false);
                String errorMessage = "Error fetching contact content: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                errorLiveData.postValue(errorMessage);
            }
        });
    }
} 