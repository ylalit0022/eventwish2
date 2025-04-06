package com.ds.eventwish.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Shared ViewModel for common functionality across fragments
 */
public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Integer> navigationEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isNetworkConnected = new MutableLiveData<>(true);

    public LiveData<Integer> getNavigationEvent() {
        return navigationEvent;
    }

    public void navigate(int destinationId) {
        navigationEvent.setValue(destinationId);
    }

    public LiveData<Boolean> getIsNetworkConnected() {
        return isNetworkConnected;
    }
    
    public void setNetworkConnected(boolean isConnected) {
        isNetworkConnected.setValue(isConnected);
    }

    public void refreshData() {
        // Implement data refresh logic
    }
} 