package com.ds.eventwish.utils;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.ds.eventwish.R;
import com.ds.eventwish.ui.history.SharedPrefsManager;

/**
 * Utility class to manage analytics consent
 */
public class AnalyticsConsentManager {
    private static final String TAG = "AnalyticsConsentManager";
    
    /**
     * Check if analytics consent dialog should be shown
     * @param context Application context
     * @return true if consent dialog should be shown, false otherwise
     */
    public static boolean shouldShowConsentDialog(Context context) {
        SharedPrefsManager prefsManager = new SharedPrefsManager(context);
        return !prefsManager.hasShownAnalyticsConsent();
    }
    
    /**
     * Show analytics consent dialog
     * @param context Application context
     */
    public static void showConsentDialog(Context context) {
        try {
            Log.d(TAG, "Showing analytics consent dialog");
            
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_analytics_consent, null);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();
            
            // Set up accept button
            dialogView.findViewById(R.id.acceptButton).setOnClickListener(v -> {
                Log.d(TAG, "User accepted analytics consent");
                
                // Update consent status in SharedPreferences
                SharedPrefsManager prefsManager = new SharedPrefsManager(context);
                prefsManager.setAnalyticsConsent(true);
                prefsManager.setAnalyticsConsentShown();
                
                // Enable analytics collection
                AnalyticsUtils.setAnalyticsEnabled(true);
                
                dialog.dismiss();
            });
            
            // Set up decline button
            dialogView.findViewById(R.id.declineButton).setOnClickListener(v -> {
                Log.d(TAG, "User declined analytics consent");
                
                // Update consent status in SharedPreferences
                SharedPrefsManager prefsManager = new SharedPrefsManager(context);
                prefsManager.setAnalyticsConsent(false);
                prefsManager.setAnalyticsConsentShown();
                
                // Disable analytics collection
                AnalyticsUtils.setAnalyticsEnabled(false);
                
                dialog.dismiss();
            });
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing analytics consent dialog", e);
        }
    }
    
    /**
     * Initialize analytics based on user consent
     * @param context Application context
     */
    public static void initializeAnalytics(Context context) {
        try {
            SharedPrefsManager prefsManager = new SharedPrefsManager(context);
            boolean hasConsent = prefsManager.hasAnalyticsConsent();
            
            Log.d(TAG, "Initializing analytics with consent status: " + hasConsent);
            
            // Initialize analytics with appropriate consent status
            AnalyticsUtils.setAnalyticsEnabled(hasConsent);
            
            // If consent dialog hasn't been shown yet, check user preferences
            if (!prefsManager.hasShownAnalyticsConsent()) {
                Log.d(TAG, "Analytics consent dialog has not been shown yet");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing analytics", e);
        }
    }
} 