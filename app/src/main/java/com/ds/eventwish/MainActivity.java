package com.ds.eventwish;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.ds.eventwish.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private Set<Integer> topLevelDestinations;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the toolbar
        setSupportActionBar(binding.toolbar);

                // Setup bottom navigation
                binding.bottomNavigation.setOnItemSelectedListener(item -> {
                    // Navigation handling
                    return true;
                });

        setupNavigation();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void setupNavigation() {
        // Initialize NavController using NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Set up top-level destinations
            topLevelDestinations = new HashSet<>();
            topLevelDestinations.add(R.id.navigation_home);
            topLevelDestinations.add(R.id.navigation_history);
            topLevelDestinations.add(R.id.navigation_reminder);
            topLevelDestinations.add(R.id.navigation_more);

            // Set up AppBarConfiguration
            appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                    .build();

            // Set up ActionBar with NavController
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // Setup bottom navigation
            binding.bottomNavigation.setOnItemSelectedListener(this);
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

            // Handle destination changes
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // Show bottom nav only for top-level destinations
                boolean isTopLevel = topLevelDestinations.contains(destination.getId());
                binding.bottomNavigation.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
            });
        } else {
            Log.e(TAG, "NavHostFragment not found!");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (topLevelDestinations.contains(itemId)) {
            // If we're already at this destination, don't navigate
            if (navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() == itemId) {
                return true;
            }
            // Navigate to the selected destination
            return NavigationUI.onNavDestinationSelected(item, navController);
        }
        return false;
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        Uri data = intent.getData();
        String scheme = data.getScheme();
        String host = data.getHost();

        // Handle deep links
        if (("https".equals(scheme) && "eventwishes.onrender.com".equals(host)) ||
            ("eventwish".equals(scheme) && "wish".equals(host))) {
            
            String path = data.getPath();
            if (path != null && path.startsWith("/wish/")) {
                // Let the Navigation component handle the deep link
                navController.handleDeepLink(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}