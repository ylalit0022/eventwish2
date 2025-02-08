package com.ds.eventwish.ui.history;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.SharedWish;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private static final String TAG = "HistoryViewModel";
    private static final String PREF_NAME = "wish_history"; 
    private static final String KEY_HISTORY = "history_items";

    private final SharedPreferences preferences;
    private final Gson gson;
    private final MutableLiveData<List<SharedWish>> historyItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HistoryViewModel(Application application) {
        super(application);
        Log.d(TAG, "Initializing HistoryViewModel");
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadHistory();
    }

    public void loadHistory() {
        Log.d(TAG, "loadHistory: Loading history from local storage");
        loading.setValue(true);
        error.setValue(null);

        try {
            String jsonHistory = preferences.getString(KEY_HISTORY, "[]"); 
            List<SharedWish> wishes;
            
            if (jsonHistory != null) {
                Type type = new TypeToken<List<SharedWish>>(){}.getType();
                wishes = gson.fromJson(jsonHistory, type);
                Log.d(TAG, "Loaded " + wishes.size() + " items from local storage");
            } else {
                wishes = new ArrayList<>();
                Log.d(TAG, "No history found in local storage");
            }
            
            historyItems.setValue(wishes);
            error.setValue(null);
        } catch (Exception e) {
            Log.e(TAG, "Error loading history from local storage", e);
            error.setValue("Failed to load history");
            historyItems.setValue(new ArrayList<>());
        }
        
        loading.setValue(false);
    }

    public void clearHistory() {
        Log.d(TAG, "clearHistory: Clearing history from local storage");
        loading.setValue(true);
        error.setValue(null);

        try {
            // Clear both the SharedPreferences and the LiveData
            preferences.edit()
                .remove(KEY_HISTORY)
                .commit(); // Using commit() instead of apply() for immediate effect
            
            historyItems.setValue(new ArrayList<>());
            Log.d(TAG, "History cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing history", e);
            error.setValue("Failed to clear history");
        } finally {
            loading.setValue(false);
        }
    }

    public void addToHistory(SharedWish wish) {
        if (wish == null || wish.getShortCode() == null) {
            Log.e(TAG, "Cannot add null wish or wish without shortCode to history");
            return;
        }

        Log.d(TAG, "addToHistory: Adding wish to history: " + wish.getShortCode());
        if (wish.getPreviewUrl() != null) {
            Log.d(TAG, "Preview URL being saved: " + wish.getPreviewUrl());
        }
        
        List<SharedWish> currentItems = historyItems.getValue();
        if (currentItems == null) {
            currentItems = new ArrayList<>();
        }

        // Remove if already exists (to avoid duplicates)
        currentItems.removeIf(existingWish -> 
            existingWish.getShortCode() != null && 
            existingWish.getShortCode().equals(wish.getShortCode())
        );
        
        // Set creation time if not set
        if (wish.getCreatedAt() == null) {
            wish.setCreatedAt(new Date());
        }
        
        // Add to beginning of list
        currentItems.add(0, wish);
        
        try {
            // Save to SharedPreferences
            String jsonHistory = gson.toJson(currentItems);
            preferences.edit().putString(KEY_HISTORY, jsonHistory).apply();
            
            // Update UI
            historyItems.setValue(currentItems);
            Log.d(TAG, "Wish added to history successfully, total items: " + currentItems.size());
        } catch (Exception e) {
            Log.e(TAG, "Error saving history to local storage", e);
            error.setValue("Failed to save history");
        }
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
}
