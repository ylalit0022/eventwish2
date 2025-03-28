package com.ds.eventwish.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.repository.CoinsRepository;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.TimeUnit;

public class CoinsViewModel extends AndroidViewModel {
    private static final String TAG = "CoinsViewModel";
    private final CoinsRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Add a tracking variable for refresh state
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    
    // Add single event LiveData for coin update events
    private final MutableLiveData<Integer> coinUpdateEvent = new MutableLiveData<>();
    
    public CoinsViewModel(Application application) {
        super(application);
        repository = CoinsRepository.getInstance(application);
    }
    
    public LiveData<Integer> getCoinsLiveData() {
        return repository.getCoinsLiveData();
    }
    
    public LiveData<Boolean> getIsUnlockedLiveData() {
        return repository.getIsUnlockedLiveData();
    }
    
    public LiveData<Long> getRemainingTimeLiveData() {
        return repository.getRemainingTimeLiveData();
    }
    
    /**
     * Get the refreshing state LiveData
     */
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    /**
     * Get LiveData for single update events
     * This can be used to trigger UI updates even when the coin value hasn't changed
     */
    public LiveData<Integer> getCoinUpdateEvent() {
        return coinUpdateEvent;
    }
    
    public void addCoins(int amount) {
        repository.addCoins(amount);
    }
    
    public void unlockFeature(int duration) {
        repository.unlockFeature(duration);
    }
    
    public boolean isFeatureUnlocked() {
        return repository.isFeatureUnlocked();
    }
    
    public int getCurrentCoins() {
        return repository.getCurrentCoins();
    }
    
    public long getRemainingTime() {
        return repository.getRemainingTime();
    }
    
    /**
     * Explicitly refresh the coins count from the repository with retry logic
     */
    public void refreshCoinsCount() {
        refreshCoinsCount(null);
    }
    
    /**
     * Interface for refresh operation callbacks
     */
    public interface RefreshCallback {
        void onRefreshComplete(boolean success);
    }
    
