package com.ds.eventwish;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
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
import com.ds.eventwish.ui.viewmodel.AppUpdateManager;
import com.ds.eventwish.R;
import com.ds.eventwish.ads.AdDemoActivity;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.AnalyticsConsentManager;
import com.ds.eventwish.ui.history.SharedPrefsManager;
import com.ds.eventwish.utils.FirebaseCrashManager;
import com.ds.eventwish.utils.PerformanceTracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.perf.metrics.Trace;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

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
    private AppUpdateManager appUpdateManager;
    private AppUpdateChecker appUpdateChecker;
    private InternetConnectivityChecker connectivityChecker;
    private boolean isConnected = true;
    private SharedViewModel sharedViewModel;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
            
            // Setup app update checking
            appUpdateManager = AppUpdateManager.getInstance(this);
            // Initialize the new AppUpdateChecker
            appUpdateChecker = new AppUpdateChecker(this);
            // Check for updates - including forced updates
            appUpdateChecker.checkForUpdate();
            
            // Setup connectivity checker
            connectivityChecker = new InternetConnectivityChecker(this);
            connectivityChecker.observe(this, isNetworkConnected -> {
                if (isConnected && !isNetworkConnected) {
                    // Just disconnected
                    showConnectivityMessage(false);
                } else if (!isConnected && isNetworkConnected) {
                    // Just reconnected
                    showConnectivityMessage(true);
                }
                isConnected = isNetworkConnected;
            });

            // Log app started
            Log.d(TAG, "MainActivity created");

            // Set up view model
            sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            
            // Initialize services
            connectivityChecker = new InternetConnectivityChecker(this);
            appUpdateManager = AppUpdateManager.getInstance(this);
            
            // Setup UI components
            setupNavigation();
            
            // Check for updates
            appUpdateManager.checkForUpdate();
            
            // Monitor network connectivity
            connectivityChecker.getConnectionStatus().observe(this, isConnected -> {
                if (isConnected) {
                    hideOfflineMessage();
                } else {
                    showOfflineMessage();
                }
            });
            
            // Handle intent (deep links)
            if (getIntent() != null) {
                DeepLinkHandler.handleDeepLink(this, getIntent());
            }

            // Initialize analytics based on user consent
            initializeAnalytics();
            
            // Set custom keys for crash reports
            FirebaseCrashManager.setCustomKey("main_activity_initialized", true);
        } catch (Exception e) {
            Log.e(TAG, "Error during onCreate", e);
            FirebaseCrashManager.logException(e);
        } finally {
            // Stop app load trace
            PerformanceTracker.stopPerformanceTrace(appStartupTrace, "app_startup");
        }
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
            
            // Setup app update checking
            appUpdateManager = AppUpdateManager.getInstance(this);
            // Initialize the new AppUpdateChecker
            appUpdateChecker = new AppUpdateChecker(this);
            // Check for updates - including forced updates
            appUpdateChecker.checkForUpdate();
            
            // Setup connectivity checker
            connectivityChecker = new InternetConnectivityChecker(this);
            connectivityChecker.observe(this, isNetworkConnected -> {
                if (isConnected && !isNetworkConnected) {
                    // Just disconnected
                    showConnectivityMessage(false);
                } else if (!isConnected && isNetworkConnected) {
                    // Just reconnected
                    showConnectivityMessage(true);
                }
                isConnected = isNetworkConnected;
            });

            // Log app started
            Log.d(TAG, "MainActivity created");

            // Set up view model
            sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            
            // Initialize services
            connectivityChecker = new InternetConnectivityChecker(this);
            appUpdateManager = AppUpdateManager.getInstance(this);
            
            // Setup UI components
            setupNavigation();
            
            // Check for updates
            appUpdateManager.checkForUpdate();
            
            // Monitor network connectivity
            connectivityChecker.getConnectionStatus().observe(this, isConnected -> {
                if (isConnected) {
                    hideOfflineMessage();
                } else {
                    showOfflineMessage();
                }
            });
            
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
                    R.id.navigation_home, R.id.navigation_reminder, R.id.navigation_profile)
                    .build();
            
            // Handle bottom navigation item reselection properly
            bottomNavigationView.setOnItemReselectedListener(item -> {
                // When the same tab is selected again, pop the back stack to the root of that tab
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home || 
                    itemId == R.id.navigation_reminder || 
                    itemId == R.id.navigation_profile) {
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
                // Special handling for profile navigation
                else if (itemId == R.id.navigation_profile) {
                    Log.d(TAG, "Navigating to Profile");
                    // Always navigate directly to the profile fragment
                    navController.navigate(R.id.navigation_profile);
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
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_test_ads) {
            startActivity(new Intent(this, AdDemoActivity.class));
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
        
        // Clean up AppUpdateChecker (it implements DefaultLifecycleObserver, but we'll call it manually)
        if (appUpdateChecker != null) {
            appUpdateChecker.onDestroy(this);
        }
        
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        try {
            // Only track MainActivity screen if this is first launch
            // (fragments will track their own navigation)
            if (isFirstAnalyticsTracking) {
                AnalyticsUtils.trackScreenView(this, "MainActivity", MainActivity.class.getName());
                isFirstAnalyticsTracking = false;
            }
            
            // Record user session
            AnalyticsUtils.setUserProperty("last_activity", "MainActivity");
            AnalyticsUtils.setUserProperty("app_foreground", "true");
            
            // Check badge count
            showReminderBadgeIfNeeded();
            
            // Check for app updates
            if (appUpdateManager != null) {
                appUpdateManager.checkForUpdate();
            }
            
            // Resume update checking with new AppUpdateChecker
            if (appUpdateChecker != null) {
                appUpdateChecker.resumeUpdates();
            }
            
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
}