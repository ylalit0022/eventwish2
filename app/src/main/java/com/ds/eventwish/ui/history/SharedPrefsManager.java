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

    }
    private void loadHistoryFromPrefs() {
        List<SharedWish> wishes = getHistoryItems();
        historyItemsLiveData.setValue(wishes);
    }

    public void saveHistoryItem(SharedWish wish) {
        try {
            if (wish == null || wish.getShortCode() == null || wish.getRecipientName() == null || wish.getSenderName() == null) {
                Log.e(TAG, "Attempted to save an invalid wish: " + wish);
                return;  // 🚨 Prevent saving incomplete data
            }

            List<SharedWish> existingWishes = getHistoryItems();
            existingWishes.add(0, wish);

            prefs.edit().putString(KEY_HISTORY, gson.toJson(existingWishes)).apply();
            historyItemsLiveData.setValue(existingWishes); // Use setValue instead of postValue

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
}
