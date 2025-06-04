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
import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.data.model.response.BaseResponse;
import androidx.annotation.NonNull;
import com.ds.eventwish.R;

public class ContactRepository extends BaseRepository {
    private static final String TAG = "ContactRepository";
    private final ApiService apiService;
    private final MutableLiveData<Contact> contactLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    private boolean isFetching = false;
    private final Context context;

    public ContactRepository(Context context) {
        super();
        apiService = ApiClient.getClient();
        contactLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
        this.context = context;
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

        apiService.getContact().enqueue(new Callback<BaseResponse<Contact>>() {
            @Override
            public void onResponse(Call<BaseResponse<Contact>> call, Response<BaseResponse<Contact>> response) {
                isFetching = false;
                loadingLiveData.postValue(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    contactLiveData.postValue(response.body().getData());
                    Log.d(TAG, "Contact content fetched successfully");
                } else {
                    handleApiError(response, errorLiveData);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Contact>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching contact content: " + t.getMessage(), t);
                
                // Create fallback content
                Contact fallbackContact = new Contact();
                fallbackContact.setTitle("Contact Us");
                fallbackContact.setHtmlCode(
                    "<html><body style='padding: 16px; font-family: sans-serif;'>" +
                    "<h2>Contact Us</h2>" +
                    "<p>We'd love to hear from you! Here's how you can reach us:</p>" +
                    "<div style='margin-top: 16px;'>" +
                    "<p><strong>Email:</strong> support@eventwish.com</p>" +
                    "<p><strong>Website:</strong> www.eventwish.com</p>" +
                    "</div>" +
                    "<div style='margin-top: 24px;'>" +
                    "<h3>Send us feedback</h3>" +
                    "<p>Your feedback helps us improve EventWish. Please let us know if you have any suggestions or encounter any issues.</p>" +
                    "</div>" +
                    "</body></html>"
                );
                
                // Post fallback content to LiveData
                contactLiveData.postValue(fallbackContact);
                
                // Post error message indicating offline content is being used
                errorLiveData.postValue(context.getString(R.string.using_offline_content));
                
                // Update loading state
                loadingLiveData.postValue(false);
            }
        });
    }
} 