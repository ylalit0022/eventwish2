package com.ds.eventwish.ui.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.RemoteConfigManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Helper class for showing update dialogs
 */
public class UpdateDialogHelper {
    private static final String TAG = "UpdateDialogHelper";
    
    /**
     * Show update dialog
     * @param activity Activity context
     * @param versionName Latest version name
     * @param message Update message
     * @param isForceUpdate Whether the update is mandatory
     */
    public static void showUpdateDialog(Activity activity, String versionName, String message, boolean isForceUpdate) {
        if (activity == null || activity.isFinishing()) {
            Log.e(TAG, "Cannot show dialog - activity is null or finishing");
            return;
        }
        
        // Track impression
        Bundle params = new Bundle();
        params.putString("current_version", versionName);
        params.putBoolean("is_force_update", isForceUpdate);
        AnalyticsUtils.getInstance().logEvent("update_dialog_shown", params);
        
        RemoteConfigManager remoteConfigManager = RemoteConfigManager.getInstance(activity);
        
        // Show dialog
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Update Available")
            .setMessage(message + "\n\nVersion: " + versionName)
            .setPositiveButton("Update Now", (dialog, which) -> {
                // Track action
                remoteConfigManager.trackUpdatePromptAction("accepted", versionName);
                openPlayStore(activity);
            })
            .setNegativeButton(isForceUpdate ? "Exit" : "Later", (dialog, which) -> {
                // Track action
                remoteConfigManager.trackUpdatePromptAction(isForceUpdate ? "declined" : "deferred", versionName);
                if (isForceUpdate) {
                    activity.finishAffinity();
                }
            })
            .setCancelable(!isForceUpdate)
            .show();
    }
    
    /**
     * Open Play Store
     * @param activity Activity context
     */
    private static void openPlayStore(Activity activity) {
        try {
            String appPackageName = activity.getPackageName();
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException e) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Play Store", e);
        }
    }
} 