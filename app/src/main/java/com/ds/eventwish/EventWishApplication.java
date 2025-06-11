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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.SharedPreferences;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.ds.eventwish.utils.EventNotificationManager;
import com.ds.eventwish.data.auth.AuthManager;

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
            // Initialize Firebase first
            FirebaseApp.initializeApp(this);
            
            // Initialize Firestore with offline persistence
            FirebaseFirestore.getInstance().setFirestoreSettings(
                new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
            );
            
            // Initialize FirestoreManager with context
            FirestoreManager.getInstance(this);
            
            // Initialize templates collection to ensure it exists
            FirestoreManager.getInstance(this).ensureTemplatesCollectionExists()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Templates collection initialized successfully");
                    // Now sync template counts from user data
                    FirestoreManager.getInstance(this).initializeTemplateCountsFromUserData()
                        .addOnSuccessListener(syncVoid -> 
                            Log.d(TAG, "Template counts synced successfully"))
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "Failed to sync template counts", e));
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Failed to initialize templates collection", e));
            
            // Initialize Firebase services before auth
            initializeFirebaseServices();
            
            // Initialize FCM token first
            initializeFcmToken();
            
            // Initialize auth last since it depends on FCM token
            FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    boolean isGoogleUser = false;
                    for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                        if ("google.com".equals(profile.getProviderId())) {
                            isGoogleUser = true;
                            break;
                        }
                    }
                    
                    if (isGoogleUser) {
                        Log.d(TAG, "Auth state: User signed in with Google: " + user.getUid());
                        // Sync with MongoDB if needed
                        FirestoreManager.getInstance().updateUserProfileInMongoDB(user)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile synced with MongoDB on app start"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to sync user profile with MongoDB", e));
                        
                        // Log additional debug info about user auth state
                        user.getIdToken(true)
                            .addOnSuccessListener(tokenResult -> {
                                Log.d(TAG, "User ID token retrieved successfully, length: " + 
                                      (tokenResult.getToken() != null ? tokenResult.getToken().length() : 0));
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to get user ID token", e));
                    } else {
                        Log.d(TAG, "Auth state: User signed in anonymously: " + user.getUid());
                    }
                } else {
                    Log.d(TAG, "Auth state: User signed out");
                }
            });
            
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
                
                // Initialize RemoteConfigManager for app updates
                com.ds.eventwish.utils.RemoteConfigManager.getInstance(this);
                Log.d(TAG, "RemoteConfigManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase In-App Messaging or RemoteConfigManager: " + e.getMessage(), e);
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
            try {
                InternetConnectivityChecker.getInstance(this);
                Log.d(TAG, "Internet Connectivity Checker initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Internet Connectivity Checker: " + e.getMessage(), e);
            }
            
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
            
            // Initialize Event Notification Manager
            try {
                EventNotificationManager.getInstance(this).initialize();
                Log.d(TAG, "Event Notification Manager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Event Notification Manager: " + e.getMessage(), e);
            }
            
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

            // Initialize notification channels
            createNotificationChannels();
            Log.d(TAG, "Notification channels created");

            // Initialize ad session manager
            adSessionManager = AdSessionManager.getInstance(this);
            Log.d(TAG, "Ad session manager initialized");

            // Initialize Internet Connectivity Checker
            InternetConnectivityChecker.getInstance(this);
            Log.d(TAG, "Internet connectivity checker initialized");
            
            // Initialize Event Notification Manager
            try {
                EventNotificationManager.getInstance(this).initialize();
                Log.d(TAG, "Event Notification Manager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Event Notification Manager: " + e.getMessage(), e);
            }

            // Register activity lifecycle callbacks
            registerActivityLifecycleCallbacks(this);
            Log.d(TAG, "Activity lifecycle callbacks registered");

            // Register fragment lifecycle callbacks
            registerFragmentLifecycleCallbacks();
            Log.d(TAG, "Fragment lifecycle callbacks registered");
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
            
            // 13. Initialize Firebase In-App Messaging
            try {
                FirebaseInAppMessagingHandler.init(this);
                Log.d(TAG, "✅ Firebase In-App Messaging Handler initialized");
                
                // Initialize RemoteConfigManager for app updates
                com.ds.eventwish.utils.RemoteConfigManager.getInstance(this);
                Log.d(TAG, "RemoteConfigManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing Firebase In-App Messaging or RemoteConfigManager: " + e.getMessage(), e);
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
        Log.d(TAG, "Initializing Firebase services");
        
        try {
            // Initialize FirestoreManager early but on a background thread
            AppExecutors.getInstance().diskIO().execute(() -> {
                // Initialize FirestoreManager
                FirestoreManager firestoreManager = FirestoreManager.getInstance();
                
                // Ensure templates collection exists - this is potentially a slow operation
                Log.d(TAG, "Starting template collection initialization (background thread)");
                firestoreManager.ensureTemplatesCollectionExists()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Templates collection initialized successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to initialize templates collection", e);
                    });
            });
            
            // Initialize Firebase Crashlytics
            FirebaseCrashManager.init(this);
            Log.d(TAG, "Firebase Crashlytics initialized");
            
            // Initialize Firebase Analytics
            FirebaseAnalytics.getInstance(this);
            Log.d(TAG, "Firebase Analytics initialized");
            
            // Initialize Firebase Performance
            PerformanceTracker.init(this);
            Log.d(TAG, "Firebase Performance initialized");
            
            // Initialize Firebase Remote Config and Event Notifications
            EventNotificationManager.init(this);
            Log.d(TAG, "Firebase Remote Config and Event Notifications initialized");
            
            // Schedule a check for event notifications
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    EventNotificationManager.getInstance(this).checkAndShowNotification();
                    Log.d(TAG, "Event notifications checked");
                } catch (Exception e) {
                    Log.e(TAG, "Error checking event notifications", e);
                }
            }, 10000); // Check after 10 seconds to allow app to initialize fully
            
            // Initialize AuthManager early to ensure proper authentication state
            try {
                AuthManager.getInstance().initialize(this);
                Log.d(TAG, "AuthManager initialized");
                
                // Check for existing user and refresh token if needed
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Log.d(TAG, "Existing user found on app start: " + currentUser.getUid());
                    // Force token refresh in background to ensure validity
                    currentUser.getIdToken(true)
                        .addOnSuccessListener(result -> 
                            Log.d(TAG, "Token refreshed successfully on app start"))
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "Failed to refresh token on app start", e));
                } else {
                    Log.d(TAG, "No existing user found on app start");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing AuthManager", e);
            }
            
            // Initialize Firebase In-App Messaging
            try {
                FirebaseInAppMessagingHandler.init(this);
                Log.d(TAG, "Firebase In-App Messaging initialized");
                
                // Initialize RemoteConfigManager for app updates
                com.ds.eventwish.utils.RemoteConfigManager.getInstance(this);
                Log.d(TAG, "RemoteConfigManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase In-App Messaging or RemoteConfigManager: " + e.getMessage(), e);
            }
            
            // Check if we're running in debug mode
            boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
            
            // Initialize AdMob if not in debug mode or if explicitly enabled in debug
            try {
                // Initialize AdMob
                AdMobManager.init(this);
                Log.d(TAG, "AdMob initialized");
                
                // Initialize App Open Ads
                appOpenManager = new AppOpenManager(this);
                Log.d(TAG, "App Open Ads initialized");
                
                // Initialize the sponsored ad manager factory
                SponsoredAdManagerFactory.init(this);
                Log.d(TAG, "Sponsored Ad Manager initialized");
                
                // Initialize AdSessionManager
                adSessionManager = AdSessionManager.getInstance(this);
                Log.d(TAG, "AdSessionManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing AdMob: " + e.getMessage(), e);
            }
            
            // Initialize Internet Connectivity Checker
            try {
                InternetConnectivityChecker.getInstance(this);
                Log.d(TAG, "Internet Connectivity Checker initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Internet Connectivity Checker: " + e.getMessage(), e);
            }
            
            // Initialize ShareMessageManager
            try {
                com.ds.eventwish.utils.ShareMessageManager.getInstance(this);
                Log.d(TAG, "ShareMessageManager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing ShareMessageManager: " + e.getMessage(), e);
            }
            
            // Verify Firebase project configuration
            verifyFirebaseProject();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase services: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize FCM token
     */
    private void initializeFcmToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                
                // Save token to SharedPreferences for backward compatibility
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
                
                // Update token in Firestore
                FirestoreManager.getInstance().setFcmToken(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token in Firestore", e));
            });
    }
}