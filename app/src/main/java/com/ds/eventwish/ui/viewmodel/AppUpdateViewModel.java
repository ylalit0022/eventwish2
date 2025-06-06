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

import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.ui.dialogs.UpdateDialogHelper;
import com.ds.eventwish.utils.AppUpdateHandler;
import com.ds.eventwish.utils.RemoteConfigManager;

/**
 * View model for managing app updates
 */
public class AppUpdateViewModel implements AppUpdateHandler.UpdateCallback {
    private static final String TAG = "AppUpdateViewModel";
    private static AppUpdateViewModel instance;
    private final Context context;
    private AppUpdateHandler updateHandler;
    private RemoteConfigManager remoteConfigManager;
    
    // LiveData for update states
    private final MutableLiveData<Boolean> isUpdateAvailable = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdateInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isImmediateUpdateRequired = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> downloadProgress = new MutableLiveData<>(0);

    private AppUpdateViewModel(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize RemoteConfigManager
        try {
            this.remoteConfigManager = RemoteConfigManager.getInstance(context);
            Log.d(TAG, "RemoteConfigManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize RemoteConfigManager", e);
        }
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
     * Check for updates silently without showing the dialog
     * Only updates the isUpdateAvailable LiveData
     */
    public void checkForUpdatesSilently() {
        if (updateHandler != null) {
            updateHandler.checkForUpdateSilently();
        } else {
            errorMessage.setValue("Update handler not initialized");
        }
    }
    
    /**
     * Check for updates using Firebase Remote Config
     * This works in both development and production environments
     */
    public void checkForUpdatesWithRemoteConfig() {
        if (remoteConfigManager == null) {
            Log.e(TAG, "RemoteConfigManager not initialized");
            errorMessage.setValue("RemoteConfigManager not initialized");
            return;
        }
        
        Log.d(TAG, "Starting Remote Config update check");
        isUpdateInProgress.setValue(true);
        
        remoteConfigManager.fetchAndActivate()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Remote Config fetch and activate successful");
                    remoteConfigManager.checkForUpdates(new RemoteConfigManager.UpdateCheckCallback() {
                        @Override
                        public void onUpdateAvailable(boolean isForceUpdate, String versionName, String updateMessage) {
                            Log.d(TAG, "Remote Config update available: " + versionName + ", force: " + isForceUpdate);
                            isUpdateAvailable.postValue(true);
                            isImmediateUpdateRequired.postValue(isForceUpdate);
                            isUpdateInProgress.postValue(false);
                            
                            // Show update dialog if needed
                            if (updateHandler != null && updateHandler.getActivity() != null) {
                                Log.d(TAG, "Showing update dialog");
                                UpdateDialogHelper.showUpdateDialog(
                                    updateHandler.getActivity(), 
                                    versionName, 
                                    updateMessage, 
                                    isForceUpdate
                                );
                            } else {
                                Log.e(TAG, "Cannot show update dialog - updateHandler or activity is null");
                                if (updateHandler == null) {
                                    Log.e(TAG, "updateHandler is null");
                                } else {
                                    Log.e(TAG, "activity is null");
                                }
                            }
                        }

                        @Override
                        public void onUpdateNotAvailable() {
                            Log.d(TAG, "Remote Config update not available");
                            isUpdateAvailable.postValue(false);
                            isUpdateInProgress.postValue(false);
                            errorMessage.postValue("No update available");
                        }

                        @Override
                        public void onError(Exception exception) {
                            Log.e(TAG, "Remote Config update check error: " + exception.getMessage(), exception);
                            errorMessage.postValue("Error checking for updates: " + exception.getMessage());
                            isUpdateInProgress.postValue(false);
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to fetch remote config", task.getException());
                    errorMessage.postValue("Failed to fetch remote config");
                    isUpdateInProgress.postValue(false);
                }
            });
    }
    
    /**
     * Check for updates silently using Firebase Remote Config
     * Only updates the isUpdateAvailable LiveData
     */
    public void checkForUpdatesWithRemoteConfigSilently() {
        if (remoteConfigManager == null) {
            Log.e(TAG, "RemoteConfigManager not initialized");
            return;
        }
        
        remoteConfigManager.fetchAndActivate()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    remoteConfigManager.checkForUpdates(null);
                }
            });
    }

    /**
     * Force check for updates using Firebase Remote Config
     * This will bypass the cache and fetch the latest values from the server
     */
    public void forceCheckForUpdatesWithRemoteConfig() {
        if (remoteConfigManager == null) {
            Log.e(TAG, "RemoteConfigManager not initialized");
            errorMessage.setValue("RemoteConfigManager not initialized");
            return;
        }
        
        Log.d(TAG, "Starting forced Remote Config update check");
        isUpdateInProgress.setValue(true);
        
        remoteConfigManager.forceFetchAndActivate()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Remote Config force fetch and activate successful");
                    remoteConfigManager.checkForUpdates(new RemoteConfigManager.UpdateCheckCallback() {
                        @Override
                        public void onUpdateAvailable(boolean isForceUpdate, String versionName, String updateMessage) {
                            Log.d(TAG, "Remote Config update available: " + versionName + ", force: " + isForceUpdate);
                            isUpdateAvailable.postValue(true);
                            isImmediateUpdateRequired.postValue(isForceUpdate);
                            isUpdateInProgress.postValue(false);
                            
                            // Show update dialog if needed
                            if (updateHandler != null && updateHandler.getActivity() != null) {
                                Log.d(TAG, "Showing update dialog");
                                UpdateDialogHelper.showUpdateDialog(
                                    updateHandler.getActivity(), 
                                    versionName, 
                                    updateMessage, 
                                    isForceUpdate
                                );
                            } else {
                                Log.e(TAG, "Cannot show update dialog - updateHandler or activity is null");
                            }
                        }

                        @Override
                        public void onUpdateNotAvailable() {
                            Log.d(TAG, "Remote Config update not available");
                            isUpdateAvailable.postValue(false);
                            isUpdateInProgress.postValue(false);
                            errorMessage.postValue("No update available");
                        }

                        @Override
                        public void onError(Exception exception) {
                            Log.e(TAG, "Remote Config update check error: " + exception.getMessage(), exception);
                            errorMessage.postValue("Error checking for updates: " + exception.getMessage());
                            isUpdateInProgress.postValue(false);
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to force fetch remote config", task.getException());
                    errorMessage.postValue("Failed to fetch remote config");
                    isUpdateInProgress.postValue(false);
                }
            });
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
    
    /**
     * Get the RemoteConfigManager instance
     * @return The RemoteConfigManager instance
     */
    public RemoteConfigManager getRemoteConfigManager() {
        return remoteConfigManager;
    }
} 