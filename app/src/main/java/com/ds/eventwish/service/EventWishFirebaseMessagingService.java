package com.ds.eventwish.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ds.eventwish.data.remote.FirestoreManager;

public class EventWishFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "EventWishFCM";
    private static final String PREF_NAME = "EventWish";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received: " + token);
        
        try {
            // Save token to SharedPreferences first
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String oldToken = prefs.getString(KEY_FCM_TOKEN, null);
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
            
            Log.d(TAG, String.format("Token saved to SharedPreferences - Old: %s, New: %s", 
                oldToken != null ? oldToken : "null", 
                token));

            // Save token to FirestoreManager
            FirestoreManager.getInstance().setFcmToken(token);
            Log.d(TAG, "Token successfully set in FirestoreManager");
            
            // Verify token was set correctly
            String currentToken = FirestoreManager.getInstance().getCurrentUserId();
            Log.d(TAG, String.format("Verification - Current token in FirestoreManager: %s", 
                currentToken != null ? currentToken : "null"));
                
            if (!token.equals(currentToken)) {
                Log.e(TAG, "Token verification failed - tokens don't match!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving FCM token", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        
        // Verify token is still valid
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedToken = prefs.getString(KEY_FCM_TOKEN, null);
        String currentToken = FirestoreManager.getInstance().getCurrentUserId();
        
        Log.d(TAG, String.format("Token check on message - Saved: %s, Current: %s", 
            savedToken != null ? savedToken : "null",
            currentToken != null ? currentToken : "null"));
            
        if (savedToken == null || !savedToken.equals(currentToken)) {
            Log.w(TAG, "Token mismatch detected, requesting new token");
            requestNewToken();
        }
    }
    
    private void requestNewToken() {
        try {
            FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Old token deleted successfully");
                    } else {
                        Log.e(TAG, "Failed to delete old token", task.getException());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error requesting new token", e);
        }
    }
} 