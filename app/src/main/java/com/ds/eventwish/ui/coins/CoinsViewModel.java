package com.ds.eventwish.ui.coins;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;

import com.ds.eventwish.data.repository.CoinsRepository;

import javax.inject.Inject;

/**
 * ViewModel for managing coins data throughout the app.
 * Provides LiveData for coins balance and methods for refreshing coins.
 */
public class CoinsViewModel extends ViewModel {
    private static final String TAG = "CoinsViewModel";

    private final CoinsRepository coinsRepository;
    private final MutableLiveData<Boolean> refreshingCoinsLiveData = new MutableLiveData<>(false);

    @Inject
    public CoinsViewModel(CoinsRepository coinsRepository) {
        this.coinsRepository = coinsRepository;
        Log.d(TAG, "CoinsViewModel initialized");
    }

    /**
     * Get LiveData for observing coins balance
     * @return LiveData containing the Coins object
     */
    public LiveData<Integer> getCoinsLiveData() {
        return coinsRepository.getCoinsLiveData();
    }

    /**
     * Get LiveData for observing whether features are unlocked
     * @return LiveData<Boolean> true if features are unlocked, false otherwise
     */
    public LiveData<Boolean> getIsUnlockedLiveData() {
        return coinsRepository.getIsUnlockedLiveData();
    }

    /**
     * Force refresh coins count from the repository
     * This should be called after any operation that changes the coins balance,
     * such as watching a rewarded ad, purchasing coins, or using coins for features.
     */
    public void refreshCoinsCount() {
        Log.d(TAG, "Refreshing coins count");
        refreshingCoinsLiveData.setValue(true);
        
        try {
            coinsRepository.forceRefreshCoinsLiveData();
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing coins", e);
        } finally {
            refreshingCoinsLiveData.setValue(false);
        }
    }

    /**
     * Get LiveData that indicates whether coins are currently being refreshed
     * @return LiveData<Boolean> true if refreshing, false otherwise
     */
    public LiveData<Boolean> isRefreshingCoins() {
        return refreshingCoinsLiveData;
    }

    /**
     * Add coins to the user's balance
     * @param amount the amount of coins to add
     */
    public void addCoins(int amount) {
        Log.d(TAG, "Adding " + amount + " coins");
        coinsRepository.addCoins(amount);
        refreshCoinsCount();
    }

    /**
     * Get the current coins balance
     * @return the current coins balance
     */
    public int getCurrentCoins() {
        return coinsRepository.getCurrentCoins();
    }

    /**
     * Get LiveData for observing the remaining time for unlocked features
     * @return LiveData containing the remaining time in milliseconds
     */
    public LiveData<Long> getRemainingTimeLiveData() {
        return coinsRepository.getRemainingTimeLiveData();
    }

    /**
     * Check if the feature is currently unlocked
     * @return true if the feature is unlocked, false otherwise
     */
    public boolean isFeatureUnlocked() {
        return coinsRepository.isFeatureUnlocked();
    }

    /**
     * Verify the unlock status with the repository
     * This triggers validation checks against time manipulation
     */
    public void verifyUnlockStatus() {
        Log.d(TAG, "Verifying unlock status");
        coinsRepository.validateUnlockStatus();
    }

    /**
     * Unlock a feature for a specific duration
     * @param duration the duration in days to unlock the feature
     */
    public void unlockFeature(int duration) {
        Log.d(TAG, "Unlocking feature for " + duration + " days");
        coinsRepository.unlockFeature(duration);
        refreshCoinsCount();
    }
}
