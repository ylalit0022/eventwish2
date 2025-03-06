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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FestivalRepository {
    private static final String TAG = "FestivalRepository";
    private static FestivalRepository instance;

    private final FestivalDao festivalDao;
    private final ApiService apiService;
    private final Executor executor;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);

    private FestivalRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        festivalDao = database.festivalDao();
        apiService = ApiClient.getClient();
        executor = Executors.newFixedThreadPool(4);
        
        // Initialize the unread count
        refreshUnreadCount();
    }

    public static synchronized FestivalRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FestivalRepository(context);
        }
        return instance;
    }

    /**
     * Get upcoming festivals from the database
     * @return LiveData containing a list of upcoming festivals
     */
    public LiveData<List<Festival>> getUpcomingFestivals() {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 3); // Get festivals for the next 3 months
        Date endDate = calendar.getTime();
        return festivalDao.getUpcomingFestivals(now, endDate);
    }

    /**
     * Get upcoming festivals by category from the database
     * @param category The category to filter by
     * @return LiveData containing a list of upcoming festivals for the given category
     */
    public LiveData<List<Festival>> getUpcomingFestivalsByCategory(String category) {
        return festivalDao.getFestivalsByCategory(category);
    }

    /**
     * Get a festival by ID from the database
     * @param festivalId The ID of the festival to retrieve
     * @return LiveData containing the festival with the given ID
     */
    public LiveData<Festival> getFestivalById(String festivalId) {
        return festivalDao.getFestivalById(festivalId);
    }

    /**
     * Get the count of unread festivals
     * @return LiveData containing the count of unread festivals
     */
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    /**
     * Get the loading state
     * @return LiveData containing the loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Get the error message
     * @return LiveData containing the error message
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Mark a festival as read
     * @param festivalId The ID of the festival to mark as read
     */
    public void markAsRead(String festivalId) {
        executor.execute(() -> {
            festivalDao.markAsRead(festivalId);
            refreshUnreadCount();
        });
    }

    /**
     * Mark a festival as notified
     * @param festivalId The ID of the festival to mark as notified
     */
    public void markAsNotified(String festivalId) {
        executor.execute(() -> {
            festivalDao.markAsNotified(festivalId);
        });
    }

    /**
     * Mark all festivals as read
     */
    public void markAllAsRead() {
        executor.execute(() -> {
            festivalDao.markAllAsRead();
            refreshUnreadCount();
        });
    }

    /**
     * Get unnotified upcoming festivals
     * @return List of unnotified upcoming festivals
     */
    public List<Festival> getUnnotifiedUpcomingFestivals() {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1); // Get festivals for the next month
        Date endDate = calendar.getTime();
        
        return festivalDao.getUnnotifiedUpcomingFestivals(now, endDate);
    }

    /**
     * Refresh the unread count
     */
    private void refreshUnreadCount() {
        // Get the unread count directly without observing
        try {
            int count = festivalDao.getUnreadCountSync();
            unreadCount.postValue(count);
            Log.d(TAG, "Updated unread count: " + count);
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing unread count", e);
        }
    }

    /**
     * Force refresh upcoming festivals from the server
     * This method will clear the local database first
     */
    public void refreshUpcomingFestivals() {
        Log.d(TAG, "Refreshing upcoming festivals from server");
        isLoading.postValue(true);
        errorMessage.postValue("");
        
        // Clear the local database first
        executor.execute(() -> {
            try {
                Log.d(TAG, "Clearing local festival database");
                festivalDao.deleteAllFestivals();
                
                // After clearing, fetch from the server
                fetchUpcomingFestivalsFromServer();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing local database", e);
                errorMessage.postValue("Error clearing local database: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Refresh upcoming festivals by category from the server
     * This method will clear the local database for the specified category first
     * @param category The category to refresh
     */
    public void refreshUpcomingFestivalsByCategory(String category) {
        Log.d(TAG, "Refreshing upcoming festivals for category: " + category);
        isLoading.postValue(true);
        errorMessage.postValue("");
        
        // Clear the local database for this category first
        executor.execute(() -> {
            try {
                Log.d(TAG, "Clearing local festival database for category: " + category);
                festivalDao.deleteFestivalsByCategory(category);
                
                // After clearing, fetch from the server
                fetchUpcomingFestivalsByCategoryFromServer(category);
            } catch (Exception e) {
                Log.e(TAG, "Error clearing local database for category: " + category, e);
                errorMessage.postValue("Error clearing local database: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Fetch upcoming festivals from the server
     */
    private void fetchUpcomingFestivalsFromServer() {
        Log.d(TAG, "Fetching upcoming festivals from server");
        Call<List<Festival>> call = apiService.getAllFestivals();
        call.enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    Log.d(TAG, "Fetched " + festivals.size() + " festivals from server");
                    
                    // Save to database
                    executor.execute(() -> {
                        try {
                            // Insert all festivals into the database
                            festivalDao.insertAllFestivals(festivals);
                            Log.d(TAG, "Saved " + festivals.size() + " festivals to database");
                            
                            // Refresh unread count
                            refreshUnreadCount();
                            
                            // Update loading state
                            isLoading.postValue(false);
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving festivals to database", e);
                            errorMessage.postValue("Error saving festivals: " + e.getMessage());
                            isLoading.postValue(false);
                        }
                    });
                } else {
                    Log.e(TAG, "Error fetching festivals: " + response.code());
                    errorMessage.postValue("Error fetching festivals: " + response.code());
                    isLoading.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                errorMessage.postValue("Network error: " + t.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Fetch upcoming festivals by category from the server
     * @param category The category to fetch
     */
    private void fetchUpcomingFestivalsByCategoryFromServer(String category) {
        Log.d(TAG, "Fetching upcoming festivals for category: " + category);
        // Since there's no specific category endpoint, we'll fetch all and filter locally
        Call<List<Festival>> call = apiService.getAllFestivals();
        call.enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> allFestivals = response.body();
                    Log.d(TAG, "Fetched " + allFestivals.size() + " festivals from server");
                    
                    // Filter festivals by category
                    List<Festival> categoryFestivals = new ArrayList<>();
                    for (Festival festival : allFestivals) {
                        if (category.equals(festival.getCategory())) {
                            categoryFestivals.add(festival);
                        }
                    }
                    
                    Log.d(TAG, "Filtered " + categoryFestivals.size() + " festivals for category: " + category);
                    
                    // Save to database
                    executor.execute(() -> {
                        try {
                            // Insert filtered festivals into the database
                            festivalDao.insertAllFestivals(categoryFestivals);
                            Log.d(TAG, "Saved " + categoryFestivals.size() + " festivals to database");
                            
                            // Refresh unread count
                            refreshUnreadCount();
                            
                            // Update loading state
                            isLoading.postValue(false);
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving festivals to database", e);
                            errorMessage.postValue("Error saving festivals: " + e.getMessage());
                            isLoading.postValue(false);
                        }
                    });
                } else {
                    Log.e(TAG, "Error fetching festivals: " + response.code());
                    errorMessage.postValue("Error fetching festivals: " + response.code());
                    isLoading.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                errorMessage.postValue("Network error: " + t.getMessage());
                isLoading.postValue(false);
            }
        });
    }
}
