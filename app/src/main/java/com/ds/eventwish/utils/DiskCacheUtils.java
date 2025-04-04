package com.ds.eventwish.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple disk cache utility for storing resources
 */
public class DiskCacheUtils {
    private static final String TAG = "DiskCacheUtils";
    private static final String CACHE_DIR_NAME = "disk_cache";
    private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    
    private static DiskCacheUtils instance;
    private final File cacheDir;
    private final Map<String, CacheEntry> cacheEntries = new HashMap<>();
    private long currentSize = 0;
    
    /**
     * Cache entry for tracking files and metadata
     */
    private static class CacheEntry {
        File file;
        long size;
        long lastAccessed;
        
        CacheEntry(File file) {
            this.file = file;
            this.size = file.length();
            this.lastAccessed = System.currentTimeMillis();
        }
        
        void updateLastAccessed() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
    
    private DiskCacheUtils(Context context) {
        cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // Scan existing cache files
        scanCacheFiles();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized DiskCacheUtils getInstance(Context context) {
        if (instance == null) {
            instance = new DiskCacheUtils(context);
        }
        return instance;
    }
    
    /**
     * Scan existing cache files and build entries map
     */
    private void scanCacheFiles() {
        currentSize = 0;
        cacheEntries.clear();
        
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String key = file.getName();
                    CacheEntry entry = new CacheEntry(file);
                    cacheEntries.put(key, entry);
                    currentSize += entry.size;
                }
            }
        }
        
        // Clean cache if too large
        if (currentSize > MAX_CACHE_SIZE) {
            trimToSize(MAX_CACHE_SIZE * 9 / 10); // Trim to 90% of max
        }
    }
    
    /**
     * Trim cache to requested size by removing oldest entries
     */
    private void trimToSize(long maxSize) {
        if (currentSize <= maxSize) return;
        
        Log.d(TAG, "Trimming cache from " + currentSize + " to " + maxSize);
        
        // Sort entries by last accessed time
        List<Map.Entry<String, CacheEntry>> entryList = new ArrayList<>(cacheEntries.entrySet());
        entryList.sort((e1, e2) -> Long.compare(e1.getValue().lastAccessed, e2.getValue().lastAccessed));
        
        // Remove oldest entries until we're under size limit
        for (Map.Entry<String, CacheEntry> entry : entryList) {
            if (currentSize <= maxSize) break;
            
            String key = entry.getKey();
            CacheEntry cacheEntry = entry.getValue();
            
            if (cacheEntry.file.delete()) {
                currentSize -= cacheEntry.size;
                cacheEntries.remove(key);
                Log.d(TAG, "Removed cache entry: " + key);
            }
        }
    }
    
    /**
     * Generate key for a file
     */
    private String getKeyForFile(String filename) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(filename.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Hash error", e);
            return String.valueOf(filename.hashCode());
        }
    }
    
    /**
     * Save a bitmap to cache
     */
    public void putBitmap(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) return;
        
        String cacheKey = getKeyForFile(key);
        File file = new File(cacheDir, cacheKey);
        
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            
            // Update cache entry
            CacheEntry entry = new CacheEntry(file);
            removeEntry(cacheKey);
            cacheEntries.put(cacheKey, entry);
            currentSize += entry.size;
            
            // Check if we need to trim
            if (currentSize > MAX_CACHE_SIZE) {
                trimToSize(MAX_CACHE_SIZE * 9 / 10);
            }
            
            Log.d(TAG, "Saved bitmap to cache: " + key);
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap", e);
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    /**
     * Save byte array to cache
     */
    public void putBytes(String key, byte[] data) {
        if (key == null || data == null) return;
        
        String cacheKey = getKeyForFile(key);
        File file = new File(cacheDir, cacheKey);
        
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            bos.write(data);
            bos.flush();
            
            // Update cache entry
            CacheEntry entry = new CacheEntry(file);
            removeEntry(cacheKey);
            cacheEntries.put(cacheKey, entry);
            currentSize += entry.size;
            
            // Check if we need to trim
            if (currentSize > MAX_CACHE_SIZE) {
                trimToSize(MAX_CACHE_SIZE * 9 / 10);
            }
            
            Log.d(TAG, "Saved data to cache: " + key);
        } catch (IOException e) {
            Log.e(TAG, "Error saving data", e);
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    /**
     * Read bitmap from cache
     */
    public Bitmap getBitmap(String key) {
        if (key == null) return null;
        
        String cacheKey = getKeyForFile(key);
        CacheEntry entry = cacheEntries.get(cacheKey);
        
        if (entry != null && entry.file.exists()) {
            try (FileInputStream fis = new FileInputStream(entry.file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                
                Bitmap bitmap = BitmapFactory.decodeStream(bis);
                if (bitmap != null) {
                    entry.updateLastAccessed();
                    return bitmap;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading bitmap", e);
            }
        }
        
        return null;
    }
    
    /**
     * Read bytes from cache
     */
    public byte[] getBytes(String key) {
        if (key == null) return null;
        
        String cacheKey = getKeyForFile(key);
        CacheEntry entry = cacheEntries.get(cacheKey);
        
        if (entry != null && entry.file.exists()) {
            try (FileInputStream fis = new FileInputStream(entry.file);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int read;
                while ((read = bis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                
                entry.updateLastAccessed();
                return baos.toByteArray();
            } catch (IOException e) {
                Log.e(TAG, "Error reading data", e);
            }
        }
        
        return null;
    }
    
    /**
     * Remove a file from cache
     */
    public boolean remove(String key) {
        if (key == null) return false;
        
        String cacheKey = getKeyForFile(key);
        return removeEntry(cacheKey);
    }
    
    /**
     * Remove entry by cache key
     */
    private boolean removeEntry(String cacheKey) {
        CacheEntry entry = cacheEntries.get(cacheKey);
        if (entry != null) {
            boolean deleted = entry.file.delete();
            if (deleted) {
                currentSize -= entry.size;
                cacheEntries.remove(cacheKey);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Clear all cache entries
     */
    public void clearCache() {
        for (CacheEntry entry : cacheEntries.values()) {
            entry.file.delete();
        }
        
        cacheEntries.clear();
        currentSize = 0;
        
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Get the current cache size
     */
    public long size() {
        return currentSize;
    }
} 