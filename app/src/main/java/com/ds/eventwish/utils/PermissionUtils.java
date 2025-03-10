package com.ds.eventwish.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Utility class for handling permissions with proper Android version checking
 */
public class PermissionUtils {
    private static final String TAG = "PermissionUtils";

    /**
     * Check if notification permission is granted
     * @param context The context
     * @return true if permission is granted or not needed (Android < 13)
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                == PackageManager.PERMISSION_GRANTED;
        }
        // Permission not needed for Android < 13
        return true;
    }

    /**
     * Request notification permission
     * @param activity The activity
     * @param launcher The permission launcher
     */
    public static void requestNotificationPermission(Activity activity, ActivityResultLauncher<String> launcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                // Show rationale dialog
                showNotificationPermissionRationale(activity, launcher);
            } else {
                // Request permission directly
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Show rationale dialog for notification permission
     */
    private static void showNotificationPermissionRationale(Activity activity, ActivityResultLauncher<String> launcher) {
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Notification Permission")
            .setMessage("EventWish needs notification permission to alert you about upcoming events and reminders.")
            .setPositiveButton("Grant Permission", (dialog, which) -> 
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS))
            .setNegativeButton("Not Now", null)
            .show();
    }

    /**
     * Show dialog when notification permission is denied
     */
    public static void showNotificationPermissionDeniedDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Notification Permission Required")
            .setMessage("EventWish needs notification permission to alert you about your reminders. Please grant this permission in Settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Check if exact alarm permission is granted
     * @param context The context
     * @return true if permission is granted or not needed (Android < 12)
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        // Permission not needed for Android < 12
        return true;
    }

    /**
     * Show dialog to request exact alarm permission
     */
    public static void showExactAlarmPermissionDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Alarm Permission Required")
            .setMessage("EventWish needs permission to schedule exact alarms for your reminders. Please grant this permission in Settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    activity.startActivity(intent);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
} 