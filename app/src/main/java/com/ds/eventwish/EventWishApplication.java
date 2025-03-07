package com.ds.eventwish;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.utils.ReminderScheduler;
import com.ds.eventwish.workers.ReminderCheckWorker;
import java.util.List;
import java.util.concurrent.Executors;

public class EventWishApplication extends Application implements Configuration.Provider {
    private static final String TAG = "EventWishApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Try to get WorkManager instance to check if it's initialized
            WorkManager.getInstance(this);
        } catch (IllegalStateException e) {
            // WorkManager not initialized, initialize it
            WorkManager.initialize(this, getWorkManagerConfiguration());
        }
        
        // Initialize the repository to trigger data loading
        FestivalRepository repository = FestivalRepository.getInstance(this);
        
        // Clear database cache on app start to ensure fresh data
        clearDatabaseCache();
        
        // Refresh data from server
        repository.refreshUpcomingFestivals();
        
        // Create notification channels
        createNotificationChannels();
        
        // Schedule periodic reminder check
        ReminderCheckWorker.schedule(this);
        
        // Restore any pending reminders
        restorePendingReminders();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel reminderChannel = new NotificationChannel(
                "reminder_channel",
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Notifications for event reminders");
            reminderChannel.enableVibration(true);
            reminderChannel.setVibrationPattern(new long[]{0, 500, 250, 500});
            reminderChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            reminderChannel.enableLights(true);
            reminderChannel.setLightColor(Color.RED);
            reminderChannel.setShowBadge(true);
            reminderChannel.setBypassDnd(true);
            
            // Create festival notification channel
            NotificationChannel festivalChannel = new NotificationChannel(
                "festival_channel",
                "Festival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            festivalChannel.setDescription("Notifications for upcoming festivals");
            festivalChannel.enableVibration(true);
            festivalChannel.setVibrationPattern(new long[]{0, 500, 250, 500});
            festivalChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            festivalChannel.enableLights(true);
            festivalChannel.setLightColor(Color.BLUE);
            festivalChannel.setShowBadge(true);
            festivalChannel.setBypassDnd(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(reminderChannel);
                manager.createNotificationChannel(festivalChannel);
            }
        }
    }

    private void restorePendingReminders() {
        try {
            ReminderDao reminderDao = new ReminderDao(this);
            List<Reminder> reminders = reminderDao.getAllReminders();
            
            if (reminders != null) {
                long currentTime = System.currentTimeMillis();
                
                for (Reminder reminder : reminders) {
                    if (!reminder.isCompleted() && reminder.getDateTime() > currentTime) {
                        try {
                            ReminderScheduler.scheduleReminder(this, reminder);
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
    
    private void clearDatabaseCache() {
        Log.d(TAG, "Clearing database cache on app start");
        
        // Use a background thread to clear the cache
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(this);
                database.festivalDao().deleteAllFestivals();
                Log.d(TAG, "Database cache cleared successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing database cache", e);
            }
        });
    }
}
