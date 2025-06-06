package com.ds.eventwish.ui.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.utils.AppUpdateHandler;

/**
 * View model for managing app updates
 */
public class AppUpdateViewModel implements AppUpdateHandler.UpdateCallback {
    private static final String TAG = "AppUpdateViewModel";
    private static AppUpdateViewModel instance;
    private final Context context;
    private AppUpdateHandler updateHandler;
    
    // LiveData for update states
    private final MutableLiveData<Boolean> isUpdateAvailable = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdateInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isImmediateUpdateRequired = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> downloadProgress = new MutableLiveData<>(0);

    private AppUpdateViewModel(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AppUpdateViewModel getInstance(Context context) {
        if (instance == null) {
            instance = new AppUpdateViewModel(context);
        }
        return instance;
    }

    /**
     * Initialize the update handler with an activity
     * @param activity The activity to use for updates
     */
    public void init(Activity activity) {
        if (updateHandler != null) {
            updateHandler.onDestroy(null);
        }
        updateHandler = new AppUpdateHandler(activity);
        updateHandler.setUpdateCallback(this);
    }

    /**
     * Check for app updates
     */
    public void checkForUpdates() {
        if (updateHandler != null) {
            isUpdateInProgress.setValue(true);
            updateHandler.checkForUpdate(false);
        } else {
            errorMessage.setValue("Update handler not initialized");
        }
    }

    /**
     * Check for app updates with force option
     * @param forceUpdate true to force an immediate update
     */
    public void checkForUpdates(boolean forceUpdate) {
        if (updateHandler != null) {
            isUpdateInProgress.setValue(true);
            updateHandler.checkForUpdate(forceUpdate);
        } else {
            errorMessage.setValue("Update handler not initialized");
        }
    }

    /**
     * Get the current app version
     * @return The current version name
     */
    public String getCurrentVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            return "Unknown";
        }
    }

    /**
     * Handle activity result for update flow
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (updateHandler != null) {
            updateHandler.onActivityResult(requestCode, resultCode);
        }
    }

    /**
     * Open the Play Store page for the app
     * @param activity The activity context
     */
    public void openPlayStore(Activity activity) {
        try {
            final String appPackageName = context.getPackageName();
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException e) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Play Store", e);
            errorMessage.setValue("Error opening Play Store: " + e.getMessage());
        }
    }

    /**
     * Clean up resources
     */
    public void onDestroy() {
        if (updateHandler != null) {
            updateHandler.onDestroy(null);
            updateHandler = null;
        }
    }

    @Override
    public void onUpdateAvailable(boolean isImmediateUpdate) {
        isUpdateAvailable.postValue(true);
        isImmediateUpdateRequired.postValue(isImmediateUpdate);
        isUpdateInProgress.postValue(true);
    }

    @Override
    public void onUpdateNotAvailable() {
        isUpdateAvailable.postValue(false);
        isUpdateInProgress.postValue(false);
        isImmediateUpdateRequired.postValue(false);
    }

    @Override
    public void onUpdateError(Exception error) {
        isUpdateInProgress.postValue(false);
        errorMessage.postValue(error.getMessage());
    }

    @Override
    public void onDownloadProgress(long bytesDownloaded, long totalBytesToDownload) {
        if (totalBytesToDownload <= 0) {
            downloadProgress.postValue(0);
            return;
        }
        int progress = (int) ((bytesDownloaded * 100) / totalBytesToDownload);
        downloadProgress.postValue(progress);
    }

    // LiveData getters
    public LiveData<Boolean> getIsUpdateAvailable() {
        return isUpdateAvailable;
    }

    public LiveData<Boolean> getIsUpdateInProgress() {
        return isUpdateInProgress;
    }

    public LiveData<Boolean> getIsImmediateUpdateRequired() {
        return isImmediateUpdateRequired;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Integer> getDownloadProgress() {
        return downloadProgress;
    }
} 