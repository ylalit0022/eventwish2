package com.ds.eventwish.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.util.Log;

import com.ds.eventwish.R;
import com.ds.eventwish.ui.MainActivity;
import com.ds.eventwish.utils.NotificationPermissionManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DURATION = 2000; // 2 seconds
    private ImageView logoImageView;
    private TextView appNameTextView;
    private ProgressBar loadingProgressBar;
    private ImageView backArrow;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private boolean permissionRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make the activity full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_splash);

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                Log.d(TAG, "Notification permission result: " + isGranted);
                // Continue with app flow after permission result
                if (!isFinishing()) {
                    startSplashFlow();
                }
            }
        );

        // Initialize views
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        backArrow = findViewById(R.id.backArrow);

        // Set up back arrow
        backArrow.setOnClickListener(v -> navigateToMain());

        // Check notification permission first
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (!NotificationPermissionManager.hasNotificationPermission(this)) {
            // Request permission using the manager
            NotificationPermissionManager.requestNotificationPermission(this);
            permissionRequested = true;
            // Start splash flow after a short delay to let the permission dialog show
            new Handler().postDelayed(this::startSplashFlow, 500);
        } else {
            // Permission already granted, continue with splash flow
            startSplashFlow();
        }
    }

    private void startSplashFlow() {
        // Start animations
        startSplashAnimations();

        // Navigate to main activity after delay
        new Handler().postDelayed(this::navigateToMain, SPLASH_DURATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        NotificationPermissionManager.handlePermissionResult(this, permissions, grantResults);
        
        // Continue with app flow if we haven't already
        if (!isFinishing() && permissionRequested) {
            permissionRequested = false;
            startSplashFlow();
        }
    }

    private void startSplashAnimations() {
        try {
            // Fade in logo
            logoImageView.setAlpha(0f);
            logoImageView.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            // Scale up app name
            appNameTextView.setScaleX(0f);
            appNameTextView.setScaleY(0f);
            appNameTextView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            // Show loading progress
            loadingProgressBar.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting splash animations", e);
            // If animations fail, still show the views
            logoImageView.setAlpha(1f);
            appNameTextView.setScaleX(1f);
            appNameTextView.setScaleY(1f);
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToMain() {
        // Check if already navigating to avoid multiple calls
        if (isFinishing()) return;

        try {
            // Fade out animations
            View decorView = getWindow().getDecorView();
            decorView.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            startMainActivity();
                        }
                    })
                    .start();
        } catch (Exception e) {
            Log.e(TAG, "Error during fade out animation", e);
            // If animation fails, just start the main activity
            startMainActivity();
        }
    }

    private void startMainActivity() {
        // Start MainActivity
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        
        // Apply fade out transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        // Close splash activity
        finish();
    }
} 