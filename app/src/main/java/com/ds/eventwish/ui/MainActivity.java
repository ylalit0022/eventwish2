package com.ds.eventwish.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.ds.eventwish.R;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.ads.NativeAdHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private NativeAdHelper nativeAdHelper;
    private FrameLayout nativeAdContainer;
    private ProgressBar nativeAdLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize navigation
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        NavigationUI.setupWithNavController(bottomNav, navController);

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