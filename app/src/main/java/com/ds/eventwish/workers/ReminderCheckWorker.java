package com.ds.eventwish.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.utils.ReminderScheduler;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReminderCheckWorker extends Worker {
    private static final String TAG = "ReminderCheckWorker";
    public static final String WORK_NAME = "reminder_check_worker";

    public ReminderCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting periodic reminder check");
        try {
            ReminderDao reminderDao = new ReminderDao(getApplicationContext());
            List<Reminder> reminders = reminderDao.getAllReminders();
            
            if (reminders != null) {
                long currentTime = System.currentTimeMillis();
                boolean hasErrors = false;
                
                for (Reminder reminder : reminders) {
                    if (!reminder.isCompleted() && reminder.getDateTime() > currentTime) {
                        try {
                            if (!ReminderScheduler.isReminderScheduled(getApplicationContext(), reminder.getId())) {
                                Log.d(TAG, "Re-scheduling missed reminder: " + reminder.getId());
                                ReminderScheduler.scheduleReminder(getApplicationContext(), reminder);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to check/schedule reminder " + reminder.getId(), e);
                            hasErrors = true;
                        }
                    }
                }
                
                return hasErrors ? Result.retry() : Result.success();
            }
            
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Failed to check reminders", e);
            return Result.retry();
        }
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            ReminderCheckWorker.class,
            15, TimeUnit.MINUTES)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        );
        
        Log.d(TAG, "Scheduled periodic reminder check");
    }
}
