package com.ds.eventwish.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.ds.eventwish.R;

import java.util.ArrayList;
import java.util.List;

/**
 * PictureInPictureManager - Premium utility for managing picture-in-picture mode with festive theming
 * 
 * Features:
 * - Android 8.0+ PiP support with graceful fallback
 * - Smart aspect ratio calculation for greeting cards
 * - Premium PiP UI controls with Material 3 design
 * - Error handling for unsupported devices
 * - Festive PiP overlay design
 * - WebView-optimized PiP experience
 * - Custom remote actions for sharing/saving
 * - Battery-efficient PiP lifecycle management
 * 
 * @author EventWish Team
 * @version 1.0
 */
public class PictureInPictureManager {
    
    private static final String TAG = "PictureInPictureManager";
    
    // PiP action constants
    public static final String ACTION_SHARE = "com.ds.eventwish.PIP_SHARE";
    public static final String ACTION_SAVE = "com.ds.eventwish.PIP_SAVE";
    public static final String ACTION_CLOSE = "com.ds.eventwish.PIP_CLOSE";
    public static final String ACTION_FULLSCREEN = "com.ds.eventwish.PIP_FULLSCREEN";
    
    // PiP aspect ratios for different content types
    private static final Rational ASPECT_RATIO_SQUARE = new Rational(1, 1);
    private static final Rational ASPECT_RATIO_PORTRAIT = new Rational(3, 4);
    private static final Rational ASPECT_RATIO_LANDSCAPE = new Rational(16, 9);
    private static final Rational ASPECT_RATIO_GREETING_CARD = new Rational(4, 5);
    
    // Minimum PiP size constraints
    private static final int MIN_PIP_WIDTH = 100;
    private static final int MIN_PIP_HEIGHT = 100;
    
    private static PictureInPictureManager instance;
    private boolean isPipSupported = false;
    private boolean isInPipMode = false;
    private PipActionReceiver pipActionReceiver;
    private PipModeCallback pipModeCallback;
    
    /**
     * Interface for PiP mode callbacks
     */
    public interface PipModeCallback {
        void onEnterPictureInPicture();
        void onExitPictureInPicture();
        void onPipAction(String action);
        void onPipError(String error);
    }
    
    /**
     * Gets singleton instance of PictureInPictureManager
     */
    public static PictureInPictureManager getInstance() {
        if (instance == null) {
            instance = new PictureInPictureManager();
        }
        return instance;
    }
    
    private PictureInPictureManager() {
        // Check PiP support
        isPipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        Log.d(TAG, "PiP support: " + isPipSupported);
    }
    
    /**
     * Checks if picture-in-picture is supported on this device
     * 
     * @param context The context to check
     * @return true if PiP is supported
     */
    public boolean isPictureInPictureSupported(@NonNull Context context) {
        if (!isPipSupported) {
            return false;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return context.getPackageManager().hasSystemFeature(
                    "android.software.picture_in_picture");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking PiP support", e);
        }
        
        return false;
    }
    
