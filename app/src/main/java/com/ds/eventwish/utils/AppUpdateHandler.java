package com.ds.eventwish.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
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
import com.ds.eventwish.R;
import com.ds.eventwish.ui.connectivity.InternetConnectivityChecker;

/**
 * Handles app updates using the Google Play Core library.
 * Supports both flexible and immediate updates.
 */
public class AppUpdateHandler implements DefaultLifecycleObserver {
    private static final String TAG = "AppUpdateManager";
    private static final int REQUEST_CODE_UPDATE = 500;
    private static final int DAYS_FOR_FLEXIBLE_UPDATE = 3;
    private static final int DAYS_FOR_IMMEDIATE_UPDATE = 7;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds

    private final com.google.android.play.core.appupdate.AppUpdateManager appUpdateManager;
    private final Activity activity;
    private final InternetConnectivityChecker connectivityChecker;
    private InstallStateUpdatedListener installStateUpdatedListener;
    private UpdateCallback updateCallback;
    private int retryAttempt = 0;
    private boolean isUpdateInProgress = false;

    public interface UpdateCallback {
        void onUpdateAvailable(boolean isImmediateUpdate);
        void onUpdateNotAvailable();
        void onUpdateError(Exception error);
        void onDownloadProgress(long bytesDownloaded, long totalBytesToDownload);
    }

    public AppUpdateHandler(@NonNull Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
        this.connectivityChecker = InternetConnectivityChecker.getInstance(activity);
        setupInstallStateListener();
    }

