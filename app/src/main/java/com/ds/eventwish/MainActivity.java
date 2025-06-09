package com.ds.eventwish;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.databinding.ActivityMainBinding;
import com.ds.eventwish.ui.reminder.ReminderFragment;
import com.ds.eventwish.utils.DeepLinkHandler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModelFactory;
import com.ds.eventwish.utils.PermissionUtils;
import com.ds.eventwish.utils.AppUpdateChecker;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.ui.festival.FestivalViewModel;
import com.ds.eventwish.utils.NotificationHelper;
import com.ds.eventwish.ui.settings.SettingsActivity;
import com.ds.eventwish.ui.viewmodel.SharedViewModel;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;
import com.ds.eventwish.R;
import com.ds.eventwish.ads.AdDemoActivity;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.AnalyticsConsentManager;
import com.ds.eventwish.ui.history.SharedPrefsManager;
import com.ds.eventwish.utils.FirebaseCrashManager;
import com.ds.eventwish.utils.PerformanceTracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.perf.metrics.Trace;
import com.ds.eventwish.firebase.FirebaseInAppMessagingHandler;
import com.ds.eventwish.services.NotificationScheduler;
import com.ds.eventwish.ui.viewmodel.AppUpdateViewModel;
import com.ds.eventwish.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_UPDATE = AppUpdateChecker.getRequestCode();

    private ActivityMainBinding binding;
    private NavController navController;
    private boolean isNavigating = false;
    private AppBarConfiguration appBarConfiguration;
    private ReminderViewModel viewModel;
    private ReminderDao reminderDao;
    private BadgeDrawable reminderBadge;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private BottomNavigationView bottomNavigationView;
    private FestivalViewModel festivalViewModel;
    private String currentFragmentTag = "";
    private boolean isFirstLaunch = true;
    private boolean isFirstAnalyticsTracking = true;
    private ApiClient apiClient;
    private AppUpdateChecker appUpdateChecker;
    private InternetConnectivityChecker connectivityChecker;
    private boolean isConnected = true;
    private SharedViewModel sharedViewModel;
    private SharedPrefsManager prefsManager;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Start app load trace
        Trace appStartupTrace = PerformanceTracker.startPerformanceTrace("app_startup");
        
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.setNavigationBarColor(Color.TRANSPARENT);
            }

            // Initialize shared preferences manager
            prefsManager = new SharedPrefsManager(this);

            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance();
            
            // Sign in anonymously if not already signed in
            if (auth.getCurrentUser() == null) {
                signInAnonymously();
            } else {
                // User already signed in, initialize components
                initializeComponents();
            }

            setupPermissionLauncher();
            checkAndRequestPermissions();

            reminderDao = new ReminderDao(this);
            ReminderViewModelFactory factory = new ReminderViewModelFactory(reminderDao, this);
            viewModel = new ViewModelProvider(this, factory).get(ReminderViewModel.class);

            setupNavigation();
            setupBadge();
            handleIntent(getIntent());

            // Initialize notification channels
            NotificationHelper.createNotificationChannels(this);

            // Request notification permission if needed
            NotificationHelper.requestNotificationPermission(this, NOTIFICATION_PERMISSION_REQUEST_CODE);

            // Initialize view model
            festivalViewModel = new ViewModelProvider(this).get(FestivalViewModel.class);

            // Clear all cache on first launch
            if (isFirstLaunch) {
                FestivalRepository.getInstance(this).clearAllCache();
                isFirstLaunch = false;
            }

            // Initialize API client
            apiClient = new ApiClient();
            
            // Setup connectivity checker first
            connectivityChecker = new InternetConnectivityChecker(this);
            connectivityChecker.observe(this, isNetworkConnected -> {
                if (isConnected && !isNetworkConnected) {
                    // Just disconnected
                    showConnectivityMessage(false);
                } else if (!isConnected && isNetworkConnected) {
                    // Just reconnected
                    showConnectivityMessage(true);
                    // Check for updates when we get internet connection
                    if (BuildConfig.DEBUG) {
                        // For debug builds, use AppUpdateViewModel with Remote Config
                        AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
                        appUpdateViewModel.init(this);
                        appUpdateViewModel.checkForUpdatesWithRemoteConfigSilently();
                    } else {
                        // For production builds, use AppUpdateChecker with Play Store
                        if (appUpdateChecker != null) {
                            appUpdateChecker.checkForUpdate();
                        }
                    }
                }
                isConnected = isNetworkConnected;
            });
            
            // Setup app update checking
            appUpdateChecker = new AppUpdateChecker(this);
            getLifecycle().addObserver(appUpdateChecker);
            appUpdateChecker.setUpdateCallback(new AppUpdateChecker.UpdateCallback() {
                @Override
                public void onUpdateAvailable(boolean isImmediateUpdate) {
                    Log.d(TAG, "Update available, immediate: " + isImmediateUpdate);
                    // The native Google Play dialog will be shown automatically
                }

                @Override
                public void onUpdateNotAvailable() {
                    Log.d(TAG, "No update available");
                }

                @Override
                public void onUpdateError(Exception error) {
                    Log.e(TAG, "Update error: " + error.getMessage());
                    if (error.getMessage() != null && error.getMessage().contains("internet")) {
                        showConnectivityMessage(false);
                    }
                }

                @Override
                public void onDownloadProgress(long bytesDownloaded, long totalBytesToDownload) {
                    if (totalBytesToDownload > 0) {
                        int progress = (int) ((bytesDownloaded * 100) / totalBytesToDownload);
                        Log.d(TAG, "Download progress: " + progress + "%");
                        // You could show this in a progress bar if desired
                    }
                }
            });

            // Check for updates if we have internet
            if (connectivityChecker.isNetworkAvailable()) {
                if (BuildConfig.DEBUG) {
                    // For debug builds, use AppUpdateViewModel with Remote Config
                    AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
                    appUpdateViewModel.init(this);
                    appUpdateViewModel.checkForUpdatesWithRemoteConfigSilently();
                } else {
                    // For production builds, use AppUpdateChecker with Play Store
                    appUpdateChecker.checkForUpdate();
                }
            }

            // Log app started
            Log.d(TAG, "MainActivity created");

            // Set up view model
            sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            
            // Handle intent (deep links)
            if (getIntent() != null) {
                DeepLinkHandler.handleDeepLink(this, getIntent());
            }

            // Initialize analytics based on user consent
            initializeAnalytics();
            
            // Set custom keys for crash reports
            FirebaseCrashManager.setCustomKey("main_activity_initialized", true);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            FirebaseCrashManager.logException(e);
        } finally {
            if (appStartupTrace != null) {
                appStartupTrace.stop();
            }
        }

        initializeUI(savedInstanceState);
    }

    /**
     * Initialize analytics and show consent dialog if needed
     */
    private void initializeAnalytics() {
        try {
            // Initialize analytics with current consent status
            AnalyticsConsentManager.initializeAnalytics(this);
            
            // Track app open event if analytics is enabled
            if (AnalyticsUtils.isAnalyticsEnabled()) {
                AnalyticsUtils.trackAppOpen();
            }
            
            // Show consent dialog if it hasn't been shown before
            // Use a slight delay to ensure UI is fully loaded
            if (AnalyticsConsentManager.shouldShowConsentDialog(this)) {
                new Handler().postDelayed(() -> {
                    AnalyticsConsentManager.showConsentDialog(this);
                }, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing analytics", e);
        }
    }

    private void initializeUI(Bundle savedInstanceState) {
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.setNavigationBarColor(Color.TRANSPARENT);
            }

            setupPermissionLauncher();
            checkAndRequestPermissions();

            reminderDao = new ReminderDao(this);
            ReminderViewModelFactory factory = new ReminderViewModelFactory(reminderDao, this);
            viewModel = new ViewModelProvider(this, factory).get(ReminderViewModel.class);

            setupNavigation();
            setupBadge();
            handleIntent(getIntent());

            // Initialize notification channels
            NotificationHelper.createNotificationChannels(this);

            // Request notification permission if needed
            NotificationHelper.requestNotificationPermission(this, NOTIFICATION_PERMISSION_REQUEST_CODE);

            // Initialize view model
            festivalViewModel = new ViewModelProvider(this).get(FestivalViewModel.class);

            // Clear all cache on first launch
            if (isFirstLaunch) {
                FestivalRepository.getInstance(this).clearAllCache();
                isFirstLaunch = false;
            }

            // Initialize API client
            apiClient = new ApiClient();
            
            // Note: Connectivity checker and app update checker are already initialized in onCreate
            
            // Log app started
            Log.d(TAG, "MainActivity UI initialized");

            // Set up view model
            sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            
            // Handle intent (deep links)
            if (getIntent() != null) {
                DeepLinkHandler.handleDeepLink(this, getIntent());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI", e);
            FirebaseCrashManager.logException(e);
        }
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                    checkAlarmPermission();
                } else {
                    Log.w(TAG, "Notification permission denied");
                    PermissionUtils.showNotificationPermissionDeniedDialog(this);
                }
            }
        );
    }

    private void checkAndRequestPermissions() {
        // Check notification permission
        if (!PermissionUtils.hasNotificationPermission(this)) {
            Log.d(TAG, "Requesting notification permission");
            PermissionUtils.requestNotificationPermission(this, requestPermissionLauncher);
        } else {
            Log.d(TAG, "Notification permission already granted");
            checkAlarmPermission();
        }
    }

    private void checkAlarmPermission() {
        // Check exact alarm permission
        if (!PermissionUtils.canScheduleExactAlarms(this)) {
            Log.w(TAG, "Exact alarm permission not granted");
            PermissionUtils.showExactAlarmPermissionDialog(this);
        } else {
            Log.d(TAG, "Alarm permission granted");
        }
    }

    private void setupBadge() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        reminderBadge = bottomNav.getOrCreateBadge(R.id.navigation_reminder);
        reminderBadge.setBackgroundColor(getResources().getColor(R.color.badge_background, getTheme()));
        reminderBadge.setBadgeTextColor(getResources().getColor(R.color.badge_text, getTheme()));

        // Observe today's reminders count
        if (viewModel != null) {
            viewModel.getTodayRemindersCount().observe(this, count -> {
                if (count > 0) {
                    reminderBadge.setVisible(true);
                    reminderBadge.setNumber(count);
                    Log.d(TAG, "Today's reminders count: " + count);
                } else {
                    reminderBadge.setVisible(false);
                }
            });

            // Initial load of reminders
            viewModel.loadReminders();
        } else {
            Log.e(TAG, "ReminderViewModel is null in setupBadge");
        }

        // Clear badge when navigating to reminder fragment
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_reminder) {
                // Reset badge when navigating to reminders tab
                if (reminderBadge != null) {
                    reminderBadge.setVisible(false);
                }
            }
        });
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            bottomNavigationView = binding.bottomNavigation;
            
            // Configure app bar
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home, R.id.navigation_reminder, R.id.navigation_more)
                    .build();
            
            // Handle bottom navigation item reselection properly
            bottomNavigationView.setOnItemReselectedListener(item -> {
                // When the same tab is selected again, pop the back stack to the root of that tab
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home || 
                    itemId == R.id.navigation_reminder || 
                    itemId == R.id.navigation_more) {
                    // Pop back stack to the start destination of the current tab
                    navController.popBackStack(itemId, false);
                }
            });
            
            // Track navigation destination changes for analytics
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // Get the destination ID's string representation for analytics
                try {
                    String destinationName = getResources().getResourceName(destination.getId());
                    if (destinationName != null) {
                        // Extract just the name part (after the slash)
                        int slashIndex = destinationName.lastIndexOf('/');
                        if (slashIndex >= 0 && slashIndex < destinationName.length() - 1) {
                            destinationName = destinationName.substring(slashIndex + 1);
                        }
                        
                        // Track screen view through NavController
                        AnalyticsUtils.trackScreenView(
                            MainActivity.this,
                            "NavDestination_" + destinationName, 
                            "navigation.destination"
                        );
                        
                        Log.d(TAG, "Navigation change: " + destinationName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error tracking navigation change", e);
                }
                
                if (destination.getId() == R.id.navigation_reminder) {
                    // Reset badge when navigating to reminders tab
                    if (reminderBadge != null) {
                        reminderBadge.setVisible(false);
                    }
                }
            });
            
            // Handle item selection with special handling for Home tab
            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (isNavigating) return false;
                
                isNavigating = true;
                
                int itemId = item.getItemId();
                
                // Special handling for home navigation to prevent issues from deep fragments
                if (itemId == R.id.navigation_home) {
                    // Check if we're already on a fragment in the home tab's navigation
                    if (isInHomeTab()) {
                        // If we're in the home tab but not on HomeFragment, pop back to HomeFragment
                        navController.popBackStack(R.id.navigation_home, false);
                    } else {
                        // Navigate to home
                        navController.navigate(R.id.navigation_home);
                    }
                    new Handler().postDelayed(() -> isNavigating = false, 300);
                    return true;
                }
                // Special handling for more navigation
                else if (itemId == R.id.navigation_more) {
                    Log.d(TAG, "Navigating to More");
                    // Always navigate directly to the more fragment
                    navController.navigate(R.id.navigation_more);
                    new Handler().postDelayed(() -> isNavigating = false, 300);
                    return true;
                }
                
                // For other tabs, use standard navigation
                boolean result = NavigationUI.onNavDestinationSelected(item, navController);
                new Handler().postDelayed(() -> isNavigating = false, 300);
                return result;
            });
            
            // Set up NavigationUI with bottom navigation
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        } else {
            Log.e(TAG, "NavHostFragment not found");
        }
    }
    
    /**
     * Check if current fragment is in the home tab's navigation graph
     */
    private boolean isInHomeTab() {
        if (navController == null) return false;
        
        // Get current destination
        int currentDestId = navController.getCurrentDestination().getId();
        
        // Check if we're in a fragment that belongs to the home tab's navigation
        return currentDestId == R.id.navigation_home || 
               currentDestId == R.id.navigation_template_detail ||
               currentDestId == R.id.navigation_template_customize ||
               currentDestId == R.id.resourceFragment;
    }

    private void handleIntent(Intent intent) {
        // Handle deep links
        if (intent != null) {
            String action = intent.getAction();
            Uri data = intent.getData();
            
            if (Intent.ACTION_VIEW.equals(action) && data != null) {
                // Process deep link
                DeepLinkHandler.handleDeepLink(this, data, navController);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            // Start the settings activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
    
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // Clean up resources
        if (connectivityChecker != null) {
            connectivityChecker.cleanup();
        }
        
        if (appUpdateChecker != null) {
            appUpdateChecker.onDestroy(this);
            appUpdateChecker = null;
        }
        
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update online status
        FirestoreManager.getInstance().updateOnlineStatus(true)
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status", e));
        
        try {
            // Only track MainActivity screen if this is first launch
            if (isFirstAnalyticsTracking) {
                AnalyticsUtils.trackScreenView(this, "MainActivity", MainActivity.class.getName());
                isFirstAnalyticsTracking = false;
            }
            
            // Check for updates if we have internet and an update is not in progress
            if (connectivityChecker != null && connectivityChecker.isNetworkAvailable()) {
                if (BuildConfig.DEBUG) {
                    // For debug builds, use AppUpdateViewModel with Remote Config
                    AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
                    appUpdateViewModel.init(this);
                    appUpdateViewModel.checkForUpdatesWithRemoteConfigSilently();
                } else {
                    // For production builds, use AppUpdateChecker with Play Store
                    if (appUpdateChecker != null && !appUpdateChecker.isUpdateInProgress()) {
                        appUpdateChecker.checkForUpdate();
                    }
                }
            }
            
            // Record user session
            AnalyticsUtils.setUserProperty("last_activity", "MainActivity");
            AnalyticsUtils.setUserProperty("app_foreground", "true");
            
            // Check badge count
            showReminderBadgeIfNeeded();
            
            // Log activity resumed
            Log.d(TAG, "MainActivity resumed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
            FirebaseCrashManager.logException(e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Handle menu item selection
     */
    public void onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Update online status
        FirestoreManager.getInstance().updateOnlineStatus(false)
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status", e));
        
        try {
            // Update analytics when app goes to background
            AnalyticsUtils.setUserProperty("app_foreground", "false");
            AnalyticsUtils.setUserProperty("last_pause_time", String.valueOf(System.currentTimeMillis()));
            
            Log.d(TAG, "MainActivity paused");
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    /**
     * Show connectivity change message
     */
    private void showConnectivityMessage(boolean isConnected) {
        String message = isConnected ? 
                "Network connected" :
                "Network disconnected";
        
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);
        
        if (isConnected) {
            snackbar.setBackgroundTint(getResources().getColor(R.color.colorAccent, getTheme()));
        } else {
            snackbar.setBackgroundTint(getResources().getColor(R.color.colorPrimary, getTheme()));
        }
        
        snackbar.show();
    }

    private void showReminderBadgeIfNeeded() {
        // Check if there are active reminders
        boolean hasReminders = false; // This would come from a repository
        
        // Show badge if needed
        if (hasReminders) {
            BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.navigation_reminder);
            badge.setVisible(true);
            badge.setNumber(1);
        } else {
            binding.bottomNavigation.removeBadge(R.id.navigation_reminder);
        }
    }

    private void showOfflineMessage() {
        if (binding.offlineMessage.getVisibility() != View.VISIBLE) {
            binding.offlineMessage.setVisibility(View.VISIBLE);
        }
    }

    private void hideOfflineMessage() {
        if (binding.offlineMessage.getVisibility() == View.VISIBLE) {
            binding.offlineMessage.setVisibility(View.GONE);
        }
    }

    /**
     * Handle search functionality
     */
    private void handleSearch(String query) {
        if (query != null && !query.isEmpty()) {
            try {
                // Trace search operation
                PerformanceTracker.startTrace("search_operation");
                
                // Existing search code...
                
                // Track search analytics
                AnalyticsUtils.trackSearch(query, /* result count */ 0);
                
                // Stop trace
                PerformanceTracker.stopTrace("search_operation");
                
            } catch (Exception e) {
                Log.e(TAG, "Error during search", e);
                FirebaseCrashManager.logException(e);
                
                // Track error
                AnalyticsUtils.trackContentLoadError("Search error: " + e.getMessage(), null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.d(TAG, "Update flow failed! Result code: " + resultCode);
                if (appUpdateChecker != null) {
                    appUpdateChecker.onActivityResult(requestCode, resultCode);
                }
            }
        }
    }

    /**
     * Check for app updates using the appropriate method based on build type
     */
    private void checkForUpdates() {
        try {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
            
            AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
            appUpdateViewModel.init(this);
            
            // Listen for errors to show to the user
            appUpdateViewModel.getErrorMessage().observe(this, errorMsg -> {
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
            
            // Listen for update availability
            appUpdateViewModel.getIsUpdateAvailable().observe(this, isAvailable -> {
                if (isAvailable != null && !isAvailable) {
                    Toast.makeText(this, "No update available", Toast.LENGTH_SHORT).show();
                }
            });
            
            if (BuildConfig.DEBUG) {
                // For debug builds, use Remote Config
                Log.d(TAG, "Checking for updates with Remote Config");
                appUpdateViewModel.checkForUpdatesWithRemoteConfig();
            } else {
                // For production builds, use Play Store
                Log.d(TAG, "Checking for updates with Play Store");
                appUpdateViewModel.checkForUpdates(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
            Toast.makeText(this, "Error checking for updates: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Force check for app updates using the appropriate method based on build type
     */
    private void forceCheckForUpdates() {
        try {
            Toast.makeText(this, "Force checking for updates...", Toast.LENGTH_SHORT).show();
            
            AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
            appUpdateViewModel.init(this);
            
            // Listen for errors to show to the user
            appUpdateViewModel.getErrorMessage().observe(this, errorMsg -> {
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
            
            // Listen for update availability
            appUpdateViewModel.getIsUpdateAvailable().observe(this, isAvailable -> {
                if (isAvailable != null && !isAvailable) {
                    Toast.makeText(this, "No update available", Toast.LENGTH_SHORT).show();
                }
            });
            
            if (BuildConfig.DEBUG) {
                // For debug builds, use Remote Config
                Log.d(TAG, "Force checking for updates with Remote Config");
                appUpdateViewModel.forceCheckForUpdatesWithRemoteConfig();
            } else {
                // For production builds, use Play Store
                Log.d(TAG, "Force checking for updates with Play Store");
                appUpdateViewModel.checkForUpdates(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error force checking for updates", e);
            Toast.makeText(this, "Error checking for updates: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testFirestoreWrite() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            String templateId = "test_template_" + System.currentTimeMillis();
            
            // Add a test like
            FirestoreManager.getInstance()
                .addToLikes(templateId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully added test like to Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add test like to Firestore", e);
                });
        }
    }

    private void signInAnonymously() {
        auth.signInAnonymously()
            .addOnSuccessListener(this, authResult -> {
                Log.d(TAG, "Anonymous sign-in success");
                // FirestoreManager now handles user ID internally through auth state listener
                initializeComponents();
            })
            .addOnFailureListener(this, e -> {
                Log.e(TAG, "Anonymous sign-in failed", e);
                showError("Authentication failed: " + e.getMessage());
            });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        } else {
            // FirestoreManager now handles user ID internally
            initializeComponents();
        }
    }

    private void initializeComponents() {
        // Existing initialization code...
    }

    private void showError(String message) {
        // Implement the logic to show an error message to the user
    }
}