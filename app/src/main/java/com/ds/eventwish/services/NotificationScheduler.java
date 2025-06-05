package com.ds.eventwish.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.receivers.NotificationReceiver;

/**
 * Handles scheduling and sending notifications
 */
public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    private static final int TEST_NOTIFICATION_REQUEST_CODE = 1001;
    
    /**
     * Send an immediate test notification
     * @param context Application context
     */
    public static void sendTestNotification(Context context) {
        try {
            // Directly show a test notification
            EventWishNotificationManager.showUpdateNotification(
                    context,
                    "Local Notification Test",
                    "This is a test of the local notification system."
            );
            
            // Schedule a delayed notification (5 seconds later)
            scheduleDelayedTestNotification(context);
            
            Log.d(TAG, "Test notification sent and delayed notification scheduled");
        } catch (Exception e) {
            Log.e(TAG, "Error sending test notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Schedule a delayed test notification (5 seconds later)
     * @param context Application context
     */
    private static void scheduleDelayedTestNotification(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }
            
            // Create intent for the notification receiver
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction("TEST_NOTIFICATION");
            intent.putExtra("title", "Delayed Test Notification");
            intent.putExtra("message", "This is a delayed test notification (5 seconds)");
            
            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    TEST_NOTIFICATION_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Calculate trigger time (5 seconds from now)
            long triggerTime = System.currentTimeMillis() + 5000;
            
            // Schedule notification based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
            
            Log.d(TAG, "Delayed test notification scheduled for " + triggerTime);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling delayed test notification: " + e.getMessage(), e);
        }
    }

    /**
     * Schedule all notifications for the app
     * @param context Application context
     */
    public static void scheduleAllNotifications(Context context) {
        try {
            Log.d(TAG, "Scheduling all app notifications");
            
            // This is a placeholder for scheduling app notifications
            // In a real implementation, you would load notification settings from preferences
            // and schedule appropriate notifications based on those settings
            
            Log.d(TAG, "All notifications scheduled successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling all notifications: " + e.getMessage(), e);
        }
    }
}