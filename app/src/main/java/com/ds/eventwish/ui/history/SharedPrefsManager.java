package com.ds.eventwish.ui.history;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.ui.home.GreetingItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SharedPrefsManager {
    private final MutableLiveData<List<SharedWish>> historyItemsLiveData = new MutableLiveData<>();
    private static final String TAG = "SharedPrefsManager";
    private static final String PREF_NAME = "wish_history";
    private static final String KEY_HISTORY = "history_items";
    private final SharedPreferences prefs;
    private final Gson gson;

    public SharedPrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadHistoryFromPrefs();
        debugPrintHistory();

    }
    private void loadHistoryFromPrefs() {
        List<SharedWish> wishes = getHistoryItems();
        historyItemsLiveData.setValue(wishes);
    }

    public void saveHistoryItem(SharedWish wish) {
        try {
            List<SharedWish> currentItems = getHistoryItems();

            // Remove existing item with same shortcode if exists
             currentItems.removeIf(item ->
             item.getShortCode().equals(wish.getShortCode())
         );

         if (wish.getPreviewUrl() == null || wish.getPreviewUrl().isEmpty()) {
            Log.w(TAG, "Saving wish without preview URL");
        } else {
            Log.d(TAG, "Saving wish with preview URL: " + wish.getPreviewUrl());
        }

            if (wish == null || wish.getShortCode() == null || wish.getRecipientName() == null || wish.getSenderName() == null) {
                Log.e(TAG, "Attempted to save an invalid wish: " + wish);
                return;  // 🚨 Prevent saving incomplete data
            }

            List<SharedWish> existingWishes = getHistoryItems();
            existingWishes.add(0, wish);

            prefs.edit().putString(KEY_HISTORY, gson.toJson(existingWishes)).apply();
            historyItemsLiveData.setValue(existingWishes); // Use setValue instead of postValue
            notifyHistoryChanged();

            Log.d(TAG, "Saved wish with preview URL: " + wish.getPreviewUrl());

            Log.d(TAG, "Saved wish: " + wish.getShortCode() + " → " + wish.getRecipientName() + " -> " +wish.getSenderName());

        } catch (Exception e) {
            Log.e(TAG, "Error saving history item", e);
        }
    }

    public LiveData<List<SharedWish>> observeHistoryChanges() {
        if (historyItemsLiveData.getValue() == null) {
            List<SharedWish> items = getHistoryItems();
            historyItemsLiveData.setValue(items);
            Log.d(TAG, "Initial history loaded: " + items.size() + " items");
        }
      return historyItemsLiveData;
    }

    public void notifyHistoryChanged() {
        List<SharedWish> items = getHistoryItems();
        historyItemsLiveData.postValue(items);
    }

//    private void setupObservers() {
//        prefs.observeHistoryChanges().observe(getViewLifecycleOwner(), items -> {
//            if (items != null) {
//                Log.d(TAG, "History items updated: " + items.size());
//                updateHistoryList(new ArrayList<>(items));
//            }
//        });
//
//        // ...existing code...
//    }

    public List<SharedWish> getHistoryItems() {
        try {
            String json = prefs.getString(KEY_HISTORY, "[]");
            Type type = new TypeToken<ArrayList<SharedWish>>() {}.getType();
            List<SharedWish> wishes = gson.fromJson(json, type);
            return (wishes != null) ? wishes : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting history items", e);
            return new ArrayList<>();
        }
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
        historyItemsLiveData.postValue(new ArrayList<>());
        notifyHistoryChanged();
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date());
    }

    // Add this debug method to SharedPrefsManager
public void debugPrintHistory() {
    String json = prefs.getString(KEY_HISTORY, "[]");
    Type type = new TypeToken<ArrayList<SharedWish>>() {}.getType();
    List<SharedWish> wishes = gson.fromJson(json, type);

    Log.d(TAG, "Current history content (" + (wishes != null ? wishes.size() : 0) + " items):");
    Map<String, ?> allPrefs = prefs.getAll();
    Log.d(TAG, "All SharedPreferences contents:");
    for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
        Log.d(TAG, entry.getKey() + ": " + entry.getValue());
    }
    }
  }
// Add this method to SharedPrefsManager
