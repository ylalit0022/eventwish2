package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.data.remote.ApiClient;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for API environment information and debugging
 */
public class ApiEnvironmentHelper {
    private static final String TAG = "ApiEnvironmentHelper";
    private static final String PREFS_NAME = "api_environment_prefs";
    private static final String KEY_OVERRIDE_ENV = "override_environment";

    /**
     * Show API environment information as a toast message
     * @param context Context
     */
    public static void showApiEnvironmentInfo(Context context) {
        if (context == null) return;
        
        String baseUrl = ApiClient.getBaseUrl();
        String apiEnv = ApiClient.getApiEnvironment();
        boolean isLocal = ApiClient.isLocalEnvironment();
        
        String deviceIp = getDeviceIpAddress(context);
        String message = String.format("API: %s\nURL: %s\nDevice IP: %s", 
                apiEnv, baseUrl, deviceIp);
        
        // Show a longer toast for better visibility
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
        
        // Log the information as well
        Log.i(TAG, "API Environment: " + apiEnv);
        Log.i(TAG, "Base URL: " + baseUrl);
        Log.i(TAG, "Device IP: " + deviceIp);
        Log.i(TAG, "Is Local Environment: " + isLocal);
    }
    
    /**
     * Get the device's IP address
     * @param context Context
     * @return IP address as string
     */
    public static String getDeviceIpAddress(Context context) {
        try {
            // Try to get WiFi IP address first
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                if (ipAddress != 0) {
                    return Formatter.formatIpAddress(ipAddress);
                }
            }
            
            // If WiFi IP not available, try other network interfaces
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        String hostAddress = addr.getHostAddress();
                        if (hostAddress != null) {
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error getting device IP address", ex);
        }
        
        return "Unknown";
    }
    
    /**
     * Toggle between local and production environments (for debugging purposes only)
     * This uses SharedPreferences to override the BuildConfig environment
     * @param context Context
     * @return New environment after toggle
     */
    public static String toggleEnvironmentOverride(Context context) {
        if (context == null) return ApiClient.getApiEnvironment();
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean currentOverride = prefs.getBoolean(KEY_OVERRIDE_ENV, false);
        
        // Toggle the override
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_OVERRIDE_ENV, !currentOverride);
        editor.apply();
        
        // Show message about restart needed
        Toast.makeText(context, 
                "Environment override toggled. App restart required for changes to take effect.", 
                Toast.LENGTH_LONG).show();
        
        return !currentOverride ? "LOCAL (Override)" : "PRODUCTION (Override)";
    }
    
    /**
     * Check if there's an environment override
     * @param context Context
     * @return true if environment is overridden
     */
    public static boolean isEnvironmentOverridden(Context context) {
        if (context == null) return false;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_OVERRIDE_ENV, false);
    }
} 