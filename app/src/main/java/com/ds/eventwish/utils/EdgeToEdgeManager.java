package com.ds.eventwish.utils;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.ds.eventwish.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ds.eventwish.MainActivity;

/**
 * EdgeToEdgeManager - Utility class for managing edge-to-edge design implementation
 * Based on Android's official edge-to-edge design guidelines:
 * https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge
 */
public class EdgeToEdgeManager {
    private static final String TAG = "EdgeToEdgeManager";
    private static final long ANIMATION_DURATION = 300L;
    private static final float FESTIVE_OVERLAY_ALPHA = 0.2f;
    private static final float WARM_OVERLAY_ALPHA = 0.1f;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000; // 3 seconds
    
    private static EdgeToEdgeManager instance;
    private boolean isEdgeToEdgeEnabled = false;
    private AnimatorSet currentAnimation;
    
    private Activity activity;
    private BottomNavigationView bottomNav;
    private boolean isBottomNavVisible = false;
    private Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private Runnable autoHideRunnable;
    
    // System UI state storage
    private int originalStatusBarColor;
    private int originalNavigationBarColor;
    private boolean originalLightStatusBar;
    private boolean originalLightNavigationBar;
    private boolean originalDecorFitsSystemWindows;
    
    // System bar protection views
    private View topProtectionView;
    private View bottomProtectionView;
    
    /**
     * Gets singleton instance of EdgeToEdgeManager
     */
    public static EdgeToEdgeManager getInstance() {
        if (instance == null) {
            instance = new EdgeToEdgeManager();
        }
        return instance;
    }
    
    private EdgeToEdgeManager() {
        // Private constructor for singleton
    }
    
    public void initialize(Activity activity) {
        this.activity = activity;
        initializeBottomNavigation();
        initializeAutoHideRunnable();
    }
    
    /**
     * Initialize bottom navigation reference
     */
    private void initializeBottomNavigation() {
        if (activity instanceof MainActivity) {
            bottomNav = activity.findViewById(R.id.bottomNavigation);
        }
    }
    
    /**
     * Initialize auto-hide runnable for bottom navigation
     */
    private void initializeAutoHideRunnable() {
        autoHideRunnable = () -> {
            if (isBottomNavVisible) {
                hideBottomNavigation();
                hideSystemBarProtection();
            }
        };
    }
    
    /**
     * Enable edge-to-edge design with transparent status bar
     * Implements Android's edge-to-edge design guidelines
     */
    public void enableEdgeToEdge() {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Store original system UI state for restoration
        storeOriginalSystemUIState(window);
        
        // Enable edge-to-edge design
        WindowCompat.setDecorFitsSystemWindows(window, false);
        
        // Set transparent system bars for immersive experience
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        
        // Configure system bar appearance for optimal contrast
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            // Use dark content for better visibility on light backgrounds
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
        }
        
        // Hide bottom navigation initially for immersive experience
        hideBottomNavigation();
        