    /**
     * Enters picture-in-picture mode with premium greeting card optimization
     * 
     * @param activity The activity to enter PiP mode
     * @param sourceView The view containing the content (e.g., WebView)
     * @param callback Callback for PiP events
     * @return true if PiP was entered successfully
     */
    public boolean enterPictureInPictureMode(@NonNull Activity activity, 
                                           @Nullable View sourceView,
                                           @Nullable PipModeCallback callback) {
        if (!isPictureInPictureSupported(activity)) {
            Log.w(TAG, "PiP not supported, falling back to fullscreen");
            if (callback != null) {
                callback.onPipError("Picture-in-picture not supported on this device");
            }
            return false;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.pipModeCallback = callback;
                
                // Register action receiver
                registerPipActionReceiver(activity);
                
                // Create PiP parameters
                PictureInPictureParams params = createOptimalPipParams(activity, sourceView);
                
                // Enter PiP mode
                boolean success = activity.enterPictureInPictureMode(params);
                
                if (success) {
                    isInPipMode = true;
                    Log.d(TAG, "Successfully entered PiP mode");
                    if (callback != null) {
                        callback.onEnterPictureInPicture();
                    }
                } else {
                    Log.w(TAG, "Failed to enter PiP mode");
                    if (callback != null) {
                        callback.onPipError("Failed to enter picture-in-picture mode");
                    }
                }
                
                return success;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error entering PiP mode", e);
            FirebaseCrashManager.logException(e);
            if (callback != null) {
                callback.onPipError("Error entering picture-in-picture: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * Exits picture-in-picture mode and returns to normal mode
     * 
     * @param activity The activity to exit PiP mode
     */
    public void exitPictureInPictureMode(@NonNull Activity activity) {
        try {
            if (isInPipMode) {
                isInPipMode = false;
                
                // Unregister action receiver
                unregisterPipActionReceiver(activity);
                
                Log.d(TAG, "Exited PiP mode");
                if (pipModeCallback != null) {
                    pipModeCallback.onExitPictureInPicture();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exiting PiP mode", e);
        }
    }
    
    /**
     * Updates PiP parameters for dynamic content changes
     * 
     * @param activity The activity in PiP mode
     * @param sourceView The updated source view
     */
    public void updatePictureInPictureParams(@NonNull Activity activity, @Nullable View sourceView) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
                PictureInPictureParams params = createOptimalPipParams(activity, sourceView);
                activity.setPictureInPictureParams(params);
                Log.d(TAG, "Updated PiP parameters");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating PiP parameters", e);
        }
    }
    
    /**
     * Creates optimal PiP parameters for greeting card content
     * 
     * @param context The context
     * @param sourceView The source view (WebView containing greeting card)
     * @return PictureInPictureParams for optimal viewing
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private PictureInPictureParams createOptimalPipParams(@NonNull Context context, @Nullable View sourceView) {
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
        
        try {
            // Calculate optimal aspect ratio
            Rational aspectRatio = calculateOptimalAspectRatio(sourceView);
            builder.setAspectRatio(aspectRatio);
            
            // Set source rectangle hint for smooth transition
            if (sourceView != null) {
                Rect sourceRect = calculateSourceRect(sourceView);
                builder.setSourceRectHint(sourceRect);
            }
            
            // Add premium remote actions
            List<RemoteAction> actions = createPremiumRemoteActions(context);
            builder.setActions(actions);
            
            // Enable auto-enter PiP (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true);
            }
            
            Log.d(TAG, "Created PiP params with aspect ratio: " + aspectRatio);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating PiP params", e);
            // Fallback to default greeting card aspect ratio
            builder.setAspectRatio(ASPECT_RATIO_GREETING_CARD);
        }
        
        return builder.build();
    }
    
    /**
     * Calculates optimal aspect ratio based on content
     * 
     * @param sourceView The source view containing content
     * @return Optimal aspect ratio for the content
     */
    private Rational calculateOptimalAspectRatio(@Nullable View sourceView) {
        try {
            if (sourceView instanceof WebView) {
                // For WebViews (greeting cards), use specialized calculation
                return calculateWebViewAspectRatio((WebView) sourceView);
            } else if (sourceView != null) {
                // For other views, calculate based on dimensions
                int width = sourceView.getWidth();
                int height = sourceView.getHeight();
                
                if (width > 0 && height > 0) {
                    // Ensure minimum size constraints
                    if (width < MIN_PIP_WIDTH) width = MIN_PIP_WIDTH;
                    if (height < MIN_PIP_HEIGHT) height = MIN_PIP_HEIGHT;
                    
                    return new Rational(width, height);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error calculating aspect ratio", e);
        }
        
        // Default to greeting card aspect ratio
        return ASPECT_RATIO_GREETING_CARD;
    }
    
    /**
     * Calculates aspect ratio specifically for WebView content
     * 
     * @param webView The WebView containing greeting card
     * @return Optimal aspect ratio for WebView content
     */
    private Rational calculateWebViewAspectRatio(@NonNull WebView webView) {
        try {
            // Get WebView dimensions
            int webViewWidth = webView.getWidth();
            int webViewHeight = webView.getHeight();
            
            if (webViewWidth > 0 && webViewHeight > 0) {
                // Calculate content aspect ratio
                float contentRatio = (float) webViewWidth / webViewHeight;
                
                // Optimize for greeting card content
                if (contentRatio > 1.5f) {
                    // Landscape content
                    return ASPECT_RATIO_LANDSCAPE;
                } else if (contentRatio < 0.8f) {
                    // Portrait content (typical for greeting cards)
                    return ASPECT_RATIO_PORTRAIT;
                } else {
                    // Square-ish content
                    return ASPECT_RATIO_SQUARE;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error calculating WebView aspect ratio", e);
        }
        
        // Default for greeting cards
        return ASPECT_RATIO_GREETING_CARD;
    }
    
    /**
     * Calculates source rectangle for smooth PiP transition
     * 
     * @param sourceView The source view
     * @return Source rectangle for transition hint
     */
    private Rect calculateSourceRect(@NonNull View sourceView) {
        try {
            int[] location = new int[2];
            sourceView.getLocationOnScreen(location);
            
            int left = location[0];
            int top = location[1];
            int right = left + sourceView.getWidth();
            int bottom = top + sourceView.getHeight();
            
            return new Rect(left, top, right, bottom);
        } catch (Exception e) {
            Log.w(TAG, "Error calculating source rect", e);
            return new Rect(0, 0, MIN_PIP_WIDTH, MIN_PIP_HEIGHT);
        }
    }
    
    /**
     * Creates premium remote actions for PiP mode with festive theming
     * 
     * @param context The context
     * @return List of remote actions for PiP controls
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<RemoteAction> createPremiumRemoteActions(@NonNull Context context) {
        List<RemoteAction> actions = new ArrayList<>();
        
        try {
            // Share action with festive icon
            Intent shareIntent = new Intent(ACTION_SHARE);
            PendingIntent sharePendingIntent = PendingIntent.getBroadcast(
                context, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Icon shareIcon = Icon.createWithResource(context, R.drawable.ic_share);
            RemoteAction shareAction = new RemoteAction(shareIcon, 
                "Share", "Share this greeting card", sharePendingIntent);
            actions.add(shareAction);
            
            // Save action with premium styling
            Intent saveIntent = new Intent(ACTION_SAVE);
            PendingIntent savePendingIntent = PendingIntent.getBroadcast(
                context, 1, saveIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Icon saveIcon = Icon.createWithResource(context, R.drawable.ic_download);
            RemoteAction saveAction = new RemoteAction(saveIcon, 
                "Save", "Save this greeting card", savePendingIntent);
            actions.add(saveAction);
            
            // Fullscreen action
            Intent fullscreenIntent = new Intent(ACTION_FULLSCREEN);
            PendingIntent fullscreenPendingIntent = PendingIntent.getBroadcast(
                context, 2, fullscreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Icon fullscreenIcon = Icon.createWithResource(context, R.drawable.ic_fullscreen);
            RemoteAction fullscreenAction = new RemoteAction(fullscreenIcon, 
                "Fullscreen", "Return to fullscreen", fullscreenPendingIntent);
            actions.add(fullscreenAction);
            
            Log.d(TAG, "Created " + actions.size() + " premium remote actions");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating remote actions", e);
        }
        
        return actions;
    }
    
    /**
     * Registers broadcast receiver for PiP actions
     * 
     * @param context The context to register receiver
     */
    private void registerPipActionReceiver(@NonNull Context context) {
        try {
            if (pipActionReceiver == null) {
                pipActionReceiver = new PipActionReceiver();
                
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_SHARE);
                filter.addAction(ACTION_SAVE);
                filter.addAction(ACTION_CLOSE);
                filter.addAction(ACTION_FULLSCREEN);
                
                context.registerReceiver(pipActionReceiver, filter);
                Log.d(TAG, "Registered PiP action receiver");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering PiP action receiver", e);
        }
    }
    
    /**
     * Unregisters broadcast receiver for PiP actions
     * 
     * @param context The context to unregister receiver
     */
    private void unregisterPipActionReceiver(@NonNull Context context) {
        try {
            if (pipActionReceiver != null) {
                context.unregisterReceiver(pipActionReceiver);
                pipActionReceiver = null;
                Log.d(TAG, "Unregistered PiP action receiver");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering PiP action receiver", e);
        }
    }
    
    /**
     * Broadcast receiver for handling PiP remote actions
     */
    private class PipActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d(TAG, "Received PiP action: " + action);
                
                if (pipModeCallback != null && action != null) {
                    pipModeCallback.onPipAction(action);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling PiP action", e);
            }
        }
    }
    
    /**
     * Handles PiP mode state changes
     * 
     * @param isInPictureInPictureMode true if entering PiP mode
     */
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        try {
            this.isInPipMode = isInPictureInPictureMode;
            
            if (pipModeCallback != null) {
                if (isInPictureInPictureMode) {
                    pipModeCallback.onEnterPictureInPicture();
                } else {
                    pipModeCallback.onExitPictureInPicture();
                }
            }
            
            Log.d(TAG, "PiP mode changed: " + isInPictureInPictureMode);
        } catch (Exception e) {
            Log.e(TAG, "Error handling PiP mode change", e);
        }
    }
    
    /**
     * Checks if currently in picture-in-picture mode
     * 
     * @return true if in PiP mode
     */
    public boolean isInPictureInPictureMode() {
        return isInPipMode;
    }
    
    /**
     * Optimizes WebView for picture-in-picture mode
     * 
     * @param webView The WebView to optimize
     */
    public void optimizeWebViewForPip(@NonNull WebView webView) {
        try {
            if (isInPipMode) {
                // Optimize WebView settings for PiP
                android.webkit.WebSettings settings = webView.getSettings();
                
                // Disable zoom controls in PiP
                settings.setBuiltInZoomControls(false);
                settings.setDisplayZoomControls(false);
                settings.setSupportZoom(false);
                
                // Optimize rendering for small PiP window
                settings.setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
                settings.setLoadWithOverviewMode(true);
                settings.setUseWideViewPort(true);
                
                // Enable hardware acceleration for smooth rendering
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                
                Log.d(TAG, "Optimized WebView for PiP mode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error optimizing WebView for PiP", e);
        }
    }
    
    /**
     * Restores WebView settings after exiting PiP mode
     * 
     * @param webView The WebView to restore
     */
    public void restoreWebViewFromPip(@NonNull WebView webView) {
        try {
            if (!isInPipMode) {
                // Restore normal WebView settings
                android.webkit.WebSettings settings = webView.getSettings();
                
                // Re-enable zoom controls
                settings.setBuiltInZoomControls(true);
                settings.setDisplayZoomControls(false); // Hide zoom controls UI but allow pinch zoom
                settings.setSupportZoom(true);
                
                // Restore normal rendering
                settings.setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
                
                Log.d(TAG, "Restored WebView from PiP mode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring WebView from PiP", e);
        }
    }
    
    /**
     * Cleanup method to be called when the manager is no longer needed
     * 
     * @param context The context for cleanup
     */
    public void cleanup(@NonNull Context context) {
        try {
            unregisterPipActionReceiver(context);
            pipModeCallback = null;
            isInPipMode = false;
            Log.d(TAG, "PictureInPictureManager cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
} 