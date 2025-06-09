package com.ds.eventwish.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

public class EventWishFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "EventWishFCM";
    private static final String PREF_NAME = "EventWish";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private void ensureUserAndUpdateToken(String token) {
        Log.d(TAG, "Ensuring user auth and updating token");
        
        // First save token to SharedPreferences for backward compatibility
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String oldToken = prefs.getString(KEY_FCM_TOKEN, null);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        
        Log.d(TAG, String.format("Token saved to SharedPreferences - Old: %s, New: %s", 
            oldToken != null ? oldToken : "null", 
            token));

        // Check if user is already signed in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            updateToken(token);
            return;
        }

        // Sign in anonymously if needed
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener(result -> {
                Log.d(TAG, "Anonymous sign in successful: " + result.getUser().getUid());
                updateToken(token);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Anonymous sign in failed", e);
                // Still try to update token for backward compatibility
                updateToken(token);
            });
    }

    private void updateToken(String token) {
        FirestoreManager.getInstance().setFcmToken(token)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated in Firestore"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update token in Firestore", e));
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received: " + token);
        ensureUserAndUpdateToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        
        // Verify auth and token are still valid
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user found, requesting token refresh");
            requestNewToken();
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedToken = prefs.getString(KEY_FCM_TOKEN, null);
        
        if (savedToken == null) {
            Log.w(TAG, "No saved token found, requesting new token");
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