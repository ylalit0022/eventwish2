package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.FestivalDao;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.NetworkUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FestivalRepository {
    private static final String TAG = "FestivalRepository";
    
    private final ApiService apiService;
    private final FestivalDao festivalDao;
    private final Executor executor;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final Context context;
    
    private static FestivalRepository INSTANCE;
    
    private FestivalRepository(Context context) {
        this.context = context.getApplicationContext();
        apiService = ApiClient.getClient();
        festivalDao = AppDatabase.getInstance(context).festivalDao();
        executor = Executors.newSingleThreadExecutor();
    }
    
    public static FestivalRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FestivalRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FestivalRepository(context);
                }
            }
        }
        return INSTANCE;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<List<Festival>> getAllFestivals() {
        refreshFestivals();
        return festivalDao.getAllFestivals();
    }
    
    public LiveData<List<Festival>> getUpcomingFestivals() {
        refreshUpcomingFestivals();
        return festivalDao.getUpcomingFestivals(new Date(), getDateAfterDays(7));
    }
    
    public LiveData<List<Festival>> getFestivalsByCategory(String category) {
        refreshFestivalsByCategory(category);
        return festivalDao.getFestivalsByCategory(category);
    }
    
    public LiveData<Festival> getFestivalById(String festivalId) {
        refreshFestivalById(festivalId);
        return festivalDao.getFestivalById(festivalId);
    }
    
    public LiveData<Integer> getUnreadCount() {
        return festivalDao.getUnreadCount();
    }
    
    public void markAllAsRead() {
        executor.execute(() -> festivalDao.markAllAsRead());
    }
    
    public void markAsRead(String festivalId) {
        executor.execute(() -> festivalDao.markAsRead(festivalId));
    }
    
    public List<Festival> getUnnotifiedUpcomingFestivals() {
        // This method is called from a background thread (WorkManager)
        return festivalDao.getUnnotifiedUpcomingFestivals(new Date(), getDateAfterDays(3));
    }
    
    public void markAsNotified(String festivalId) {
        executor.execute(() -> festivalDao.markAsNotified(festivalId));
    }
    
    public void insertFestival(Festival festival) {
        executor.execute(() -> festivalDao.insertFestival(festival));
    }
    
    public void deleteAllFestivals() {
        executor.execute(() -> festivalDao.deleteAllFestivals());
    }
    
    private void refreshFestivals() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            errorMessage.postValue("No internet connection");
            return;
        }
        
        isLoading.postValue(true);
        apiService.getAllFestivals().enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    if (festivals.isEmpty()) {
                        Log.d(TAG, "No festivals returned from API");
                    } else {
                        executor.execute(() -> {
                            festivalDao.insertAllFestivals(festivals);
                        });
                    }
                } else {
                    errorMessage.postValue("Failed to fetch festivals");
                    Log.e(TAG, "API error: " + response.code() + " - " + response.message());
                }
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "Error fetching festivals", t);
                errorMessage.postValue("Error: " + t.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    private void refreshUpcomingFestivals() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            errorMessage.postValue("No internet connection");
            return;
        }
        
        isLoading.postValue(true);
        apiService.getUpcomingFestivals().enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    if (festivals.isEmpty()) {
                        Log.d(TAG, "No upcoming festivals returned from API");
                    } else {
                        executor.execute(() -> {
                            festivalDao.insertAllFestivals(festivals);
                        });
                    }
                } else {
                    errorMessage.postValue("Failed to fetch upcoming festivals");
                    Log.e(TAG, "API error: " + response.code() + " - " + response.message());
                }
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "Error fetching upcoming festivals", t);
                errorMessage.postValue("Error: " + t.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    private void refreshFestivalsByCategory(String category) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            errorMessage.postValue("No internet connection");
            return;
        }
        
        isLoading.postValue(true);
        apiService.getFestivalsByCategory(category).enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    if (festivals.isEmpty()) {
                        Log.d(TAG, "No festivals found for category: " + category);
                    } else {
                        executor.execute(() -> {
                            festivalDao.insertAllFestivals(festivals);
                        });
                    }
                } else {
                    errorMessage.postValue("Failed to fetch festivals by category");
                    Log.e(TAG, "API error: " + response.code() + " - " + response.message());
                }
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "Error fetching festivals by category", t);
                errorMessage.postValue("Error: " + t.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    private void refreshFestivalById(String festivalId) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            errorMessage.postValue("No internet connection");
            return;
        }
        
        isLoading.postValue(true);
        apiService.getFestivalById(festivalId).enqueue(new Callback<Festival>() {
            @Override
            public void onResponse(Call<Festival> call, Response<Festival> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Festival festival = response.body();
                    executor.execute(() -> {
                        festivalDao.insertFestival(festival);
                    });
                } else {
                    errorMessage.postValue("Failed to fetch festival details");
                    Log.e(TAG, "API error: " + response.code() + " - " + response.message());
                }
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Call<Festival> call, Throwable t) {
                Log.e(TAG, "Error fetching festival details", t);
                errorMessage.postValue("Error: " + t.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    private Date getDateAfterDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }
}
