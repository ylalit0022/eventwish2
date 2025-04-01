package com.ds.eventwish;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.data.ads.AdManager;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.data.repository.TokenRepository;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.CacheManager;
import com.ds.eventwish.utils.ReminderScheduler;
import com.ds.eventwish.workers.ReminderCheckWorker;
import com.ds.eventwish.workers.TemplateUpdateWorker;
import com.google.android.gms.ads.MobileAds;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.ds.eventwish.ui.ads.AppOpenAdHelper;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.NotificationScheduler;
import android.app.Activity;
import com.google.android.gms.ads.RequestConfiguration;
import com.ds.eventwish.config.AdConfig;
import java.util.Arrays;
import com.ds.eventwish.util.SecureTokenManager;

public class EventWishApplication extends Application implements Configuration.Provider {
    private static final String TAG = "EventWishApplication";

    private AppOpenAdHelper appOpenAdHelper;
    
    // Static instance of the application
    private static EventWishApplication instance;

    private static Context context;

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
        
        // Initialize SecureTokenManager
        initializeSecureTokenManager();
        
        // Initialize API client
        initializeApiClient();
        
        // Initialize Analytics
        try {
            AnalyticsUtils.init(this);
            Log.d(TAG, "Analytics initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Analytics initialization failed", e);
        }
        
        // Create notification channels
        createNotificationChannels();
        
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
        
        // Initialize AdMob
        initializeAdMob();
        
        // Initialize CategoryIconRepository and preload icons
        Log.d(TAG, "Initializing CategoryIconRepository");
        try {
            CategoryIconRepository.getInstance().loadCategoryIcons();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing CategoryIconRepository: " + e.getMessage());
            // Continue with app initialization even if category icons fail to load
        }

        Log.d(TAG, "EventWish Application initialized");
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
     * Initialize AdMob and AdMobManager
     */
    private void initializeAdMob() {
        try {
            // Initialize AdMobManager
            AdMobManager.getInstance(this);
            Log.d(TAG, "AdMobManager initialized successfully");

            // Configure test devices if in test mode
            if (AdConfig.USE_TEST_ADS) {
                RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(Arrays.asList(
                        "2077EF0E09001FB2DCA76B5415D53BFA", // Replace with your test device ID
                        "B3EEABB8EE11C2BE770B684D95219ECB"  // Generic test device ID
                    ))
                    .build();
                MobileAds.setRequestConfiguration(configuration);
                Log.d(TAG, "AdMob test configuration set");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdMob: " + e.getMessage());
        }
    }

    // Method to initialize AdMobManager from an Activity
    public void initializeAdMobManager(Activity activity) {
        AdMobManager.getInstance(this).setCurrentActivity(activity);
    }

    private void initializeSecureTokenManager() {
        try {
            SecureTokenManager.init(this);
            Log.d(TAG, "SecureTokenManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "SecureTokenManager initialization failed", e);
        }
    }

    private void initializeApiClient() {
        try {
            ApiClient.init(this);
            Log.d(TAG, "ApiClient initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "ApiClient initialization failed", e);
        }
    }

    public static Context getAppContext() {
        return context;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Clean up resources
    }
}
