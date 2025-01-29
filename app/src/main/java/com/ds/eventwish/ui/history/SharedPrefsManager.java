package com.ds.eventwish.ui.history;

import android.content.Context;
import android.content.SharedPreferences;

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
    private static final String PREF_NAME = "shared_history";
    private static final String KEY_HISTORY = "history_items";
    private final SharedPreferences prefs;
    private final Gson gson;

    public SharedPrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveHistoryItem(GreetingItem item) {
        List<GreetingItem> items = getHistoryItems();
        if (!items.contains(item)) {
            item.setShareTime(getCurrentTime());
            items.add(0, item);
            String json = gson.toJson(items);
            prefs.edit().putString(KEY_HISTORY, json).apply();
        }
    }

    public List<GreetingItem> getHistoryItems() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        Type type = new TypeToken<ArrayList<GreetingItem>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date());
    }
}
