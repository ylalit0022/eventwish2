package com.ds.eventwish;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

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
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.CacheManager;
import com.ds.eventwish.utils.ReminderScheduler;
import com.ds.eventwish.workers.ReminderCheckWorker;
import com.ds.eventwish.workers.TemplateUpdateWorker;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.NotificationScheduler;
import com.ds.eventwish.util.SecureTokenManager;
import java.util.Map;
import com.ds.eventwish.utils.TimeUtils;
import com.ds.eventwish.utils.DeviceUtils;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.data.repository.ResourceRepository;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.ads.AdMobManager;

public class EventWishApplication extends Application implements Configuration.Provider, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "EventWishApplication";

    // Static instance of the application
    private static EventWishApplication instance;

    private static Context context;

    private ApiClient apiClient;
    private CategoryIconRepository categoryIconRepository;
    private SecureTokenManager secureTokenManager;
    private DeviceUtils deviceUtils;
    private TimeUtils timeUtils;
    private Activity currentActivity;

    // Repositories
    private FestivalRepository festivalRepository;
    private TemplateRepository templateRepository;
    private ResourceRepository resourceRepository;
    private UserRepository userRepository;

    // Services
    private AppExecutors appExecutors;
    
    // AdMob manager
    private AdMobManager adMobManager;

    // Activity tracking
    private int runningActivities = 0;

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
        
        // Set application instance and context first, before any other initialization
        instance = this;
        context = getApplicationContext();
        
        // Log app started for debugging
        Log.d(TAG, "EventWish application starting...");
        
        try {
            // Initialize services
            initializeServices();
            
            // Register activity lifecycle callbacks
            registerActivityLifecycleCallbacks(this);
            
            // Register user in background
            registerUserInBackground();
            
            Log.d(TAG, "EventWish application started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during application startup", e);
        }
    }

    /**
     * Register user with server in background
     */
    private void registerUserInBackground() {
        // Check that all required services are initialized first
        if (!isServicesInitialized()) {
            Log.w(TAG, "Cannot register user: Core services not fully initialized");
            
            // Only retry once at most to avoid loops
            if (appExecutors != null) {
                // Schedule a delayed retry if AppExecutors is available
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "Retrying user registration after delay");
                    registerUserInBackground();
                }, 5000); // 5 second delay
            } else {
                Log.e(TAG, "Cannot register user and cannot retry: AppExecutors not initialized");
            }
            return;
        }
        
        // Use the appExecutors for better thread management
        appExecutors.networkIO().execute(() -> {
            try {
                if (userRepository != null) {
                    // Check if SecureTokenManager is properly initialized
                    if (secureTokenManager == null) {
                        Log.e(TAG, "Cannot register user: SecureTokenManager not initialized");
                        return;
                    }
                    
                    userRepository.registerUserIfNeeded();
                    Log.d(TAG, "Background user registration initiated");
                } else {
                    Log.e(TAG, "Cannot register user: UserRepository is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during background user registration", e);
                // Don't try to recover automatically as this might cause loops
            }
        });
    }

    /**
     * Check if all required services are initialized
     */
    private boolean isServicesInitialized() {
        if (appExecutors == null) {
            Log.e(TAG, "AppExecutors not initialized");
            return false;
        }
        
        if (secureTokenManager == null) {
            Log.e(TAG, "SecureTokenManager not initialized");
            return false;
        }
        
        if (userRepository == null) {
            Log.e(TAG, "UserRepository not initialized");
            return false;
        }
        
        return true;
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
     * Method to initialize components that depend on the EventWishApplication
     */
    private void initializeComponents() {
        try {
            // Initialize Analytics
            AnalyticsUtils.init(this);
            
            // Create notification channels
            createNotificationChannels();
            
            // Schedule workers
            scheduleWorkers();
            
            // Initialize cache manager
            CacheManager.getInstance(this);
            
            // Clear database cache if needed
            clearDatabaseCache();
            
            // Restore pending reminders
            restorePendingReminders();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
        }
    }

    public static Context getAppContext() {
        if (context == null) {
            Log.w(TAG, "Application context requested before initialization");
            if (instance != null) {
                // Try to set context if instance exists but context wasn't set
                context = instance.getApplicationContext();
                Log.d(TAG, "Application context recovered from instance");
            } else {
                Log.e(TAG, "Cannot get application context: instance is null");
            }
        }
        return context;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Clean up resources
    }

    private void scheduleWorkers() {
        Log.d(TAG, "Scheduling workers");
        
        // Schedule template update check
        scheduleTemplateUpdateCheck();
        
        // Schedule notifications
        scheduleNotifications();
        
        // Schedule reminder check
        scheduleReminderCheck();
    }
    
    private void scheduleReminderCheck() {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            ReminderCheckWorker.class,
            15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build();
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        );
        
        Log.d(TAG, "Scheduled reminder check worker");
    }

    private void setupWorkManager() {
        // Implementation of setupWorkManager method
    }

    private void initializeServices() {
        try {
            Log.d(TAG, "Initializing services...");
            
            // Initialize SecureTokenManager first - other services may depend on it
            try {
                Log.d(TAG, "Initializing SecureTokenManager");
                SecureTokenManager.init(this);
                secureTokenManager = SecureTokenManager.getInstance();
                Log.d(TAG, "SecureTokenManager initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SecureTokenManager", e);
                // Continue without secure token manager - some features may be limited
            }
            
            // Initialize DeviceUtils next - many services depend on it
            try {
                Log.d(TAG, "Initializing DeviceUtils");
                DeviceUtils.init(this);
                deviceUtils = DeviceUtils.getInstance();
                Log.d(TAG, "DeviceUtils initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing DeviceUtils", e);
                // Continue without device utils - some features may be limited
            }
            
            // Initialize API client - depends on SecureTokenManager
            ApiClient.init(this);
            // For backward compatibility
            com.ds.eventwish.network.ApiClient.init(this);
            
            // Initialize AppExecutors
            appExecutors = AppExecutors.getInstance();
            
            // Initialize repositories
            Log.d(TAG, "Initializing repositories...");
            
            // Initialize repositories in background
            categoryIconRepository = CategoryIconRepository.getInstance(this);
            templateRepository = TemplateRepository.getInstance();
            festivalRepository = FestivalRepository.getInstance(this);
            resourceRepository = ResourceRepository.getInstance(this);
            
            // User repository (needs secure token manager)
            if (secureTokenManager != null) {
                userRepository = UserRepository.getInstance(this);
            } else {
                Log.e(TAG, "Failed to initialize UserRepository: SecureTokenManager is null");
            }
            
            // Time utils needs to be initialized with server time
            timeUtils = new TimeUtils();
            
            // Initialize AdMobManager
            try {
                AdMobManager.init(this);
                adMobManager = AdMobManager.getInstance();
                Log.d(TAG, "AdMobManager initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing AdMobManager", e);
            }
            
            // Initialize components like analytics, notifications, etc.
            initializeComponents();
            
            // Set up work manager
            setupWorkManager();
            
            // Schedule workers
            scheduleWorkers();
            
            // Restore pending reminders
            restorePendingReminders();
            
            Log.d(TAG, "Services initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing services", e);
        }
    }

    public FestivalRepository getFestivalRepository() {
        return festivalRepository;
    }
    
    public TemplateRepository getTemplateRepository() {
        return templateRepository;
    }
    
    public ResourceRepository getResourceRepository() {
        return resourceRepository;
    }
    
    public CategoryIconRepository getCategoryIconRepository() {
        return categoryIconRepository;
    }
    
    public ApiClient getApiClient() {
        return apiClient;
    }
    
    public TimeUtils getTimeUtils() {
        return timeUtils;
    }
    
    public AppExecutors getAppExecutors() {
        return appExecutors;
    }
    
    public boolean isAppInForeground() {
        return runningActivities > 0;
    }

    // Activity lifecycle callbacks
    
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "Activity created: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (runningActivities == 0) {
            Log.d(TAG, "App went to foreground");
        }
        runningActivities++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        this.currentActivity = activity;
        
        // Update user's last online status
        if (userRepository != null) {
            userRepository.updateUserActivity(null);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "Activity paused: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        runningActivities--;
        if (runningActivities == 0) {
            Log.d(TAG, "App went to background");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // Not used
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "Activity destroyed: " + activity.getClass().getSimpleName());
    }

    /**
     * Get the UserRepository instance
     * @return UserRepository instance
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }

    /**
     * Get the AdMobManager instance
     * @return AdMobManager instance or null if not initialized
     */
    public AdMobManager getAdMobManager() {
        return adMobManager;
    }

    public SecureTokenManager getSecureTokenManager() {
        return secureTokenManager;
    }
}
