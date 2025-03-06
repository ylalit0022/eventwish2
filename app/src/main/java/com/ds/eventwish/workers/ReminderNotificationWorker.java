package com.ds.eventwish.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.receivers.ReminderActionReceiver;
import com.ds.eventwish.ui.reminder.ReminderFragment;

public class ReminderNotificationWorker extends Worker {
    private static final String TAG = "ReminderNotificationWorker";
    private static final String CHANNEL_ID = "reminder_channel";

    public ReminderNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String title = getInputData().getString("title");
            String description = getInputData().getString("description");
            long reminderId = getInputData().getLong("reminderId", -1);

            if (title == null || reminderId == -1) {
                Log.e(TAG, "Missing required data");
                return Result.failure();
            }

            createNotificationChannel();
            showNotification(reminderId, title, description);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    private void createNotificationChannel() {
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
            
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Created notification channel: " + CHANNEL_ID);
            }
        }
    }

    private void showNotification(long reminderId, String title, String description) {
        Context context = getApplicationContext();

        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted");
                return;
            }
        }

        try {
            // Create intent for notification tap action
            Intent contentIntent = new Intent(context, MainActivity.class);
            contentIntent.putExtra("navigate_to", "reminder");
            contentIntent.putExtra("reminderId", reminderId);
            contentIntent.putExtra("show_reminder_details", true); // 🔥 This line will open Dialog
            contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);


            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) reminderId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create action intents
            Intent snoozeIntent = new Intent(context, ReminderActionReceiver.class)
                .setAction("SNOOZE")
                .putExtra("reminderId", reminderId);

            Intent completeIntent = new Intent(context, ReminderActionReceiver.class)
                .setAction("COMPLETE")
                .putExtra("reminderId", reminderId);

            Intent deleteIntent = new Intent(context, ReminderActionReceiver.class)
                .setAction("DELETE")
                .putExtra("reminderId", reminderId);

            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (reminderId * 10 + 1),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (reminderId * 10 + 2),
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (reminderId * 10 + 3),
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
                .addAction(R.drawable.ic_check, "Complete", completePendingIntent)
                .addAction(R.drawable.ic_delete, "Delete", deletePendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setLights(Color.RED, 1000, 1000);

            NotificationManagerCompat.from(context).notify((int) reminderId, builder.build());
            Log.d(TAG, "Notification posted for reminder: " + reminderId);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Permission denied for showing notification", e);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }
}