    private void setupInstallStateListener() {
        installStateUpdatedListener = state -> {
            logInstallState(state);
            switch (state.installStatus()) {
                case InstallStatus.DOWNLOADING:
                    handleDownloadingState(state);
                    break;
                case InstallStatus.DOWNLOADED:
                    handleDownloadedState();
                    break;
                case InstallStatus.INSTALLED:
                    handleInstalledState();
                    break;
                case InstallStatus.FAILED:
                    handleFailedState(state);
                    break;
                case InstallStatus.CANCELED:
                    handleCanceledState();
                    break;
            }
        };
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    private void logInstallState(InstallState state) {
        Log.d(TAG, String.format("Install State: %d, Error Code: %d", 
            state.installStatus(), state.installErrorCode()));
    }

    private void handleDownloadingState(InstallState state) {
        if (state.totalBytesToDownload() <= 0) {
            Log.w(TAG, "Invalid total bytes in download state");
            return;
        }

        long progress = (state.bytesDownloaded() * 100) / state.totalBytesToDownload();
        Log.d(TAG, "Download progress: " + progress + "%");
        
        if (updateCallback != null) {
            updateCallback.onDownloadProgress(state.bytesDownloaded(), state.totalBytesToDownload());
        }
    }

    private void handleDownloadedState() {
        Log.d(TAG, "Update downloaded successfully");
        showCompleteUpdateSnackbar();
    }

    private void handleInstalledState() {
        Log.d(TAG, "Update installed successfully");
        isUpdateInProgress = false;
        cleanupInstallListener();
    }

    private void handleFailedState(InstallState state) {
        Log.e(TAG, "Update failed with code: " + state.installErrorCode());
        isUpdateInProgress = false;
        if (updateCallback != null) {
            updateCallback.onUpdateError(
                new Exception("Installation failed with code: " + state.installErrorCode())
            );
        }
        retryUpdate();
    }

    private void handleCanceledState() {
        Log.d(TAG, "Update canceled by user");
        isUpdateInProgress = false;
    }

    public void checkForUpdate() {
        checkForUpdate(false);
    }

    public void checkForUpdate(boolean forceUpdate) {
        if (!validatePreConditions()) {
            return;
        }

        Log.d(TAG, "Checking for updates (forced: " + forceUpdate + ")");
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            processUpdateInfo(appUpdateInfo, forceUpdate);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Update check failed: " + e.getMessage(), e);
            if (updateCallback != null) {
                updateCallback.onUpdateError(e);
            }
        });
    }

    private boolean validatePreConditions() {
        if (!connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "No network connection available");
            if (updateCallback != null) {
                updateCallback.onUpdateError(new Exception("No network connection"));
            }
            return false;
        }

        if (isUpdateInProgress) {
            Log.d(TAG, "Update already in progress");
            return false;
        }

        return true;
    }

    private void processUpdateInfo(AppUpdateInfo appUpdateInfo, boolean forceUpdate) {
        Log.d(TAG, String.format("Update info - Availability: %d, Version code: %d, Staleness: %d",
            appUpdateInfo.updateAvailability(),
            appUpdateInfo.availableVersionCode(),
            appUpdateInfo.clientVersionStalenessDays() != null ? 
                appUpdateInfo.clientVersionStalenessDays() : -1));

        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            showCompleteUpdateSnackbar();
            return;
        }

        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            handleAvailableUpdate(appUpdateInfo, forceUpdate);
        } else {
            Log.d(TAG, "No update available");
            if (updateCallback != null) {
                updateCallback.onUpdateNotAvailable();
            }
        }
    }

    private void handleAvailableUpdate(AppUpdateInfo appUpdateInfo, boolean forceUpdate) {
        Integer stalenessDays = appUpdateInfo.clientVersionStalenessDays();
        boolean isStale = stalenessDays != null && 
            (forceUpdate ? stalenessDays >= DAYS_FOR_IMMEDIATE_UPDATE 
                        : stalenessDays >= DAYS_FOR_FLEXIBLE_UPDATE);

        if ((forceUpdate || isStale) && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            startImmediateUpdate(appUpdateInfo);
        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            startFlexibleUpdate(appUpdateInfo);
        }
    }

    private void startImmediateUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            isUpdateInProgress = true;
            Log.d(TAG, "Starting immediate update");
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                    .setAllowAssetPackDeletion(true)
                    .build(),
                REQUEST_CODE_UPDATE
            );
            if (updateCallback != null) {
                updateCallback.onUpdateAvailable(true);
            }
        } catch (Exception e) {
            handleUpdateError("Failed to start immediate update", e);
        }
    }

    private void startFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            isUpdateInProgress = true;
            Log.d(TAG, "Starting flexible update");
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)
                    .setAllowAssetPackDeletion(true)
                    .build(),
                REQUEST_CODE_UPDATE
            );
            if (updateCallback != null) {
                updateCallback.onUpdateAvailable(false);
            }
        } catch (Exception e) {
            handleUpdateError("Failed to start flexible update", e);
        }
    }

    private void handleUpdateError(String message, Exception e) {
        Log.e(TAG, message + ": " + e.getMessage(), e);
        isUpdateInProgress = false;
        if (updateCallback != null) {
            updateCallback.onUpdateError(e);
        }
        retryUpdate();
    }

    private void retryUpdate() {
        if (retryAttempt < MAX_RETRY_ATTEMPTS) {
            retryAttempt++;
            long delay = RETRY_DELAY_MS * (1L << (retryAttempt - 1));
            Log.d(TAG, String.format("Scheduling retry %d/%d in %d ms", 
                retryAttempt, MAX_RETRY_ATTEMPTS, delay));
            
            new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> checkForUpdate(false), delay);
        } else {
            Log.e(TAG, "Max retry attempts reached");
            retryAttempt = 0;
            if (updateCallback != null) {
                updateCallback.onUpdateError(new Exception("Max retry attempts reached"));
            }
        }
    }

    private void showCompleteUpdateSnackbar() {
        if (activity.isFinishing()) return;
        
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            R.string.update_downloaded,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.restart, view -> {
            appUpdateManager.completeUpdate();
        }).show();
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_CODE_UPDATE) {
            isUpdateInProgress = false;
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: " + resultCode);
                retryUpdate();
            }
        }
    }

    public void setUpdateCallback(UpdateCallback callback) {
        this.updateCallback = callback;
    }

    public boolean isUpdateInProgress() {
        return isUpdateInProgress;
    }

    public static int getRequestCode() {
        return REQUEST_CODE_UPDATE;
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        cleanupInstallListener();
    }

    private void cleanupInstallListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }
} 