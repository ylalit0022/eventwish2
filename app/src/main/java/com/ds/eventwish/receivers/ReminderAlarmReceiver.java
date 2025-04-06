package com.ds.eventwish.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.workers.ReminderNotificationWorker;
import java.util.concurrent.TimeUnit;

public class ReminderAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderAlarmReceiver";
    private static final String CHANNEL_ID = "reminder_channel";
    public static final String ACTION_SHOW_REMINDER = "com.ds.eventwish.ACTION_SHOW_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received alarm intent: " + intent.getAction());
        dumpIntent(intent);

        long reminderId = intent.getLongExtra("reminderId", -1);
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");

        Log.d(TAG, String.format("Reminder data - id: %d, title: %s, description: %s", 
            reminderId, title, description));

        if (reminderId == -1 || title == null) {
            Log.e(TAG, "Missing reminder data in intent");
            return;
        }

        try {
            if (checkNotificationPermission(context)) {
                createNotificationChannel(context);
                showNotification(context, reminderId, title, description);
                Log.d(TAG, "Successfully showed notification for reminder: " + reminderId);
            } else {
                Log.e(TAG, "Notification permission not granted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification: " + e.getMessage(), e);
        }
    }

    private void dumpIntent(Intent intent) {
        Log.d(TAG, "Dumping Intent data:");
        Log.d(TAG, "Action: " + intent.getAction());
        Log.d(TAG, "Component: " + intent.getComponent());
        Log.d(TAG, "Flags: " + Integer.toHexString(intent.getFlags()));
        Log.d(TAG, "Categories: " + intent.getCategories());
        Log.d(TAG, "Extras:");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d(TAG, key + ": " + extras.get(key));
            }
        }
    }

    private boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(context, 
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Notification permission check: " + hasPermission);
            return hasPermission;
        }
        return true;
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            
            channel.setDescription("Notifications for reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setShowBadge(true);
            channel.setBypassDnd(true);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Created notification channel: " + CHANNEL_ID);
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        }
    }

    private void showNotification(Context context, long reminderId, String title, String description) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        // Create an Intent for opening the app directly to the ReminderFragment
        Intent contentIntent = new Intent(context, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        contentIntent.putExtra("reminderId", reminderId);
        contentIntent.putExtra("navigate_to", "reminder");
        contentIntent.setData(android.net.Uri.parse("reminder://" + reminderId));
        
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
            context,
            (int) reminderId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create snooze intent
        Intent snoozeIntent = new Intent(context, com.ds.eventwish.receivers.ReminderActionReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("reminderId", reminderId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (int) (reminderId + 1),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create complete intent
        Intent completeIntent = new Intent(context, com.ds.eventwish.receivers.ReminderActionReceiver.class);
        completeIntent.setAction("COMPLETE");
        completeIntent.putExtra("reminderId", reminderId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
            context,
            (int) (reminderId + 2),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze 15min", snoozePendingIntent)
            .addAction(R.drawable.ic_check, "Complete", completePendingIntent);

        // Show notification
        try {
            notificationManager.notify((int) reminderId, builder.build());
            Log.d(TAG, "Showed notification for reminder: " + reminderId);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception showing notification: " + e.getMessage());
            // Schedule a retry using WorkManager
            scheduleNotificationRetry(context, reminderId, title, description);
        }
    }

    private void scheduleNotificationRetry(Context context, long reminderId, String title, String description) {
        Data inputData = new Data.Builder()
            .putLong("reminderId", reminderId)
            .putString("title", title)
            .putString("description", description)
            .build();

        OneTimeWorkRequest retryWork = new OneTimeWorkRequest.Builder(ReminderNotificationWorker.class)
            .setInputData(inputData)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build();

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "notification_retry_" + reminderId,
                ExistingWorkPolicy.REPLACE,
                retryWork
            );
        
        Log.d(TAG, "Scheduled notification retry for reminder: " + reminderId);
    }
}
