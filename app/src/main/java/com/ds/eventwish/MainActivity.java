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

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";
    private boolean isNavigating = false; // Flag to prevent navigation loops

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setupNavigation();

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Setup Bottom Navigation
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                if (isNavigating) return false;
                
                int itemId = item.getItemId();
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getId() == itemId) {
                    return true; // Already at this destination
                }
                
                try {
                    isNavigating = true;
                    
                    if (itemId == R.id.navigation_home) {
                        navController.popBackStack(R.id.navigation_home, false);
                        return true;
                    } else if (itemId == R.id.navigation_history) {
                        navController.navigate(R.id.navigation_history);
                        return true;
                    } else if (itemId == R.id.navigation_more) {
                        navController.navigate(R.id.navigation_more);
                        return true;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Navigation failed", e);
                } finally {
                    isNavigating = false;
                }
                return false;
            });

            // Update bottom nav selection based on destination changes
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.navigation_home || 
                    id == R.id.navigation_history || 
                    id == R.id.navigation_more) {
                    binding.bottomNavigation.setVisibility(View.VISIBLE);
                    if (!isNavigating) {
                        binding.bottomNavigation.setSelectedItemId(id);
                    }
                } else {
                    binding.bottomNavigation.setVisibility(View.GONE);
                }
            });

            // Setup default NavController behavior
            NavigationUI.setupActionBarWithNavController(this, navController);
        } else {
            Log.e(TAG, "NavHostFragment not found!");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}