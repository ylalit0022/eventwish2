package com.ds.eventwish.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.R;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.ads.NativeAdHelper;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.badge.BadgeDrawable;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";

    private NativeAdHelper nativeAdHelper;
    private FrameLayout nativeAdContainer;
    private ProgressBar nativeAdLoading;
    private NavController navController;
    private BottomNavigationView bottomNav;
    private ReminderViewModel reminderViewModel;
    private BadgeDrawable reminderBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize API client early to prevent initialization issues
        try {
            com.ds.eventwish.data.remote.ApiClient.init(getApplicationContext());
            Log.d(TAG, "ApiClient initialized in MainActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ApiClient in MainActivity", e);
        }

        // Initialize views
        bottomNav = findViewById(R.id.bottomNavigation);

        // Set up navigation - FIXED ORDER
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
            bottomNav.setOnItemSelectedListener(this);
        }

        // Initialize ViewModel using the Factory
        reminderViewModel = new ViewModelProvider(this, new ReminderViewModel.Factory(this))
                .get(ReminderViewModel.class);

        // Set up reminder badge
        setupReminderBadge();

        // Initialize native ad
        initializeNativeAd();
    }

    private void initializeNativeAd() {
        nativeAdContainer = findViewById(R.id.native_ad_container);
        nativeAdLoading = findViewById(R.id.native_ad_loading);

        // Initialize AdMobManager if not already initialized
        try {
            AdMobManager.getInstance();
        } catch (IllegalStateException e) {
            AdMobManager.init(getApplicationContext());
        }

        // Create native ad helper
        nativeAdHelper = new NativeAdHelper(this, nativeAdContainer, nativeAdLoading);

        // Load native ad
        loadNativeAd();
    }

    private void loadNativeAd() {
        Log.d(TAG, "Loading native ad");
        nativeAdContainer.setVisibility(View.VISIBLE);
        nativeAdHelper.loadAd();
    }

    private void setupReminderBadge() {
        // Get or create the badge for reminder menu item
        reminderBadge = bottomNav.getOrCreateBadge(R.id.navigation_reminder);
        reminderBadge.setBackgroundColor(getResources().getColor(R.color.badge_background, null));
        reminderBadge.setBadgeTextColor(getResources().getColor(R.color.white, null));

        // Observe today's reminders count
        reminderViewModel.getTodayRemindersCount().observe(this, count -> {
            if (count > 0) {
                reminderBadge.setVisible(true);
                reminderBadge.setNumber(count);
            } else {
                reminderBadge.setVisible(false);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, navController);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reminderViewModel.onResume();
        reminderViewModel.setAppInForeground(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        reminderViewModel.onPause();
        reminderViewModel.setAppInForeground(false);
    }

    @Override
    protected void onDestroy() {
        if (nativeAdHelper != null) {
            nativeAdHelper.cleanup();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            // Handle settings action
            return true;
        } else if (id == R.id.action_ad_debugger) {
            // Launch ad debugger activity
            Intent intent = new Intent(this, com.ds.eventwish.ui.ads.AdTestActivity.class);
            startActivity(intent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
} 