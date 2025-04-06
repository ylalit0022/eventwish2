package com.ds.eventwish.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.utils.CacheManager;

/**
 * Worker to clear the cache periodically
 */
public class CacheClearWorker extends Worker {
    private static final String TAG = "CacheClearWorker";

    public CacheClearWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting cache clear operation");
        
        try {
            // Use the clearAllCache method instead of clearCache
            CacheManager.getInstance(getApplicationContext()).clearAllCache();
            Log.d(TAG, "Cache cleared successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache", e);
            return Result.failure();
        }
    }
} 