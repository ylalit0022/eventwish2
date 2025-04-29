package com.ds.eventwish.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.ds.eventwish.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * Utility class for checking and handling app updates with forced update capability
 */
public class AppUpdateChecker implements DefaultLifecycleObserver {
    private static final String TAG = "AppUpdateChecker";
    private static final int REQUEST_CODE_UPDATE = 100;
    
    private final AppUpdateManager appUpdateManager;
    private final Activity activity;
    private InstallStateUpdatedListener installStateUpdatedListener;
    private boolean isForceUpdateMode = false;
    
    /**
     * Create a new AppUpdateChecker
     * @param activity The activity
     */
    public AppUpdateChecker(@NonNull Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
        
        // Register the install state listener
        installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // If we get here and we're in force update mode, the user has downloaded the update but not installed it
                // We should prompt them to complete installation
                popupSnackbarForCompleteUpdate();
            } else if (state.installStatus() == InstallStatus.INSTALLED) {
                // Update has been installed, clean up
                if (appUpdateManager != null) {
                    appUpdateManager.unregisterListener(installStateUpdatedListener);
                }
            }
        };
        
        appUpdateManager.registerListener(installStateUpdatedListener);
    }
    
    /**
     * Check for updates and show appropriate UI, with force update if required
     */
    public void checkForUpdate() {
        checkForUpdate(false);
    }
    
    /**
     * Check for updates and show appropriate UI
     * @param forceUpdateOnly If true, only show the update dialog if a force update is required
     */
    public void checkForUpdate(boolean forceUpdateOnly) {
        Log.d(TAG, "Checking for app updates, forceUpdateOnly: " + forceUpdateOnly);
        
        // First check if there's a downloaded update that hasn't been installed yet
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                // Update has been downloaded but not installed, prompt the user to complete installation
                popupSnackbarForCompleteUpdate();
                return;
            }
            
            // Check if an update is available
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Log.d(TAG, "Update available, checking if immediate update is allowed");
                
                // Check if an immediate update is allowed (indicates a critical update)
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    Log.d(TAG, "Immediate update is allowed, starting update flow");
                    // This is a critical update that should be forced
                    isForceUpdateMode = true;
                    startImmediateUpdate(appUpdateInfo);
                } else if (!forceUpdateOnly && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    // This is a regular update
                    Log.d(TAG, "Flexible update is allowed, showing update dialog");
                    showUpdateDialog(appUpdateInfo, false);
                }
            } else {
                Log.d(TAG, "No update available or update not needed");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking for update", e);
        });
    }
    
    /**
     * Show a dialog to the user prompting them to update the app
     * @param appUpdateInfo The app update info
     * @param isForced Whether this is a forced update that cannot be skipped
     */
    private void showUpdateDialog(AppUpdateInfo appUpdateInfo, boolean isForced) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update_available)
                .setMessage(isForced ? R.string.force_update_message : R.string.update_message)
                .setPositiveButton(R.string.update_now, (dialog, which) -> {
                    try {
                        if (isForced) {
                            startImmediateUpdate(appUpdateInfo);
                        } else {
                            startFlexibleUpdate(appUpdateInfo);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting update flow", e);
                        // Fallback to Play Store
                        openPlayStore(activity);
                    }
                });
        
        if (!isForced) {
            // Only add "Later" button if this is not a forced update
            builder.setNegativeButton(R.string.later, (dialog, which) -> {
                dialog.dismiss();
            });
        }
        
        // Make the dialog non-cancelable if this is a forced update
        builder.setCancelable(!isForced);
        builder.show();
    }
    
    /**
     * Start an immediate (forced) update flow
     * @param appUpdateInfo The app update info
     */
    private void startImmediateUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    REQUEST_CODE_UPDATE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start immediate update flow", e);
            // Fallback to Play Store
            openPlayStore(activity);
        }
    }
    
    /**
     * Start a flexible (non-forced) update flow
     * @param appUpdateInfo The app update info
     */
    private void startFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    REQUEST_CODE_UPDATE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start flexible update flow", e);
            // Fallback to Play Store
            openPlayStore(activity);
        }
    }
    
    /**
     * Show a snackbar to complete a downloaded update
     */
    private void popupSnackbarForCompleteUpdate() {
        appUpdateManager.completeUpdate();
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
     * Resume update checks - should be called in onResume() of the Activity
     */
    public void resumeUpdates() {
        // If in force update mode, check again to ensure the user can't skip the update
        if (isForceUpdateMode) {
            checkForUpdate(true);
        }
        
        // Check for downloaded updates
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            }
            
            // If there's a pending update that requires immediate installation,
            // make sure it's still shown to the user
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_UPDATE);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to resume update flow", e);
                }
            }
        });
    }
    
    /**
     * Clean up resources - should be called in onDestroy() of the Activity
     */
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (appUpdateManager != null && installStateUpdatedListener != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
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