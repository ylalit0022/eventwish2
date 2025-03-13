package com.ds.eventwish.data.cache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.utils.AppExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache for resources with memory and disk caching
 */
public class ResourceCache {
    private static final String TAG = "ResourceCache";
    
    // Cache sizes
    private static final int MEMORY_CACHE_SIZE = 4 * 1024 * 1024; // 4MB
    private static final int DISK_CACHE_SIZE = 20 * 1024 * 1024; // 20MB
    
    // Cache directories
    private static final String DISK_CACHE_DIR = "resource_cache";
    
    // Default expiration time
    private static final long DEFAULT_EXPIRATION = TimeUnit.HOURS.toMillis(1); // 1 hour
    
    // Singleton instance
    private static volatile ResourceCache instance;
    
    // Memory cache
    private final LruCache<String, CacheEntry<JsonObject>> memoryCache;
    
    // Disk cache
    private final DiskLruCache diskCache;
    
    // Dependencies
    private final Context context;
    private final AppExecutors executors;
    private final Gson gson;
    
    // Expiration times
    private final Map<String, Long> expirationTimes = new HashMap<>();
    
    /**
     * Get the singleton instance of ResourceCache
     * @param context Application context
     * @return ResourceCache instance
     */
    public static ResourceCache getInstance(Context context) {
        if (instance == null) {
            synchronized (ResourceCache.class) {
                if (instance == null) {
                    instance = new ResourceCache(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize caches
     * @param context Application context
     */
    private ResourceCache(Context context) {
        this.context = context;
        this.executors = AppExecutors.getInstance();
        this.gson = new Gson();
        
        // Initialize memory cache
        memoryCache = new LruCache<String, CacheEntry<JsonObject>>(MEMORY_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, CacheEntry<JsonObject> value) {
                // Rough estimate of size in bytes
                return value.toString().length() * 2;
            }
        };
        
        // Initialize disk cache
        File cacheDir = new File(context.getCacheDir(), DISK_CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        DiskLruCache cache = null;
        try {
            cache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
        } catch (IOException e) {
            Log.e(TAG, "Error opening disk cache", e);
        }
        diskCache = cache;
        
        Log.d(TAG, "ResourceCache initialized");
        
        // Start cleanup task
        startCleanupTask();
    }
    
    /**
     * Start a periodic task to clean up expired cache entries
     */
    private void startCleanupTask() {
        executors.diskIO().execute(() -> {
            try {
                // Clean up expired entries
                cleanupExpiredEntries();
                
                // Schedule next cleanup
                executors.mainThread().execute(() -> {
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(() -> startCleanupTask(), TimeUnit.HOURS.toMillis(1));
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during cache cleanup", e);
            }
        });
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        
        // Clean up memory cache
        List<String> keysToRemove = new ArrayList<>();
        synchronized (memoryCache) {
            for (Map.Entry<String, Long> entry : expirationTimes.entrySet()) {
                if (entry.getValue() < now) {
                    keysToRemove.add(entry.getKey());
                }
            }
            
            for (String key : keysToRemove) {
                memoryCache.remove(key);
                expirationTimes.remove(key);
                Log.d(TAG, "Removed expired entry from memory cache: " + key);
            }
        }
        
        // Clean up disk cache
        try {
            diskCache.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error flushing disk cache", e);
        }
    }
    
    /**
     * Put a value in the cache
     * @param key Cache key
     * @param value Value to cache
     * @param expirationTime Expiration time in milliseconds
     * @param <T> Type of value
     */
    public <T> void put(String key, T value, long expirationTime) {
        if (key == null || value == null) {
            return;
        }
        
        JsonObject jsonObject;
        if (value instanceof JsonObject) {
            jsonObject = (JsonObject) value;
        } else {
            jsonObject = gson.toJsonTree(value).getAsJsonObject();
        }
        
        // Add to memory cache
        long expiration = System.currentTimeMillis() + expirationTime;
        CacheEntry<JsonObject> entry = new CacheEntry<>(jsonObject, expiration);
        synchronized (memoryCache) {
            memoryCache.put(key, entry);
            expirationTimes.put(key, expiration);
        }
        
        // Add to disk cache
        executors.diskIO().execute(() -> {
            try {
                DiskLruCache.Editor editor = diskCache.edit(key.hashCode() + "");
                if (editor != null) {
                    String json = gson.toJson(entry);
                    editor.set(0, json);
                    editor.commit();
                    Log.d(TAG, "Added to disk cache: " + key);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing to disk cache", e);
            }
        });
    }
    
    /**
     * Put a value in the cache with default expiration time
     * @param key Cache key
     * @param value Value to cache
     * @param <T> Type of value
     */
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_EXPIRATION);
    }
    
    /**
     * Get a value from the cache
     * @param key Cache key
     * @param <T> Type of value
     * @return Cached value or null if not found or expired
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        
        // Try memory cache first
        CacheEntry<JsonObject> entry;
        synchronized (memoryCache) {
            entry = memoryCache.get(key);
        }
        
        if (entry != null) {
            if (entry.isExpired()) {
                // Remove expired entry
                synchronized (memoryCache) {
                    memoryCache.remove(key);
                    expirationTimes.remove(key);
                }
                Log.d(TAG, "Memory cache entry expired: " + key);
                return null;
            }
            
            Log.d(TAG, "Cache hit (memory): " + key);
            return (T) entry.getValue();
        }
        
        // Try disk cache
        try {
            DiskLruCache.Snapshot snapshot = diskCache.get(key.hashCode() + "");
            if (snapshot != null) {
                try {
                    String json = snapshot.getString(0);
                    CacheEntry<JsonObject> diskEntry = gson.fromJson(json, CacheEntry.class);
                    
                    if (diskEntry.isExpired()) {
                        // Remove expired entry
                        diskCache.remove(key.hashCode() + "");
                        Log.d(TAG, "Disk cache entry expired: " + key);
                        return null;
                    }
                    
                    // Add to memory cache
                    synchronized (memoryCache) {
                        memoryCache.put(key, diskEntry);
                        expirationTimes.put(key, diskEntry.getExpiration());
                    }
                    
                    Log.d(TAG, "Cache hit (disk): " + key);
                    return (T) diskEntry.getValue();
                } finally {
                    snapshot.close();
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            Log.e(TAG, "Error reading from disk cache", e);
        }
        
        Log.d(TAG, "Cache miss: " + key);
        return null;
    }
    
    /**
     * Remove a value from the cache
     * @param key Cache key
     */
    public void remove(String key) {
        if (key == null) {
            return;
        }
        
        // Remove from memory cache
        synchronized (memoryCache) {
            memoryCache.remove(key);
            expirationTimes.remove(key);
        }
        
        // Remove from disk cache
        executors.diskIO().execute(() -> {
            try {
                diskCache.remove(key.hashCode() + "");
            } catch (IOException e) {
                Log.e(TAG, "Error removing from disk cache", e);
            }
        });
        
        Log.d(TAG, "Removed from cache: " + key);
    }
    
    /**
     * Remove all values with a specific prefix
     * @param prefix Key prefix
     */
    public void removeByPrefix(String prefix) {
        if (prefix == null) {
            return;
        }
        
        // Remove from memory cache
        List<String> keysToRemove = new ArrayList<>();
        synchronized (memoryCache) {
            for (Map.Entry<String, Long> entry : expirationTimes.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    keysToRemove.add(entry.getKey());
                }
            }
            
            for (String key : keysToRemove) {
                memoryCache.remove(key);
                expirationTimes.remove(key);
            }
        }
        
        Log.d(TAG, "Removed " + keysToRemove.size() + " entries with prefix: " + prefix);
        
        // We can't easily remove by prefix from disk cache, so we'll just flush it
        executors.diskIO().execute(() -> {
            try {
                diskCache.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error flushing disk cache", e);
            }
        });
    }
    
    /**
     * Clear the entire cache
     */
    public void clear() {
        // Clear memory cache
        synchronized (memoryCache) {
            memoryCache.evictAll();
            expirationTimes.clear();
        }
        
        // Clear disk cache
        executors.diskIO().execute(() -> {
            try {
                diskCache.delete();
                
                // Reinitialize disk cache
                File cacheDir = new File(context.getCacheDir(), DISK_CACHE_DIR);
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                
                DiskLruCache newCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                
                synchronized (ResourceCache.class) {
                    // Replace the disk cache reference
                    try {
                        ResourceCache.class.getDeclaredField("diskCache")
                                .set(this, newCache);
                    } catch (Exception e) {
                        Log.e(TAG, "Error replacing disk cache reference", e);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error clearing disk cache", e);
            }
        });
        
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Check if a key exists in the cache
     * @param key Cache key
     * @return True if the key exists and is not expired, false otherwise
     */
    public boolean contains(String key) {
        if (key == null) {
            return false;
        }
        
        // Check memory cache first
        CacheEntry<JsonObject> entry;
        synchronized (memoryCache) {
            entry = memoryCache.get(key);
        }
        
        if (entry != null) {
            if (entry.isExpired()) {
                // Remove expired entry
                synchronized (memoryCache) {
                    memoryCache.remove(key);
                    expirationTimes.remove(key);
                }
                return false;
            }
            return true;
        }
        
        // Check disk cache
        try {
            DiskLruCache.Snapshot snapshot = diskCache.get(key.hashCode() + "");
            if (snapshot != null) {
                try {
                    String json = snapshot.getString(0);
                    CacheEntry<JsonObject> diskEntry = gson.fromJson(json, CacheEntry.class);
                    
                    if (diskEntry.isExpired()) {
                        // Remove expired entry
                        diskCache.remove(key.hashCode() + "");
                        return false;
                    }
                    
                    return true;
                } finally {
                    snapshot.close();
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            Log.e(TAG, "Error checking disk cache", e);
        }
        
        return false;
    }
    
    /**
     * Get the size of the memory cache
     * @return Size in bytes
     */
    public int getMemoryCacheSize() {
        synchronized (memoryCache) {
            return memoryCache.size();
        }
    }
    
    /**
     * Get the size of the disk cache
     * @return Size in bytes
     */
    public long getDiskCacheSize() {
        try {
            return diskCache.size();
        } catch (Exception e) {
            Log.e(TAG, "Error getting disk cache size", e);
            return 0;
        }
    }
    
    /**
     * Cache entry with expiration time
     * @param <T> Type of value
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expiration;
        
        public CacheEntry(T value, long expiration) {
            this.value = value;
            this.expiration = expiration;
        }
        
        public T getValue() {
            return value;
        }
        
        public long getExpiration() {
            return expiration;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiration;
        }
    }
} 