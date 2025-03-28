package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.AdMobDao;
import com.ds.eventwish.data.local.entity.AdMobEntity;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.AppExecutors;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdMobRepository {
    private static final String TAG = "AdMobRepository";
    private static AdMobRepository instance;
    private final AdMobDao adMobDao;
    private final ApiService apiService;
    private final Context context;
    private final AppExecutors executors;
    private Call<List<AdMobEntity>> currentCall;

    private AdMobRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.adMobDao = database.adMobDao();
        
        // Make sure ApiClient is initialized
        try {
            ApiClient.init(context);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ApiClient", e);
        }
        
        // Get ApiService - attempt to get client reference regardless of initialization success
        this.apiService = ApiClient.getClient();
        
        this.executors = AppExecutors.getInstance();
        
        // Load AdMob data from server
        refreshAdMobData();
    }

    public static synchronized AdMobRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AdMobRepository.class) {
                if (instance == null) {
                    instance = new AdMobRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Cancel any ongoing API call
     */
    private void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            currentCall = null;
        }
    }

    /**
     * Refresh AdMob data from server
     */
    public void refreshAdMobData() {
        Log.d(TAG, "Refreshing AdMob data from server");
        
        // Cancel any ongoing call
        cancelCurrentCall();
        
        // Check if API service is available
        if (apiService == null) {
            Log.e(TAG, "Cannot refresh AdMob data: API service is null");
            loadFromDatabaseAsFallback();
            return;
        }
        
        try {
            currentCall = apiService.getAdMobData();
            currentCall.enqueue(new Callback<List<AdMobEntity>>() {
                @Override
                public void onResponse(Call<List<AdMobEntity>> call, Response<List<AdMobEntity>> response) {
                    // Clear the reference to the call
                    currentCall = null;
                    
                    if (response.isSuccessful() && response.body() != null) {
                        List<AdMobEntity> adMobEntities = response.body();
                        saveAdMobData(adMobEntities);
                    } else {
                        int code = response.code();
                        String message = "";
                        try {
                            if (response.errorBody() != null) {
                                message = response.errorBody().string();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        
                        Log.e(TAG, "Error refreshing AdMob data: " + code + " " + message);
                        
                        // Handle 401 Unauthorized specifically
                        if (code == 401) {
                            Log.e(TAG, "Authentication failed. Using test ad units.");
                            addDefaultTestAdUnits();
                        } else {
                            // Load from database as fallback for other errors
                            loadFromDatabaseAsFallback();
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<AdMobEntity>> call, Throwable t) {
                    // Clear the reference to the call
                    currentCall = null;
                    
                    if (call.isCanceled()) {
                        Log.d(TAG, "AdMob data request was canceled");
                    } else {
                        Log.e(TAG, "Network error while refreshing AdMob data", t);
                    }
                    
                    // Load from database as fallback
                    loadFromDatabaseAsFallback();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while making AdMob API call", e);
            // Clear the call reference
            currentCall = null;
            // Load from database as fallback
            loadFromDatabaseAsFallback();
        }
    }

    /**
     * Load AdMob data from database as fallback
     */
    private void loadFromDatabaseAsFallback() {
        executors.diskIO().execute(() -> {
            try {
                List<AdMobEntity> adMobs = adMobDao.getActiveAdMobs();
                if (adMobs == null || adMobs.isEmpty()) {
                    // Add default test ad units if database is empty
                    addDefaultTestAdUnits();
                } else {
                    Log.d(TAG, "Loaded " + adMobs.size() + " AdMob units from database as fallback");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading AdMob data from database", e);
                // Add default test ad units as last resort
                addDefaultTestAdUnits();
            }
        });
    }

    /**
     * Add default test ad units to database
     */
    private void addDefaultTestAdUnits() {
        List<AdMobEntity> defaultAdUnits = new ArrayList<>();
        defaultAdUnits.add(createTestAdUnit("Banner"));
        defaultAdUnits.add(createTestAdUnit("Interstitial"));
        defaultAdUnits.add(createTestAdUnit("Rewarded"));
        defaultAdUnits.add(createTestAdUnit("Native"));
        defaultAdUnits.add(createTestAdUnit("AppOpen"));
        
        executors.diskIO().execute(() -> {
            try {
                adMobDao.insertAll(defaultAdUnits);
                Log.d(TAG, "Added " + defaultAdUnits.size() + " default test ad units to database");
            } catch (Exception e) {
                Log.e(TAG, "Error adding default test ad units to database", e);
            }
        });
    }

    /**
     * Save AdMob data to database
     * @param adMobEntities AdMob entities to save
     */
    private void saveAdMobData(List<AdMobEntity> adMobEntities) {
        executors.diskIO().execute(() -> {
            try {
                adMobDao.insertAll(adMobEntities);
                Log.d(TAG, "Saved " + adMobEntities.size() + " AdMob units to database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving AdMob data to database", e);
                // Try to add default test ad units as fallback
                addDefaultTestAdUnits();
            }
        });
    }

    /**
     * Get all active AdMob entities with improved error handling
     * @return List of active AdMob entities
     */
    public List<AdMobEntity> getActiveAdMobs() {
        AtomicReference<List<AdMobEntity>> result = new AtomicReference<>(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        executors.diskIO().execute(() -> {
            try {
                List<AdMobEntity> adMobs = adMobDao.getActiveAdMobs();
                if (adMobs != null && !adMobs.isEmpty()) {
                    result.set(adMobs);
                } else {
                    // Fallback to test ad units
                    List<AdMobEntity> testUnits = new ArrayList<>();
                    testUnits.add(createTestAdUnit("Banner"));
                    testUnits.add(createTestAdUnit("Interstitial"));
                    testUnits.add(createTestAdUnit("Rewarded"));
                    testUnits.add(createTestAdUnit("Native"));
                    testUnits.add(createTestAdUnit("AppOpen"));
                    result.set(testUnits);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting active AdMob entities", e);
                // Fallback to test ad units
                List<AdMobEntity> testUnits = new ArrayList<>();
                testUnits.add(createTestAdUnit("Banner"));
                testUnits.add(createTestAdUnit("Interstitial"));
                testUnits.add(createTestAdUnit("Rewarded"));
                testUnits.add(createTestAdUnit("Native"));
                testUnits.add(createTestAdUnit("AppOpen"));
                result.set(testUnits);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for AdMob data", e);
            Thread.currentThread().interrupt();
        }
        
        return result.get();
    }

    /**
     * Get active AdMob entities by type with improved error handling
     * @param adType AdMob type (banner, interstitial, rewarded, etc.)
     * @return List of active AdMob entities of the given type
     */
    public List<AdMobEntity> getActiveAdMobsByType(String adType) {
        AtomicReference<List<AdMobEntity>> result = new AtomicReference<>(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        
        executors.diskIO().execute(() -> {
            try {
                List<AdMobEntity> adMobs = adMobDao.getActiveAdMobsByType(adType);
                if (adMobs != null && !adMobs.isEmpty()) {
                    result.set(adMobs);
                } else {
                    // Add test ad unit as fallback if no ads found in database
                    Log.d(TAG, "No active " + adType + " ads found in database, using test ad unit");
                    List<AdMobEntity> testUnits = new ArrayList<>();
                    testUnits.add(createTestAdUnit(adType));
                    result.set(testUnits);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting AdMob entities by type: " + adType, e);
                // Add test ad unit as fallback in case of error
                List<AdMobEntity> testUnits = new ArrayList<>();
                testUnits.add(createTestAdUnit(adType));
                result.set(testUnits);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            // Wait for the database operation to complete (with a timeout)
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for AdMob data", e);
            Thread.currentThread().interrupt();
            
            // Add test ad unit as fallback in case of error
            if (result.get().isEmpty()) {
                List<AdMobEntity> testUnits = new ArrayList<>();
                testUnits.add(createTestAdUnit(adType));
                result.set(testUnits);
            }
        }
        
        return result.get();
    }

    /**
     * Create a test ad unit for the given type
     * @param adType AdMob type
     * @return Test AdMob entity
     */
    private AdMobEntity createTestAdUnit(String adType) {
        String adUnitId;
        String adName = "Test " + adType + " Ad";
        
        // Use appropriate test ad unit IDs based on ad type
        switch (adType) {
            case "Banner":
                adUnitId = "ca-app-pub-3940256099942544/6300978111";
                break;
            case "Interstitial":
                adUnitId = "ca-app-pub-3940256099942544/1033173712";
                break;
            case "Rewarded":
                adUnitId = "ca-app-pub-3940256099942544/5224354917";
                break;
            case "Native":
                adUnitId = "ca-app-pub-3940256099942544/2247696110";
                break;
            case "AppOpen":
                adUnitId = "ca-app-pub-3940256099942544/3419835294";
                break;
            default:
                adUnitId = "ca-app-pub-3940256099942544/5224354917"; // Default to rewarded
                adName = "Default Test Ad";
                break;
        }
        
        return new AdMobEntity(adUnitId, adName, adType, true);
    }

    /**
     * Get AdMob entity by unit ID with error handling
     * @param adUnitId The ad unit ID to look up
     * @return The AdMob entity or null if not found
     */
    public AdMobEntity getAdMobByUnitId(String adUnitId) {
        AtomicReference<AdMobEntity> result = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);
        
        executors.diskIO().execute(() -> {
            try {
                AdMobEntity entity = adMobDao.getAdMobByUnitId(adUnitId);
                result.set(entity);
            } catch (Exception e) {
                Log.e(TAG, "Error getting AdMob by unit ID: " + adUnitId, e);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while getting AdMob by unit ID", e);
            Thread.currentThread().interrupt();
        }
        
        return result.get();
    }

    /**
     * Update AdMob status with error handling
     * @param adUnitId Ad unit ID to update
     * @param status New status value
     */
    public void updateAdMobStatus(String adUnitId, boolean status) {
        executors.diskIO().execute(() -> {
            try {
                AdMobEntity entity = adMobDao.getAdMobByUnitId(adUnitId);
                if (entity != null) {
                    entity.setStatus(status);
                    adMobDao.update(entity);
                    Log.d(TAG, "Updated status of ad unit " + adUnitId + " to " + status);
                } else {
                    Log.e(TAG, "Cannot update status: Ad unit " + adUnitId + " not found");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating AdMob status for unit ID: " + adUnitId, e);
            }
        });
    }

    /**
     * Refresh AdMob data
     */
    public void refreshData() {
        refreshAdMobData();
    }
    
    /**
     * Cancel all ongoing operations when the app is closing
     * Should be called from Application.onTerminate or similar lifecycle method
     */
    public void cleanup() {
        cancelCurrentCall();
    }

    /**
     * Callback interface for receiving ad unit IDs asynchronously
     */
    public interface AdUnitCallback {
        void onAdUnitReceived(String adUnitId);
        void onError(Exception error);
    }
    
    /**
     * Get the active rewarded ad unit ID using callback
     * @param callback Callback to receive the ad unit ID
     */
    public void getActiveRewardedAdUnitId(AdUnitCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                List<AdMobEntity> rewardedAds = adMobDao.getActiveAdMobsByType("Rewarded");
                if (rewardedAds.isEmpty()) {
                    Log.e(TAG, "No rewarded ad units available");
                    executors.mainThread().execute(() -> callback.onError(
                        new Exception("No rewarded ad units available")
                    ));
                    return;
                }
                
                String adUnitId = rewardedAds.get(0).getAdUnitId();
                Log.d(TAG, "Found active rewarded ad unit ID: " + adUnitId);
                
                executors.mainThread().execute(() -> callback.onAdUnitReceived(adUnitId));
            } catch (Exception e) {
                Log.e(TAG, "Error getting active rewarded ad unit ID", e);
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }
} 