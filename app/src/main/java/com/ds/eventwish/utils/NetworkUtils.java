package com.ds.eventwish.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for monitoring network state and providing information about network connectivity
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    
    // Singleton instance
    private static volatile NetworkUtils instance;
    
    // Dependencies
    private final Context context;
    private final ConnectivityManager connectivityManager;
    
    // Network state
    private final MutableLiveData<Boolean> networkAvailable = new MutableLiveData<>(false);
    private final MutableLiveData<ConnectionType> connectionType = new MutableLiveData<>(ConnectionType.NONE);
    private final MutableLiveData<Boolean> meteredConnection = new MutableLiveData<>(true);
    
    /**
     * Connection type enum
     */
    public enum ConnectionType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        OTHER
    }
    
    /**
     * Connection quality enum
     */
    public enum ConnectionQuality {
        UNKNOWN,
        POOR,
        MODERATE,
        GOOD,
        EXCELLENT
    }
    
    /**
     * Get the singleton instance of NetworkUtils
     * @param context Application context
     * @return NetworkUtils instance
     */
    public static NetworkUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkUtils.class) {
                if (instance == null) {
                    instance = new NetworkUtils(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private NetworkUtils(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        // Initialize network state
        updateNetworkState();
        
        // Register network callback
        registerNetworkCallback();
        
        Log.d(TAG, "NetworkUtils initialized");
    }
    
    /**
     * Register network callback to monitor network changes
     */
    private void registerNetworkCallback() {
        if (connectivityManager == null) {
            return;
        }
        
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        
        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        Log.d(TAG, "Network available");
                        updateNetworkState();
                    }
                    
                    @Override
                    public void onLost(@NonNull Network network) {
                        Log.d(TAG, "Network lost");
                        updateNetworkState();
                    }
                    
                    @Override
                    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                        Log.d(TAG, "Network capabilities changed");
                        updateNetworkState();
                    }
                }
        );
    }
    
    /**
     * Update network state
     */
    private void updateNetworkState() {
        boolean isConnected = isConnected();
        ConnectionType type = getConnectionType();
        boolean isMetered = isConnectionMetered();
        
        networkAvailable.postValue(isConnected);
        connectionType.postValue(type);
        meteredConnection.postValue(isMetered);
        
        Log.d(TAG, "Network state updated: connected=" + isConnected + 
                  ", type=" + type + 
                  ", metered=" + isMetered);
    }
    
    /**
     * Check if device is connected to a network
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Get the current connection type
     * @return ConnectionType
     */
    public ConnectionType getConnectionType() {
        if (connectivityManager == null) {
            return ConnectionType.NONE;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return ConnectionType.NONE;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return ConnectionType.NONE;
            }
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return ConnectionType.WIFI;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return ConnectionType.CELLULAR;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return ConnectionType.ETHERNET;
            } else {
                return ConnectionType.OTHER;
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                return ConnectionType.NONE;
            }
            
            int type = networkInfo.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                return ConnectionType.WIFI;
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                return ConnectionType.CELLULAR;
            } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                return ConnectionType.ETHERNET;
            } else {
                return ConnectionType.OTHER;
            }
        }
    }
    
    /**
     * Check if the current connection is metered
     * @return true if metered, false otherwise
     */
    public boolean isConnectionMetered() {
        if (connectivityManager == null) {
            return true;
        }
        
        return connectivityManager.isActiveNetworkMetered();
    }
    
    /**
     * Get the current connection quality
     * @return ConnectionQuality
     */
    public ConnectionQuality getConnectionQuality() {
        if (!isConnected()) {
            return ConnectionQuality.UNKNOWN;
        }
        
        ConnectionType type = getConnectionType();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return ConnectionQuality.UNKNOWN;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return ConnectionQuality.UNKNOWN;
            }
            
            // Check for bandwidth
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)) {
                if (type == ConnectionType.WIFI || type == ConnectionType.ETHERNET) {
                    return ConnectionQuality.EXCELLENT;
                } else {
                    return ConnectionQuality.GOOD;
                }
            } else {
                if (type == ConnectionType.WIFI || type == ConnectionType.ETHERNET) {
                    return ConnectionQuality.GOOD;
                } else {
                    return ConnectionQuality.MODERATE;
                }
            }
        } else {
            // For older devices, make a best guess based on connection type
            if (type == ConnectionType.WIFI || type == ConnectionType.ETHERNET) {
                return ConnectionQuality.GOOD;
            } else if (type == ConnectionType.CELLULAR) {
                return ConnectionQuality.MODERATE;
            } else {
                return ConnectionQuality.UNKNOWN;
            }
        }
    }
    
    /**
     * Get network availability as LiveData
     * @return LiveData with network availability
     */
    public LiveData<Boolean> getNetworkAvailableLiveData() {
        return networkAvailable;
    }
    
    /**
     * Get connection type as LiveData
     * @return LiveData with connection type
     */
    public LiveData<ConnectionType> getConnectionTypeLiveData() {
        return connectionType;
    }
    
    /**
     * Get metered connection as LiveData
     * @return LiveData with metered connection
     */
    public LiveData<Boolean> getMeteredConnectionLiveData() {
        return meteredConnection;
    }
    
    /**
     * Generate cache control header based on network state
     * @return Cache control header value
     */
    public String getCacheControlHeader() {
        if (!isConnected()) {
            return "public, max-stale=2592000"; // 30 days
        }
        
        ConnectionType type = getConnectionType();
        boolean isMetered = isConnectionMetered();
        
        if (type == ConnectionType.WIFI || type == ConnectionType.ETHERNET) {
            return "public, max-age=600"; // 10 minutes
        } else if (isMetered) {
            return "public, max-age=3600, max-stale=86400"; // 1 hour, stale for 1 day
        } else {
            return "public, max-age=1800"; // 30 minutes
        }
    }
    
    /**
     * Generate offline cache control header
     * @return Offline cache control header value
     */
    public String getOfflineCacheControlHeader() {
        return "public, only-if-cached, max-stale=2592000"; // 30 days
    }
    
    /**
     * Check if the current connection is unmetered (e.g., WiFi)
     * @return True if unmetered, false otherwise
     */
    public boolean isConnectionUnmetered() {
        return !meteredConnection.getValue();
    }
    
    /**
     * Get LiveData for network availability
     * @return LiveData with network availability
     */
    public LiveData<Boolean> getNetworkAvailability() {
        return networkAvailable;
    }
    
    /**
     * Get cache control header value based on network conditions
     * @param maxAgeSeconds Maximum age in seconds
     * @return Cache control header value
     */
    public String getCacheControlHeaderValue(int maxAgeSeconds) {
        if (!isConnected()) {
            // If offline, use cached data regardless of age
            return "public, only-if-cached, max-stale=" + Integer.MAX_VALUE;
        } else if (isConnectionUnmetered()) {
            // If on unmetered connection (WiFi), use shorter max-age
            return "public, max-age=" + maxAgeSeconds;
        } else {
            // If on metered connection (cellular), use longer max-age
            return "public, max-age=" + (maxAgeSeconds * 2) + ", max-stale=" + (maxAgeSeconds * 4);
        }
    }
    
    /**
     * Check if network is available
     * @return true if network is available, false otherwise
     */
    public boolean isNetworkAvailable() {
        Boolean value = networkAvailable.getValue();
        return value != null && value;
    }

    /**
     * Read an input stream into a string
     * @param inputStream The input stream to read
     * @return The string content
     * @throws IOException If an I/O error occurs
     */
    public static String readStream(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    /**
     * Gets the current network type as a string
     * @return A string representing the network type (WIFI, MOBILE, NONE)
     */
    public String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return "NONE";
        }
        
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return "NONE";
        }
        
        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
            return "MOBILE";
        } else {
            return "OTHER";
        }
    }

    /**
     * Check if the device has an active internet connection
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    
    /**
     * Convert a JSONObject to a Map<String, Object>
     * @param jsonObject JSONObject to convert
     * @return Map representation of the JSONObject
     */
    public static Map<String, Object> jsonToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        
        if (jsonObject == null) {
            return map;
        }
        
        try {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                map.put(key, value);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error converting JSONObject to Map", e);
        }
        
        return map;
    }

    /**
     * Get human-readable network type name
     * @return String representation of current network type (WIFI, MOBILE, etc.)
     */
    public String getNetworkTypeName() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return "UNKNOWN";
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            return "NONE";
        }

        int type = activeNetwork.getType();
        switch (type) {
            case ConnectivityManager.TYPE_WIFI:
                return "WIFI";
            case ConnectivityManager.TYPE_MOBILE:
                // Get more detailed mobile network info
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    int networkType = telephonyManager.getNetworkType();
                    switch (networkType) {
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            return "MOBILE_2G";
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                            return "MOBILE_3G";
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            return "MOBILE_4G";
                        case TelephonyManager.NETWORK_TYPE_NR:
                            return "MOBILE_5G";
                        default:
                            return "MOBILE_UNKNOWN";
                    }
                }
                return "MOBILE";
            case ConnectivityManager.TYPE_ETHERNET:
                return "ETHERNET";
            case ConnectivityManager.TYPE_BLUETOOTH:
                return "BLUETOOTH";
            case ConnectivityManager.TYPE_VPN:
                return "VPN";
            default:
                return "OTHER";
        }
    }
}
