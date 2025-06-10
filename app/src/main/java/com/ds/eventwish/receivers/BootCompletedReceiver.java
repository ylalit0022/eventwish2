package com.ds.eventwish.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.utils.ReminderScheduler;
import com.ds.eventwish.utils.EventNotificationManager;

import java.util.List;

/**
 * Receiver for BOOT_COMPLETED action to reschedule notifications and reminders after device restart
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted, restoring reminders and notifications");
            
            // Restore reminders immediately
            restoreReminders(context);
            
            // Wait a bit to ensure all services are initialized before setting up event notifications
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // Initialize EventNotificationManager
                    EventNotificationManager.init(context);
                    
                    // Check for notifications
                    EventNotificationManager.getInstance(context).checkAndShowNotification();
                    
                    Log.d(TAG, "Event notifications rescheduled after boot");
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling event notifications after boot", e);
                }
            }, 10000); // Wait 10 seconds after boot
        }
    }

    /**
     * Restore reminders from database and reschedule them
     */
    private void restoreReminders(Context context) {
        try {
            ReminderDao reminderDao = new ReminderDao(context);
            List<Reminder> reminders = reminderDao.getAllReminders();
            
            if (reminders != null) {
                long currentTime = System.currentTimeMillis();
                
                for (Reminder reminder : reminders) {
                    if (!reminder.isCompleted() && reminder.getDateTime() > currentTime) {
                        try {
                            ReminderScheduler.scheduleReminder(context, reminder);
                            Log.d(TAG, "Restored reminder: " + reminder.getId());
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to restore reminder " + reminder.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore reminders: " + e.getMessage());
        }
    }
}
