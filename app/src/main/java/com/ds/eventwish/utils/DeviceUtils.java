package com.ds.eventwish.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.ds.eventwish.data.repository.CoinsRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for device-related functions
 */
public class DeviceUtils {
    private static final String TAG = "DeviceUtils";
    private static final String DEVICE_ID_PREFS = "device_id_secure_prefs";
    private static final String DEVICE_ID_KEY = "secure_device_id";
    private static final String DEVICE_FINGERPRINT_KEY = "device_fingerprint";
    private static final String DEVICE_SECURITY_SALT = "device_security_salt";
    private static String cachedDeviceId = null;
    
    /**
     * Get a unique device ID for this installation
     * @param context Application context
     * @return A unique device ID
     */
    public static String getDeviceId(Context context) {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }
        
        try {
            // Try to get from secure storage first
            SharedPreferences securePrefs = context.getSharedPreferences(DEVICE_ID_PREFS, Context.MODE_PRIVATE);
            String savedDeviceId = securePrefs.getString(DEVICE_ID_KEY, null);
            String savedFingerprint = securePrefs.getString(DEVICE_FINGERPRINT_KEY, null);
            
            // If we have a saved device ID and the device fingerprint matches, use it
            if (savedDeviceId != null && savedFingerprint != null) {
                String currentFingerprint = generateDeviceFingerprint(context);
                if (savedFingerprint.equals(currentFingerprint)) {
                    cachedDeviceId = savedDeviceId;
                    return savedDeviceId;
                } else {
                    Log.w(TAG, "Device fingerprint changed, generating new device ID");
                    // Fingerprint mismatch could indicate tampering or device change
                }
            }
            
            // Generate a new ID with multiple factors
            String newDeviceId = generateSecureDeviceId(context);
            String fingerprint = generateDeviceFingerprint(context);
            
            // Save to secure storage
            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putString(DEVICE_ID_KEY, newDeviceId);
            editor.putString(DEVICE_FINGERPRINT_KEY, fingerprint);
            editor.apply();
            
            cachedDeviceId = newDeviceId;
            return newDeviceId;
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID", e);
            // Fallback to random UUID if there's an issue
            String fallbackId = UUID.randomUUID().toString();
            cachedDeviceId = fallbackId;
            return fallbackId;
        }
    }
    
    /**
     * Generates a secure unique device ID based on multiple device factors
     */
    private static String generateSecureDeviceId(Context context) throws Exception {
        // Get basic device information
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        
        // If we don't have a valid Android ID, use other factors
        if (androidId == null || androidId.isEmpty() || "9774d56d682e549c".equals(androidId)) {
            androidId = UUID.randomUUID().toString();
        }
        
        // Generate or retrieve a persistent salt
        SharedPreferences securePrefs = context.getSharedPreferences(DEVICE_ID_PREFS, Context.MODE_PRIVATE);
        String salt = securePrefs.getString(DEVICE_SECURITY_SALT, null);
        if (salt == null) {
            // Generate new salt
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
            
            // Save the salt
            securePrefs.edit().putString(DEVICE_SECURITY_SALT, salt).apply();
        }
        
        // Combine factors
        StringBuilder builder = new StringBuilder();
        builder.append(androidId)
               .append(":")
               .append(Build.FINGERPRINT)
               .append(":")
               .append(context.getPackageName())
               .append(":")
               .append(Build.SERIAL)
               .append(":")
               .append(salt);
               
        // Hash it
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
        
        // Format as a UUID-like string for consistency
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < Math.min(hash.length, 16); i++) {
            formatted.append(String.format("%02x", hash[i] & 0xff));
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                formatted.append("-");
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Generate a device fingerprint for tamper detection
     */
    private static String generateDeviceFingerprint(Context context) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append(Build.MANUFACTURER)
               .append(":")
               .append(Build.MODEL)
               .append(":")
               .append(Build.DEVICE)
               .append(":")
               .append(Build.FINGERPRINT);
        
        // Add installed app signatures
        try {
            android.content.pm.Signature[] sigs = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            for (android.content.pm.Signature sig : sigs) {
                builder.append(":").append(sig.hashCode());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app signatures", e);
        }
        
        // Hash it
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    /**
     * Verify the integrity of the device ID
     * @param context Application context
     * @return True if the device ID is valid
     */
    public static boolean verifyDeviceIdIntegrity(Context context) {
        try {
            SharedPreferences securePrefs = context.getSharedPreferences(DEVICE_ID_PREFS, Context.MODE_PRIVATE);
            String savedDeviceId = securePrefs.getString(DEVICE_ID_KEY, null);
            String savedFingerprint = securePrefs.getString(DEVICE_FINGERPRINT_KEY, null);
            
            if (savedDeviceId == null || savedFingerprint == null) {
                return false;
            }
            
            String currentFingerprint = generateDeviceFingerprint(context);
            return savedFingerprint.equals(currentFingerprint);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying device ID integrity", e);
            return false;
        }
    }
    
    /**
     * Checks if the device is likely rooted
     */
    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }
    
    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }
    
    private static boolean checkRootMethod2() {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su",
                "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su",
                "/su/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
    
    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
    
    /**
     * Checks if the device is an emulator
     */
    public static boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT));
    }
    
    /**
     * Checks if the device has Google Play Services installed
     * @param context Application context
     * @return True if Google Play Services is available
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        try {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
            return resultCode == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Play Services availability", e);
            return false;
        }
    }
    
    /**
     * Gets the device manufacturer and model for logging
     * @return A string with manufacturer and model
     */
    public static String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }
    
    /**
     * Get detailed device information for security reporting
     * @return Map containing device details
     */
    public static Map<String, Object> getDetailedDeviceInfo(Context context) {
        Map<String, Object> deviceInfo = new HashMap<>();
        try {
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("brand", Build.BRAND);
            deviceInfo.put("device", Build.DEVICE);
            deviceInfo.put("product", Build.PRODUCT);
            deviceInfo.put("hardware", Build.HARDWARE);
            deviceInfo.put("sdkInt", Build.VERSION.SDK_INT);
            deviceInfo.put("releaseVersion", Build.VERSION.RELEASE);
            deviceInfo.put("fingerprint", Build.FINGERPRINT);
            deviceInfo.put("bootloader", Build.BOOTLOADER);
            deviceInfo.put("cpuAbi", Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0 ? 
                Build.SUPPORTED_ABIS[0] : "unknown");
            
            // Add memory info
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (actManager != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                actManager.getMemoryInfo(memInfo);
                deviceInfo.put("totalMemory", memInfo.totalMem);
                deviceInfo.put("availableMemory", memInfo.availMem);
            }
            
            // Add some system properties
            deviceInfo.put("kernelVersion", System.getProperty("os.version"));
            
            // Add app info
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                deviceInfo.put("appVersion", pInfo.versionName);
                deviceInfo.put("appVersionCode", pInfo.versionCode);
            } catch (Exception e) {
                Log.e(TAG, "Error getting package info", e);
            }
            
            // Add security checks
            deviceInfo.put("isRooted", isDeviceRooted());
            deviceInfo.put("isEmulator", isEmulator());
            
            // Add some network info
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    deviceInfo.put("networkType", activeNetwork.getTypeName());
                    deviceInfo.put("networkSubtype", activeNetwork.getSubtypeName());
                    deviceInfo.put("isConnected", activeNetwork.isConnected());
                }
            }
            
            // Add locale info
            deviceInfo.put("locale", Locale.getDefault().toString());
            deviceInfo.put("language", Locale.getDefault().getLanguage());
            deviceInfo.put("country", Locale.getDefault().getCountry());
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting device info", e);
            deviceInfo.put("error", e.getMessage());
        }
        
        return deviceInfo;
    }
}