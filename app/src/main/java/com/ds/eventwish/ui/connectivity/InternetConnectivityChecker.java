package com.ds.eventwish.ui.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public class InternetConnectivityChecker {
    
    private static final String TAG = "ConnectivityChecker";
    
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private ConnectivityManager.NetworkCallback networkCallback;
    
    // Singleton instance
    private static volatile InternetConnectivityChecker instance;
    
    /**
     * Get the singleton instance of InternetConnectivityChecker
     * @param context Application context
     * @return InternetConnectivityChecker instance
     */
    public static InternetConnectivityChecker getInstance(Context context) {
        if (instance == null) {
            synchronized (InternetConnectivityChecker.class) {
                if (instance == null) {
                    instance = new InternetConnectivityChecker(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    public InternetConnectivityChecker(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        setupNetworkCallback();
    }
    
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
    
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // Legacy support for older Android versions
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
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