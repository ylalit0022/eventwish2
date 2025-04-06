package com.ds.eventwish.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * A cache utility class that provides both memory and disk caching capabilities
 * for various types of resources including strings, JSON, bitmaps, and serializable objects.
 */
public class ResourceCache {
    private static final String TAG = "ResourceCache";
    
    // Singleton instance
    private static volatile ResourceCache instance;
    
    // Cache sizes
    private static final int DEFAULT_MEMORY_CACHE_SIZE = 4 * 1024 * 1024; // 4MB
    private static final int DEFAULT_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    
    // Cache expiration time (default: 1 hour)
    private static final long DEFAULT_CACHE_EXPIRATION = TimeUnit.HOURS.toMillis(1);
    
    // Cache directories
    private static final String DISK_CACHE_DIR = "resource_cache";
    
    // Memory caches
    private final LruCache<String, Object> memoryCache;
    private final LruCache<String, CacheEntry> memoryCacheMetadata;
    
    // Disk cache
    private DiskLruCache diskCache;
    
    // App executors for background operations
    private final AppExecutors appExecutors;
    
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
        // Initialize memory cache
        int memoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE;
        memoryCache = new LruCache<>(memoryCacheSize);
        memoryCacheMetadata = new LruCache<>(1000); // Metadata for 1000 entries
        
        // Initialize disk cache
        try {
            File cacheDir = new File(context.getCacheDir(), DISK_CACHE_DIR);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            diskCache = DiskLruCache.open(cacheDir, 1, 1, DEFAULT_DISK_CACHE_SIZE);
            Log.d(TAG, "Disk cache initialized at " + cacheDir.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize disk cache", e);
        }
        
        // Initialize app executors
        appExecutors = AppExecutors.getInstance();
        
        Log.d(TAG, "ResourceCache initialized with memory cache size: " + memoryCacheSize + " bytes");
    }
    