        Log.d(TAG, "Edge-to-edge design enabled with transparent system bars");
    }
    
    /**
     * Disable edge-to-edge design and restore original system UI
     */
    public void disableEdgeToEdge() {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Restore original system UI state
        restoreOriginalSystemUIState(window);
        
        // Show bottom navigation
        showBottomNavigation();
        
        // Remove any pending auto-hide callbacks
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        Log.d(TAG, "Edge-to-edge design disabled, original system UI restored");
    }
    
    /**
     * Store original system UI state for later restoration
     */
    private void storeOriginalSystemUIState(Window window) {
        originalStatusBarColor = window.getStatusBarColor();
        originalNavigationBarColor = window.getNavigationBarColor();
        
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            originalLightStatusBar = windowInsetsController.isAppearanceLightStatusBars();
            originalLightNavigationBar = windowInsetsController.isAppearanceLightNavigationBars();
        }
    }
    
    /**
     * Restore original system UI state
     */
    private void restoreOriginalSystemUIState(Window window) {
        // Restore window insets behavior
        WindowCompat.setDecorFitsSystemWindows(window, true);
        
        // Restore original system bar colors
        window.setStatusBarColor(originalStatusBarColor);
        window.setNavigationBarColor(originalNavigationBarColor);
        
        // Restore original system bar appearance
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(originalLightStatusBar);
            windowInsetsController.setAppearanceLightNavigationBars(originalLightNavigationBar);
        }
    }
    
    /**
     * Hide bottom navigation for immersive experience
     */
    public void hideBottomNavigation() {
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            bottomNav.setVisibility(View.GONE);
            isBottomNavVisible = false;
            Log.d(TAG, "Bottom navigation hidden for immersive experience");
        }
    }
    
    /**
     * Show bottom navigation
     */
    public void showBottomNavigation() {
        if (bottomNav != null && !isBottomNavVisible) {
            bottomNav.setVisibility(View.VISIBLE);
            isBottomNavVisible = true;
            Log.d(TAG, "Bottom navigation shown");
            
            // Schedule auto-hide for immersive experience
            scheduleAutoHide();
        }
    }
    
    /**
     * Toggle bottom navigation visibility
     */
    public void toggleBottomNavigation() {
        if (isBottomNavVisible) {
            hideBottomNavigation();
            hideSystemBarProtection();
        } else {
            showBottomNavigation();
            showSystemBarProtection();
        }
    }
    
    /**
     * Schedule auto-hide for bottom navigation after delay
     */
    private void scheduleAutoHide() {
        // Remove any existing callbacks
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        // Schedule auto-hide after delay
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MILLIS);
    }
    
    /**
     * Set system bar protection views for gradient overlays
     */
    public void setSystemBarProtectionViews(View topProtection, View bottomProtection) {
        this.topProtectionView = topProtection;
        this.bottomProtectionView = bottomProtection;
    }
    
    /**
     * Show system bar protection gradients for better content visibility
     */
    public void showSystemBarProtection() {
        if (topProtectionView != null) {
            topProtectionView.setVisibility(View.VISIBLE);
        }
        if (bottomProtectionView != null) {
            bottomProtectionView.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "System bar protection gradients shown");
    }
    
    /**
     * Hide system bar protection gradients for cleaner immersive experience
     */
    public void hideSystemBarProtection() {
        if (topProtectionView != null) {
            topProtectionView.setVisibility(View.GONE);
        }
        if (bottomProtectionView != null) {
            bottomProtectionView.setVisibility(View.GONE);
        }
        Log.d(TAG, "System bar protection gradients hidden");
    }
    
    /**
     * Setup touch listener for WebView to handle bottom navigation toggle
     * Implements touch-based UI visibility toggle as per edge-to-edge guidelines
     */
    public void setupWebViewTouchListener(View webView) {
        if (webView == null) return;
        
        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                toggleBottomNavigation();
            }
            // Return false to allow WebView to handle the touch event as well
            return false;
        });
        
        Log.d(TAG, "WebView touch listener setup for bottom navigation toggle");
    }
    
    /**
     * Setup touch listener for any view to handle bottom navigation toggle
     */
    public void setupViewTouchListener(View view) {
        if (view == null) return;
        
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                toggleBottomNavigation();
            }
            // Return false to allow other views to handle the touch event
            return false;
        });
        
        Log.d(TAG, "View touch listener setup for bottom navigation toggle");
    }
    
    /**
     * Check if bottom navigation is currently visible
     */
    public boolean isBottomNavigationVisible() {
        return isBottomNavVisible;
    }
    
    /**
     * Clean up resources and remove callbacks
     */
    public void cleanup() {
        if (autoHideHandler != null) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
        }
        activity = null;
        bottomNav = null;
        topProtectionView = null;
        bottomProtectionView = null;
        Log.d(TAG, "EdgeToEdgeManager cleaned up");
    }
    
    /**
     * Check if edge-to-edge design is currently enabled
     */
    public boolean isEdgeToEdgeEnabled() {
        if (activity == null) return false;
        
        Window window = activity.getWindow();
        if (window == null) return false;
        
        // Check if status bar is transparent (indicating edge-to-edge is enabled)
        return window.getStatusBarColor() == Color.TRANSPARENT;
    }
    
    /**
     * Enable edge-to-edge design with options (backward compatibility)
     */
    public void enableEdgeToEdge(Activity activity, boolean enablePictureInPicture) {
        // Update activity reference if different
        if (this.activity != activity) {
            this.activity = activity;
            initializeBottomNavigation();
        }
        
        // Enable edge-to-edge design
        enableEdgeToEdge();
        
        Log.d(TAG, "Edge-to-edge enabled with PiP option: " + enablePictureInPicture);
    }
    
    /**
     * Disable edge-to-edge design with activity parameter (backward compatibility)
     */
    public void disableEdgeToEdge(Activity activity) {
        // Update activity reference if different
        if (this.activity != activity) {
            this.activity = activity;
            initializeBottomNavigation();
        }
        
        // Disable edge-to-edge design
        disableEdgeToEdge();
    }
    
    /**
     * Apply dynamic system bar theming based on content color
     */
    public void applyDynamicSystemBarTheming(Activity activity, int primaryColor) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Apply dynamic theming while maintaining edge-to-edge design
        if (isEdgeToEdgeEnabled()) {
            // Keep status bar transparent but update appearance
            WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
            if (windowInsetsController != null) {
                // Determine if we need light or dark content based on primary color
                boolean isLightBackground = isColorLight(primaryColor);
                windowInsetsController.setAppearanceLightStatusBars(isLightBackground);
                windowInsetsController.setAppearanceLightNavigationBars(isLightBackground);
            }
        }
        
        Log.d(TAG, "Dynamic system bar theming applied with primary color: " + Integer.toHexString(primaryColor));
    }
    
    /**
     * Handle configuration changes for edge-to-edge design
     */
    public void handleConfigurationChange(Activity activity, android.content.res.Configuration newConfig) {
        if (activity == null) return;
        
        // Update activity reference
        this.activity = activity;
        initializeBottomNavigation();
        
        // Reapply edge-to-edge design if it was enabled
        if (isEdgeToEdgeEnabled()) {
            enableEdgeToEdge();
        }
        
        Log.d(TAG, "Configuration change handled for edge-to-edge design");
    }
    
    /**
     * Determine if a color is light (for contrast calculations)
     */
    private boolean isColorLight(int color) {
        // Calculate luminance using the relative luminance formula
        double red = Color.red(color) / 255.0;
        double green = Color.green(color) / 255.0;
        double blue = Color.blue(color) / 255.0;
        
        // Apply gamma correction
        red = red <= 0.03928 ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);
        green = green <= 0.03928 ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);
        blue = blue <= 0.03928 ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);
        
        // Calculate relative luminance
        double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        
        // Return true if luminance is greater than 0.5 (light color)
        return luminance > 0.5;
    }
} 