    /**
     * Refresh coins with callback and retry logic
     * @param callback Optional callback to notify when refresh completes
     */
    public void refreshCoinsCount(RefreshCallback callback) {
        Log.d(TAG, "Explicitly refreshing coins count");
        
        // Set refreshing state
        isRefreshing.setValue(true);
        
        try {
            // Ensure we're on the main thread for LiveData updates
            mainHandler.post(() -> {
                // Force the LiveData to emit a new value
                repository.forceRefreshCoinsLiveData();
                
                // Add extra logging to track the update
                int currentCoins = repository.getCurrentCoins();
                Log.d(TAG, "Current coins after initial refresh: " + currentCoins);
                
                // Broadcast coin update immediately for quick UI response
                broadcastCoinUpdate();
                
                // Schedule a second refresh after a short delay to ensure updates propagate
                mainHandler.postDelayed(() -> {
                    // Second refresh
                    repository.forceRefreshCoinsLiveData();
                    int updatedCoins = repository.getCurrentCoins();
                    Log.d(TAG, "Coins after delayed refresh: " + updatedCoins);
                    
                    // Broadcast second update
                    broadcastCoinUpdate();
                    
                    // Set refreshing state to false
                    isRefreshing.postValue(false);
                    
                    // Call callback if provided
                    if (callback != null) {
                        callback.onRefreshComplete(true);
                    }
                }, 800);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing coins", e);
            isRefreshing.postValue(false);
            
            // Call callback with failure if provided
            if (callback != null) {
                callback.onRefreshComplete(false);
            }
        }
    }
    
    /**
     * Force a background refresh of all coin data
     * This does a complete refresh from the server
     * @param callback Optional callback to notify when the operation completes
     */
    public void forceBackgroundRefresh(RefreshCallback callback) {
        Log.d(TAG, "Forcing background refresh of all coin data");
        
        // Set refreshing state
        isRefreshing.setValue(true);
        
        try {
            // Call repository to do full refresh from server and observe the result
            final LiveData<Boolean> refreshResult = repository.refreshFromServer();
            final boolean[] responseReceived = {false}; // Use array to allow modification in lambda
            
            // Create the observer
            androidx.lifecycle.Observer<Boolean> observer = new androidx.lifecycle.Observer<Boolean>() {
                @Override
                public void onChanged(Boolean success) {
                    try {
                        // Remove observer to prevent memory leaks
                        refreshResult.removeObserver(this);
                        responseReceived[0] = true;
                        
                        Log.d(TAG, "Server refresh completed with result: " + success);
                        
                        // Force UI update on main thread
                        mainHandler.post(() -> {
                            repository.forceRefreshCoinsLiveData();
                            int currentCoins = repository.getCurrentCoins();
                            Log.d(TAG, "Background refresh completed, coins: " + currentCoins);
                            
                            // Verify unlock status
                            repository.validateUnlockStatus();
                            
                            // Broadcast coin update
                            broadcastCoinUpdate();
                            
                            // Set refreshing state to false
                            isRefreshing.setValue(false);
                            
                            // Call callback if provided
                            if (callback != null) {
                                callback.onRefreshComplete(success != null && success);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling refresh result", e);
                        handleRefreshError(callback);
                    }
                }
            };
            
            // Add observer
            refreshResult.observeForever(observer);
            
            // Set a timeout to prevent observer from hanging indefinitely
            mainHandler.postDelayed(() -> {
                try {
                    // Check if we received a response
                    if (!responseReceived[0]) {
                        Log.w(TAG, "Refresh operation timed out after 15 seconds");
                        refreshResult.removeObserver(observer);
                        handleRefreshError(callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in timeout handler", e);
                }
            }, 15000); // 15 second timeout
        } catch (Exception e) {
            Log.e(TAG, "Error in background refresh", e);
            handleRefreshError(callback);
        }
    }
    
    // Overload for backward compatibility
    public void forceBackgroundRefresh() {
        forceBackgroundRefresh(null);
    }
    
    /**
     * Handle refresh error cases
     */
    private void handleRefreshError(RefreshCallback callback) {
        // Set refreshing state to false
        mainHandler.post(() -> {
            isRefreshing.setValue(false);
            
            // Try to refresh from local database as fallback
            repository.forceRefreshCoinsLiveData();
            
            // Broadcast coins update even in error case
            broadcastCoinUpdate();
            
            // Call callback with failure if provided
            if (callback != null) {
                callback.onRefreshComplete(false);
            }
        });
    }
    
    /**
     * Verify the unlock status of features
     * This should be called when the dialog is shown to ensure accuracy
     */
    public void verifyUnlockStatus() {
        Log.d(TAG, "Verifying unlock status");
        repository.validateUnlockStatus();
    }
    
    /**
     * Broadcast a coin update event to all observers
     * This will trigger UI updates even if the actual coin value hasn't changed
     */
    private void broadcastCoinUpdate() {
        int currentCoins = repository.getCurrentCoins();
        Log.d(TAG, "Broadcasting coin update event with value: " + currentCoins);
        coinUpdateEvent.setValue(currentCoins);
    }
    
    /**
     * Format the remaining time in a user-friendly way
     * @param remainingTimeMs Time in milliseconds
     * @return Formatted string like "2 days, 5 hours remaining"
     */
    public String formatRemainingTime(long remainingTimeMs) {
        if (remainingTimeMs <= 0) {
            return ""; // No time remaining
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(remainingTimeMs);
        long hours = TimeUnit.MILLISECONDS.toHours(remainingTimeMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs) % 60;
        
        StringBuilder result = new StringBuilder();
        
        // Add days
        if (days > 0) {
            result.append(days).append(days == 1 ? " day" : " days");
        }
        
        // Add hours
        if (hours > 0 || days > 0) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        
        // Add minutes (only if less than a day)
        if (minutes > 0 && days == 0) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        
        if (result.length() > 0) {
            result.append(" remaining");
        }
        
        return result.toString();
    }
    
    /**
     * Get the remaining time in a format suitable for display
     * @return Formatted time string
     */
    public String getFormattedRemainingTime() {
        long remainingTime = repository.getRemainingTime();
        return formatRemainingTime(remainingTime);
    }
} 