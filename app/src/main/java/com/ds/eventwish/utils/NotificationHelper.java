package com.ds.eventwish.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ds.eventwish.R;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String EVENT_CHANNEL_ID = "event_channel";
    private static final String FESTIVAL_CHANNEL_ID = "festival_channel";
    
    /**
     * Initialize notification channels
     * @param context The application context
     */
    public static void createNotificationChannels(Context context) {
        // Only needed on Android 8.0 (API level 26) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the event notification channel
            NotificationChannel eventChannel = new NotificationChannel(
                    EVENT_CHANNEL_ID,
                    "Event Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            eventChannel.setDescription("Notifications for upcoming events");
            
            // Create the festival notification channel
            NotificationChannel festivalChannel = new NotificationChannel(
                    FESTIVAL_CHANNEL_ID,
                    "Festival Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            festivalChannel.setDescription("Notifications for upcoming festivals");
            
            // Register the channels with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(eventChannel);
                notificationManager.createNotificationChannel(festivalChannel);
                Log.d(TAG, "Notification channels created");
            }
        }
    }
    
    /**
     * Create and show a notification
     * @param context The application context
     * @param title The notification title
     * @param message The notification message
     */
    public static void createNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        showNotification(context, 1, builder.build());
    }
    
    /**
     * Show a notification with permission check
     * @param context The application context
     * @param notificationId The notification ID
     * @param notification The notification to show
     */
    public static void showNotification(Context context, int notificationId, android.app.Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        // Check for notification permission on Android 13+
        if (hasNotificationPermission(context)) {
            try {
                notificationManager.notify(notificationId, notification);
                Log.d(TAG, "Notification posted with ID: " + notificationId);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when posting notification: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Notification permission not granted, skipping notification");
            // Store the notification for later delivery when permission is granted
            storeNotificationForLaterDelivery(context, notificationId, notification);
        }
    }
    
    /**
     * Store a notification for later delivery when permission is granted
     * @param context The application context
     * @param notificationId The notification ID
     * @param notification The notification to store
     */
    private static void storeNotificationForLaterDelivery(Context context, int notificationId, android.app.Notification notification) {
        // In a real app, you would store the notification data in a database or shared preferences
        // and show it when the permission is granted
        Log.d(TAG, "Storing notification with ID " + notificationId + " for later delivery");
        
        // For now, we'll just log it
        // In a real app, you would implement a mechanism to check for permission changes
        // and show stored notifications when permission is granted
    }
    
    /**
     * Check if the app has notification permission
     * @param context The application context
     * @return true if permission is granted or not required (pre-Android 13), false otherwise
     */
    public static boolean hasNotificationPermission(Context context) {
        // Only Android 13+ (API 33+) requires the POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        // For older Android versions, permission is granted at install time
        return true;
    }
    
    /**
     * Request notification permission
     * This should be called from an Activity
     * @param context The activity context
     * @param requestCode The request code for the permission request
     */
    public static void requestNotificationPermission(android.app.Activity context, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(context)) {
                ActivityCompat.requestPermissions(
                        context,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        requestCode);
                Log.d(TAG, "Requesting notification permission");
            } else {
                Log.d(TAG, "Notification permission already granted");
            }
        }
    }
}