package com.ds.eventwish.ui.coins;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.repository.CoinsRepository;

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

    private final CoinsRepository coinsRepository;
    private final MutableLiveData<Integer> coins = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isUnlocked = new MutableLiveData<>(false);
    private final MutableLiveData<Long> remainingTime = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<Integer> transactionCoins = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> refreshSuccess = new MutableLiveData<>(false);

    public CoinsViewModel(@NonNull Application application) {
        super(application);
        coinsRepository = CoinsRepository.getInstance(application);
        
        // Observe repository LiveData
        coinsRepository.getCoinsLiveData().observeForever(newCoins -> {
            coins.setValue(newCoins);
        });
        
        coinsRepository.getIsUnlockedLiveData().observeForever(newIsUnlocked -> {
            isUnlocked.setValue(newIsUnlocked);
        });
        
        coinsRepository.getRemainingTimeLiveData().observeForever(newRemainingTime -> {
            remainingTime.setValue(newRemainingTime);
        });
        
        // Force refresh to get latest coins status
        refreshCoinsStatus();
    }
    
    public LiveData<Integer> getCoins() {
        return coins;
    }
    
    public LiveData<Boolean> getIsUnlocked() {
        return isUnlocked;
    }
    
    public LiveData<Long> getRemainingTime() {
        return remainingTime;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Integer> getTransactionCoins() {
        return transactionCoins;
    }
    
    public LiveData<Boolean> getRefreshing() {
        return refreshing;
    }
    
    public LiveData<Boolean> getRefreshSuccess() {
        return refreshSuccess;
    }
    
    public void addCoins(int amount) {
        isLoading.setValue(true);
        errorMessage.setValue("");
        
        coinsRepository.addCoins(amount);
        
        // Update transaction amount
        transactionCoins.setValue(amount);
    }
    
    public void unlockFeature(int duration) {
        isLoading.setValue(true);
        errorMessage.setValue("");
        
        coinsRepository.unlockFeature(duration);
    }
    
    public void refreshCoinsStatus() {
        refreshing.setValue(true);
        
        MutableLiveData<Boolean> result = coinsRepository.refreshFromServer();
        result.observeForever(success -> {
            refreshing.setValue(false);
            refreshSuccess.setValue(success);
            
            if (success) {
                errorMessage.setValue("");
            }
        });
    }
    
    public void setError(String error) {
        errorMessage.setValue(error);
    }
    
    public boolean shouldShowErrorDialog() {
        String error = errorMessage.getValue();
        return error != null && !error.isEmpty();
    }
    
    public void clearError() {
        errorMessage.setValue("");
    }
    
    public void clearTransactionCoins() {
        transactionCoins.setValue(0);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove observers to avoid memory leaks
        coinsRepository.getCoinsLiveData().removeObserver(newCoins -> {});
        coinsRepository.getIsUnlockedLiveData().removeObserver(newIsUnlocked -> {});
        coinsRepository.getRemainingTimeLiveData().removeObserver(newRemainingTime -> {});
    }
}
