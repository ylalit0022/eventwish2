package com.ds.eventwish.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.ds.eventwish.EventWishApplication;

public class InternetConnectivityChecker {
    private static final String TAG = "ConnectivityChecker";
    private static volatile InternetConnectivityChecker instance;
    private final Context context;

    private InternetConnectivityChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    public static InternetConnectivityChecker getInstance() {
        if (instance == null) {
            synchronized (InternetConnectivityChecker.class) {
                if (instance == null) {
                    instance = new InternetConnectivityChecker(EventWishApplication.getAppContext());
                }
            }
        }
        return instance;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            Log.d(TAG, "ConnectivityManager is null");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities == null) {
                Log.d(TAG, "Network capabilities not available");
                return false;
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.d(TAG, "WiFi connection available");
                return true;
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.d(TAG, "Cellular connection available");
                return true;
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.d(TAG, "Ethernet connection available");
                return true;
            }
            Log.d(TAG, "No valid transport found");
            return false;
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d(TAG, "Legacy network check: " + (isConnected ? "connected" : "not connected"));
            return isConnected;
        }
    }
} 