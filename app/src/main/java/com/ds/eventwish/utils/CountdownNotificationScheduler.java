package com.ds.eventwish.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.receivers.CountdownNotificationReceiver;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for scheduling countdown notifications for reminders
 * (e.g., "3 days left", "2 days left", etc.)
 */
public class CountdownNotificationScheduler {
    private static final String TAG = "CountdownNotifier";
    
    // Days before the reminder date to show countdown notifications
    private static final int[] COUNTDOWN_DAYS = {3, 2, 1};
    
    /**
     * Schedule countdown notifications for a reminder that is more than 3 days away
     * @param context Application context
     * @param reminder The reminder to schedule countdown notifications for
     */
    public static void scheduleCountdownNotifications(Context context, Reminder reminder) {
        long reminderTime = reminder.getDateTime();
        long currentTime = System.currentTimeMillis();
        
        // Calculate days until reminder
        long daysUntilReminder = TimeUnit.MILLISECONDS.toDays(reminderTime - currentTime);
        Log.d(TAG, "Days until reminder: " + daysUntilReminder);
        
        // Only schedule countdown notifications if the reminder is more than 3 days away
        if (daysUntilReminder <= COUNTDOWN_DAYS[0]) {
            Log.d(TAG, "Reminder is too close for countdown notifications");
            return;
        }
        
        // Schedule notifications for each countdown day
        for (int daysLeft : COUNTDOWN_DAYS) {
            scheduleCountdownForDay(context, reminder, daysLeft);
        }
    }
    
    /**
     * Schedule a single countdown notification for a specific number of days before the reminder
     */
    private static void scheduleCountdownForDay(Context context, Reminder reminder, int daysLeft) {
        long reminderId = reminder.getId();
        String title = reminder.getTitle();
        
        // Calculate when to show this countdown notification
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.setTimeInMillis(reminder.getDateTime());
        
        // Schedule for days before the reminder
        notificationTime.add(Calendar.DAY_OF_YEAR, -daysLeft);
        
        // Set time to 9:00 AM for the notification
        notificationTime.set(Calendar.HOUR_OF_DAY, 9);
        notificationTime.set(Calendar.MINUTE, 0);
        notificationTime.set(Calendar.SECOND, 0);
        notificationTime.set(Calendar.MILLISECOND, 0);
        
        // If the calculated time is in the past, don't schedule it
        if (notificationTime.getTimeInMillis() <= System.currentTimeMillis()) {
            Log.d(TAG, "Countdown for " + daysLeft + " days is in the past, skipping");
            return;
        }
        
        // Create intent for the countdown notification
        Intent intent = new Intent(context, CountdownNotificationReceiver.class);
        intent.setAction("com.ds.eventwish.COUNTDOWN_NOTIFICATION");
        intent.putExtra("reminderId", reminderId);
        intent.putExtra("title", title);
        intent.putExtra("daysLeft", daysLeft);
        
        // Create unique request code for this countdown notification
        int requestCode = (int) ((reminderId * 100) + daysLeft);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Get the AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }
        
        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.getTimeInMillis(),
                    pendingIntent
            );
        }
        
        Log.d(TAG, "Scheduled countdown notification for reminder " + reminderId + 
                " with " + daysLeft + " days left at " + notificationTime.getTime());
    }
    
    /**
     * Cancel all countdown notifications for a reminder
     */
    public static void cancelCountdownNotifications(Context context, long reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }
        
        // Cancel each countdown notification
        for (int daysLeft : COUNTDOWN_DAYS) {
            Intent intent = new Intent(context, CountdownNotificationReceiver.class);
            intent.setAction("com.ds.eventwish.COUNTDOWN_NOTIFICATION");
            int requestCode = (int) ((reminderId * 100) + daysLeft);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        
        Log.d(TAG, "Cancelled all countdown notifications for reminder " + reminderId);
    }
}
