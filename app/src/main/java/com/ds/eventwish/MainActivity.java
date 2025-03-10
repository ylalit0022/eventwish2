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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
           // window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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
        // ✅ FIX: Ensure navController is not null before using it
        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.navigation_reminder) {
                    if (reminderBadge != null) {
                        reminderBadge.setVisible(false);
                    }
                } else if (reminderBadge != null && viewModel != null) {
                    Integer count = viewModel.getTodayRemindersCount().getValue();
                    if (count != null && count > 0) {
                        reminderBadge.setVisible(true);
                    }
                }
            });
        } else {
            Log.e(TAG, "navController is null in setupBadge");
        }    }

    private void setupNavigation() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.parseColor("#DEDBE0")); // Set status bar color to white
        }

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d("MainActivity", "NavController initialized successfully");

                binding.bottomNavigation.setOnItemSelectedListener(item -> {
                    return NavigationUI.onNavDestinationSelected(item, navController);
                });

                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    Log.d("MainActivity", "Navigated to: " + destination.getId());
                });

            } else {
                Log.e("MainActivity", "NavHostFragment is NULL!");
            }

        //binding.bottomNavigation.setItemIconTintList(null); // Disable Default Tint

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            resetNavItems(); // Reset All Items

            View navItem = binding.bottomNavigation.findViewById(item.getItemId());

            if (navItem != null) {
                navItem.setBackgroundResource(R.drawable.bottom_nav_background); // Active Background
                navItem.startAnimation(AnimationUtils.loadAnimation(this, R.anim.nav_item_zoom)); // Ripple Animation

                ImageView icon = (ImageView) navItem.findViewById(androidx.appcompat.R.id.icon);
                if (icon != null) {
                    icon.setBackgroundResource(R.drawable.icon_circle); // Shape Background to Icon
                    icon.setColorFilter(getResources().getColor(R.color.black)); // Active Icon Color
                }
            }

            if (navController != null) {
                return NavigationUI.onNavDestinationSelected(item, navController); // Navigate to Destination
            } else {
                Log.e("MainActivity", "NavController is null");
                return false;
            }
        });
    }

    private void resetNavItems() {
        for (int i = 0; i < binding.bottomNavigation.getMenu().size(); i++) {
            View navItem = binding.bottomNavigation.findViewById(binding.bottomNavigation.getMenu().getItem(i).getItemId());

            if (navItem != null) {
                navItem.setBackgroundResource(R.drawable.nav_inactive_background);
                ImageView icon = (ImageView) navItem.findViewById(androidx.appcompat.R.id.icon);
                if (icon != null) {
                    icon.setColorFilter(getResources().getColor(R.color.gray));
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
            || super.onSupportNavigateUp();
    }

    private void handleIntent(Intent intent) {
        if (intent == null || navController == null) return;

        String action = intent.getAction();
        Uri data = intent.getData();

        // Use the DeepLinkHandler to handle deep links
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            // Let the DeepLinkHandler handle the deep link
            if (DeepLinkHandler.handleDeepLink(this, intent)) {
                // Deep link was handled by DeepLinkHandler
                Log.d(TAG, "Deep link handled by DeepLinkHandler");
                return;
            }
        }

        // Check if the intent is for showing reminders
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("SHOW_REMINDERS")) {
                // Navigate to the reminders fragment
                navController.navigate(R.id.navigation_reminder);
            }
        }

        // Handle extras from DeepLinkHandler
        if (intent != null) {
            // Handle SHORT_CODE extra
            if (intent.hasExtra("SHORT_CODE")) {
                String shortCode = intent.getStringExtra("SHORT_CODE");
                if (shortCode != null && !shortCode.isEmpty()) {
                    Log.d(TAG, "Handling SHORT_CODE from intent: " + shortCode);
                    Bundle args = new Bundle();
                    args.putString("shortCode", shortCode);
                    navController.navigate(R.id.action_global_resourceFragment, args);
                }
            }

            // Handle FESTIVAL_ID extra
            if (intent.hasExtra("FESTIVAL_ID")) {
                String festivalId = intent.getStringExtra("FESTIVAL_ID");
                if (festivalId != null && !festivalId.isEmpty()) {
                    Log.d(TAG, "Handling FESTIVAL_ID from intent: " + festivalId);
                    Bundle args = new Bundle();
                    args.putString("festivalId", festivalId);
                    navController.navigate(R.id.navigation_festival_notification, args);
                }
            }

            // Handle TEMPLATE_ID extra
            if (intent.hasExtra("TEMPLATE_ID")) {
                String templateId = intent.getStringExtra("TEMPLATE_ID");
                if (templateId != null && !templateId.isEmpty()) {
                    Log.d(TAG, "Handling TEMPLATE_ID from intent: " + templateId);
                    Bundle args = new Bundle();
                    args.putString("templateId", templateId);
                    navController.navigate(R.id.navigation_template_detail, args);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Set the new intent to be processed
        setIntent(intent);

        if (intent != null) {
            String navigateTo = intent.getStringExtra("navigate_to");

            if ("reminder".equals(navigateTo)) {
                // Navigate to reminder fragment
                long reminderId = intent.getLongExtra("reminderId", -1);
                if (reminderId != -1) {
                    Log.d(TAG, "Navigating to reminder fragment with ID: " + reminderId);
                    Bundle bundle = new Bundle();
                    bundle.putLong("reminderId", reminderId);

                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_reminder, bundle);
                    return;
                }
            } else if ("festival".equals(navigateTo)) {
                // Navigate to festival notification fragment
                String festivalId = intent.getStringExtra("FESTIVAL_ID");
                if (festivalId != null && !festivalId.isEmpty()) {
                    Log.d(TAG, "Navigating to festival notification fragment with ID: " + festivalId);
                    Bundle bundle = new Bundle();
                    bundle.putString("festivalId", festivalId);

                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_festival_notification, bundle);
                    return;
                }
            }
        }

        // If no specific navigation was handled, process the intent normally
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.setAppInForeground(true); // App is in foreground
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.setAppInForeground(false); // App is in background
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check for app updates
        AppUpdateChecker.checkForUpdate(this);

        // Set app in foreground state
        if (viewModel != null) {
            viewModel.setAppInForeground(true);
        }

        // Check if we need to refresh data
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                // Permission granted, you can now show notifications
            } else {
                Log.d(TAG, "Notification permission denied");
                // Permission denied, show a message to the user
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Notification permission is required to receive festival updates",
                        Snackbar.LENGTH_LONG
                ).setAction("Settings", v -> {
                    // Open app settings
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't clear cache when app is paused/backgrounded
    }
}