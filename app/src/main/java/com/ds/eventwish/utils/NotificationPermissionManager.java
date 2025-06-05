package com.ds.eventwish.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ds.eventwish.R;

/**
 * Utility class to manage notification permissions for Android 13+ (API level 33+)
 */
public class NotificationPermissionManager {
    private static final String TAG = "NotificationPermMgr";
    private static final String PREF_NAME = "notification_permission_prefs";
    private static final String PREF_PERMISSION_REQUESTED = "notification_permission_requested";
    private static final String PREF_PERMISSION_DENIED_COUNT = "notification_permission_denied_count";
    private static final int MAX_PERMISSION_REQUESTS = 2; // Maximum number of times to ask for permission

    /**
     * Check if notification permission is granted
     * @param context The context
     * @return true if permission is granted, false otherwise
     */
    public static boolean hasNotificationPermission(@NonNull Context context) {
        // For Android 13+ (API level 33+), check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        // For older versions, notification permission is granted by default
        return true;
    }

    /**
     * Request notification permission for Android 13+ (API level 33+)
     * @param activity The activity
     * @return true if permission is already granted or requested, false if cannot request
     */
    public static boolean requestNotificationPermission(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission(activity)) {
                Log.d(TAG, "Notification permission already granted");
                return true;
            }

            SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            int deniedCount = prefs.getInt(PREF_PERMISSION_DENIED_COUNT, 0);

            // If user has denied permission too many times, show settings dialog
            if (deniedCount >= MAX_PERMISSION_REQUESTS) {
                Log.d(TAG, "Notification permission denied too many times, showing settings dialog");
                showSettingsDialog(activity);
                return false;
            }

            // Request permission
            Log.d(TAG, "Requesting notification permission");
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100); // Request code doesn't matter for this API

            // Mark that we've requested permission
            prefs.edit().putBoolean(PREF_PERMISSION_REQUESTED, true).apply();
            return true;
        }
        // For older versions, notification permission is granted by default
        return true;
    }

    /**
     * Request notification permission for Android 13+ (API level 33+) using the new Activity Result API
     * @param fragment The fragment
     * @param permissionLauncher The permission launcher
     * @return true if permission is already granted or requested, false if cannot request
     */
    public static boolean requestNotificationPermission(
            @NonNull Fragment fragment,
            @NonNull ActivityResultLauncher<String> permissionLauncher) {
        
        Context context = fragment.requireContext();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission(context)) {
                Log.d(TAG, "Notification permission already granted");
                return true;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            int deniedCount = prefs.getInt(PREF_PERMISSION_DENIED_COUNT, 0);

            // If user has denied permission too many times, show settings dialog
            if (deniedCount >= MAX_PERMISSION_REQUESTS) {
                Log.d(TAG, "Notification permission denied too many times, showing settings dialog");
                showSettingsDialog(context);
                return false;
            }

            // Request permission using the launcher
            Log.d(TAG, "Requesting notification permission");
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);

            // Mark that we've requested permission
            prefs.edit().putBoolean(PREF_PERMISSION_REQUESTED, true).apply();
            return true;
        }
        // For older versions, notification permission is granted by default
        return true;
    }

    /**
     * Handle permission result
     * @param context The context
     * @param permissions The permissions
     * @param grantResults The grant results
     */
    public static void handlePermissionResult(
            @NonNull Context context,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i])) {
                    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        // Permission granted, reset denied count
                        Log.d(TAG, "Notification permission granted");
                        prefs.edit()
                                .putInt(PREF_PERMISSION_DENIED_COUNT, 0)
                                .apply();
                    } else {
                        // Permission denied, increment denied count
                        Log.d(TAG, "Notification permission denied");
                        int deniedCount = prefs.getInt(PREF_PERMISSION_DENIED_COUNT, 0) + 1;
                        prefs.edit()
                                .putInt(PREF_PERMISSION_DENIED_COUNT, deniedCount)
                                .apply();
                        
                        // If denied too many times, show settings dialog
                        if (deniedCount >= MAX_PERMISSION_REQUESTS) {
                            showSettingsDialog(context);
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Show dialog to direct user to app settings
     * @param context The context
     */
    private static void showSettingsDialog(@NonNull Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_disabled_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    /**
     * Reset permission request state (for testing)
     * @param context The context
     */
    public static void resetPermissionState(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_PERMISSION_REQUESTED, false)
                .putInt(PREF_PERMISSION_DENIED_COUNT, 0)
                .apply();
    }

    /**
     * Request notification permission from any context
     * This will start an activity to request permission if on Android 13+
     * @param context The context
     */
    public static void requestNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission(context)) {
                Log.d(TAG, "Notification permission already granted");
                return;
            }

            try {
                // If context is an activity, use the activity version
                if (context instanceof Activity) {
                    requestNotificationPermission((Activity) context);
                    return;
                }

                // Otherwise, try to start an activity to request permission
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                int deniedCount = prefs.getInt(PREF_PERMISSION_DENIED_COUNT, 0);

                // If user has denied permission too many times, we can't do much from a non-activity context
                if (deniedCount >= MAX_PERMISSION_REQUESTS) {
                    Log.d(TAG, "Notification permission denied too many times from non-activity context");
                    return;
                }

                // Try to start main activity to request permission
                Intent intent = new Intent(context, com.ds.eventwish.ui.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("REQUEST_NOTIFICATION_PERMISSION", true);
                context.startActivity(intent);

                // Mark that we've requested permission
                prefs.edit().putBoolean(PREF_PERMISSION_REQUESTED, true).apply();
            } catch (Exception e) {
                Log.e(TAG, "Error requesting notification permission: " + e.getMessage(), e);
            }
        }
    }
} 