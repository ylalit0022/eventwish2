package com.ds.eventwish.workers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.R;
import com.ds.eventwish.ui.template.TemplateSelectionActivity;
import me.leolin.shortcutbadger.ShortcutBadger;

public class EventNotificationWorker extends Worker {
    public EventNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Check if the user is interested in Valentine's Day
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("user_interests", Context.MODE_PRIVATE);
        boolean isInterested = prefs.getBoolean("valentines_day", false);

        if (isInterested) {
            // Create and show the notification
            sendNotification("💖 Valentine's Day is near! Find the perfect romantic greeting for your loved one! 💕");
        }

        // Update badge count
        updateBadgeCount(getApplicationContext());

        return Result.success();
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(getApplicationContext(), TemplateSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "event_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Event Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(1, builder.build());
    }

    private void updateBadgeCount(Context context) {
        // Retrieve the count of upcoming reminders from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("user_interests", Context.MODE_PRIVATE);
        int count = prefs.getInt("upcoming_reminders_count", 0); // Default to 0 if not set
        ShortcutBadger.applyCount(context, count); // Update the badge count
            }

}