package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ds.eventwish.workers.FestivalNotificationWorker;

import java.util.concurrent.TimeUnit;

/**
 * Utility class to schedule notification workers
 */
public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    
    // Work request tags
    public static final String FESTIVAL_NOTIFICATION_WORK = "festival_notification_work";
    
    // Default intervals
    private static final long DEFAULT_FESTIVAL_CHECK_INTERVAL_HOURS = 12;
    
    private NotificationScheduler() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Schedule all notification workers
     * @param context Application context
     */
    public static void scheduleAllNotifications(Context context) {
        scheduleFestivalNotifications(context);
    }
    
    /**
     * Schedule festival notification worker
     * @param context Application context
     */
    public static void scheduleFestivalNotifications(Context context) {
        Log.d(TAG, "Scheduling festival notifications");
        
        // Create constraints - we want to run this when the device is charging and has network
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        // Create the periodic work request
        PeriodicWorkRequest festivalWorkRequest =
                new PeriodicWorkRequest.Builder(
                        FestivalNotificationWorker.class,
                        DEFAULT_FESTIVAL_CHECK_INTERVAL_HOURS,
                        TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .addTag(FESTIVAL_NOTIFICATION_WORK)
                        .build();
        
        // Enqueue the work request
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        FESTIVAL_NOTIFICATION_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,  // Keep existing if already scheduled
                        festivalWorkRequest);
        
        Log.d(TAG, "Festival notifications scheduled to run every " + 
                DEFAULT_FESTIVAL_CHECK_INTERVAL_HOURS + " hours");
    }
    
    /**
     * Cancel all notification workers
     * @param context Application context
     */
    public static void cancelAllNotifications(Context context) {
        Log.d(TAG, "Cancelling all notification workers");
        
        // Cancel festival notifications
        cancelFestivalNotifications(context);
    }
    
    /**
     * Cancel festival notification worker
     * @param context Application context
     */
    public static void cancelFestivalNotifications(Context context) {
        Log.d(TAG, "Cancelling festival notifications");
        
        // Cancel by unique work name
        WorkManager.getInstance(context)
                .cancelUniqueWork(FESTIVAL_NOTIFICATION_WORK);
    }
    
    /**
     * Run festival notifications immediately (for testing)
     * @param context Application context
     */
    public static void runFestivalNotificationsNow(Context context) {
        Log.d(TAG, "Running festival notifications immediately");
        
        // Create a one-time work request
        androidx.work.OneTimeWorkRequest festivalWorkRequest =
                new androidx.work.OneTimeWorkRequest.Builder(FestivalNotificationWorker.class)
                        .addTag(FESTIVAL_NOTIFICATION_WORK + "_immediate")
                        .build();
        
        // Enqueue the work request
        WorkManager.getInstance(context)
                .enqueue(festivalWorkRequest);
    }
} 