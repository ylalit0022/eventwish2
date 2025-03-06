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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
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
import com.ds.eventwish.utils.DeepLinkUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModelFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    private boolean isNavigating = false;
    private AppBarConfiguration appBarConfiguration;
    private ReminderViewModel viewModel;
    private ReminderDao reminderDao;
    private BadgeDrawable reminderBadge;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPermissionLauncher();
        checkAndRequestPermissions();

        reminderDao = new ReminderDao(this);
        ReminderViewModelFactory factory = new ReminderViewModelFactory(reminderDao, this);
        viewModel = new ViewModelProvider(this, factory).get(ReminderViewModel.class);

        setupNavigation();
        setupBadge();
        handleIntent(getIntent());
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
                    showPermissionDeniedDialog();
                }
            }
        );
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting notification permission");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Notification permission already granted");
                checkAlarmPermission();
            }
        } else {
            checkAlarmPermission();
        }
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted");
                showAlarmPermissionDialog();
            } else {
                Log.d(TAG, "Alarm permission granted");
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to alert you about your reminders. Please grant this permission in Settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAlarmPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Alarm Permission Required")
            .setMessage("This app needs permission to schedule exact alarms for your reminders. Please grant this permission in Settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupBadge() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        reminderBadge = bottomNav.getOrCreateBadge(R.id.navigation_reminder);
        reminderBadge.setBackgroundColor(getResources().getColor(R.color.badge_background, getTheme()));
        reminderBadge.setBadgeTextColor(getResources().getColor(R.color.badge_text, getTheme()));
        
        // Observe unread count
        reminderDao.getUnreadCount().observe(this, count -> {
            if (count > 0) {
                reminderBadge.setVisible(true);
                reminderBadge.setNumber(count);
            } else {
                reminderBadge.setVisible(false);
            }
        });
    }

//    private void setupNavigation() {
//
//        binding.bottomNavigation.setItemIconTintList(null); // Disable default tint
//
//        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.nav_host_fragment);
//
//        if (navHostFragment != null) {
//            navController = navHostFragment.getNavController();
//
//            binding.bottomNavigation.setOnItemSelectedListener(item -> {
//                if (isNavigating) return false;
//
//                int itemId = item.getItemId();
//                if (navController.getCurrentDestination() != null &&
//                    navController.getCurrentDestination().getId() == itemId) {
//                    return true;
//                }
//
//                try {
//                    isNavigating = true;
//                    // Clear badge when navigating to reminder fragment
//                    if (itemId == R.id.navigation_reminder) {
//                        viewModel.clearBadgeCount();
//                    }
//                    navController.navigate(itemId);
//                } catch (Exception e) {
//                    Log.e(TAG, "Navigation failed", e);
//                } finally {
//                    isNavigating = false;
//                }
//                return true;
//            });
//
//            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
//                int id = destination.getId();
//                binding.bottomNavigation.setVisibility(View.VISIBLE);
//                binding.bottomNavigation.setSelectedItemId(id);
//            });
//        } else {
//            Log.e(TAG, "NavHostFragment not found!");
//        }
//    }
private void setupNavigation() {
    Window window = getWindow();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(Color.parseColor("#DEDBE0")); // Set status bar color to white
    }

    NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    if (navHostFragment != null) {
        navController = navHostFragment.getNavController(); // Initialize NavController correctly
    } else {
        Log.e("MainActivity", "NavHostFragment not found");
        return; // Stop execution if NavController is not found
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
        
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String shortCode = DeepLinkUtil.extractShortCode(data);
            if (shortCode != null) {
                try {
                    Bundle args = new Bundle();
                    args.putString("shortCode", shortCode);
                    navController.navigate(R.id.action_global_resourceFragment, args);
                } catch (Exception e) {
                    Log.e(TAG, "Deep link navigation failed", e);
                }
            }
        }

        // Check if the intent is for showing reminders
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("SHOW_REMINDERS")) {
                // Navigate to the reminders fragment or activity
                // Example: navController.navigate(R.id.navigation_reminder);
            }
        }
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        setIntent(intent);
//        handleIntent(intent);
//    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && "reminder".equals(intent.getStringExtra("navigate_to"))) {
            long reminderId = intent.getLongExtra("reminderId", -1);
            if (reminderId != -1) {
                Bundle bundle = new Bundle();
                bundle.putLong("reminderId", reminderId);

                NavController navController = Navigation.findNavController(this, R.id.fragment_container);
                navController.navigate(R.id.navigation_reminder, bundle);
            }
        }
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
        checkAndRequestPermissions();
    }
}