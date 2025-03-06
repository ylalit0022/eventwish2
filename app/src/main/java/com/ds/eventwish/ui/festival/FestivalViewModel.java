package com.ds.eventwish.ui.festival;

import android.app.Application;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FestivalViewModel extends AndroidViewModel {
    private static final String TAG = "FestivalViewModel";
    private static final String FESTIVAL_NOTIFICATION_WORK = "festival_notification_work";
    
    private final FestivalRepository repository;
    private final MutableLiveData<String> currentCategory = new MutableLiveData<>("All");
    private final MediatorLiveData<Result<List<Festival>>> festivalsResult = new MediatorLiveData<>();
    
    // Keep references to our observers to prevent memory leaks and duplicate observers
    private Observer<List<Festival>> festivalsObserver = null;
    private Observer<String> errorObserver = null;
    private LiveData<List<Festival>> upcomingFestivalsSource = null;
    private LiveData<String> errorMessageSource = null;
    
    public FestivalViewModel(@NonNull Application application) {
        super(application);
        repository = FestivalRepository.getInstance(application);
        
        // Set up the worker for checking upcoming festivals
        setupFestivalNotificationWorker();
        
        // Initialize the sources
        upcomingFestivalsSource = repository.getUpcomingFestivals();
        errorMessageSource = repository.getErrorMessage();
        
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
        }
    }
    
    /**
     * Force refresh festivals from the server
     */
    public void refreshFestivals() {
        // Set loading state
        festivalsResult.setValue(Result.loading());
        
        // Force refresh from the repository
        repository.refreshUpcomingFestivals();
    }
    
    public LiveData<Result<List<Festival>>> getFestivals() {
        return festivalsResult;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return repository.getIsLoading();
    }
    
    public LiveData<String> getErrorMessage() {
        return repository.getErrorMessage();
    }
    
    public LiveData<Integer> getUnreadCount() {
        return repository.getUnreadCount();
    }
    
    public void setCategory(String category) {
        if (!category.equals(currentCategory.getValue())) {
            currentCategory.setValue(category);
            loadFestivals();
        }
    }
    
    public String getCurrentCategory() {
        return currentCategory.getValue();
    }
    
    public void markAllAsRead() {
        repository.markAllAsRead();
    }
    
    public void markAsRead(String festivalId) {
        repository.markAsRead(festivalId);
    }
    
    /**
     * Get upcoming festivals for the notification fragment
     * @return LiveData containing a list of upcoming festivals
     */
    public LiveData<List<Festival>> getUpcomingFestivals() {
        return repository.getUpcomingFestivals();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove sources when ViewModel is cleared
        festivalsResult.removeSource(upcomingFestivalsSource);
        festivalsResult.removeSource(errorMessageSource);
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
