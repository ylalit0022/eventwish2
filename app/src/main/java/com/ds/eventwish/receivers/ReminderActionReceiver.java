package com.ds.eventwish.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.utils.ReminderScheduler;
import java.util.concurrent.TimeUnit;

public class ReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderActionReceiver";
    private static final long SNOOZE_DURATION = TimeUnit.MINUTES.toMillis(15);

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long reminderId = intent.getLongExtra("reminderId", -1);
        
        Log.d(TAG, String.format("Received action: %s for reminder: %d", action, reminderId));
        
        if (reminderId == -1) {
            Log.e(TAG, "Invalid reminder ID");
            return;
        }

        ReminderDao reminderDao = new ReminderDao(context);
        Reminder reminder = reminderDao.getReminderById(reminderId);
        
        if (reminder == null) {
            Log.e(TAG, "Reminder not found: " + reminderId);
            return;
        }

        try {
            switch (action) {
                case "SNOOZE":
                    handleSnooze(context, reminder);
                    break;
                case "COMPLETE":
                    handleComplete(context, reminder, reminderDao);
                    break;
                case "DELETE":
                    handleDelete(context, reminder, reminderDao);
                    break;
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling action: " + e.getMessage(), e);
        }
    }

    private void handleSnooze(Context context, Reminder reminder) {
        try {
            // Cancel current notification
            NotificationManagerCompat.from(context).cancel((int) reminder.getId());
            
            // Schedule new notification for 15 minutes later
            reminder.setDateTime(System.currentTimeMillis() + SNOOZE_DURATION);
            ReminderScheduler.scheduleReminder(context, reminder);
            
            Log.d(TAG, "Snoozed reminder: " + reminder.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error snoozing reminder: " + e.getMessage(), e);
        }
    }

    private void handleComplete(Context context, Reminder reminder, ReminderDao reminderDao) {
        try {
            // Cancel notification
            NotificationManagerCompat.from(context).cancel((int) reminder.getId());
            
            // Mark as completed
            reminder.setCompleted(true);
            reminderDao.updateReminder(reminder);
            
            // If repeating, schedule next occurrence
            if (reminder.isRepeating()) {
                Reminder nextReminder = new Reminder(
                    reminder.getTitle(),
                    reminder.getDescription(),
                    reminder.getDateTime() + TimeUnit.DAYS.toMillis(reminder.getRepeatInterval()),
                    reminder.getPriority(),
                    true,
                    reminder.getRepeatInterval()
                );
                reminderDao.addReminder(nextReminder);
                ReminderScheduler.scheduleReminder(context, nextReminder);
            }
            
            Log.d(TAG, "Completed reminder: " + reminder.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error completing reminder: " + e.getMessage(), e);
        }
    }

    private void handleDelete(Context context, Reminder reminder, ReminderDao reminderDao) {
        try {
            // Cancel notification
            NotificationManagerCompat.from(context).cancel((int) reminder.getId());
            
            // Delete reminder
            reminderDao.deleteReminder(reminder.getId());
            ReminderScheduler.cancelReminder(context, reminder.getId());
            
            Log.d(TAG, "Deleted reminder: " + reminder.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error deleting reminder: " + e.getMessage(), e);
        }
    }
}
