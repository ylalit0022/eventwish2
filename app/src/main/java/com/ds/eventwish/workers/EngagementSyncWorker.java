package com.ds.eventwish.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.repository.EngagementRepository;

/**
 * Worker that periodically syncs pending engagements to the server
 */
public class EngagementSyncWorker extends Worker {
    private static final String TAG = "EngagementSyncWorker";

    public EngagementSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting engagement sync worker");
        
        try {
            // Get the engagement repository 
            EngagementRepository engagementRepository = EngagementRepository.getInstance(getApplicationContext());
            
            // Check if there are pending engagements
            if (!engagementRepository.hasPendingEngagements()) {
                Log.d(TAG, "No pending engagements to sync");
                return Result.success();
            }
            
            // Sync pending engagements
            int syncedCount = engagementRepository.syncPendingEngagements();
            
            Log.d(TAG, "Synced " + syncedCount + " pending engagements");
            
            // If some items failed to sync, schedule a retry
            if (engagementRepository.hasPendingEngagements()) {
                return Result.retry();
            }
            
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error syncing engagements", e);
            return Result.retry();
        }
    }
} 