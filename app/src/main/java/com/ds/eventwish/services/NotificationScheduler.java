package com.ds.eventwish.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.ds.eventwish.utils.EventWishNotificationManager;

/**
 * Handles scheduling and sending notifications
 */
public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    
    /**
     * Schedule a notification for a specific time
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     * @param timeInMillis Time to show notification in milliseconds
     * @param requestCode Unique request code for the notification
     * @return true if scheduled successfully, false otherwise
     */
    public static boolean scheduleNotification(Context context, String title, String message, long timeInMillis, int requestCode) {
        try {
            Intent intent = new Intent(context, com.ds.eventwish.receivers.ReminderAlarmReceiver.class);
            intent.putExtra("title", title);
            intent.putExtra("message", message);
            intent.putExtra("notification_id", requestCode);
            intent.setAction("com.ds.eventwish.SHOW_REMINDER");
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                }
                Log.d(TAG, "Notification scheduled for " + timeInMillis + " with ID " + requestCode);
                return true;
            } else {
                Log.e(TAG, "AlarmManager is null");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling notification: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Cancel a scheduled notification
     * @param context Application context
     * @param requestCode Request code of the notification to cancel
     */
    public static void cancelNotification(Context context, int requestCode) {
        try {
            Intent intent = new Intent(context, com.ds.eventwish.receivers.ReminderAlarmReceiver.class);
            intent.setAction("com.ds.eventwish.SHOW_REMINDER");
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Notification canceled with ID " + requestCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error canceling notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Run the festival notification worker immediately
     * @param context Application context
     */
    public static void runFestivalNotificationsNow(Context context) {
        try {
            // Create and enqueue a one-time work request
            androidx.work.OneTimeWorkRequest workRequest = 
                    new androidx.work.OneTimeWorkRequest.Builder(com.ds.eventwish.workers.FestivalNotificationWorker.class)
                    .build();
            
            // Enqueue the work
            androidx.work.WorkManager.getInstance(context).enqueue(workRequest);
            
            Log.d(TAG, "Festival notification worker enqueued");
        } catch (Exception e) {
            Log.e(TAG, "Error running festival notification worker: " + e.getMessage(), e);
        }
    }
}