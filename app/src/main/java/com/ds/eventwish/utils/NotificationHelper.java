package com.ds.eventwish.utils;

import android.Manifest;
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
    
    public static void createNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "event_channel")
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
        }
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
}