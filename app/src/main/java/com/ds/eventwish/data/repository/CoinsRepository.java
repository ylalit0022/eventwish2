package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.AdMobDao;
import com.ds.eventwish.data.local.dao.CoinsDao;
import com.ds.eventwish.data.local.entity.AdMobEntity;
import com.ds.eventwish.data.local.entity.CoinsEntity;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.model.ServerTimeResponse;
import com.ds.eventwish.util.AppExecutors;
import com.ds.eventwish.utils.DeviceUtils;
import com.google.gson.JsonObject;
import com.ds.eventwish.EventWishApplication;

import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Repository for managing coins data
 */
public class CoinsRepository {
    private static final String TAG = "CoinsRepository";
    private static CoinsRepository instance;
    private final CoinsDao coinsDao;
    private final MutableLiveData<Integer> coinsLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isUnlockedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Long> remainingTimeLiveData = new MutableLiveData<>(0L);
    private static final String DEFAULT_USER_ID = "user_coins";
    private final Context context;
    private final AppExecutors executors;
    private final ApiService apiService;
    
    private static final String KEYSTORE_ALIAS = "coins_security_key";
    private static final String SECURE_PREFS_NAME = "coins_secure_prefs";
    private static final String SECURE_PREF_UNLOCK_DATA = "unlock_data";
    private static final String SECURE_PREF_LAST_TIME = "last_time_check";
    private static final String SECURE_PREF_SIGNATURE = "unlock_signature";
    
    private long lastServerTime = 0L;
    private long lastTimeOffset = 0L;
    private long lastLocalTime = 0L;

    private CoinsRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.coinsDao = database.coinsDao();
        this.executors = AppExecutors.getInstance();
        this.apiService = ApiClient.getClient();
        
        // Initialize coins data
        initializeCoinsData();
        
        // Start remaining time check
        startRemainingTimeCheck();
        
