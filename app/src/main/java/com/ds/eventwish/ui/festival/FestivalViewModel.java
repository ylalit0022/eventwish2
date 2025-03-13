package com.ds.eventwish.ui.festival;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.Result;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.workers.FestivalNotificationWorker;
import com.ds.eventwish.utils.TimeUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FestivalViewModel extends AndroidViewModel {
    private static final String TAG = "FestivalViewModel";
    private static final String FESTIVAL_NOTIFICATION_WORK = "festival_notification_work";
    
    private final FestivalRepository repository;
    private final MutableLiveData<String> currentCategory = new MutableLiveData<>("All");
    private final MediatorLiveData<Result<List<Festival>>> festivalsResult = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> showCacheSnackbar = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> staleData = new MutableLiveData<>(false);
    
    // Keep references to our observers to prevent memory leaks and duplicate observers
    private Observer<List<Festival>> festivalsObserver = null;
    private Observer<String> errorObserver = null;
    private Observer<Boolean> cacheObserver = null;
    private LiveData<List<Festival>> upcomingFestivalsSource = null;
    private LiveData<String> errorMessageSource = null;
    private LiveData<Boolean> isFromCacheSource = null;
    
    public FestivalViewModel(@NonNull Application application) {
        super(application);
        repository = FestivalRepository.getInstance(application);
        
        // Set up the worker for checking upcoming festivals
        setupFestivalNotificationWorker();
        
        // Initialize the sources
        upcomingFestivalsSource = repository.getUpcomingFestivals();
        errorMessageSource = repository.getErrorMessage();
        isFromCacheSource = repository.getIsFromCache();
        
        // Create the observers
        createObservers();
        
        // Load festivals when the view model is created
        loadFestivals();
    }
    
    private void createObservers() {
        // Create observers that will be reused
        festivalsObserver = festivals -> {
            if (festivals != null) {
                Log.d(TAG, "Received festivals update: " + festivals.size() + " festivals");
                festivalsResult.setValue(Result.success(festivals));
            } else {
                Log.e(TAG, "Received null festivals");
                festivalsResult.setValue(Result.error("Failed to load festivals"));
            }
        };
        
        errorObserver = error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Received error: " + error);
                festivalsResult.setValue(Result.error(error));
            }
        };
        
        cacheObserver = isFromCache -> {
            if (isFromCache != null && isFromCache) {
                Log.d(TAG, "Data loaded from cache");
                showCacheSnackbar.setValue(true);
            } else {
                showCacheSnackbar.setValue(false);
            }
        };
    }
    
    /**
     * Load festivals from the repository
     */
    public void loadFestivals() {
        // Set loading state
        festivalsResult.setValue(Result.loading());
        
        // Add sources if they haven't been added yet
        if (!festivalsResult.hasActiveObservers()) {
            festivalsResult.addSource(upcomingFestivalsSource, festivalsObserver);
            festivalsResult.addSource(errorMessageSource, errorObserver);
            festivalsResult.addSource(isFromCacheSource, cacheObserver);
        }
        
        // Load festivals from repository
        repository.loadUpcomingFestivals();
        
        // Schedule countdown notifications, but only once per day
        scheduleNotificationsIfNeeded();
    }
    
    /**
     * Refresh festivals from the server
     */
    public void refreshFestivals() {
        // Set loading state
        festivalsResult.setValue(Result.loading());
        
        // Refresh festivals from repository
        repository.refreshUpcomingFestivals();
        
        // Schedule countdown notifications, but only once per day
        scheduleNotificationsIfNeeded();
    }
    
    /**
     * Schedule notifications if they haven't been scheduled today
     */
    private void scheduleNotificationsIfNeeded() {
        if (getApplication() != null) {
            SharedPreferences prefs = getApplication().getSharedPreferences(
                    "festival_notifications", Context.MODE_PRIVATE);
            
            // Get today's date as a string (yyyy-MM-dd) using server time
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String today = dateFormat.format(TimeUtils.getCurrentServerTime());
            
            // Check if we've already scheduled notifications today
            String lastScheduled = prefs.getString("last_notification_schedule", "");
            if (!lastScheduled.equals(today)) {
                // Schedule notifications
                repository.scheduleCountdownNotifications();
                
                // Save that we've scheduled notifications today
                prefs.edit().putString("last_notification_schedule", today).apply();
                
                Log.d(TAG, "Scheduled countdown notifications for today (server time): " + today);
            } else {
                Log.d(TAG, "Notifications already scheduled today (server time), skipping");
            }
        }
    }
    
    /**
     * Get the festivals result
     * @return LiveData containing the festivals result
     */
    public LiveData<Result<List<Festival>>> getFestivals() {
        return festivalsResult;
    }
    
    /**
     * Get the loading state
     * @return LiveData containing the loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return repository.getIsLoading();
    }
    
    /**
     * Get the error message
     * @return LiveData containing the error message
     */
    public LiveData<String> getErrorMessage() {
        return repository.getErrorMessage();
    }
    
    /**
     * Get the unread count
     * @return LiveData containing the unread count
     */
    public LiveData<Integer> getUnreadCount() {
        return repository.getUnreadCount();
    }
    
    /**
     * Get whether to show the cache snackbar
     * @return LiveData containing whether to show the cache snackbar
     */
    public LiveData<Boolean> getShowCacheSnackbar() {
        return showCacheSnackbar;
    }
    
    /**
     * Get the stale data state as LiveData
     * @return LiveData with stale data state
     */
    public LiveData<Boolean> getStaleData() {
        return staleData;
    }
    
    /**
     * Set the stale data state
     * @param isStale Whether the data is stale
     */
    public void setStaleData(boolean isStale) {
        Log.d(TAG, "Setting stale data state: " + isStale);
        staleData.setValue(isStale);
    }
    
    /**
     * Set the current category
     * @param category The category to set
     */
    public void setCategory(String category) {
        if (!category.equals(currentCategory.getValue())) {
            currentCategory.setValue(category);
            loadFestivalsByCategory(category);
        }
    }
    
    /**
     * Get the current category
     * @return The current category
     */
    public String getCurrentCategory() {
        return currentCategory.getValue();
    }
    
    /**
     * Load festivals by category
     * @param category The category to load
     */
    private void loadFestivalsByCategory(String category) {
        // Set loading state
        festivalsResult.setValue(Result.loading());
        
        if ("All".equals(category)) {
            // Load all festivals
            repository.loadUpcomingFestivals();
        } else {
            // Load festivals by category
            repository.loadUpcomingFestivalsByCategory(category);
        }
    }
    
    /**
     * Mark all festivals as read
     */
    public void markAllAsRead() {
        repository.markAllAsRead();
    }
    
    /**
     * Mark a festival as read
     * @param festivalId The ID of the festival to mark as read
     */
    public void markAsRead(String festivalId) {
        repository.markAsRead(festivalId);
    }
    
    /**
     * Get upcoming festivals
     * @return LiveData containing upcoming festivals
     */
    public LiveData<List<Festival>> getUpcomingFestivals() {
        return repository.getUpcomingFestivals();
    }
    
    /**
     * Clear the cache snackbar flag
     */
    public void clearCacheSnackbarFlag() {
        showCacheSnackbar.setValue(false);
    }
    
    /**
     * Clear memory cache
     * Call this when the app is sent to the background
     */
    public void clearMemoryCache() {
        repository.clearMemoryCache();
    }
    
    /**
     * Clear all cache
     * Call this when the app is first launched
     */
    public void clearAllCache() {
        repository.clearAllCache();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // Remove sources when ViewModel is cleared
        festivalsResult.removeSource(upcomingFestivalsSource);
        festivalsResult.removeSource(errorMessageSource);
        festivalsResult.removeSource(isFromCacheSource);
    }
    
    private void setupFestivalNotificationWorker() {
        // Define constraints - we want to run the worker when the device is connected to the network
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        // Create a periodic work request that runs once a day
        PeriodicWorkRequest festivalCheckRequest =
                new PeriodicWorkRequest.Builder(FestivalNotificationWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();
        
        // Enqueue the work request
        WorkManager.getInstance(getApplication())
                .enqueueUniquePeriodicWork(
                        FESTIVAL_NOTIFICATION_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        festivalCheckRequest);
    }
}
