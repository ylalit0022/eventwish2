package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.gson.JsonObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository class for managing user registration and activity tracking
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final String PREF_USER_REGISTERED = "user_registered";
    private static final String PREF_LAST_CATEGORY_VISIT = "last_category_visit";
    private static final String PREF_LAST_ACTIVITY_UPDATE = "last_activity_update";
    
    // Minimum time between activity updates (5 minutes)
    private static final long MIN_ACTIVITY_UPDATE_INTERVAL = 5 * 60 * 1000;
    
    private ApiService apiService;
    private final Context context;
    private final SharedPreferences prefs;
    
    private final MutableLiveData<Boolean> isRegistering = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdatingActivity = new MutableLiveData<>(false);
    
    // Singleton instance
    private static UserRepository instance;
    
    /**
     * Get singleton instance of UserRepository
     * @param context Application context
     * @return UserRepository instance
     */
    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private UserRepository(Context context) {
        this.context = context;
        
        // Initialize apiService as null initially to avoid compiler error
        this.apiService = null;
        
        // Ensure SecureTokenManager is initialized before ApiClient
        try {
            // Initialize SecureTokenManager if not already done
            com.ds.eventwish.util.SecureTokenManager.init(context);
            
            // Initialize ApiClient
            ApiClient.init(context);
            this.apiService = ApiClient.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ApiClient: " + e.getMessage());
        }
        
        this.prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        
        // Initialize device ID if not already set
        if (!prefs.contains(PREF_DEVICE_ID)) {
            generateAndSaveDeviceId();
        }
    }
    
    /**
     * Generate a unique device ID and save it in SharedPreferences
     */
    private void generateAndSaveDeviceId() {
        String deviceId;
        
        // Try to use Android ID first (most devices)
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        
        if (androidId != null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {
            // Android ID is available and not the known fake value on some devices
            deviceId = androidId;
        } else {
            // Fallback to generated UUID + device info
            deviceId = UUID.randomUUID().toString() + 
                    "_" + Build.MANUFACTURER + "_" + Build.MODEL;
            
            // Hash the deviceId for privacy
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(deviceId.getBytes());
                StringBuilder hexString = new StringBuilder();
                
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                
                deviceId = hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Error hashing device ID", e);
            }
        }
        
        // Save the deviceId
        prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply();
        Log.d(TAG, "Generated and saved new device ID: " + deviceId);
    }
    
    /**
     * Get the device ID
     * @return Device ID string
     */
    public String getDeviceId() {
        return prefs.getString(PREF_DEVICE_ID, null);
    }
    
    /**
     * Check if user is registered
     * @return true if registered, false otherwise
     */
    public boolean isUserRegistered() {
        return prefs.getBoolean(PREF_USER_REGISTERED, false);
    }
    
    /**
     * Register user with the server (if not already registered)
     */
    public void registerUserIfNeeded() {
        // Skip if already registered or currently registering
        if (isUserRegistered() || Boolean.TRUE.equals(isRegistering.getValue())) {
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot register user: Device ID is null or empty");
            return;
        }
        
        // Use postValue instead of setValue for background thread safety
        isRegistering.postValue(true);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", deviceId);
        
        apiService.registerDeviceUser(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Mark user as registered
                    prefs.edit().putBoolean(PREF_USER_REGISTERED, true).apply();
                    Log.d(TAG, "User registration successful: " + deviceId);
                } else {
                    Log.e(TAG, "User registration failed: " + response.code() + " " + 
                            (response.errorBody() != null ? response.errorBody().toString() : ""));
                }
                
                // Reset registering state
                isRegistering.postValue(false);
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "User registration request failed", t);
                
                // Reset registering state
                isRegistering.postValue(false);
            }
        });
    }
    
    /**
     * Update user activity
     * @param category Optional category name (if a category was visited)
     */
    public void updateUserActivity(@Nullable String category) {
        // Skip if not registered or currently updating
        if (!isUserRegistered() || Boolean.TRUE.equals(isUpdatingActivity.getValue())) {
            if (!isUserRegistered()) {
                Log.d(TAG, "Cannot update activity: User not registered");
            }
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot update activity: Device ID is null or empty");
            return;
        }
        
        // Check if sufficient time has passed since last update
        long lastUpdate = prefs.getLong(PREF_LAST_ACTIVITY_UPDATE, 0);
        long now = System.currentTimeMillis();
        
        // For category visits, also store the last visited category
        if (category != null) {
            String lastCategory = prefs.getString(PREF_LAST_CATEGORY_VISIT, null);
            long timeSinceLastUpdate = now - lastUpdate;
            
            // Rate limit category updates to prevent spam
            if (lastCategory != null && lastCategory.equals(category) && 
                    timeSinceLastUpdate < MIN_ACTIVITY_UPDATE_INTERVAL) {
                Log.d(TAG, "Skipping category update (rate limited): " + category);
                return;
            }
            
            // Save this category as last visited
            prefs.edit().putString(PREF_LAST_CATEGORY_VISIT, category).apply();
        } else {
            // For regular activity updates (no category), rate limit
            if (now - lastUpdate < MIN_ACTIVITY_UPDATE_INTERVAL) {
                Log.d(TAG, "Skipping activity update (rate limited)");
                return;
            }
        }
        
        // Use postValue instead of setValue for background thread safety
        isUpdatingActivity.postValue(true);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", deviceId);
        if (category != null) {
            requestBody.put("category", category);
            requestBody.put("source", "direct");
        }
        
        apiService.updateUserActivity(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Save last update time
                    prefs.edit().putLong(PREF_LAST_ACTIVITY_UPDATE, System.currentTimeMillis()).apply();
                    
                    Log.d(TAG, "User activity update successful: " + deviceId + 
                            (category != null ? ", category: " + category : ""));
                } else {
                    Log.e(TAG, "User activity update failed: " + response.code() + " " + 
                            (response.errorBody() != null ? response.errorBody().toString() : ""));
                }
                
                // Reset updating state
                isUpdatingActivity.postValue(false);
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "User activity update request failed", t);
                
                // Reset updating state
                isUpdatingActivity.postValue(false);
            }
        });
    }
    
    /**
     * Record a template view with its category
     * @param templateId The ID of the template that was viewed
     * @param category The category the template belongs to
     */
    public void recordTemplateView(String templateId, String category) {
        // Skip if not registered or currently updating
        if (!isUserRegistered()) {
            Log.d(TAG, "Cannot record template view: User not registered");
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty() || templateId == null || category == null) {
            Log.e(TAG, "Cannot record template view: Missing required data");
            return;
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", deviceId);
        requestBody.put("templateId", templateId);
        requestBody.put("category", category);
        
        apiService.recordTemplateView(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Template view recorded: " + templateId + " in category: " + category);
                } else {
                    Log.e(TAG, "Failed to record template view: " + response.code() + " " + 
                            (response.errorBody() != null ? response.errorBody().toString() : ""));
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Template view request failed", t);
            }
        });
    }
    
    /**
     * Track a template view - convenience method that calls recordTemplateView
     * @param templateId The ID of the template that was viewed
     * @param category The category the template belongs to
     */
    public void trackTemplateView(String templateId, String category) {
        recordTemplateView(templateId, category);
    }
    
    /**
     * Get personalized recommendations for the user
     * @param callback Callback to receive recommendations
     */
    public void getRecommendations(final RecommendationsCallback callback) {
        // Skip if not registered
        if (!isUserRegistered()) {
            Log.d(TAG, "Cannot get recommendations: User not registered");
            if (callback != null) {
                callback.onFailure("User not registered");
            }
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot get recommendations: Device ID is null or empty");
            if (callback != null) {
                callback.onFailure("Invalid device ID");
            }
            return;
        }
        
        apiService.getUserRecommendations(deviceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    Log.d(TAG, "Recommendations received: " + data);
                    if (callback != null) {
                        callback.onSuccess(data);
                    }
                } else {
                    Log.e(TAG, "Failed to get recommendations: " + response.code());
                    if (callback != null) {
                        callback.onFailure("Failed to get recommendations: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Recommendations request failed", t);
                if (callback != null) {
                    callback.onFailure("Request failed: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * Callback interface for recommendations
     */
    public interface RecommendationsCallback {
        void onSuccess(JsonObject recommendations);
        void onFailure(String errorMessage);
    }
    
    /**
     * Get registration status LiveData
     */
    public LiveData<Boolean> getRegistrationStatus() {
        return isRegistering;
    }
    
    /**
     * Get activity update status LiveData
     */
    public LiveData<Boolean> getActivityUpdateStatus() {
        return isUpdatingActivity;
    }
} 