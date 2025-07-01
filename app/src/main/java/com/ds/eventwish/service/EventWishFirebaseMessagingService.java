package com.ds.eventwish.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ds.eventwish.data.auth.AuthManager;
import com.google.firebase.auth.FirebaseAuth;

public class EventWishFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "EventWishFCM";
    private static final String PREF_NAME = "fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private void ensureUserAndUpdateToken(String token) {
        Log.d(TAG, "Ensuring user auth and updating token");
        
        // Save token to SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String oldToken = prefs.getString(KEY_FCM_TOKEN, null);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        
        Log.d(TAG, String.format("Token saved to SharedPreferences - Old: %s, New: %s", 
            oldToken != null ? oldToken.substring(0, Math.min(20, oldToken.length())) + "..." : "null", 
            token.substring(0, Math.min(20, token.length())) + "..."));

        // Note: Removed FirestoreManager.setFcmToken() call
        // FCM token will be sent to backend API during user sync operations
        // This prevents permission errors when user is not authenticated
        
        Log.d(TAG, "FCM token will be synced with backend during next user operation");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Received new FCM token");
        ensureUserAndUpdateToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Received FCM message from: " + remoteMessage.getFrom());
        
        // Handle message here
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
        }
    }
} 