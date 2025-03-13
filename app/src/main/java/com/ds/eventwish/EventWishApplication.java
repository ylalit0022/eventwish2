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
import com.ds.eventwish.data.ads.AdManager;
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
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.ds.eventwish.ui.ads.AppOpenAdHelper;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.NotificationScheduler;

public class EventWishApplication extends Application implements Configuration.Provider {
    private static final String TAG = "EventWishApplication";

    private AppOpenAdHelper appOpenAdHelper;
    
    // Static instance of the application
    private static EventWishApplication instance;

    /**
     * Get the application instance
     * @return The application instance
     */
    public static EventWishApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set the instance
        instance = this;
        
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
        
        // Initialize ApiClient before using it
        ApiClient.init(this);
        
        // Initialize TokenRepository and check for unsent tokens
        TokenRepository.getInstance(this, ApiClient.getClient())
                .checkAndSendToken();
        
        // Note: We don't need to initialize WorkManager here since we're using Configuration.Provider
        // WorkManager will be initialized automatically using the configuration from getWorkManagerConfiguration()
        
        // Schedule periodic reminder check using the helper method
        ReminderCheckWorker.schedule(this);
        
        // Schedule template update check
        scheduleTemplateUpdateCheck();
        
        // Schedule festival notifications
        scheduleNotifications();
        
        // Initialize cache manager
        CacheManager.getInstance(this);
        
        // Clear database cache if needed
        clearDatabaseCache();
        
        // Restore any pending reminders
        restorePendingReminders();
        
        // Initialize CategoryIconRepository and preload icons
        Log.d(TAG, "Initializing CategoryIconRepository");
        try {
            CategoryIconRepository.getInstance().loadCategoryIcons();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing CategoryIconRepository: " + e.getMessage());
            // Continue with app initialization even if category icons fail to load
        }
        
        // Initialize AdMob and AdManager
        initializeAdMob();
        
        // Log initialization complete
        Log.i(TAG, "Application initialized successfully");
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build();
    }

    /**
     * Create notification channels for Android O and above
     */
    private void createNotificationChannels() {
        // Create notification channels using NotificationManager
        EventWishNotificationManager.createNotificationChannels(this);
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
    
    /**
     * Schedule all notification workers
     */
    private void scheduleNotifications() {
        Log.d(TAG, "Scheduling notification workers");
        
        // Schedule all notifications
        NotificationScheduler.scheduleAllNotifications(this);
    }
    
    /**
     * Initialize AdMob and AdManager
     */
    private void initializeAdMob() {
        try {
            MobileAds.initialize(this, initializationStatus -> {
                Log.d(TAG, "AdMob SDK initialized successfully");
                
                // Initialize AdManager
                AdManager.getInstance(this);
                
                // Initialize AppOpenAdHelper
                appOpenAdHelper = new AppOpenAdHelper(this);
            });
            Log.d(TAG, "AdMob initialization started");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdMob: " + e.getMessage());
        }
    }
}
