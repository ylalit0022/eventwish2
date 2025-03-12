package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Utility class to log Firebase tokens for verification
 */
public class FirebaseTokenLogger {
    private static final String TAG = "FirebaseTokenLogger";
    
    /**
     * Log the Firebase FCM token to verify Firebase integration
     * @param context Application context
     * @param showToast Whether to show the token in a toast (for debugging)
     */
    public static void logFirebaseToken(Context context, boolean showToast) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "Firebase FCM Token: " + token);
                
                // Optionally show token in a toast for easy copying
                if (showToast && context != null) {
                    Toast.makeText(context, "FCM Token: " + token, Toast.LENGTH_LONG).show();
                }
            });
    }
    
    /**
     * Get the Firebase Installation ID for testing in-app messaging
     * @param context Application context
     */
    public static void logFirebaseInstallationId(Context context) {
        try {
            com.google.firebase.installations.FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String id = task.getResult();
                        Log.d(TAG, "Firebase Installation ID: " + id);
                        
                        // Show ID in a toast for easy copying
                        if (context != null) {
                            Toast.makeText(context, "Firebase Installation ID: " + id, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Failed to get Firebase Installation ID", task.getException());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error getting Firebase Installation ID", e);
        }
    }
} 