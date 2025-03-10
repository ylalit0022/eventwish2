package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.room.Room;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.workers.CacheClearWorker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for managing cache data
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String PREF_NAME = "eventwish_cache";
    private static final String KEY_CACHE_TIMESTAMP = "cache_timestamp";
    private static final String KEY_DATA_SOURCE = "data_source";
    private static final long CACHE_EXPIRY_TIME = 30 * 60 * 1000; // 30 minutes in milliseconds
    
    private static CacheManager instance;
    private final SharedPreferences preferences;
    private final Gson gson;
    private final Map<String, Object> memoryCache;
    
    public enum DataSource {
        NETWORK,
        DATABASE,
        CACHE
    }
    
    private CacheManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        memoryCache = new HashMap<>();
    }
    
    public static synchronized CacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new CacheManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Save data to cache
     * @param key The cache key
     * @param data The data to cache
     * @param <T> The type of data
     */
    public <T> void saveToCache(String key, T data) {
        if (data == null) {
            Log.w(TAG, "Attempted to cache null data for key: " + key);
            return;
        }
        
        try {
            // Save to memory cache
            memoryCache.put(key, data);
            
            // Save to disk cache
            String json = gson.toJson(data);
            preferences.edit()
                    .putString(key, json)
                    .putLong(KEY_CACHE_TIMESTAMP + "_" + key, new Date().getTime())
                    .putString(KEY_DATA_SOURCE + "_" + key, DataSource.NETWORK.name())
                    .apply();
            
            Log.d(TAG, "Saved data to cache for key: " + key);
        } catch (Exception e) {
            Log.e(TAG, "Error saving data to cache for key: " + key, e);
        }
    }
    
    /**
     * Get data from cache
     * @param key The cache key
     * @param typeToken The type of data
     * @param <T> The type of data
     * @return The cached data, or null if not found or expired
     */
    public <T> T getFromCache(String key, TypeToken<T> typeToken) {
        // Check memory cache first
        if (memoryCache.containsKey(key)) {
            try {
                @SuppressWarnings("unchecked")
                T data = (T) memoryCache.get(key);
                if (data != null) {
                    Log.d(TAG, "Retrieved data from memory cache for key: " + key);
                    return data;
                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Error casting data from memory cache for key: " + key, e);
            }
        }
        
        // Check disk cache
        try {
            String json = preferences.getString(key, null);
            if (json != null) {
                Type type = typeToken.getType();
                T data = gson.fromJson(json, type);
                
                // Update memory cache
                if (data != null) {
                    memoryCache.put(key, data);
                    Log.d(TAG, "Retrieved data from disk cache for key: " + key);
                    return data;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving data from disk cache for key: " + key, e);
        }
        
        Log.d(TAG, "No cached data found for key: " + key);
        return null;
    }
    
    /**
     * Check if cache is expired
     * @param key The cache key
     * @return true if cache is expired, false otherwise
     */
    public boolean isCacheExpired(String key) {
        long timestamp = preferences.getLong(KEY_CACHE_TIMESTAMP + "_" + key, 0);
        long currentTime = new Date().getTime();
        boolean isExpired = (currentTime - timestamp) > CACHE_EXPIRY_TIME;
        
        if (isExpired) {
            Log.d(TAG, "Cache expired for key: " + key);
        }
        
        return isExpired;
    }
    
    /**
     * Get the data source for the cached data
     * @param key The cache key
     * @return The data source
     */
    public DataSource getDataSource(String key) {
        String source = preferences.getString(KEY_DATA_SOURCE + "_" + key, DataSource.NETWORK.name());
        return DataSource.valueOf(source);
    }
    
    /**
     * Set the data source for the cached data
     * @param key The cache key
     * @param source The data source
     */
    public void setDataSource(String key, DataSource source) {
        preferences.edit()
                .putString(KEY_DATA_SOURCE + "_" + key, source.name())
                .apply();
    }
    
    /**
     * Clear the cache for a specific key
     * @param key The cache key
     */
    public void clearCache(String key) {
        memoryCache.remove(key);
        preferences.edit()
                .remove(key)
                .remove(KEY_CACHE_TIMESTAMP + "_" + key)
                .remove(KEY_DATA_SOURCE + "_" + key)
                .apply();
        Log.d(TAG, "Cleared cache for key: " + key);
    }
    
    /**
     * Clear all cache data
     */
    public void clearAllCache() {
        memoryCache.clear();
        preferences.edit().clear().apply();
        Log.d(TAG, "Cleared all cache data");
    }
    
    /**
     * Clear memory cache only (keep disk cache)
     */
    public void clearMemoryCache() {
        memoryCache.clear();
        Log.d(TAG, "Cleared memory cache");
    }
} 