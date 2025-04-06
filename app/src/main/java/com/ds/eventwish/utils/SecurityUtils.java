package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;

/**
 * Utility class for security-related functions
 */
public class SecurityUtils {
    private static final String TAG = "SecurityUtils";

    /**
     * Get the device ID for API authentication
     * @param context Application context
     * @return Device ID
     */
    public static String getDeviceId(Context context) {
        Log.d(TAG, "Getting device ID for API authentication");
        return DeviceUtils.getDeviceId(context);
    }
} 