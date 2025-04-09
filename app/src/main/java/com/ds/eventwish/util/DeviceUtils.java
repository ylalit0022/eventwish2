package com.ds.eventwish.util;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class DeviceUtils {
    private static final String TAG = "DeviceUtils";

    public static String getDeviceId(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID", e);
            return null;
        }
    }
} 