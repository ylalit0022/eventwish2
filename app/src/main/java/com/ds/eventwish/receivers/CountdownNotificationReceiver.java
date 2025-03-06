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
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.utils.NotificationHelper;

/**
 * BroadcastReceiver for handling countdown notifications
 * (e.g., "3 days left", "2 days left", etc.)
 */
public class CountdownNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "CountdownNotification";
    private static final String CHANNEL_ID = "countdown_channel";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received countdown notification intent");
        
        // Extract data from intent
        long reminderId = intent.getLongExtra("reminderId", -1);
        String title = intent.getStringExtra("title");
        int daysLeft = intent.getIntExtra("daysLeft", 0);
        
        Log.d(TAG, String.format("Countdown data - id: %d, title: %s, daysLeft: %d", 
                reminderId, title, daysLeft));
        
        if (reminderId == -1 || title == null || daysLeft <= 0) {
            Log.e(TAG, "Missing or invalid countdown data in intent");
            return;
        }
        
        // Verify the reminder still exists
        ReminderDao reminderDao = new ReminderDao(context);
        Reminder reminder = reminderDao.getReminderById(reminderId);
        
        if (reminder == null) {
            Log.d(TAG, "Reminder no longer exists, skipping countdown notification");
            return;
        }
        
        // Check if reminder is completed
        if (reminder.isCompleted()) {
            Log.d(TAG, "Reminder is already completed, skipping countdown notification");
            return;
        }
        
        try {
            createNotificationChannel(context);
            showCountdownNotification(context, reminderId, title, daysLeft);
            Log.d(TAG, "Successfully showed countdown notification for reminder: " + reminderId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show countdown notification: " + e.getMessage(), e);
        }
    }
    
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminder Countdowns",
                    NotificationManager.IMPORTANCE_HIGH
            );
            
            channel.setDescription("Countdown notifications for upcoming reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 150, 300});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.setShowBadge(true);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Created notification channel: " + CHANNEL_ID);
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        }
    }
    
    private void showCountdownNotification(Context context, long reminderId, String title, int daysLeft) {
        // Create content text based on days left
        String contentText;
        if (daysLeft == 1) {
            contentText = "Tomorrow is the deadline! Don't miss it.";
        } else {
            contentText = daysLeft + " days left until the deadline. Don't forget!";
        }
        
        // Create an Intent for opening the app
        Intent contentIntent = new Intent(context, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        contentIntent.putExtra("reminderId", reminderId);
        
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                (int) (reminderId * 1000 + daysLeft),
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentPendingIntent);
        
        // Show notification with a unique ID based on reminder ID and days left
        int notificationId = (int) ((reminderId * 100) + daysLeft);
        
        // Use NotificationHelper to handle permission checks
        NotificationHelper.showNotification(context, notificationId, builder.build());
    }
}
