package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ds.eventwish.data.remote.ApiService;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing FCM tokens
 */
public class TokenRepository {
    private static final String TAG = "TokenRepository";
    private static final String PREF_NAME = "fcm_preferences";
    private static final String KEY_TOKEN = "fcm_token";
    private static final String KEY_TOKEN_SENT = "fcm_token_sent";
    
    private final ApiService apiService;
    private final SharedPreferences preferences;
    
    private static TokenRepository instance;
    
    public static synchronized TokenRepository getInstance(Context context, ApiService apiService) {
        if (instance == null) {
            instance = new TokenRepository(context, apiService);
        }
        return instance;
    }
    
    private TokenRepository(Context context, ApiService apiService) {
        this.apiService = apiService;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Save the FCM token to shared preferences
     */
    public void saveToken(String token) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot save null or empty token");
            return;
        }
        
        String existingToken = getToken();
        
        // Only update if token has changed
        if (!token.equals(existingToken)) {
            preferences.edit()
                    .putString(KEY_TOKEN, token)
                    .putBoolean(KEY_TOKEN_SENT, false)
                    .apply();
            
            Log.d(TAG, "Saved new FCM token: " + token);
            
            // Send the token to the server
            sendTokenToServer(token);
        }
    }
    
    /**
     * Get the saved FCM token
     */
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }
    
    /**
     * Send the FCM token to the server
     */
    public void sendTokenToServer(String token) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot send null or empty token to server");
            return;
        }
        
        // Check if token has already been sent
        boolean tokenSent = preferences.getBoolean(KEY_TOKEN_SENT, false);
        if (tokenSent) {
            Log.d(TAG, "Token already sent to server: " + token);
            return;
        }
        
        // Create JSON object with token
        JsonObject tokenObject = new JsonObject();
        tokenObject.addProperty("token", token);
        
        // Send token to server
        apiService.registerToken(tokenObject).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Token sent to server successfully");
                    
                    // Mark token as sent
                    preferences.edit()
                            .putBoolean(KEY_TOKEN_SENT, true)
                            .apply();
                } else {
                    Log.e(TAG, "Failed to send token to server: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error sending token to server", t);
            }
        });
    }
    
    /**
     * Check if the token needs to be sent to the server and send it if needed
     */
    public void checkAndSendToken() {
        String token = getToken();
        boolean tokenSent = preferences.getBoolean(KEY_TOKEN_SENT, false);
        
        if (token != null && !token.isEmpty() && !tokenSent) {
            sendTokenToServer(token);
        }
    }
} 