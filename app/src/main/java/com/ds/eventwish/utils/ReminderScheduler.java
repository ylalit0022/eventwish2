package com.ds.eventwish.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.receivers.ReminderAlarmReceiver;
import com.ds.eventwish.workers.ReminderNotificationWorker;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static com.ds.eventwish.utils.ReminderException.ErrorType;

public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";

    public static void scheduleReminder(Context context, Reminder reminder) throws ReminderException {
        validateReminder(reminder);
        checkPermissions(context);

        long currentTime = System.currentTimeMillis();
        long reminderTime = reminder.getDateTime();
        long delay = reminderTime - currentTime;

        Log.d(TAG, String.format("Scheduling reminder %d: current=%d, reminder=%d, delay=%d", 
            reminder.getId(), currentTime, reminderTime, delay));

        try {
            if (delay < 0) {
                Log.d(TAG, "Reminder is overdue, showing notification directly");
                showNotificationDirectly(context, reminder);
            } else {
                Log.d(TAG, "Scheduling reminder with AlarmManager");
                scheduleWithAlarmManager(context, reminder);
            }
        } catch (Exception e) {
            String error = String.format("Failed to schedule reminder %d: %s", 
                reminder.getId(), e.getMessage());
            Log.e(TAG, error, e);
            throw new ReminderException(
                ErrorType.SCHEDULING_ERROR,
                error,
                e
            );
        }
    }

    public static void testScheduleImmediate(Context context, Reminder reminder) {
        // Removed test method
    }

    private static void validateReminder(Reminder reminder) throws ReminderException {
        if (reminder == null) {
            throw new ReminderException(
                ErrorType.INVALID_REMINDER,
                "Reminder cannot be null"
            );
        }
        if (reminder.isCompleted()) {
            throw new ReminderException(
                ErrorType.INVALID_REMINDER,
                "Cannot schedule completed reminder"
            );
        }
        if (reminder.getTitle() == null || reminder.getTitle().trim().isEmpty()) {
            throw new ReminderException(
                ErrorType.INVALID_REMINDER,
                "Reminder title cannot be empty"
            );
        }
        if (reminder.getDateTime() <= 0) {
            throw new ReminderException(
                ErrorType.INVALID_REMINDER,
                "Invalid reminder date/time"
            );
        }
    }

    private static void checkPermissions(Context context) throws ReminderException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new ReminderException(
                    ErrorType.PERMISSION_DENIED,
                    "Notification permission not granted"
                );
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                throw new ReminderException(
                    ErrorType.PERMISSION_DENIED,
                    "Exact alarm permission not granted"
                );
            }
        }
    }

    private static void scheduleWithAlarmManager(Context context, Reminder reminder) throws ReminderException {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            throw new ReminderException(
                ErrorType.SYSTEM_SERVICE_UNAVAILABLE,
                "AlarmManager not available"
            );
        }

        try {
            // Create explicit intent
            Intent intent = new Intent(context, ReminderAlarmReceiver.class);
            intent.setAction("com.ds.eventwish.SHOW_REMINDER");
            intent.setPackage(context.getPackageName());
            intent.putExtra("reminderId", reminder.getId());
            intent.putExtra("title", reminder.getTitle());
            intent.putExtra("description", reminder.getDescription());

            // Create unique request code based on reminder ID and time
            int requestCode = (int) ((reminder.getId() + reminder.getDateTime()) % Integer.MAX_VALUE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Use setExactAndAllowWhileIdle for more reliable alarms
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getDateTime(),
                        pendingIntent
                    );
                    Log.d(TAG, "Scheduled exact alarm for reminder " + reminder.getId() + " at " + reminder.getDateTime());
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getDateTime(),
                        pendingIntent
                    );
                    Log.d(TAG, "Scheduled inexact alarm for reminder " + reminder.getId() + " at " + reminder.getDateTime());
                }
            } else {
                // For older versions, use setExactAndAllowWhileIdle
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.getDateTime(),
                    pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm for reminder " + reminder.getId() + " at " + reminder.getDateTime());
            }

            // Also schedule with WorkManager as a backup
            scheduleWithWorkManager(context, reminder);
        } catch (Exception e) {
            String error = String.format("Failed to schedule alarm for reminder %d: %s", 
                reminder.getId(), e.getMessage());
            Log.e(TAG, error, e);
            throw new ReminderException(
                ErrorType.ALARM_MANAGER_ERROR,
                error,
                e
            );
        }
    }

    private static void scheduleWithWorkManager(Context context, Reminder reminder) throws ReminderException {
        try {
            long delay = Math.max(0, reminder.getDateTime() - System.currentTimeMillis());

            Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

            Data notificationData = new Data.Builder()
                .putString("title", reminder.getTitle())
                .putString("description", reminder.getDescription())
                .putLong("reminderId", reminder.getId())
                .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ReminderNotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(notificationData)
                .addTag("reminder_" + reminder.getId())
                .build();

            String uniqueWorkName = "reminder_" + reminder.getId();
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                );

            Log.d(TAG, "Scheduled reminder with WorkManager: " + reminder.getId());
        } catch (Exception e) {
            throw new ReminderException(
                ErrorType.WORK_MANAGER_ERROR,
                "Failed to schedule work: " + e.getMessage(),
                e
            );
        }
    }

    private static void showNotificationDirectly(Context context, Reminder reminder) {
        try {
            Intent intent = new Intent(context, ReminderAlarmReceiver.class);
            intent.setAction("com.ds.eventwish.SHOW_REMINDER");
            intent.putExtra("reminderId", reminder.getId());
            intent.putExtra("title", reminder.getTitle());
            intent.putExtra("description", reminder.getDescription());
            context.sendBroadcast(intent);
            Log.d(TAG, "Sent immediate notification broadcast for reminder: " + reminder.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification directly: " + e.getMessage(), e);
        }
    }

    public static void cancelReminder(Context context, long reminderId) {
        try {
            // Cancel AlarmManager
            Intent intent = new Intent(context, ReminderAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }

            // Cancel WorkManager
            WorkManager.getInstance(context).cancelUniqueWork("reminder_" + reminderId);
            
            Log.d(TAG, "Cancelled reminder: " + reminderId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling reminder: " + e.getMessage(), e);
        }
    }

    public static void rescheduleAllReminders(Context context, List<Reminder> reminders) throws ReminderException {
        if (reminders == null) {
            throw new ReminderException(
                ErrorType.INVALID_REMINDER,
                "Reminder list cannot be null"
            );
        }

        for (Reminder reminder : reminders) {
            try {
                if (!reminder.isCompleted() && reminder.getDateTime() >= System.currentTimeMillis()) {
                    scheduleReminder(context, reminder);
                }
            } catch (ReminderException e) {
                Log.e(TAG, "Failed to reschedule reminder " + reminder.getId() + ": " + e.getMessage());
                // Continue with other reminders even if one fails
            }
        }
    }

    public static boolean isReminderScheduled(Context context, long reminderId) throws ReminderException {
        try {
            // Check WorkManager
            List<WorkInfo> workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTag("reminder_" + reminderId)
                .get();

            for (WorkInfo workInfo : workInfos) {
                if (workInfo.getState() == WorkInfo.State.ENQUEUED || 
                    workInfo.getState() == WorkInfo.State.RUNNING) {
                    return true;
                }
            }

            // Check AlarmManager
            Intent intent = new Intent(context, ReminderAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            return pendingIntent != null;
        } catch (Exception e) {
            throw new ReminderException(
                ErrorType.SCHEDULING_ERROR,
                "Failed to check reminder status: " + e.getMessage(),
                e
            );
        }
    }
}
