package com.ds.eventwish.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.ds.eventwish.util.SecureTokenManager;
import com.ds.eventwish.EventWishApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for device-related operations
 */
public class DeviceUtils {
    private static final String TAG = "DeviceUtils";
    private static DeviceUtils instance;
    private Context context;
    private String cachedDeviceId;
    
    private DeviceUtils(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.cachedDeviceId = getUniqueDeviceId(context);
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized DeviceUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DeviceUtils must be initialized first");
        }
        return instance;
    }

    /**
     * Initialize with application context
     */
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new DeviceUtils(context.getApplicationContext());
            Log.d(TAG, "DeviceUtils initialized");
        }
    }

    /**
     * Generate a unique device identifier based on hardware and software information.
     * This ID remains consistent across app installations but changes if the device
     * is reset or major hardware components change.
     */
    public static String getUniqueDeviceId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        
        // We combine Android ID with device-specific information
        String deviceInfo = Build.BOARD + Build.BRAND + Build.DEVICE + Build.HARDWARE
                + Build.MANUFACTURER + Build.MODEL + Build.PRODUCT;
        
        String combined = androidId + deviceInfo;
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error generating device ID", e);
            // Fallback to UUID if hashing fails
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Alias for getUniqueDeviceId for backward compatibility
     */
    public static String getDeviceId(Context context) {
        return getUniqueDeviceId(context);
    }

    /**
     * Regenerate device ID (for cases where the ID needs to be refreshed)
     */
    public static String regenerateDeviceId(Context context) {
        String newId = getUniqueDeviceId(context);
        try {
            SecureTokenManager tokenManager = SecureTokenManager.getInstance();
            if (tokenManager != null) {
                tokenManager.saveDeviceId(newId);
                Log.d(TAG, "Device ID regenerated and saved to SecureTokenManager");
            } else {
                Log.w(TAG, "SecureTokenManager is null, cannot save regenerated device ID");
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "SecureTokenManager not initialized yet, cannot save regenerated device ID: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error saving regenerated device ID", e);
        }
        return newId;
    }

    /**
     * Check if the device is rooted.
     */
    public static boolean isDeviceRooted() {
        // Check for common root indicators
        String[] rootIndicators = {
                "/system/app/Superuser.apk",
                "/system/xbin/su",
                "/system/bin/su",
                "/sbin/su",
                "/system/xbin/busybox",
                "/data/local/su",
                "/data/local/xbin/su"
        };
        
        for (String path : rootIndicators) {
            if (new File(path).exists()) {
                return true;
            }
        }
        
        // Try to run su command
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return bufferedReader.readLine() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the device is an emulator.
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.toLowerCase(Locale.ROOT).contains("droid4x")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.HARDWARE.contains("vbox86")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.BOARD.toLowerCase(Locale.ROOT).contains("nox")
                || Build.BOOTLOADER.toLowerCase(Locale.ROOT).contains("nox");
    }

    /**
     * Get the device model (e.g., "SM-G950F").
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * Get the app signature for verification.
     */
    public static String getAppSignature(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Error getting app signature", e);
        }
        
        return null;
    }

    /**
     * Get detailed information about the device.
     */
    public static Map<String, Object> getDetailedDeviceInfo(Context context) {
        Map<String, Object> deviceInfo = new HashMap<>();
        
        // Basic device information
        deviceInfo.put("deviceModel", Build.MODEL);
        deviceInfo.put("manufacturer", Build.MANUFACTURER);
        deviceInfo.put("brand", Build.BRAND);
        deviceInfo.put("device", Build.DEVICE);
        deviceInfo.put("product", Build.PRODUCT);
        deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
        deviceInfo.put("sdkVersion", Build.VERSION.SDK_INT);
        deviceInfo.put("buildID", Build.ID);
        deviceInfo.put("hardware", Build.HARDWARE);
        deviceInfo.put("fingerprint", Build.FINGERPRINT);
        deviceInfo.put("isEmulator", isEmulator());
        deviceInfo.put("isRooted", isDeviceRooted());
        deviceInfo.put("language", getDeviceLanguage());
        
        // App information
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            deviceInfo.put("appVersion", packageInfo.versionName);
            deviceInfo.put("appVersionCode", packageInfo.versionCode);
            deviceInfo.put("appPackage", packageInfo.packageName);
            deviceInfo.put("firstInstallTime", packageInfo.firstInstallTime);
            deviceInfo.put("lastUpdateTime", packageInfo.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
        }
        
        // Device ID (secure hashed identifier)
        deviceInfo.put("deviceId", getUniqueDeviceId(context));
        
        return deviceInfo;
    }

    /**
     * Get the device language.
     */
    public static String getDeviceLanguage() {
        return Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
    }

    /**
     * Get the app installation source.
     */
    public static String getAppInstallationSource(Context context) {
        try {
            String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            if (installer == null) {
                return "unknown";
            }
            if (installer.contains("google")) {
                return "google_play";
            } else if (installer.contains("amazon")) {
                return "amazon";
            } else if (installer.contains("huawei")) {
                return "huawei_app_gallery";
            } else if (installer.contains("samsung")) {
                return "samsung_galaxy_store";
            } else if (installer.contains("xiaomi")) {
                return "mi_store";
            } else {
                return installer;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting installation source", e);
            return "unknown";
        }
    }

    /**
     * Verify the integrity of the device ID to detect potential spoofing.
     */
    public static boolean verifyDeviceIdIntegrity(Context context) {
        try {
            // Get the current device ID
            String currentDeviceId = getUniqueDeviceId(context);
            
            // Get the stored device ID from secure storage
            String storedDeviceId = null;
            try {
                SecureTokenManager tokenManager = SecureTokenManager.getInstance();
                if (tokenManager != null) {
                    storedDeviceId = tokenManager.getDeviceId();
                    
                    if (storedDeviceId == null || storedDeviceId.isEmpty()) {
                        // First run, store the current ID
                        tokenManager.saveDeviceId(currentDeviceId);
                        return true;
                    }
                    
                    // Compare the stored ID with the current one
                    return storedDeviceId.equals(currentDeviceId);
                } else {
                    Log.w(TAG, "SecureTokenManager is null, cannot verify device ID integrity");
                    return true; // Assume valid if we can't verify
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "SecureTokenManager not initialized yet, cannot verify device ID integrity: " + e.getMessage());
                return true; // Assume valid if SecureTokenManager is not initialized
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying device ID integrity", e);
            return false;
        }
    }

    /**
     * Convert a Map to a JSONObject
     */
    public static JSONObject mapToJson(Map<String, Object> map) {
        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error converting map to JSON", e);
        }
        return json;
    }

    /**
     * Get device ID without needing to pass context (for singleton instance)
     */
    public String getDeviceId() {
        if (instance == null) {
            Log.e(TAG, "DeviceUtils instance is null, cannot get device ID");
            return null;
        }
        
        // If we have a stored device ID, use it
        if (cachedDeviceId != null && !cachedDeviceId.isEmpty()) {
            return cachedDeviceId;
        }
        
        // Otherwise, generate a new one
        return getUniqueDeviceId(context);
    }

    /**
     * Get the device brand
     * @return Device brand
     */
    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    /**
     * Get the Android version
     * @return Android version
     */
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Get the app version name
     * @return App version name
     */
    public static String getAppVersionName() {
        try {
            Context context = EventWishApplication.getAppContext();
            if (context != null) {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return packageInfo.versionName;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version name", e);
        }
        return "unknown";
    }

    /**
     * Get the app version code
     * @return App version code
     */
    public static int getAppVersionCode() {
        try {
            Context context = EventWishApplication.getAppContext();
            if (context != null) {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    return (int) packageInfo.getLongVersionCode();
                } else {
                    return packageInfo.versionCode;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version code", e);
        }
        return 0;
    }
} 