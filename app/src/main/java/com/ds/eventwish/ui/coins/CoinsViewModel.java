package com.ds.eventwish.ui.coins;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.repository.CoinsRepository;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * ViewModel for managing coins data throughout the app.
 * Provides LiveData for coins balance and methods for refreshing coins.
 */
public class CoinsViewModel extends AndroidViewModel {
    private static final String TAG = "CoinsViewModel";
    
    /**
     * Interface for refresh operation callbacks
     */
    public interface RefreshCallback {
        void onRefreshComplete(boolean success);
    }

    private final CoinsRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> coinUpdateEvent = new MutableLiveData<>();

    @Inject
    public CoinsViewModel(Application application, CoinsRepository repository) {
        super(application);
        this.repository = repository;
        Log.d(TAG, "CoinsViewModel initialized");
    }

    /**
     * Get LiveData for observing coins balance
     * @return LiveData containing the Coins object
     */
    public LiveData<Integer> getCoinsLiveData() {
        return repository.getCoinsLiveData();
    }

    /**
     * Get LiveData for observing whether features are unlocked
     * @return LiveData<Boolean> true if features are unlocked, false otherwise
     */
    public LiveData<Boolean> getIsUnlockedLiveData() {
        return repository.getIsUnlockedLiveData();
    }

    /**
     * Force refresh coins count from the repository
     * This should be called after any operation that changes the coins balance,
     * such as watching a rewarded ad, purchasing coins, or using coins for features.
     */
    public void refreshCoinsCount() {
        Log.d(TAG, "Refreshing coins count");
        isRefreshing.setValue(true);
        
        try {
            repository.forceRefreshCoinsLiveData();
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing coins", e);
        } finally {
            isRefreshing.setValue(false);
        }
    }

    /**
     * Refresh coins with callback and retry logic
     * @param callback Optional callback to notify when refresh completes
     */
    public void refreshCoinsCount(RefreshCallback callback) {
        Log.d(TAG, "Explicitly refreshing coins count");
        isRefreshing.setValue(true);
        
        try {
            mainHandler.post(() -> {
                repository.forceRefreshCoinsLiveData();
                broadcastCoinUpdate();

                mainHandler.postDelayed(() -> {
                    repository.forceRefreshCoinsLiveData();
                    broadcastCoinUpdate();
                    isRefreshing.postValue(false);
                    if (callback != null) {
                        callback.onRefreshComplete(true);
                    }
                }, 800);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing coins", e);
            handleRefreshError(callback);
        }
    }

    /**
     * Get LiveData that indicates whether coins are currently being refreshed
     * @return LiveData<Boolean> true if refreshing, false otherwise
     */
    public LiveData<Boolean> isRefreshingCoins() {
        return isRefreshing;
    }

    /**
     * Add coins to the user's balance
     * @param amount the amount of coins to add
     */
    public void addCoins(int amount) {
        Log.d(TAG, "Adding " + amount + " coins");
        repository.addCoins(amount);
        refreshCoinsCount();
    }

    /**
     * Get the current coins balance
     * @return the current coins balance
     */
    public int getCurrentCoins() {
        return repository.getCurrentCoins();
    }

    /**
     * Get LiveData for observing the remaining time for unlocked features
     * @return LiveData containing the remaining time in milliseconds
     */
    public LiveData<Long> getRemainingTimeLiveData() {
        return repository.getRemainingTimeLiveData();
    }

    /**
     * Check if the feature is currently unlocked
     * @return true if the feature is unlocked, false otherwise
     */
    public boolean isFeatureUnlocked() {
        return repository.isFeatureUnlocked();
    }

    /**
     * Verify the unlock status with the repository
     * This triggers validation checks against time manipulation
     */
    public void verifyUnlockStatus() {
        Log.d(TAG, "Verifying unlock status");
        repository.validateUnlockStatus();
    }

    /**
     * Unlock a feature for a specific duration
     * @param duration the duration in days to unlock the feature
     */
    public void unlockFeature(int duration) {
        Log.d(TAG, "Unlocking feature for " + duration + " days");
        repository.unlockFeature(duration);
        refreshCoinsCount();
    }

    /**
     * Handle refresh error cases
     */
    private void handleRefreshError(RefreshCallback callback) {
        mainHandler.post(() -> {
            isRefreshing.setValue(false);
            repository.forceRefreshCoinsLiveData();
            broadcastCoinUpdate();
            if (callback != null) {
                callback.onRefreshComplete(false);
            }
        });
    }

    /**
     * Broadcast a coin update event to all observers
     */
    private void broadcastCoinUpdate() {
        int currentCoins = repository.getCurrentCoins();
        Log.d(TAG, "Broadcasting coin update event with value: " + currentCoins);
        coinUpdateEvent.setValue(currentCoins);
    }
}
