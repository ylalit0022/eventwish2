package com.ds.eventwish.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.utils.EventNotificationManager;
import com.ds.eventwish.utils.NotificationPermissionManager;

/**
 * Worker class to sync notification configuration from Firebase Remote Config
 */
public class NotificationSyncWorker extends Worker {
    private static final String TAG = "NotificationSyncWorker";

    public NotificationSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting notification configuration sync");

        // Check notification permission
        if (!NotificationPermissionManager.hasNotificationPermission(getApplicationContext())) {
            Log.d(TAG, "Notification permission not granted, skipping sync");
            return Result.success(); // Not a failure, just no need to sync
        }

        try {
            // Get the notification manager instance
            EventNotificationManager notificationManager = EventNotificationManager.getInstance(getApplicationContext());

            // Force fetch remote config to get latest notification configuration
            boolean fetchSuccess = notificationManager.fetchRemoteConfigSync();

            if (fetchSuccess) {
                Log.d(TAG, "Remote config fetched successfully, processing notifications");
                
                // Process all notification types based on new configuration
                notificationManager.processAllNotificationTypes();
                
                Log.d(TAG, "Notification configuration sync completed successfully");
                return Result.success();
            } else {
                Log.w(TAG, "Failed to fetch remote config, using cached configuration");
                
                // Still try to process notifications with cached config
                notificationManager.processAllNotificationTypes();
                
                // Return success as we still processed with cached data
                return Result.success();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during notification configuration sync", e);
            return Result.retry(); // Retry on error
        }
    }
} 