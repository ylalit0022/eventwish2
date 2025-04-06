package com.ds.eventwish.ui.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

/**
 * Utility class to manage app updates (view model version)
 */
public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private static AppUpdateManager instance;
    private final Context context;
    private UpdateListener updateListener;

    public interface UpdateListener {
        void onUpdateAvailable(boolean isUpdateAvailable);
    }

    private AppUpdateManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AppUpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppUpdateManager(context);
        }
        return instance;
    }

    public void checkForUpdate() {
        // Stub implementation for checking updates
        Log.d(TAG, "Checking for app updates");
    }

    public void resumeUpdateIfNeeded() {
        // Stub implementation for resuming updates
        Log.d(TAG, "Resuming updates if needed");
    }

    public String getCurrentVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            return "Unknown";
        }
    }

    public void registerListener(Activity activity) {
        // Updated to accept Activity instead of UpdateListener
        Log.d(TAG, "Registered listener: " + activity);
    }

    public void unregisterListener() {
        this.updateListener = null;
    }

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
        }
    }
} 