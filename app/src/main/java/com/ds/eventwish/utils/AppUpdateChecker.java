package com.ds.eventwish.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.ds.eventwish.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;

/**
 * Utility class for checking and handling app updates
 */
public class AppUpdateChecker implements DefaultLifecycleObserver {
    private static final String TAG = "AppUpdateChecker";
    private static final int REQUEST_CODE_UPDATE = 100;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second
    private static final int DAYS_FOR_FLEXIBLE_UPDATE = 2;
    private static final int DAYS_FOR_IMMEDIATE_UPDATE = 5;
    
    private final AppUpdateManager appUpdateManager;
    private final Activity activity;
    private InstallStateUpdatedListener installStateUpdatedListener;
    private final InternetConnectivityChecker connectivityChecker;
    private int retryAttempt = 0;
    private boolean isUpdateInProgress = false;
    private UpdateCallback updateCallback;

    /**
     * Interface for update callbacks
     */
    public interface UpdateCallback {
        void onUpdateAvailable(boolean isImmediateUpdate);
        void onUpdateNotAvailable();
        void onUpdateError(Exception error);
        void onDownloadProgress(long bytesDownloaded, long totalBytesToDownload);
    }

    /**
     * Create a new AppUpdateChecker
     * @param activity The activity
     */
    public AppUpdateChecker(@NonNull Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
        this.connectivityChecker = new InternetConnectivityChecker(activity);
        
        // Register the install state listener
        setupInstallStateListener();
    }
    
    public void setUpdateCallback(UpdateCallback callback) {
        this.updateCallback = callback;
    }
    
    /**
     * Check for updates with default settings (non-forced)
     */
    public void checkForUpdate() {
        checkForUpdate(false);
    }
    
    /**
     * Check for updates and show appropriate UI
     * @param forceUpdate If true, try to use immediate update
     */
    public void checkForUpdate(boolean forceUpdate) {
        if (!connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "No internet connection available, skipping update check");
            if (updateCallback != null) {
                updateCallback.onUpdateError(new Exception("No internet connection available"));
            }
            return;
        }
        
