package com.ds.eventwish.ui.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

/**
 * Utility class to check internet connectivity
 */
public class InternetConnectivityChecker {
    private static final String TAG = "ConnectivityChecker";
    
    private final Context context;
    private static InternetConnectivityChecker instance;
    
    // Network state
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    /**
     * Constructor with context
     * @param context Application context
     */
    public InternetConnectivityChecker(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        // Initialize network state
        setupNetworkCallback();
    }
    
    /**
     * Get singleton instance
     * @param context Application context
     * @return InternetConnectivityChecker instance
     */
    public static InternetConnectivityChecker getInstance(Context context) {
        if (instance == null) {
            synchronized (InternetConnectivityChecker.class) {
                if (instance == null) {
                    instance = new InternetConnectivityChecker(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Setup network callback to receive network state changes
     */
    private void setupNetworkCallback() {
        if (connectivityManager == null) return;
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network available");
                isConnected.postValue(true);
            }
            
            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost");
                isConnected.postValue(false);
            }
        };
        
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callback", e);
        }
        
        // Initial check
        isConnected.postValue(isNetworkAvailable());
    }
    
    /**
     * Check if network is available
     * @return true if network is available, false otherwise
     */
    public boolean isNetworkAvailable() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot check network availability");
            return false;
        }
        
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            }
        } else {
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            } catch (Exception e) {
                Log.e(TAG, "Error checking network availability", e);
            }
        }
        
        return false;
    }
    
    /**
     * Get connection status LiveData
     * @return LiveData of connection status
     */
    public LiveData<Boolean> getConnectionStatus() {
        return isConnected;
    }
    
    /**
     * Observe connection status changes
     * @param owner LifecycleOwner to observe with
     * @param observer Observer to receive connection status updates
     */
    public void observe(LifecycleOwner owner, Observer<Boolean> observer) {
        isConnected.observe(owner, observer);
    }
    
    /**
     * Check if WiFi is connected
     * @return true if WiFi is connected, false otherwise
     */
    public boolean isWifiConnected() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Check if mobile data is connected
     * @return true if mobile data is connected, false otherwise
     */
    public boolean isMobileDataConnected() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }
}