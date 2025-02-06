package com.ds.eventwish;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.ds.eventwish.databinding.ActivityMainBinding;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.HashSet;
import java.util.List;
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

        setSupportActionBar(binding.toolbar);
        setupNavigation();

        // Handle deep link intent
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
        if (intent == null) {
            Log.d(TAG, "handleIntent: Intent is null");
            return;
        }

        String action = intent.getAction();
        Uri data = intent.getData();
        
        Log.d(TAG, "handleIntent: Action=" + action + ", Data=" + (data != null ? data.toString() : "null"));

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String shortCode = DeepLinkUtil.extractShortCode(data);
            Log.d(TAG, "handleIntent: Extracted shortCode=" + shortCode);
            
            if (shortCode != null) {
                try {
                    Log.d(TAG, "handleIntent: Attempting to navigate to ResourceFragment");
                    Bundle args = new Bundle();
                    args.putString("shortCode", shortCode);
                    // Use the global action instead of direct navigation
                    navController.navigate(R.id.action_global_resourceFragment, args);
                    Log.d(TAG, "handleIntent: Navigation successful");
                } catch (Exception e) {
                    Log.e(TAG, "handleIntent: Navigation failed", e);
                }
            } else {
                Log.e(TAG, "handleIntent: Failed to extract shortCode from URI: " + data);
            }
        } else {
            Log.d(TAG, "handleIntent: Not a VIEW action or no data present");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}