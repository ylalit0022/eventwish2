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
import com.ds.eventwish.data.remote.ApiService;
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
import com.ds.eventwish.ads.AppOpenManager;
import com.ds.eventwish.utils.FirebaseCrashManager;
import com.ds.eventwish.utils.PerformanceTracker;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;
import com.ds.eventwish.ui.ads.SponsoredAdManagerFactory;
import com.ds.eventwish.utils.AdSessionManager;

public class EventWishApplication extends Application implements Configuration.Provider, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "EventWishApplication";

    // Static instance of the application
    private static EventWishApplication instance;

    private static Context context;

    private ApiService apiService;
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
    
    // Activity tracking
    private int runningActivities = 0;

    private AppOpenManager appOpenManager;

    private boolean wasInBackground = true;
    private AdSessionManager adSessionManager;

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
            // Verify Firebase project configuration
            verifyFirebaseProject();
            
            // Initialize services
            initializeServices();
            
            // Register activity lifecycle callbacks
            registerActivityLifecycleCallbacks(this);
            
            // Register fragment lifecycle callbacks to track navigation
            registerFragmentLifecycleCallbacks();
            
            // Register user in background
            registerUserInBackground();
            
            // Initialize AdMob
            AdMobManager.init(this);

            // Initialize app open ads
            appOpenManager = new AppOpenManager(this);
            appOpenManager.fetchAd(); // Pre-fetch first ad
            
            // Initialize Sponsored Ads
            SponsoredAdManagerFactory.init(this);
            Log.d(TAG, "SponsoredAdManagerFactory initialized");
            
            // Initialize AdSessionManager - for simple impression tracking
            adSessionManager = AdSessionManager.getInstance(this);
            Log.d(TAG, "AdSessionManager initialized with new session");
            
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
            
            Log.d(TAG, "Firebase Analytics initialized");
            
            // Verify Firebase Analytics is properly initialized
            if (com.google.firebase.analytics.FirebaseAnalytics.getInstance(this) != null) {
                Log.d(TAG, "Firebase Analytics instance successfully obtained");
            } else {
                Log.e(TAG, "Failed to get Firebase Analytics instance - this may cause tracking issues");
            }
            
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

    /**
     * Initialize all required services
     */
    private void initializeServices() {
        try {
            // Log start of service initialization
            Log.d(TAG, "Initializing application services...");
            
            // Initialize performance tracking (must be done early)
            PerformanceTracker.init(this);
            
            // Create notification channels
            createNotificationChannels();
            
            // Initialize Firebase first to enable crash reporting for initialization
            FirebaseCrashManager.init(this);
            
            // Initialize API client
            com.ds.eventwish.data.remote.ApiClient.init(this);
            apiService = com.ds.eventwish.data.remote.ApiClient.getClient();
            
            // Initialize executors with proper parameters
            appExecutors = AppExecutors.getInstance();
            
            // Initialize security - this must be done before other services that need it
            SecureTokenManager.init(this);
            secureTokenManager = SecureTokenManager.getInstance();
            
            // Initialize device utils
            DeviceUtils.init(this);
            deviceUtils = DeviceUtils.getInstance();
            
            // Initialize time utilities
            timeUtils = new TimeUtils();
            
            // Initialize analytics
            AnalyticsUtils.init(this);
            
            // Enable debug mode for analytics in debug builds
            if (BuildConfig.DEBUG) {
                AnalyticsUtils.setDebugMode(true);
                
                // Set metadata tag to enable debug analytics
                try {
                    FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
                    
                    // Add special debug parameter to force data collection
                    Bundle debugParams = new Bundle();
                    debugParams.putBoolean("debug_mode", true);
                    debugParams.putString("app_version", BuildConfig.VERSION_NAME);
                    debugParams.putString("device_model", android.os.Build.MODEL);
                    debugParams.putLong("startup_time", System.currentTimeMillis());
                    FirebaseAnalytics.getInstance(this).logEvent("debug_analytics_startup", debugParams);
                    
                    Log.d(TAG, "Firebase Analytics debug mode enabled");
                } catch (Exception e) {
                    Log.e(TAG, "Error enabling Firebase Analytics debug mode", e);
                }
            }
            
            // Track detailed device information
            AnalyticsUtils.trackDeviceInfo(this);
            
            // Initialize repositories
            initializeRepositories();
            
            // Set initial analytics user properties
            setInitialAnalyticsUserProperties();
            
            // Verify Analytics tracking
            AnalyticsUtils.verifyConfiguration(this);
            
            // Force dispatch analytics events to verify data is being sent
            AnalyticsUtils.forceDispatchEvents();
            
            // Initialize all fragments
            initializeComponents();
            
            // Clear database cache
            clearDatabaseCache();
            
            // Schedule worker threads
            scheduleWorkers();
            
            // Restore pending reminders
            restorePendingReminders();
            
            Log.d(TAG, "Service initialization complete");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing services", e);
            FirebaseCrashManager.logException(e);
        }
    }

    /**
     * Set initial analytics user properties for better segmentation
     */
    private void setInitialAnalyticsUserProperties() {
        try {
            // Set user properties for analytics
            AnalyticsUtils.setUserProperty("device_model", DeviceUtils.getDeviceModel());
            AnalyticsUtils.setUserProperty("os_version", DeviceUtils.getAndroidVersion());
            AnalyticsUtils.setUserProperty("app_version", DeviceUtils.getAppVersionName());
            
            // Get a unique identifier for the user
            String deviceId = DeviceUtils.getDeviceId(this);
            if (deviceId != null && !deviceId.isEmpty()) {
                // Set the user ID for analytics - this is critical for user tracking
                AnalyticsUtils.setUserId(deviceId);
                
                // Set additional user properties
                AnalyticsUtils.setUserProperty("user_type", "app_user");
                AnalyticsUtils.setUserProperty("install_date", String.valueOf(System.currentTimeMillis()));
                
                // Also set for Crashlytics
                FirebaseCrashManager.setUserId(deviceId);
            } else {
                Log.e(TAG, "Failed to get device ID for analytics user identification");
            }
            
            Log.d(TAG, "Initial analytics user properties set");
        } catch (Exception e) {
            Log.e(TAG, "Error setting initial analytics user properties", e);
            try {
                FirebaseCrashManager.logException(e);
            } catch (Exception ignored) {
                // Ignore if Crashlytics not initialized
            }
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
    
    public ApiService getApiService() {
        return apiService;
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
        // Initialize activity tracking
        Log.d(TAG, "Activity created: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // Track app coming to foreground
        runningActivities++;
        currentActivity = activity;
        
        // Check if app is coming from background to foreground
        if (wasInBackground) {
            Log.d(TAG, "App coming to foreground, creating new ad impression session");
            
            // Create a new ad impression session if coming from background
            if (adSessionManager != null) {
                adSessionManager.createNewSession();
            }
            
            wasInBackground = false;
        }
        
        // Track screen views for analytics
        if (activity != null) {
            String screenName = activity.getClass().getSimpleName();
            AnalyticsUtils.trackScreenView(activity, screenName, activity.getClass().getName());
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
        
        // Start performance trace for activity display
        PerformanceTracker.startTrace("activity_display_" + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Stop performance trace for activity display
        PerformanceTracker.stopTrace("activity_display_" + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        runningActivities--;
        if (runningActivities == 0) {
            Log.d(TAG, "App went to background");
            wasInBackground = true;
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

    public SecureTokenManager getSecureTokenManager() {
        return secureTokenManager;
    }

    /**
     * Verify Firebase project configuration
     * This will log the Firebase project details from google-services.json
     */
    private void verifyFirebaseProject() {
        try {
            Log.d(TAG, "======= FIREBASE PROJECT VERIFICATION =======");
            
            // Get the Application meta-data
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            
            if (appInfo.metaData != null) {
                // Try to get project info from metadata
                String gcmDefaultSenderId = appInfo.metaData.getString("com.google.android.gms.version");
                String googleAppId = appInfo.metaData.getString("google_app_id");
                String firebaseProjectId = appInfo.metaData.getString("project_id");
                
                Log.d(TAG, "Package Name: " + getPackageName());
                Log.d(TAG, "GCM Sender ID: " + gcmDefaultSenderId);
                Log.d(TAG, "Google App ID: " + googleAppId);
                Log.d(TAG, "Firebase Project ID: " + firebaseProjectId);
                
                // Get Firebase instance to verify it's working
                if (FirebaseAnalytics.getInstance(this) != null) {
                    Log.d(TAG, "Firebase Analytics instance successfully created");
                }
            } else {
                Log.e(TAG, "No metadata found in the AndroidManifest");
            }
            
            Log.d(TAG, "======= END FIREBASE PROJECT VERIFICATION =======");
        } catch (Exception e) {
            Log.e(TAG, "Error verifying Firebase project", e);
        }
    }

    /**
     * Register fragment lifecycle callbacks to track navigation
     */
    private void registerFragmentLifecycleCallbacks() {
        try {
            androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks = 
                new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull androidx.fragment.app.FragmentManager fm, 
                                                 @NonNull androidx.fragment.app.Fragment fragment) {
                        super.onFragmentResumed(fm, fragment);
                        
                        // Track the fragment view
                        String fragmentName = fragment.getClass().getSimpleName();
                        // Get the activity associated with the fragment for proper context
                        Activity activity = fragment.getActivity();
                        if (activity != null) {
                            AnalyticsUtils.trackScreenView(activity, fragmentName, fragment.getClass().getName());
                        } else {
                            // Fall back to the old method if no activity is available
                            AnalyticsUtils.trackScreenView(fragmentName, fragment.getClass().getName());
                        }
                        
                        Log.d(TAG, "Fragment navigation: " + fragmentName);
                    }
                };
                
            // Register the callback with all activities that use fragments
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    if (activity instanceof androidx.fragment.app.FragmentActivity) {
                        ((androidx.fragment.app.FragmentActivity) activity)
                            .getSupportFragmentManager()
                            .registerFragmentLifecycleCallbacks(fragmentCallbacks, true);
                        
                        Log.d(TAG, "Registered fragment tracking for: " + activity.getClass().getSimpleName());
                    }
                }
                
                @Override
                public void onActivityStarted(Activity activity) {}
                
                @Override
                public void onActivityResumed(Activity activity) {}
                
                @Override
                public void onActivityPaused(Activity activity) {}
                
                @Override
                public void onActivityStopped(Activity activity) {}
                
                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
                
                @Override
                public void onActivityDestroyed(Activity activity) {}
            });
            
            Log.d(TAG, "Fragment lifecycle callbacks registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error registering fragment lifecycle callbacks", e);
        }
    }

    /**
     * Initialize all repositories
     */
    private void initializeRepositories() {
        try {
            Log.d(TAG, "Initializing repositories in proper sequence...");
            
            // First initialize repositories that don't depend on others
            // Initialize ResourceRepository first as others may depend on it
            ResourceRepository.getInstance(this);
            resourceRepository = ResourceRepository.getInstance();
            Log.d(TAG, "✅ ResourceRepository initialized");
            
            // Initialize UserRepository next as it handles authentication
            userRepository = UserRepository.getInstance(this);
            Log.d(TAG, "✅ UserRepository initialized");
            
            // Initialize FestivalRepository
            festivalRepository = FestivalRepository.getInstance(this);
            Log.d(TAG, "✅ FestivalRepository initialized");
            
            // Initialize CategoryIconRepository which depends on ResourceRepository
            categoryIconRepository = CategoryIconRepository.getInstance(this);
            Log.d(TAG, "✅ CategoryIconRepository initialized");
            
            // Initialize TemplateRepository last as it may depend on other repositories
            TemplateRepository.init(this);
            templateRepository = TemplateRepository.getInstance();
            Log.d(TAG, "✅ TemplateRepository initialized");
            
            Log.d(TAG, "✅ All repositories initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing repositories", e);
            FirebaseCrashManager.logException(e);
        }
    }

    /**
     * Main thread executor for AppExecutors
     */
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    /**
     * Get the AdSessionManager instance
     */
    public AdSessionManager getAdSessionManager() {
        return adSessionManager;
    }
}