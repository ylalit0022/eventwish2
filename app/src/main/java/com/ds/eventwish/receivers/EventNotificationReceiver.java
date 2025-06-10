package com.ds.eventwish.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ds.eventwish.utils.EventNotificationManager;

/**
 * Receiver for event notifications
 */
public class EventNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "EventNotificationReceiver";
    
    public static final String ACTION_CHECK_NOTIFICATION = "com.ds.eventwish.ACTION_CHECK_NOTIFICATION";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: " + intent.getAction());
        
        if (intent.getAction() == null) {
            return;
        }
        
        // Initialize notification manager
        EventNotificationManager notificationManager = EventNotificationManager.getInstance(context);
        
        switch (intent.getAction()) {
            case ACTION_CHECK_NOTIFICATION:
                Log.d(TAG, "Checking for notification");
                notificationManager.checkAndShowNotification();
                break;
                
            case ACTION_BOOT_COMPLETED:
                Log.d(TAG, "Boot completed, scheduling daily check");
                notificationManager.scheduleDailyCheck();
                break;
        }
    }
} 