package com.ds.eventwish.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.ds.eventwish.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

// Stub classes for Play Core
class AppUpdateInfo {
    public int updateAvailability() {
        return UpdateAvailability.UPDATE_AVAILABLE;
    }
    
    public boolean isUpdateTypeAllowed(int updateType) {
        return true;
    }
}

class AppUpdateManager {
    public Task<AppUpdateInfo> getAppUpdateInfo() {
        return new Task<>();
    }
    
    public void startUpdateFlowForResult(AppUpdateInfo appUpdateInfo, int updateType, Activity activity, int requestCode) {
        // Stub implementation
    }
}

class AppUpdateManagerFactory {
    public static AppUpdateManager create(Context context) {
        return new AppUpdateManager();
    }
}

class UpdateAvailability {
    public static final int UPDATE_AVAILABLE = 2;
}

class AppUpdateType {
    public static final int IMMEDIATE = 1;
}

class Task<T> {
    public Task<T> addOnSuccessListener(OnSuccessListener<T> listener) {
        // Create a dummy AppUpdateInfo for the listener
        listener.onSuccess((T) new AppUpdateInfo());
        return this;
    }
    
    public Task<T> addOnFailureListener(OnFailureListener listener) {
        return this;
    }
}

interface OnSuccessListener<T> {
    void onSuccess(T result);
}

interface OnFailureListener {
    void onFailure(Exception e);
}

/**
 * Utility class for checking and handling app updates
 */
public class AppUpdateChecker {
    private static final String TAG = "AppUpdateChecker";
    private static final int REQUEST_CODE_UPDATE = 100;
    
    /**
     * Check if an update is available and show a dialog if it is
     * @param activity The activity
     */
    public static void checkForUpdate(@NonNull Activity activity) {
        // Create the update manager
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
            
        // Check for an update using method chaining instead of storing the Task
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Show update dialog
                showUpdateDialog(activity, appUpdateManager, appUpdateInfo);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking for update", e);
        });
    }
    
    /**
     * Show a dialog to the user prompting them to update the app
     * @param activity The activity
     * @param appUpdateManager The app update manager
     * @param appUpdateInfo The app update info
     */
    private static void showUpdateDialog(Activity activity, 
                                        AppUpdateManager appUpdateManager, 
                                        AppUpdateInfo appUpdateInfo) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Update Available")
                .setMessage("A new version of the app is available. Please update to continue using the app.")
                .setPositiveButton("Update Now", (dialog, which) -> {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                activity,
                                REQUEST_CODE_UPDATE);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting update flow", e);
                        // Fallback to Play Store
                        openPlayStore(activity);
                    }
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * Open the Play Store to the app's page
     * @param context The context
     */
    public static void openPlayStore(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // If Play Store app is not installed, open in browser
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    /**
     * Get the app version name
     * @param context The context
     * @return The app version name
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version", e);
            return "Unknown";
        }
    }
} 