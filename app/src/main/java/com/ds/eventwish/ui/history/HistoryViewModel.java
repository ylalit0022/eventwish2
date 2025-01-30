package com.ds.eventwish.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryViewModel extends ViewModel {
    private final MutableLiveData<List<SharedWish>> historyItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ApiService apiService;

    public HistoryViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<List<SharedWish>> getHistoryItems() {
        return historyItems;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadHistory() {
        loading.setValue(true);
        error.setValue(null);

        apiService.getMyWishes().enqueue(new Callback<List<SharedWish>>() {
            @Override
            public void onResponse(Call<List<SharedWish>> call, Response<List<SharedWish>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    historyItems.setValue(response.body());
                } else {
                    error.setValue("Failed to load history");
                }
            }

            @Override
            public void onFailure(Call<List<SharedWish>> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }
}
