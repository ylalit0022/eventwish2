package com.ds.eventwish;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.ds.eventwish.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private ActivityMainBinding binding;
    private NavController navController;
    private Set<Integer> topLevelDestinations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
    }

    private void setupNavigation() {
        // Initialize NavController using NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Set up top-level destinations
        topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.navigation_home);
        topLevelDestinations.add(R.id.navigation_history);
        topLevelDestinations.add(R.id.navigation_reminder);
        topLevelDestinations.add(R.id.navigation_more);

        // Setup bottom navigation with NavController
        binding.bottomNavigation.setOnItemSelectedListener(this);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // Handle destination changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Update bottom navigation visibility
            boolean isTopLevel = topLevelDestinations.contains(destination.getId());
            binding.bottomNavigation.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (topLevelDestinations.contains(itemId)) {
            // If we're already at this destination, don't navigate
            if (navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() == itemId) {
                Log.d("NAV",String.valueOf(itemId));
                return true;
            }
            // Navigate to the selected destination
            navController.navigate(itemId);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        topLevelDestinations.clear();
        topLevelDestinations = null;
    }
}