package com.ds.eventwish.ui.history;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.SharedWish;
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
    private final MutableLiveData<List<SharedWish>> _historyItems = new MutableLiveData<>();
    private static final String TAG = "SharedPrefsManager";
    private static final String PREF_NAME = "wish_history";
    private static final String KEY_HISTORY = "history_items";
    private static final String KEY_ANALYTICS_CONSENT = "analytics_consent";
    private static final String KEY_ANALYTICS_CONSENT_SHOWN = "analytics_consent_shown";
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;

    public SharedPrefsManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        
        loadHistoryFromPrefs();
        debugPrintHistory();
    }

    public void shareWish(Activity activity, SharedWish wish, ShareCallback callback) {
        if (wish == null || callback == null) {
            Log.e(TAG, "Invalid parameters for sharing wish");
            if (callback != null) {
                callback.onError("Invalid wish data");
            }
            return;
        }

        // Share directly since ads are disabled
        Log.d(TAG, "Proceeding with share");
        callback.onShare(wish);
    }

    // Callback interface for share operations
    public interface ShareCallback {
        void onShare(SharedWish wish);
        void onError(String errorMessage);
    }

    private void loadHistoryFromPrefs() {
        try {
            List<SharedWish> wishes = getHistoryItems();
            _historyItems.postValue(wishes);
            Log.d(TAG, "Loaded " + (wishes != null ? wishes.size() : 0) + " history items");
        } catch (Exception e) {
            Log.e(TAG, "Error loading history from preferences", e);
            _historyItems.postValue(new ArrayList<>());
        }
    }

    public void saveHistoryItem(SharedWish wish) {
        if (wish == null || wish.getShortCode() == null || wish.getRecipientName() == null || wish.getSenderName() == null) {
            Log.e(TAG, "Attempted to save an invalid wish: " + wish);
            return;
        }

        try {
            List<SharedWish> currentItems = getHistoryItems();

            // Remove existing item with same shortcode if exists
            currentItems.removeIf(item -> 
                item.getShortCode() != null && item.getShortCode().equals(wish.getShortCode())
            );

            if (wish.getPreviewUrl() == null || wish.getPreviewUrl().isEmpty()) {
                Log.w(TAG, "Saving wish without preview URL");
            } else {
                Log.d(TAG, "Saving wish with preview URL: " + wish.getPreviewUrl());
            }

            currentItems.add(0, wish);

            prefs.edit()
                 .putString(KEY_HISTORY, gson.toJson(currentItems))
                 .apply();
            
            _historyItems.postValue(currentItems);
            notifyHistoryChanged();

            Log.d(TAG, String.format("Saved wish: %s → %s → %s", 
                wish.getShortCode(), wish.getRecipientName(), wish.getSenderName()));

        } catch (Exception e) {
            Log.e(TAG, "Error saving history item", e);
        }
    }

    public LiveData<List<SharedWish>> observeHistoryChanges() {
        if (_historyItems.getValue() == null) {
            List<SharedWish> items = getHistoryItems();
            _historyItems.postValue(items);
            Log.d(TAG, "Initial history loaded: " + items.size() + " items");
        }
        return _historyItems;
    }

    public void notifyHistoryChanged() {
        List<SharedWish> items = getHistoryItems();
        _historyItems.postValue(items);
    }

    public List<SharedWish> getHistoryItems() {
        try {
            String json = prefs.getString(KEY_HISTORY, "[]");
            Type type = new TypeToken<ArrayList<SharedWish>>() {}.getType();
            List<SharedWish> wishes = gson.fromJson(json, type);
            return wishes != null ? wishes : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting history items", e);
            return new ArrayList<>();
        }
    }

    public void clearHistory() {
        try {
            prefs.edit().remove(KEY_HISTORY).apply();
            _historyItems.postValue(new ArrayList<>());
            notifyHistoryChanged();
            Log.d(TAG, "History cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing history", e);
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date());
    }

    public void debugPrintHistory() {
        try {
            String json = prefs.getString(KEY_HISTORY, "[]");
            Type type = new TypeToken<ArrayList<SharedWish>>() {}.getType();
            List<SharedWish> wishes = gson.fromJson(json, type);

            Log.d(TAG, "Current history content (" + (wishes != null ? wishes.size() : 0) + " items):");
            Map<String, ?> allPrefs = prefs.getAll();
            Log.d(TAG, "All SharedPreferences contents:");
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                Log.d(TAG, entry.getKey() + ": " + entry.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error printing debug history", e);
        }
    }
    
    /**
     * Check if analytics consent has been given
     * @return true if consent was given, false otherwise
     */
    public boolean hasAnalyticsConsent() {
        return prefs.getBoolean(KEY_ANALYTICS_CONSENT, false);
    }
    
    /**
     * Set analytics consent status
     * @param consent true to enable analytics, false to disable
     */
    public void setAnalyticsConsent(boolean consent) {
        prefs.edit().putBoolean(KEY_ANALYTICS_CONSENT, consent).apply();
        Log.d(TAG, "Analytics consent set to: " + consent);
    }
    
    /**
     * Check if analytics consent dialog has been shown
     * @return true if consent dialog was shown, false otherwise
     */
    public boolean hasShownAnalyticsConsent() {
        return prefs.getBoolean(KEY_ANALYTICS_CONSENT_SHOWN, false);
    }
    
    /**
     * Set that analytics consent dialog has been shown
     */
    public void setAnalyticsConsentShown() {
        prefs.edit().putBoolean(KEY_ANALYTICS_CONSENT_SHOWN, true).apply();
        Log.d(TAG, "Analytics consent dialog marked as shown");
    }
}
