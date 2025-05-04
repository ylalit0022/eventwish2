package com.ds.eventwish.ui.ads;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.R;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;
import com.ds.eventwish.utils.AdImpressionDebugger;
import com.ds.eventwish.utils.AdSessionManager;

/**
 * Test activity for debugging ad impressions
 * This activity provides controls to test and debug ad impression tracking
 */
public class AdTestActivity extends AppCompatActivity {
    private static final String TAG = "AdTestActivity";
    
    private SponsoredAdViewModel viewModel;
    private SponsoredAdView adView;
    private TextView statusText;
    private TextView networkStatus;
    private TextView impressionCount;
    private TextView databaseStatus;
    private AdImpressionDebugger debugger;
    private InternetConnectivityChecker connectivityChecker;
    private AdSessionManager sessionManager;
    private Handler refreshHandler;
    private final Runnable statusUpdateRunnable = this::updateStatusInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_test);
        
        // Set up refresh handler
        refreshHandler = new Handler(Looper.getMainLooper());
        
        // Get UI references
        initializeUIComponents();
        
        // Set up ViewModel
        viewModel = new ViewModelProvider(this).get(SponsoredAdViewModel.class);
        
        // Set up ad view
        adView.setViewModel(viewModel);
        adView.setLocation("test_activity");
        
        // Set up services and managers
        setupServices();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Observe ad loading
        viewModel.getAdForLocation("test_activity").observe(this, this::onAdLoaded);
        
        // Observe network status
        connectivityChecker.observe(this, isConnected -> {
            updateNetworkStatus(isConnected);
            if (isConnected) {
                updateStatusText("Network connected. Ready to fetch ads.");
            } else {
                updateStatusText("No network connection. Some features may not work.");
            }
        });
        
        // Load ad initially
        fetchAd();
        
        // Start periodic status updates
        startStatusUpdates();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh status when activity comes to foreground
        updateStatusInfo();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop updates when activity is in background
        stopStatusUpdates();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure updates are stopped
        stopStatusUpdates();
    }
    
    private void initializeUIComponents() {
        adView = findViewById(R.id.sponsored_ad_view);
        statusText = findViewById(R.id.status_text);
        networkStatus = findViewById(R.id.network_status);
        impressionCount = findViewById(R.id.impression_count);
        databaseStatus = findViewById(R.id.database_status);
    }
    
    private void setupServices() {
        try {
            // Set up debugger
            debugger = AdImpressionDebugger.getInstance();
            
            // Initialize connectivity checker
            connectivityChecker = InternetConnectivityChecker.getInstance(this);
            
            // Initialize session manager
            sessionManager = EventWishApplication.getInstance().getAdSessionManager();
            if (sessionManager != null) {
                sessionManager.setDebugMode(true);
                Log.d(TAG, "SessionManager initialized with debug mode");
            } else {
                Log.e(TAG, "Failed to get AdSessionManager instance");
                updateStatusText("Error: SessionManager not initialized correctly");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during setup", e);
            updateStatusText("Error during setup: " + e.getMessage());
        }
    }
    
    private void setupButtonListeners() {
        Button btnRunDiagnostics = findViewById(R.id.btn_run_diagnostics);
        Button btnClearData = findViewById(R.id.btn_clear_data);
        Button btnForceSync = findViewById(R.id.btn_force_sync);
        Button btnRefreshAd = findViewById(R.id.btn_refresh_ad);
        Button btnForceServerRefresh = findViewById(R.id.force_server_refresh_button);
        
        btnRunDiagnostics.setOnClickListener(v -> runDiagnostics());
        btnClearData.setOnClickListener(v -> clearData());
        btnForceSync.setOnClickListener(v -> forceSync());
        btnRefreshAd.setOnClickListener(v -> refreshAd());
        btnForceServerRefresh.setOnClickListener(v -> forceServerRefresh());
    }
    
    private void startStatusUpdates() {
        // Schedule periodic updates (every 3 seconds)
        refreshHandler.postDelayed(statusUpdateRunnable, 3000);
    }
    
    private void stopStatusUpdates() {
        refreshHandler.removeCallbacks(statusUpdateRunnable);
    }
    
    private void updateStatusInfo() {
        // Update network status
        boolean isConnected = connectivityChecker.isNetworkAvailable();
        updateNetworkStatus(isConnected);
        
        // Update impression count
        if (sessionManager != null) {
            updateImpressionCount(sessionManager.getTrackedImpressionCount());
        }
        
        // Update database status
        updateDatabaseStatus();
        
        // Schedule next update
        refreshHandler.postDelayed(statusUpdateRunnable, 3000);
    }
    
    private void updateNetworkStatus(boolean isConnected) {
        String statusText = "Network: " + (isConnected ? "CONNECTED" : "DISCONNECTED");
        networkStatus.setText(statusText);
        networkStatus.setTextColor(getResources().getColor(
            isConnected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
    }
    
    private void updateImpressionCount(int count) {
        impressionCount.setText("Session Impressions: " + count);
    }
    
    private void updateDatabaseStatus() {
        new Thread(() -> {
            try {
                int adCount = AppDatabase.getInstance(this)
                    .sponsoredAdDao()
                    .count();
                
                runOnUiThread(() -> {
                    databaseStatus.setText("Database: " + adCount + " ads in cache");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking database status", e);
                runOnUiThread(() -> {
                    databaseStatus.setText("Database: Error checking status");
                    databaseStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                });
            }
        }).start();
    }
    
    private void fetchAd() {
        updateStatusText("Fetching ad...");
        viewModel.fetchSponsoredAds();
    }
    
    private void onAdLoaded(SponsoredAd ad) {
        if (ad == null) {
            updateStatusText("No ad loaded");
            return;
        }
        
        updateStatusText("Ad loaded: " + ad.getTitle() + "\nID: " + ad.getId() +
                        "\nLocation: " + ad.getLocation() +
                        "\nPriority: " + ad.getPriority() +
                        "\nImpressions: " + ad.getImpressionCount());
    }
    
    private void runDiagnostics() {
        updateStatusText("Running diagnostics...");
        
        // Run in background to avoid UI freeze
        new Thread(() -> {
            debugger.runDiagnostics();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                updateStatusText("Diagnostics complete - check logcat");
                Toast.makeText(this, "Diagnostics complete", Toast.LENGTH_SHORT).show();
                updateStatusInfo(); // Refresh all status immediately
            });
        }).start();
    }
    
    private void clearData() {
        updateStatusText("Clearing impression data...");
        
        // Run in background
        new Thread(() -> {
            debugger.clearAllImpressionData();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                updateStatusText("Impression data cleared");
                Toast.makeText(this, "Impression data cleared", Toast.LENGTH_SHORT).show();
                updateStatusInfo(); // Refresh all status immediately
            });
        }).start();
    }
    
    private void forceSync() {
        updateStatusText("Forcing server sync...");
        
        // Run in background
        new Thread(() -> {
            debugger.forceSyncWithServer();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                updateStatusText("Server sync complete");
                Toast.makeText(this, "Server sync complete", Toast.LENGTH_SHORT).show();
                updateStatusInfo(); // Refresh all status immediately
            });
        }).start();
    }
    
    private void refreshAd() {
        updateStatusText("Refreshing ad...");
        
        // Reset ad view
        adView.setVisibility(View.GONE);
        adView.setVisibility(View.VISIBLE);
        
        // Fetch new ad
        fetchAd();
    }
    
    /**
     * Force refresh directly from the server, bypassing all caches
     */
    private void forceServerRefresh() {
        updateStatusText("Forcing refresh from server (bypassing cache)...");
        
        // Force the ad view to refresh from server
        if (adView != null) {
            adView.forceRefreshFromServer();
        }
        
        // Also force the view model to refresh
        if (viewModel != null) {
            viewModel.forceRefreshAds();
        }
        
        Toast.makeText(this, "Forced server refresh initiated", Toast.LENGTH_SHORT).show();
        
        // Update statuses after a delay to see results
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateStatusInfo();
            runDiagnostics();
        }, 2000); // Wait 2 seconds for refresh to complete
    }
    
    private void updateStatusText(String message) {
        Log.d(TAG, message);
        statusText.setText(message);
    }
} 