        // Initialize security
        initializeSecurity();
    }

    public static CoinsRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (CoinsRepository.class) {
                if (instance == null) {
                    instance = new CoinsRepository(context);
                }
            }
        }
        return instance;
    }
    
    private void initializeSecurity() {
        // Generate or retrieve security key
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                
                keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
                
                keyGenerator.generateKey();
                Log.d(TAG, "Security key generated successfully");
            } else {
                Log.d(TAG, "Security key already exists");
            }
            
            // Sync server time on initialization
            syncServerTime();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing security", e);
        }
    }
    
    private void syncServerTime() {
        apiService.getServerTime().enqueue(new Callback<ServerTimeResponse>() {
            @Override
            public void onResponse(Call<ServerTimeResponse> call, Response<ServerTimeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        long serverTime = response.body().getTimestamp();
                        long localTime = System.currentTimeMillis();
                        lastServerTime = serverTime;
                        lastLocalTime = localTime;
                        lastTimeOffset = serverTime - localTime;
                        
                        // Store last time check securely
                        storeSecurely(SECURE_PREF_LAST_TIME, 
                                serverTime + "," + localTime + "," + lastTimeOffset);
                        
                        Log.d(TAG, "Time synced with server. Offset: " + lastTimeOffset + "ms");
                        
                        // Validate existing unlock if needed
                        validateUnlockStatus();
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing server time", e);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ServerTimeResponse> call, Throwable t) {
                Log.e(TAG, "Failed to sync server time", t);
                // Use last known time offset if available
                String lastTimeData = retrieveSecurely(SECURE_PREF_LAST_TIME);
                if (lastTimeData != null) {
                    try {
                        String[] parts = lastTimeData.split(",");
                        lastServerTime = Long.parseLong(parts[0]);
                        lastLocalTime = Long.parseLong(parts[1]);
                        lastTimeOffset = Long.parseLong(parts[2]);
                        Log.d(TAG, "Using cached time offset: " + lastTimeOffset + "ms");
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing cached time data", e);
                    }
                }
            }
        });
    }
    
    /**
     * Validate unlock status
     * This should be called regularly to prevent time manipulation
     */
    public void validateUnlockStatus() {
        executors.diskIO().execute(() -> {
            try {
                // First, verify device security
                boolean isRooted = DeviceUtils.isDeviceRooted();
                boolean isEmulator = DeviceUtils.isEmulator();
                boolean deviceIdValid = DeviceUtils.verifyDeviceIdIntegrity(context);
                
                // Log security status
                Log.d(TAG, "Device security check - Rooted: " + isRooted + 
                      ", Emulator: " + isEmulator + ", ID valid: " + deviceIdValid);
                
                // If security issues detected, consider revoking access
                if (isRooted || !deviceIdValid) {
                    Log.w(TAG, "Device security compromised! Revoking premium access.");
                    // Revoke access with security violation flag
                    revokeAccessDueToSecurity();
                    return;
                }
                
                // Get the coins entity from the database
                CoinsEntity coinsEntity = coinsDao.getCoinsById(DEFAULT_USER_ID);
                if (coinsEntity == null) {
                    Log.d(TAG, "No coins entity found, creating default");
                    coinsEntity = new CoinsEntity();
                    coinsDao.insert(coinsEntity);
                }
                
                // Check if the feature is unlocked
                if (coinsEntity.isUnlocked()) {
                    long unlockTimestamp = coinsEntity.getUnlockTimestamp();
                    int unlockDuration = coinsEntity.getUnlockDuration();
                    
                    // Calculate remaining time
                    long currentTime = System.currentTimeMillis();
                    long expirationTime = unlockTimestamp + (unlockDuration * DateUtils.DAY_IN_MILLIS);
                    long remainingTime = expirationTime - currentTime;
                    
                    // Check if the feature has expired
                    if (remainingTime <= 0) {
                        // Feature has expired, update database
                        Log.d(TAG, "Feature has expired, updating database");
                        coinsEntity.setUnlocked(false);
                        coinsEntity.setUnlockTimestamp(0);
                        coinsEntity.setUnlockDuration(0);
                        coinsDao.update(coinsEntity);
                        
                        // Update LiveData
                        executors.mainThread().execute(() -> {
                            isUnlockedLiveData.setValue(false);
                            remainingTimeLiveData.setValue(0L);
                        });
                    } else {
                        // Feature is still valid, update LiveData
                        Log.d(TAG, "Feature is still valid, remaining time: " + remainingTime);
                        
                        // Update LiveData
                        executors.mainThread().execute(() -> {
                            isUnlockedLiveData.setValue(true);
                            remainingTimeLiveData.setValue(remainingTime);
                        });
                        
                        // Validate with server-side check to prevent time manipulation
                        validateServerSide();
                    }
                } else {
                    // Feature is not unlocked, make sure LiveData reflects this
                    Log.d(TAG, "Feature is not unlocked");
                    
                    // Update LiveData
                    executors.mainThread().execute(() -> {
                        isUnlockedLiveData.setValue(false);
                        remainingTimeLiveData.setValue(0L);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error validating unlock status", e);
            }
        });
    }
    
    private void validateServerSide() {
        String deviceId = getDeviceId();
        if (deviceId == null) {
            Log.e(TAG, "Device ID is null, cannot validate unlock server-side");
            return;
        }
        
        // Get server time first to prevent time manipulation
        apiService.getServerTime().enqueue(new Callback<ServerTimeResponse>() {
            @Override
            public void onResponse(Call<ServerTimeResponse> call, Response<ServerTimeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long serverTime = response.body().getTimestamp();
                    validateWithServerTime(serverTime);
                } else {
                    Log.e(TAG, "Failed to get server time: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ServerTimeResponse> call, Throwable t) {
                Log.e(TAG, "Network error while getting server time", t);
            }
        });
    }
    
    private void validateWithServerTime(long serverTime) {
        // ... existing code ...
    }
    
    private void revokeAccess() {
        executors.diskIO().execute(() -> {
            coinsDao.updateUnlockStatus(DEFAULT_USER_ID, false, 0, 0);
            isUnlockedLiveData.postValue(false);
            remainingTimeLiveData.postValue(0L);
            
            // Clear secure storage
            storeSecurely(SECURE_PREF_UNLOCK_DATA, null);
            storeSecurely(SECURE_PREF_SIGNATURE, null);
        });
    }
    
    private String getDeviceId() {
        return DeviceUtils.getDeviceId(context);
    }
    
    private long getAdjustedTime() {
        // Calculate current server time based on last known offset
        return System.currentTimeMillis() + lastTimeOffset;
    }

    private void initializeCoinsData() {
        executors.diskIO().execute(() -> {
            CoinsEntity entity = coinsDao.getCoinsById(DEFAULT_USER_ID);
            if (entity == null) {
                // Create initial entity if not exists
                entity = new CoinsEntity();
                entity.setId(DEFAULT_USER_ID);
                entity.setCoins(0);
                entity.setUnlockTimestamp(0);
                entity.setUnlockDuration(30);
                entity.setUnlocked(false);
                coinsDao.insert(entity);
            }
            
            // Update LiveData
            coinsLiveData.postValue(entity.getCoins());
            isUnlockedLiveData.postValue(entity.isUnlocked());
            updateRemainingTime(entity);
            
            // Validate unlock status securely
            validateUnlockStatus();
        });
    }

    private void startRemainingTimeCheck() {
        executors.diskIO().execute(() -> {
            while (true) {
                CoinsEntity entity = coinsDao.getCoinsById(DEFAULT_USER_ID);
                if (entity != null && entity.isUnlocked()) {
                    updateRemainingTime(entity);
                    
                    // Periodically check for time manipulation
                    if (Math.random() < 0.05) { // 5% chance each check
                        validateUnlockStatus();
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(30); // Check every 30 seconds instead of every second
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error in remaining time check", e);
                    break;
                }
            }
        });
    }

    private void updateRemainingTime(CoinsEntity entity) {
        if (!entity.isUnlocked()) {
            Log.d(TAG, "Feature not unlocked, setting remaining time to 0");
            remainingTimeLiveData.postValue(0L);
            return;
        }

        // Use adjusted time for more accurate remaining time
        long currentTime = getAdjustedTime();
        long unlockTimestamp = entity.getUnlockTimestamp();
        int unlockDuration = entity.getUnlockDuration();
        long unlockEndTime = unlockTimestamp + (TimeUnit.DAYS.toMillis(unlockDuration));
        
        long remainingTime = Math.max(0, unlockEndTime - currentTime);
        
        // Log remaining time in a human-readable format
        long days = TimeUnit.MILLISECONDS.toDays(remainingTime);
        long hours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
        
        Log.d(TAG, String.format("Remaining unlock time: %d days, %d hours, %d minutes (%d ms)", 
            days, hours, minutes, remainingTime));
        
        // Update LiveData with the raw milliseconds value
        // The formatting will be done in the UI layer
        remainingTimeLiveData.postValue(remainingTime);

        if (remainingTime == 0) {
            // Feature expired, update status
            Log.d(TAG, "Feature has expired, updating unlock status");
            executors.diskIO().execute(() -> {
                coinsDao.updateUnlockStatus(DEFAULT_USER_ID, false, 0, 0);
                isUnlockedLiveData.postValue(false);
                
                // Clear secure storage
                storeSecurely(SECURE_PREF_UNLOCK_DATA, null);
                storeSecurely(SECURE_PREF_SIGNATURE, null);
            });
        }
    }

    public void addCoins(int amount) {
        if (amount <= 0) {
            Log.w(TAG, "Attempted to add invalid amount of coins: " + amount);
            return;
        }
        
        Log.d(TAG, "Adding " + amount + " coins");
        
        executors.diskIO().execute(() -> {
            // Get the current coins entity
            CoinsEntity coinsEntity = coinsDao.getCoinsById(DEFAULT_USER_ID);
            if (coinsEntity == null) {
                coinsEntity = new CoinsEntity();
                coinsEntity.setCoins(amount);
                coinsDao.insert(coinsEntity);
            } else {
                // Update coins in database
                coinsDao.addCoins(DEFAULT_USER_ID, amount);
                coinsEntity.setCoins(coinsEntity.getCoins() + amount);
            }
            
            // Update LiveData on main thread
            final int newCoinsAmount = coinsEntity.getCoins();
            executors.mainThread().execute(() -> {
                coinsLiveData.setValue(newCoinsAmount);
                Log.d(TAG, "Updated coins LiveData: " + newCoinsAmount);
            });
        });
    }

    public void unlockFeature(int duration) {
        // Synchronize with server time before unlocking
        apiService.getServerTime().enqueue(new Callback<ServerTimeResponse>() {
            @Override
            public void onResponse(Call<ServerTimeResponse> call, Response<ServerTimeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Get server time for timestamp
                        long serverTime = response.body().getTimestamp();
                        long localTime = System.currentTimeMillis();
                        lastServerTime = serverTime;
                        lastLocalTime = localTime;
                        lastTimeOffset = serverTime - localTime;
                        
                        // Store time offset
                        storeSecurely(SECURE_PREF_LAST_TIME, 
                                serverTime + "," + localTime + "," + lastTimeOffset);
                        
                        // Use server timestamp for unlock
                        performUnlock(serverTime, duration);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing server time for unlock", e);
                        // Fallback to local time with offset
                        performUnlock(getAdjustedTime(), duration);
                    }
                } else {
                    Log.e(TAG, "Server time request failed");
                    // Fallback to local time with offset
                    performUnlock(getAdjustedTime(), duration);
                }
            }
            
            @Override
            public void onFailure(Call<ServerTimeResponse> call, Throwable t) {
                Log.e(TAG, "Failed to get server time for unlock", t);
                // Fallback to local time with offset
                performUnlock(getAdjustedTime(), duration);
            }
        });
    }
    
    private void performUnlock(long timestamp, int duration) {
        executors.diskIO().execute(() -> {
            // Update database
            coinsDao.updateUnlockStatus(DEFAULT_USER_ID, true, timestamp, duration);
            isUnlockedLiveData.postValue(true);
            
            // Store unlock data securely
            String unlockData = timestamp + ":" + duration;
            storeSecurely(SECURE_PREF_UNLOCK_DATA, unlockData);
            
            // Generate and store signature
            try {
                String signature = generateSignature(unlockData);
                storeSecurely(SECURE_PREF_SIGNATURE, signature);
            } catch (Exception e) {
                Log.e(TAG, "Error generating signature", e);
            }
            
            // Report unlock to server for validation
            reportUnlockToServer(timestamp, duration);
            
            CoinsEntity entity = coinsDao.getCoinsById(DEFAULT_USER_ID);
            if (entity != null) {
                updateRemainingTime(entity);
            }
        });
    }
    
    private void reportUnlockToServer(long timestamp, int duration) {
        try {
            // Create payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("deviceId", getDeviceId());
            payload.put("timestamp", timestamp);
            payload.put("duration", duration);
            payload.put("signature", retrieveSecurely(SECURE_PREF_SIGNATURE));
            
            // Report to server
            apiService.reportUnlock(payload).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Unlock reported to server successfully");
                    } else {
                        Log.e(TAG, "Error reporting unlock: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Network error reporting unlock", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error reporting unlock", e);
        }
    }
    
    private String generateSignature(String data) throws Exception {
        String combined = data + ":" + getDeviceId();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hash, Base64.DEFAULT);
    }
    
    private void storeSecurely(String key, String value) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE);
            
            if (value == null) {
                // Remove the key if value is null
                prefs.edit().remove(key).apply();
                return;
            }
            
            // Get the encryption key
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
            
            // Encrypt the value
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);
            
            // Store the encrypted data
            String encoded = Base64.encodeToString(combined, Base64.DEFAULT);
            prefs.edit().putString(key, encoded).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error storing data securely", e);
        }
    }
    
    private String retrieveSecurely(String key) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE);
            String encoded = prefs.getString(key, null);
            
            if (encoded == null) {
                return null;
            }
            
            // Get the decryption key
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
            
            // Decode the combined data
            byte[] combined = Base64.decode(encoded, Base64.DEFAULT);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[12]; // GCM IV size is 12 bytes
            byte[] encryptedData = new byte[combined.length - iv.length];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);
            
            // Decrypt the data
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] decryptedData = cipher.doFinal(encryptedData);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving data securely", e);
            return null;
        }
    }

    public LiveData<Integer> getCoinsLiveData() {
        return coinsLiveData;
    }

    public LiveData<Boolean> getIsUnlockedLiveData() {
        return isUnlockedLiveData;
    }

    public LiveData<Long> getRemainingTimeLiveData() {
        return remainingTimeLiveData;
    }

    public boolean isFeatureUnlocked() {
        // Don't call validateUnlockStatus directly, as it would cause a main thread database access
        // Instead, just use the LiveData value which is updated asynchronously
        return Boolean.TRUE.equals(isUnlockedLiveData.getValue());
    }

    public int getCurrentCoins() {
        return coinsLiveData.getValue() != null ? coinsLiveData.getValue() : 0;
    }

    public long getRemainingTime() {
        return remainingTimeLiveData.getValue() != null ? remainingTimeLiveData.getValue() : 0;
    }

    /**
     * Used for diagnostics to log current coin status
     * @return String representation of coin data
     */
    public int getCoins() {
        Integer coins = coinsLiveData.getValue();
        return coins != null ? coins : 0;
    }

    public boolean isUnlocked() {
        Boolean unlocked = isUnlockedLiveData.getValue();
        return unlocked != null ? unlocked : false;
    }

    public String getCoinsStatusString() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("Coins: ").append(getCoins()).append("\n");
            status.append("Is Unlocked: ").append(isUnlocked() ? "Yes" : "No").append("\n");
            
            if (isUnlocked()) {
                long remainingTime = getRemainingTime();
                long days = remainingTime / (24 * 60 * 60 * 1000);
                long hours = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
                
                status.append("Remaining Time: ").append(days).append(" days, ")
                      .append(hours).append(" hours\n");
            }
            
            return status.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting coins status string", e);
            return "Error getting coins status: " + e.getMessage();
        }
    }

    /**
     * Force refresh of coins LiveData
     * This ensures the UI updates correctly even if the actual value hasn't changed
     */
    public void forceRefreshCoinsLiveData() {
        Log.d(TAG, "Force refreshing coins LiveData called");
        
        // First, immediately force a UI update with the current in-memory value
        // This helps avoid delay while waiting for the database query
        final Integer currentValue = coinsLiveData.getValue();
        if (currentValue != null) {
            Log.d(TAG, "Immediately refreshing with in-memory value: " + currentValue);
            // Use postValue here to avoid CalledFromWrongThreadException if called from non-main thread
            coinsLiveData.postValue(currentValue);
        }
        
        // Then do the actual database query to get the latest value
        executors.diskIO().execute(() -> {
            try {
                CoinsEntity entity = coinsDao.getCoinsById(DEFAULT_USER_ID);
                if (entity != null) {
                    final int coins = entity.getCoins();
                    Log.d(TAG, "Force refreshing coins LiveData with database value: " + coins);
                    
                    // Update on main thread to ensure UI updates properly
                    executors.mainThread().execute(() -> {
                        // Use setValue to ensure immediate update on main thread
                        coinsLiveData.setValue(coins);
                        
                        // Schedule another refresh after a short delay to catch any pending updates
                        new android.os.Handler().postDelayed(() -> {
                            // Make sure we access the database on a background thread
                            executors.diskIO().execute(() -> {
                                try {
                                    Log.d(TAG, "Performing final delayed refresh for coins");
                                    CoinsEntity delayedEntity = coinsDao.getCoinsById(DEFAULT_USER_ID);
                                    if (delayedEntity != null && delayedEntity.getCoins() != coins) {
                                        Log.d(TAG, "Value changed during delay: " + delayedEntity.getCoins());
                                        // Post back to main thread for LiveData update
                                        executors.mainThread().execute(() -> {
                                            coinsLiveData.setValue(delayedEntity.getCoins());
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in delayed refresh", e);
                                }
                            });
                        }, 300); // Short delay to catch any pending updates
                    });
                } else {
                    Log.w(TAG, "Cannot refresh coins LiveData - no entity found");
                    
                    // Create a default entity to avoid null references
                    Log.d(TAG, "Creating default coins entity");
                    CoinsEntity newEntity = new CoinsEntity();
                    newEntity.setId(DEFAULT_USER_ID);
                    newEntity.setCoins(0);
                    coinsDao.insert(newEntity);
                    
                    // Update LiveData with default value
                    executors.mainThread().execute(() -> {
                        coinsLiveData.setValue(0);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing coins LiveData", e);
            }
        });
    }
    
    /**
     * Full refresh of coin data from server
     * @return MutableLiveData<Boolean> that will be updated with success/failure
     */
    public MutableLiveData<Boolean> refreshFromServer() {
        MutableLiveData<Boolean> refreshResult = new MutableLiveData<>();
        
        String deviceId = getDeviceId();
        if (deviceId == null) {
            Log.e(TAG, "Cannot refresh: device ID is null");
            refreshResult.setValue(false);
            return refreshResult;
        }
        
        Log.d(TAG, "Refreshing coin data from server for device: " + deviceId);
        
        // Make API call to get current coins data
        apiService.getCoins(deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject responseBody = response.body();
                        boolean success = responseBody.has("success") && responseBody.get("success").getAsBoolean();
                        
                        if (success) {
                            // Extract data from response
                            int serverCoins = responseBody.has("coins") ? responseBody.get("coins").getAsInt() : 0;
                            boolean isUnlocked = responseBody.has("isUnlocked") && responseBody.get("isUnlocked").getAsBoolean();
                            
                            Log.d(TAG, "Server data received - Coins: " + serverCoins + 
                                  ", Unlocked: " + isUnlocked);
                            
                            // Update local database
                            executors.diskIO().execute(() -> {
                                try {
                                    CoinsEntity entity = coinsDao.getCoinsById(DEFAULT_USER_ID);
                                    if (entity == null) {
                                        entity = new CoinsEntity();
                                        entity.setId(DEFAULT_USER_ID);
                                    }
                                    
                                    // Update entity with server data
                                    entity.setCoins(serverCoins);
                                    entity.setUnlocked(isUnlocked);
                                    
                                    // If unlocked, update timestamp and duration
                                    if (isUnlocked && responseBody.has("unlockExpiry")) {
                                        try {
                                            String expiryStr = responseBody.get("unlockExpiry").getAsString();
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                            Date expiryDate = sdf.parse(expiryStr);
                                            long expiryTime = expiryDate != null ? expiryDate.getTime() : 0;
                                            
                                            if (expiryTime > 0) {
                                                // Calculate duration from now to expiry
                                                long currentTime = System.currentTimeMillis();
                                                long durationMs = expiryTime - currentTime;
                                                int durationDays = (int)(durationMs / (24 * 60 * 60 * 1000));
                                                
                                                // Ensure minimum of 1 day if unlocked but almost expired
                                                if (durationDays < 1 && durationMs > 0) {
                                                    durationDays = 1;
                                                }
                                                
                                                entity.setUnlockDuration(durationDays);
                                                
                                                // Set unlock timestamp to current time
                                                entity.setUnlockTimestamp(currentTime);
                                                
                                                Log.d(TAG, "Updated unlock duration to " + durationDays + " days");
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing expiry date", e);
                                        }
                                    } else if (!isUnlocked) {
                                        // Clear unlock data if not unlocked
                                        entity.setUnlockTimestamp(0);
                                        entity.setUnlockDuration(0);
                                    }
                                    
                                    // Update database
                                    if (entity.getId() != null && !entity.getId().isEmpty()) {
                                        coinsDao.update(entity);
                                    } else {
                                        coinsDao.insert(entity);
                                    }
                                    
                                    // Update LiveData on main thread
                                    final int finalCoins = entity.getCoins();
                                    final boolean finalIsUnlocked = entity.isUnlocked();
                                    final CoinsEntity finalEntity = entity;
                                    executors.mainThread().execute(() -> {
                                        coinsLiveData.setValue(finalCoins);
                                        isUnlockedLiveData.setValue(finalIsUnlocked);
                                        
                                        // Update remaining time if unlocked
                                        if (finalIsUnlocked) {
                                            updateRemainingTime(finalEntity);
                                        } else {
                                            remainingTimeLiveData.setValue(0L);
                                        }
                                        
                                        refreshResult.setValue(true);
                                        
                                        Log.d(TAG, "LiveData updated from server - Coins: " + 
                                              finalCoins + ", Unlocked: " + finalIsUnlocked);
                                    });
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating local database with server data", e);
                                    executors.mainThread().execute(() -> refreshResult.setValue(false));
                                }
                            });
                        } else {
                            // Server error
                            String message = responseBody.has("message") ? 
                                responseBody.get("message").getAsString() : "Unknown error";
                            Log.e(TAG, "Server error during refresh: " + message);
                            refreshResult.setValue(false);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing server response during refresh", e);
                        refreshResult.setValue(false);
                    }
                } else {
                    Log.e(TAG, "Server error during refresh: " + 
                          (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    refreshResult.setValue(false);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error during refresh", t);
                refreshResult.setValue(false);
            }
        });
        
        return refreshResult;
    }

    /**
     * Revoke access due to security issues
     */
    private void revokeAccessDueToSecurity() {
        executors.diskIO().execute(() -> {
            try {
                // Update database to revoke access
                coinsDao.updateUnlockStatus(DEFAULT_USER_ID, false, 0, 0);
                
                // Flag this as a security issue in a separate field
                CoinsEntity entity = coinsDao.getCoinsById(DEFAULT_USER_ID);
                if (entity != null) {
                    // We can't add a new field, but we could use the unlock timestamp to store a special value
                    // that indicates security violation, like a negative value
                    coinsDao.updateUnlockStatus(DEFAULT_USER_ID, false, -1, 0);
                }
                
                // Update LiveData
                isUnlockedLiveData.postValue(false);
                remainingTimeLiveData.postValue(0L);
                
                // Clear secure storage
                storeSecurely(SECURE_PREF_UNLOCK_DATA, null);
                storeSecurely(SECURE_PREF_SIGNATURE, null);
                
                // Report to server using the specialized security endpoint
                reportSecurityViolation();
            } catch (Exception e) {
                Log.e(TAG, "Error revoking access due to security issue", e);
            }
        });
    }
    
    /**
     * Report security violation to the server
     */
    private void reportSecurityViolation() {
        try {
            String deviceId = getDeviceId();
            if (deviceId == null) {
                Log.e(TAG, "Cannot report security violation: device ID is null");
                return;
            }
            
            // Create payload with device info
            Map<String, Object> payload = new HashMap<>();
            payload.put("deviceId", deviceId);
            payload.put("securityViolation", true);
            payload.put("deviceInfo", DeviceUtils.getDetailedDeviceInfo(context));
            payload.put("isRooted", DeviceUtils.isDeviceRooted());
            payload.put("isEmulator", DeviceUtils.isEmulator());
            
            // Report to server using the specialized security endpoint
            apiService.reportSecurityViolation(payload).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Security violation reported to server");
                    } else {
                        Log.e(TAG, "Failed to report security violation: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Network error reporting security violation", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error reporting security violation", e);
        }
    }
}