    /**
     * Put a string value in the cache
     * @param key Cache key
     * @param value String value to cache
     * @param expirationTimeMillis Cache expiration time in milliseconds
     */
    public void putString(final String key, final String value, final long expirationTimeMillis) {
        if (key == null || value == null) {
            return;
        }
        
        final String cacheKey = hashKey(key);
        final long expirationTime = System.currentTimeMillis() + (expirationTimeMillis > 0 ? expirationTimeMillis : DEFAULT_CACHE_EXPIRATION);
        
        // Put in memory cache
        memoryCache.put(cacheKey, value);
        memoryCacheMetadata.put(cacheKey, new CacheEntry(expirationTime));
        
        // Put in disk cache
        appExecutors.diskIO().execute(() -> {
            DiskLruCache.Editor editor = null;
            try {
                editor = diskCache.edit(cacheKey);
                if (editor != null) {
                    CacheEntry entry = new CacheEntry(expirationTime, value);
                    writeEntryToCache(editor, entry);
                    editor.commit();
                    Log.d(TAG, "String cached to disk: " + key);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to cache string to disk: " + key, e);
                try {
                    if (editor != null) {
                        editor.abort();
                    }
                } catch (IOException ignored) {
                }
            }
        });
    }
    
    /**
     * Put a string value in the cache with default expiration time
     * @param key Cache key
     * @param value String value to cache
     */
    public void putString(String key, String value) {
        putString(key, value, DEFAULT_CACHE_EXPIRATION);
    }
    
    /**
     * Get a string value from the cache
     * @param key Cache key
     * @return String value or null if not found or expired
     */
    @Nullable
    public String getString(final String key) {
        if (key == null) {
            return null;
        }
        
        final String cacheKey = hashKey(key);
        
        // Try memory cache first
        Object memoryValue = memoryCache.get(cacheKey);
        CacheEntry metadata = memoryCacheMetadata.get(cacheKey);
        
        if (memoryValue instanceof String && metadata != null) {
            if (System.currentTimeMillis() < metadata.expirationTime) {
                Log.d(TAG, "String retrieved from memory cache: " + key);
                return (String) memoryValue;
            } else {
                // Expired, remove from memory cache
                memoryCache.remove(cacheKey);
                memoryCacheMetadata.remove(cacheKey);
            }
        }
        
        // Try disk cache
        try {
            DiskLruCache.Snapshot snapshot = diskCache.get(cacheKey);
            if (snapshot != null) {
                try {
                    CacheEntry entry = readEntryFromCache(snapshot);
                    if (entry != null && entry.value instanceof String) {
                        if (System.currentTimeMillis() < entry.expirationTime) {
                            // Put back in memory cache
                            String value = (String) entry.value;
                            memoryCache.put(cacheKey, value);
                            memoryCacheMetadata.put(cacheKey, entry);
                            Log.d(TAG, "String retrieved from disk cache: " + key);
                            return value;
                        }
                    }
                } finally {
                    snapshot.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to retrieve string from disk cache: " + key, e);
        }
        
        return null;
    }
    
    /**
     * Put a serializable object in the cache
     * @param key Cache key
     * @param value Serializable object to cache
     * @param expirationTimeMillis Cache expiration time in milliseconds
     */
    public void putObject(final String key, final Serializable value, final long expirationTimeMillis) {
        if (key == null || value == null) {
            return;
        }
        
        final String cacheKey = hashKey(key);
        final long expirationTime = System.currentTimeMillis() + (expirationTimeMillis > 0 ? expirationTimeMillis : DEFAULT_CACHE_EXPIRATION);
        
        // Put in memory cache
        memoryCache.put(cacheKey, value);
        memoryCacheMetadata.put(cacheKey, new CacheEntry(expirationTime));
        
        // Put in disk cache
        appExecutors.diskIO().execute(() -> {
            DiskLruCache.Editor editor = null;
            try {
                editor = diskCache.edit(cacheKey);
                if (editor != null) {
                    CacheEntry entry = new CacheEntry(expirationTime, value);
                    writeEntryToCache(editor, entry);
                    editor.commit();
                    Log.d(TAG, "Object cached to disk: " + key);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to cache object to disk: " + key, e);
                try {
                    if (editor != null) {
                        editor.abort();
                    }
                } catch (IOException ignored) {
                }
            }
        });
    }
    
    /**
     * Put a serializable object in the cache with default expiration time
     * @param key Cache key
     * @param value Serializable object to cache
     */
    public void putObject(String key, Serializable value) {
        putObject(key, value, DEFAULT_CACHE_EXPIRATION);
    }
    
    /**
     * Get a serializable object from the cache
     * @param key Cache key
     * @param <T> Type of the object
     * @return Object or null if not found or expired
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getObject(final String key) {
        if (key == null) {
            return null;
        }
        
        final String cacheKey = hashKey(key);
        
        // Try memory cache first
        Object memoryValue = memoryCache.get(cacheKey);
        CacheEntry metadata = memoryCacheMetadata.get(cacheKey);
        
        if (memoryValue instanceof Serializable && metadata != null) {
            if (System.currentTimeMillis() < metadata.expirationTime) {
                Log.d(TAG, "Object retrieved from memory cache: " + key);
                return (T) memoryValue;
            } else {
                // Expired, remove from memory cache
                memoryCache.remove(cacheKey);
                memoryCacheMetadata.remove(cacheKey);
            }
        }
        
        // Try disk cache
        try {
            DiskLruCache.Snapshot snapshot = diskCache.get(cacheKey);
            if (snapshot != null) {
                try {
                    CacheEntry entry = readEntryFromCache(snapshot);
                    if (entry != null && entry.value instanceof Serializable) {
                        if (System.currentTimeMillis() < entry.expirationTime) {
                            // Put back in memory cache
                            T value = (T) entry.value;
                            memoryCache.put(cacheKey, value);
                            memoryCacheMetadata.put(cacheKey, entry);
                            Log.d(TAG, "Object retrieved from disk cache: " + key);
                            return value;
                        }
                    }
                } finally {
                    snapshot.close();
                }
            }
        } catch (IOException | ClassCastException e) {
            Log.e(TAG, "Failed to retrieve object from disk cache: " + key, e);
        }
        
        return null;
    }
    
    /**
     * Put a bitmap in the cache
     * @param key Cache key
     * @param bitmap Bitmap to cache
     * @param expirationTimeMillis Cache expiration time in milliseconds
     */
    public void putBitmap(final String key, final Bitmap bitmap, final long expirationTimeMillis) {
        if (key == null || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        
        final String cacheKey = hashKey(key);
        final long expirationTime = System.currentTimeMillis() + (expirationTimeMillis > 0 ? expirationTimeMillis : DEFAULT_CACHE_EXPIRATION);
        
        // Put in memory cache
        memoryCache.put(cacheKey, bitmap);
        memoryCacheMetadata.put(cacheKey, new CacheEntry(expirationTime));
        
        // Bitmaps are not serialized to disk cache to avoid excessive disk usage
        // They should be handled by image loading libraries like Glide
    }
    
    /**
     * Put a bitmap in the cache with default expiration time
     * @param key Cache key
     * @param bitmap Bitmap to cache
     */
    public void putBitmap(String key, Bitmap bitmap) {
        putBitmap(key, bitmap, DEFAULT_CACHE_EXPIRATION);
    }
    
    /**
     * Get a bitmap from the cache
     * @param key Cache key
     * @return Bitmap or null if not found or expired
     */
    @Nullable
    public Bitmap getBitmap(final String key) {
        if (key == null) {
            return null;
        }
        
        final String cacheKey = hashKey(key);
        
        // Try memory cache only (bitmaps are not stored in disk cache)
        Object memoryValue = memoryCache.get(cacheKey);
        CacheEntry metadata = memoryCacheMetadata.get(cacheKey);
        
        if (memoryValue instanceof Bitmap && metadata != null) {
            Bitmap bitmap = (Bitmap) memoryValue;
            if (!bitmap.isRecycled() && System.currentTimeMillis() < metadata.expirationTime) {
                Log.d(TAG, "Bitmap retrieved from memory cache: " + key);
                return bitmap;
            } else {
                // Expired or recycled, remove from memory cache
                memoryCache.remove(cacheKey);
                memoryCacheMetadata.remove(cacheKey);
            }
        }
        
        return null;
    }
    
    /**
     * Check if a key exists in the cache and is not expired
     * @param key Cache key
     * @return true if the key exists and is not expired, false otherwise
     */
    public boolean contains(final String key) {
        if (key == null) {
            return false;
        }
        
        final String cacheKey = hashKey(key);
        
        // Check memory cache
        CacheEntry metadata = memoryCacheMetadata.get(cacheKey);
        if (metadata != null && System.currentTimeMillis() < metadata.expirationTime) {
            return true;
        }
        
        // Check disk cache
        try {
            DiskLruCache.Snapshot snapshot = diskCache.get(cacheKey);
            if (snapshot != null) {
                try {
                    CacheEntry entry = readEntryFromCache(snapshot);
                    return entry != null && System.currentTimeMillis() < entry.expirationTime;
                } finally {
                    snapshot.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to check if key exists in disk cache: " + key, e);
        }
        
        return false;
    }
    
    /**
     * Remove a key from the cache
     * @param key Cache key
     */
    public void remove(final String key) {
        if (key == null) {
            return;
        }
        
        final String cacheKey = hashKey(key);
        
        // Remove from memory cache
        memoryCache.remove(cacheKey);
        memoryCacheMetadata.remove(cacheKey);
        
        // Remove from disk cache
        appExecutors.diskIO().execute(() -> {
            try {
                diskCache.remove(cacheKey);
                Log.d(TAG, "Removed from cache: " + key);
            } catch (IOException e) {
                Log.e(TAG, "Failed to remove from disk cache: " + key, e);
            }
        });
    }
    
    /**
     * Clear all cached data
     */
    public void clearAll() {
        // Clear memory cache
        memoryCache.evictAll();
        memoryCacheMetadata.evictAll();
        
        // Clear disk cache
        appExecutors.diskIO().execute(() -> {
            try {
                diskCache.delete();
                Log.d(TAG, "Cache cleared");
            } catch (IOException e) {
                Log.e(TAG, "Failed to clear disk cache", e);
            }
        });
    }
    
    /**
     * Clear expired entries from the cache
     */
    public void clearExpired() {
        final long now = System.currentTimeMillis();
        
        // Clear expired entries from memory cache
        for (String key : memoryCacheMetadata.snapshot().keySet()) {
            CacheEntry metadata = memoryCacheMetadata.get(key);
            if (metadata != null && now >= metadata.expirationTime) {
                memoryCache.remove(key);
                memoryCacheMetadata.remove(key);
            }
        }
        
        // Disk cache expiration is checked on read
        Log.d(TAG, "Expired cache entries cleared");
    }
    
    /**
     * Hash a key to make it suitable for disk cache
     * @param key Original key
     * @return Hashed key
     */
    @NonNull
    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a simple hash if MD5 is not available
            return String.valueOf(key.hashCode());
        }
    }
    
    /**
     * Write a cache entry to disk
     * @param editor DiskLruCache.Editor
     * @param entry CacheEntry to write
     * @throws IOException If an I/O error occurs
     */
    private void writeEntryToCache(DiskLruCache.Editor editor, CacheEntry entry) throws IOException {
        OutputStream outputStream = editor.newOutputStream(0);
        if (outputStream != null) {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(new BufferedOutputStream(outputStream));
                oos.writeLong(entry.expirationTime);
                oos.writeObject(entry.value);
                oos.flush();
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }
    
    /**
     * Read a cache entry from disk
     * @param snapshot DiskLruCache.Snapshot
     * @return CacheEntry or null if an error occurs
     */
    @Nullable
    private CacheEntry readEntryFromCache(DiskLruCache.Snapshot snapshot) {
        InputStream inputStream = snapshot.getInputStream(0);
        if (inputStream != null) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new BufferedInputStream(inputStream));
                long expirationTime = ois.readLong();
                Object value = ois.readObject();
                return new CacheEntry(expirationTime, value);
            } catch (IOException | ClassNotFoundException e) {
                Log.e(TAG, "Failed to read cache entry", e);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Cache entry class to store metadata and value
     */
    private static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final long expirationTime;
        final Object value;
        
        CacheEntry(long expirationTime) {
            this.expirationTime = expirationTime;
            this.value = null;
        }
        
        CacheEntry(long expirationTime, Object value) {
            this.expirationTime = expirationTime;
            this.value = value;
        }
    }
} 