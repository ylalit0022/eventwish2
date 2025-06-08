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
import com.ds.eventwish.firebase.FirebaseInAppMessagingHandler;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.SharedPreferences;
import com.ds.eventwish.data.remote.FirestoreManager;

public class EventWishApplication extends Application implements Configuration.Provider, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "EventWishApplication";
    private static final String PREF_NAME = "EventWish";
    private static final String KEY_FCM_TOKEN = "fcm_token";

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
        
        // Set application instance and context first
        instance = this;
        context = getApplicationContext();
        
        try {
            // Initialize Firebase first, before any other Firebase services
            FirebaseApp.initializeApp(this);
            
            // Initialize Firebase Auth early
            FirebaseAuth.getInstance();
            
            // Initialize Firestore
            FirebaseFirestore.getInstance().setFirestoreSettings(
                new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
            );
            
            // Rest of your initialization code...
            
            // Initialize other Firebase services
            initializeFirebaseServices();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during Firebase initialization", e);
        }
        
        // Rest of your onCreate code...
        
        // Log app started for debugging
        Log.d(TAG, "EventWish application starting...");
        
        try {
            // INITIALIZATION ORDER: Core utilities first, then services
            
            // 1. Initialize AppExecutors - required by many other components
            appExecutors = AppExecutors.getInstance();
            Log.d(TAG, "1. AppExecutors initialized");
            
            // 2. Initialize SecureTokenManager - required for security
            try {
                SecureTokenManager.init(this);
                secureTokenManager = SecureTokenManager.getInstance();
                Log.d(TAG, "2. SecureTokenManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SecureTokenManager: " + e.getMessage(), e);
                // Continue without SecureTokenManager
            }
            
            // 3. Initialize DeviceUtils - required for device identification
            try {
                DeviceUtils.init(this);
                deviceUtils = DeviceUtils.getInstance();
                Log.d(TAG, "3. DeviceUtils initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing DeviceUtils: " + e.getMessage(), e);
                // Continue without DeviceUtils
            }
            
            // 4. Initialize ApiClient - required for API communication
            try {
                ApiClient.init(this);
                Log.d(TAG, "4. ApiClient initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing ApiClient: " + e.getMessage(), e);
                // Continue without ApiClient
            }
            
            // 5. Initialize time utils
            timeUtils = new TimeUtils();
            Log.d(TAG, "6. TimeUtils initialized");
            
            // 7. Initialize API service
            try {
                apiService = ApiClient.getClient();
                Log.d(TAG, "7. API service initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing API service: " + e.getMessage(), e);
                // Continue without API service
            }
            
            // 8. Initialize repositories
            try {
                initializeRepositories();
                Log.d(TAG, "8. Repositories initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing repositories: " + e.getMessage(), e);
                // Continue without repositories
            }
            
            // Setup WorkManager and schedule workers
            try {
                setupWorkManager();
                scheduleWorkers();
                Log.d(TAG, "9. WorkManager initialized and workers scheduled");
            } catch (Exception e) {
                Log.e(TAG, "Error setting up WorkManager: " + e.getMessage(), e);
                // Continue without WorkManager
            }
            
            // Initialize analytics
            try {
                setInitialAnalyticsUserProperties();
                Log.d(TAG, "10. Analytics initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing analytics: " + e.getMessage(), e);
                // Continue without analytics
            }
            
            // Initialize Firebase In-App Messaging
            try {
                FirebaseInAppMessagingHandler.init(this);
                Log.d(TAG, "11. Firebase In-App Messaging initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase In-App Messaging: " + e.getMessage(), e);
                // Continue without In-App Messaging
            }
            
            // Create notification channels
            try {
                EventWishNotificationManager.createNotificationChannels(this);
                Log.d(TAG, "12. Notification channels created");
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channels: " + e.getMessage(), e);
                // Continue without notification channels
            }
            
            // Register lifecycle callbacks
            registerActivityLifecycleCallbacks(this);
            
            // Register fragment lifecycle callbacks
            registerFragmentLifecycleCallbacks();
            
            // Register user in background
            registerUserInBackground();
            
            // Initialize AdMob (if needed)
            try {
                AdMobManager.init(this);
                appOpenManager = new AppOpenManager(this);
                Log.d(TAG, "13. AdMob and AppOpenManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing AdMob: " + e.getMessage(), e);
                // Continue without AdMob
            }
            
            // Initialize InternetConnectivityChecker
            InternetConnectivityChecker.getInstance(this);
            
            // Initialize repositories
            TemplateRepository.getInstance().init(this);
            
            // Initialize FirestoreManager with saved FCM token
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String savedToken = prefs.getString(KEY_FCM_TOKEN, null);
            if (savedToken != null) {
                Log.d(TAG, "Found saved FCM token, setting in FirestoreManager");
                FirestoreManager.getInstance().setFcmToken(savedToken);
            }
            
            // Get current FCM token
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                        return;
                    }
                    
                    String token = task.getResult();
                    Log.d(TAG, "Got current FCM token: " + token);
                    
                    // Save token if it's different from saved one
                    if (token != null && !token.equals(savedToken)) {
                        Log.d(TAG, "Saving new FCM token");
                        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
                        FirestoreManager.getInstance().setFcmToken(token);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting FCM token", e));
            
            Log.d(TAG, "EventWish application initialization complete");
        } catch (Exception e) {
            Log.e(TAG, "Error during application startup", e);
            // Allow app to continue even if there are errors during startup
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
            Log.d(TAG, "Initializing core services in proper order...");
            
            // 1. AppExecutors should be initialized first as other components may need it
            appExecutors = AppExecutors.getInstance();
            Log.d(TAG, "✅ AppExecutors initialized");
            
            // 2. Initialize SecureTokenManager early
            try {
                SecureTokenManager.init(this);
                secureTokenManager = SecureTokenManager.getInstance();
                Log.d(TAG, "✅ SecureTokenManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing SecureTokenManager: " + e.getMessage(), e);
                // Continue without SecureTokenManager - this will cause login features to be disabled
            }
            
            // 3. Initialize DeviceUtils before any component that might need device info
            try {
                DeviceUtils.init(this);
                deviceUtils = DeviceUtils.getInstance();
                Log.d(TAG, "✅ DeviceUtils initialized");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing DeviceUtils: " + e.getMessage(), e);
                // Continue without DeviceUtils - some features will be limited
            }
            
            // 4. Create notification channels for Android O and above
            createNotificationChannels();
            Log.d(TAG, "✅ Notification channels created");
            
            // 5. Initialize time utils (no dependencies)
            timeUtils = new TimeUtils();
            Log.d(TAG, "✅ TimeUtils initialized");
            
            // 6. Verify the API client is initialized (should have been done in onCreate)
            if (!ApiClient.isInitialized()) {
                Log.w(TAG, "ApiClient not initialized yet, initializing now");
                ApiClient.init(this);
            }
            
            // 7. Get the API service instance
            try {
                apiService = ApiClient.getClient();
                Log.d(TAG, "✅ ApiService obtained");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting ApiService: " + e.getMessage(), e);
                // Continue without API service - app will work in offline mode
            }
            
            // 8. Initialize Firebase services
            try {
                initializeFirebaseServices();
                Log.d(TAG, "✅ Firebase services initialized");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing Firebase services: " + e.getMessage(), e);
                // Continue without Firebase - analytics and crash reporting will be disabled
            }
            
            // 9. Initialize repositories - depends on ApiClient and other services
            try {
                initializeRepositories();
                Log.d(TAG, "✅ Repositories initialized");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing repositories: " + e.getMessage(), e);
                // Continue with partial repository initialization
            }
            
            // 10. Setup WorkManager for background tasks
            try {
                setupWorkManager();
                Log.d(TAG, "✅ WorkManager setup complete");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error setting up WorkManager: " + e.getMessage(), e);
                // Continue without WorkManager - background tasks will not run
            }
            
            // 11. Schedule reminders and notifications - requires WorkManager
            try {
                scheduleWorkers();
                Log.d(TAG, "✅ Workers scheduled");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error scheduling workers: " + e.getMessage(), e);
                // Continue without scheduled workers - reminders will not work
            }
            
            // 12. Set initial analytics properties - requires Firebase
            try {
                setInitialAnalyticsUserProperties();
                Log.d(TAG, "✅ Initial analytics properties set");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error setting initial analytics properties: " + e.getMessage(), e);
                // Continue without analytics - tracking will be limited
            }
            
            // 13. Initialize Firebase In-App Messaging Handler - should be last Firebase component
            try {
                FirebaseInAppMessagingHandler.init(this);
                Log.d(TAG, "✅ Firebase In-App Messaging Handler initialized");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing Firebase In-App Messaging Handler: " + e.getMessage(), e);
                // Continue without In-App Messaging - no in-app messages will be shown
            }
            
            Log.d(TAG, "✅ All services initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Fatal error during service initialization: " + e.getMessage(), e);
            // Try to log the exception to Firebase if possible
            try {
                FirebaseCrashManager.logException(e);
            } catch (Exception ignored) {
                // Ignore if Firebase is not initialized
            }
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

    /**
     * Initialize Firebase services (Analytics, Crashlytics, Performance, etc.)
     */
    private void initializeFirebaseServices() {
        try {
            // Initialize Firebase Analytics
            FirebaseAnalytics.getInstance(this);
            Log.d(TAG, "Firebase Analytics initialized");
            
            // Initialize Firebase Crashlytics
            FirebaseCrashManager.init(this);
            Log.d(TAG, "Firebase Crashlytics initialized");
            
            // Initialize Firebase Performance
            PerformanceTracker.init(this);
            Log.d(TAG, "Firebase Performance initialized");
            
            // Initialize Firebase Remote Config
            try {
                FirebaseInAppMessagingHandler.init(this);
                Log.d(TAG, "Firebase In-App Messaging initialized");
                com.ds.eventwish.utils.RemoteConfigManager.getInstance(this);
                Log.d(TAG, "Firebase Remote Config initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase Remote Config: " + e.getMessage(), e);
                Log.e(TAG, "Error initializing Firebase In-App Messaging: " + e.getMessage(), e);
            }
            
            // Initialize ShareMessageManager for dynamic share messages
            try {
                com.ds.eventwish.utils.RemoteConfigManager.getInstance(this);
                Log.d(TAG, "Firebase Remote Config initialized");
                com.ds.eventwish.utils.ShareMessageManager.getInstance(this);
                Log.d(TAG, "ShareMessageManager initialized with Firebase Remote Config");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase Remote Config: " + e.getMessage(), e);
                Log.e(TAG, "Error initializing ShareMessageManager: " + e.getMessage(), e);
            }
            
            // Verify Firebase project configuration
            verifyFirebaseProject();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase services: " + e.getMessage(), e);
            Log.e(TAG, "Error initializing Firebase services", e);
        }
    }
}