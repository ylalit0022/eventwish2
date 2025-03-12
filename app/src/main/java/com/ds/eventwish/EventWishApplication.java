package com.ds.eventwish;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.data.repository.TokenRepository;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.CacheManager;
import com.ds.eventwish.utils.ReminderScheduler;
import com.ds.eventwish.workers.ReminderCheckWorker;
import com.ds.eventwish.workers.TemplateUpdateWorker;
import com.ds.eventwish.utils.FirebaseInAppMessagingHandler;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventWishApplication extends Application implements Configuration.Provider {
    private static final String TAG = "EventWishApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase with logging
        try {
            FirebaseApp.initializeApp(this);
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            Log.d(TAG, "Firebase initialized successfully");
            Log.d(TAG, "Firebase project ID: " + options.getProjectId());
            Log.d(TAG, "Firebase application ID: " + options.getApplicationId());
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }
        
        // Create notification channels
        createNotificationChannels();
        
        // Initialize Firebase In-App Messaging Handler
        FirebaseInAppMessagingHandler.initialize(this);
        
        // Initialize TokenRepository and check for unsent tokens
        TokenRepository.getInstance(this, ApiClient.getClient())
                .checkAndSendToken();
        
        // Note: We don't need to initialize WorkManager here since we're using Configuration.Provider
        // WorkManager will be initialized automatically using the configuration from getWorkManagerConfiguration()
        
        // Schedule periodic reminder check using the helper method
        ReminderCheckWorker.schedule(this);
        
        // Schedule template update check
        scheduleTemplateUpdateCheck();
        
        // Initialize cache manager
        CacheManager.getInstance(this);
        
        // Clear database cache if needed
        clearDatabaseCache();
        
        // Restore any pending reminders
        restorePendingReminders();
        
        // Initialize CategoryIconRepository and preload icons
        Log.d(TAG, "Initializing CategoryIconRepository");
        CategoryIconRepository.getInstance().loadCategoryIcons();
        
        // Log initialization complete
        Log.i(TAG, "Application initialized successfully");
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
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
                if (database != null) {
                    // Only clear festivals, not other data
                    database.festivalDao().deleteAllFestivals();
                    Log.d(TAG, "Festival database cache cleared successfully");
                } else {
                    Log.w(TAG, "Database instance is null, skipping cache clear");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error clearing database cache", e);
            }
        });
    }

    /**
     * Schedule a worker to check for template updates every 15 minutes
     */
    private void scheduleTemplateUpdateCheck() {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            TemplateUpdateWorker.class,
            15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build();
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "template_update_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        );
        
        Log.d(TAG, "Scheduled template update check worker");
    }
}
