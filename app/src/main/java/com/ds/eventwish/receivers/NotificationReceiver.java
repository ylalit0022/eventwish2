package com.ds.eventwish.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ds.eventwish.utils.EventWishNotificationManager;

/**
 * Receiver for notification events
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            
            if ("TEST_NOTIFICATION".equals(action)) {
                handleTestNotification(context, intent);
            } else {
                Log.d(TAG, "Unknown action: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in NotificationReceiver: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle test notification intent
     * @param context Application context
     * @param intent Intent with notification data
     */
    private void handleTestNotification(Context context, Intent intent) {
        try {
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");
            
            if (title == null || title.isEmpty()) {
                title = "Delayed Notification";
            }
            
            if (message == null || message.isEmpty()) {
                message = "This is a delayed test notification";
            }
            
            // Show notification
            EventWishNotificationManager.showUpdateNotification(context, title, message);
            
            Log.d(TAG, "Showing delayed test notification: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error handling test notification: " + e.getMessage(), e);
        }
    }
} 