        Log.d(TAG, "Checking for app updates, forceUpdate: " + forceUpdate);
        
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            Log.d(TAG, "Update check success. Availability: " + appUpdateInfo.updateAvailability() +
                      ", Version code: " + appUpdateInfo.availableVersionCode() +
                      ", Staleness days: " + appUpdateInfo.clientVersionStalenessDays());
            
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Log.d(TAG, "Update already downloaded, showing completion snackbar");
                popupSnackbarForCompleteUpdate();
                return;
            }
            
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Integer stalenessDays = appUpdateInfo.clientVersionStalenessDays();
                boolean isStale = stalenessDays != null && 
                    (forceUpdate ? stalenessDays >= DAYS_FOR_IMMEDIATE_UPDATE 
                                : stalenessDays >= DAYS_FOR_FLEXIBLE_UPDATE);
                
                if ((forceUpdate || isStale) && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    Log.d(TAG, "Starting immediate update");
                    startImmediateUpdate(appUpdateInfo);
                    if (updateCallback != null) {
                        updateCallback.onUpdateAvailable(true);
                    }
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    Log.d(TAG, "Starting flexible update");
                    startFlexibleUpdate(appUpdateInfo);
                    if (updateCallback != null) {
                        updateCallback.onUpdateAvailable(false);
                    }
                }
            } else {
                Log.d(TAG, "No update available");
                if (updateCallback != null) {
                    updateCallback.onUpdateNotAvailable();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking for update: " + e.getMessage(), e);
            if (updateCallback != null) {
                updateCallback.onUpdateError(e);
            }
        });
    }
    
    /**
     * Retry update with exponential backoff
     */
    private void retryUpdate() {
        if (retryAttempt < MAX_RETRY_ATTEMPTS) {
            retryAttempt++;
            long delay = RETRY_DELAY_MS * (1L << (retryAttempt - 1));
            Log.d(TAG, String.format("Scheduling update retry attempt %d/%d in %d ms", 
                retryAttempt, MAX_RETRY_ATTEMPTS, delay));
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                checkForUpdate(false);
            }, delay);
        } else {
            Log.e(TAG, "Max retry attempts reached");
            retryAttempt = 0;
            if (updateCallback != null) {
                updateCallback.onUpdateError(new Exception("Max retry attempts reached"));
            }
        }
    }
    
    /**
     * Start an immediate update flow
     */
    private void startImmediateUpdate(AppUpdateInfo appUpdateInfo) {
        if (isUpdateInProgress) {
            Log.d(TAG, "Update already in progress");
            return;
        }
        
        try {
            isUpdateInProgress = true;
            Log.d(TAG, "Starting immediate update flow");
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                        .setAllowAssetPackDeletion(true)
                        .build(),
                    REQUEST_CODE_UPDATE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start immediate update flow: " + e.getMessage(), e);
            isUpdateInProgress = false;
            if (updateCallback != null) {
                updateCallback.onUpdateError(e);
            }
            retryUpdate();
        }
    }
    
    /**
     * Start a flexible update flow
     */
    private void startFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        if (isUpdateInProgress) {
            Log.d(TAG, "Update already in progress");
            return;
        }
        
        try {
            isUpdateInProgress = true;
            Log.d(TAG, "Starting flexible update flow");
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)
                        .setAllowAssetPackDeletion(true)
                        .build(),
                    REQUEST_CODE_UPDATE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start flexible update flow: " + e.getMessage(), e);
            isUpdateInProgress = false;
            if (updateCallback != null) {
                updateCallback.onUpdateError(e);
            }
            retryUpdate();
        }
    }
    
    /**
     * Show a snackbar to complete a downloaded update
     */
    private void popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            activity.getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(activity.getString(R.string.restart), view -> {
            appUpdateManager.completeUpdate();
        }).show();
    }
    
    /**
     * Clean up resources
     */
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (appUpdateManager != null && installStateUpdatedListener != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }
    
    /**
     * Handle update result
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_CODE_UPDATE) {
            isUpdateInProgress = false;
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: " + resultCode);
                retryUpdate();
            }
        }
    }

    /**
     * Check if an update is in progress
     */
    public boolean isUpdateInProgress() {
        return isUpdateInProgress;
    }
    
    public static int getRequestCode() {
        return REQUEST_CODE_UPDATE;
    }

    private void setupInstallStateListener() {
        installStateUpdatedListener = state -> {
            logInstallState(state);
            switch (state.installStatus()) {
                case InstallStatus.DOWNLOADING:
                    if (state.bytesDownloaded() > 0 && state.totalBytesToDownload() > 0) {
                        if (updateCallback != null) {
                            updateCallback.onDownloadProgress(state.bytesDownloaded(), state.totalBytesToDownload());
                        }
                    }
                    break;
                case InstallStatus.DOWNLOADED:
                    popupSnackbarForCompleteUpdate();
                    break;
                case InstallStatus.INSTALLED:
                    isUpdateInProgress = false;
                    if (appUpdateManager != null) {
                        appUpdateManager.unregisterListener(installStateUpdatedListener);
                    }
                    break;
                case InstallStatus.FAILED:
                    isUpdateInProgress = false;
                    Log.e(TAG, "Update failed! Error code: " + state.installErrorCode());
                    if (updateCallback != null) {
                        updateCallback.onUpdateError(new Exception("Update installation failed with code: " + state.installErrorCode()));
                    }
                    retryUpdate();
                    break;
                case InstallStatus.CANCELED:
                    isUpdateInProgress = false;
                    break;
            }
        };
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    private void logInstallState(InstallState state) {
        Log.d(TAG, "Install state updated: " + state.installStatus());
    }
} 