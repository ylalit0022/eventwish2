package com.ds.eventwish.ads.core;

import android.content.Context;
import android.util.Log;

import com.ds.eventwish.ads.AdMobRepository;
import com.ds.eventwish.data.model.ads.AdUnit;
import com.ds.eventwish.data.model.ads.AdUnitResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.util.SecureTokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class responsible for managing all ad-related operations.
 * This includes fetching ad units from server, caching them, and managing their lifecycle.
 */
public class AdManager {
    private static final String TAG = "AdManager";
    private static volatile AdManager instance;
    private static Context applicationContext;

    // Core dependencies
    private final AdMobRepository adMobRepository;
    private final SecureTokenManager secureTokenManager;

    // Cache for ad units
    private final Map<String, List<AdUnit>> adUnitCache;
    private final Map<String, Long> lastRefreshTime;
    private static final long CACHE_EXPIRY_MS = 30 * 60 * 1000; // 30 minutes

    // Callbacks for ad events
    private final Map<String, List<AdCallback>> adCallbacks;

    private AdManager(Context context) {
        if (applicationContext == null) {
            throw new IllegalStateException("AdManager not properly initialized. Call init() first.");
        }
        
        this.adMobRepository = new AdMobRepository(context, ApiClient.getClient());
        this.secureTokenManager = SecureTokenManager.getInstance();
        this.adUnitCache = new ConcurrentHashMap<>();
        this.lastRefreshTime = new ConcurrentHashMap<>();
        this.adCallbacks = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the AdManager with application context.
     * Must be called before getInstance().
     */
    public static void init(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        applicationContext = context.getApplicationContext();
    }

    /**
     * Gets the singleton instance of AdManager.
     * Must call init() first.
     */
    public static AdManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AdManager.class) {
                if (instance == null) {
                    instance = new AdManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Fetches ad units from server or cache
     */
    public void getAdUnits(String adType, final AdCallback callback) {
        // Check cache first
        if (isCacheValid(adType)) {
            List<AdUnit> cachedUnits = adUnitCache.get(adType);
            if (cachedUnits != null && !cachedUnits.isEmpty()) {
                Log.d(TAG, "Using cached ad units for type: " + adType);
                callback.onSuccess(cachedUnits);
                return;
            }
        }

        // Fetch from server
        adMobRepository.fetchAdUnit(adType, new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(AdUnit adUnit) {
                if (adUnit != null) {
                    List<AdUnit> adUnits = new ArrayList<>();
                    adUnits.add(adUnit);
                    
                    // Update cache
                    adUnitCache.put(adType, adUnits);
                    lastRefreshTime.put(adType, System.currentTimeMillis());
                    
                    // Notify callback
                    callback.onSuccess(adUnits);
                } else {
                    callback.onError("Invalid response from server");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching ad units: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Registers a callback for ad events of a specific type.
     */
    public void registerCallback(String adType, AdCallback callback) {
        adCallbacks.computeIfAbsent(adType, k -> new ArrayList<>()).add(callback);
    }

    /**
     * Unregisters a callback for ad events of a specific type.
     */
    public void unregisterCallback(String adType, AdCallback callback) {
        List<AdCallback> callbacks = adCallbacks.get(adType);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    /**
     * Checks if the cache for the specified ad type is still valid.
     */
    private boolean isCacheValid(String adType) {
        Long lastRefresh = lastRefreshTime.get(adType);
        return lastRefresh != null && (System.currentTimeMillis() - lastRefresh) < CACHE_EXPIRY_MS;
    }

    /**
     * Clears the cache for all ad types.
     */
    public void clearCache() {
        adUnitCache.clear();
        lastRefreshTime.clear();
    }

    /**
     * Updates the status of an ad unit.
     */
    public void updateAdUnitStatus(String adType, String adUnitCode, boolean status) {
        List<AdUnit> units = adUnitCache.get(adType);
        if (units != null) {
            for (AdUnit unit : units) {
                if (unit.getAdUnitCode().equals(adUnitCode)) {
                    unit.setStatus(status);
                    break;
                }
            }
        }
    }

    /**
     * Callback interface for ad operations
     */
    public interface AdCallback {
        void onSuccess(List<AdUnit> adUnits);
        void onError(String error);
    }
} 