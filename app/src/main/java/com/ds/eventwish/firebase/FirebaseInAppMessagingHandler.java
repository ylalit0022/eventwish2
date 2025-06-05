package com.ds.eventwish.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.model.Action;
import com.google.firebase.inappmessaging.model.InAppMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton handler for Firebase In-App Messaging
 */
public class FirebaseInAppMessagingHandler {
    private static final String TAG = "FirebaseInAppMessaging";
    private static FirebaseInAppMessagingHandler instance;
    private FirebaseInAppMessaging firebaseInAppMessaging;
    private Context context;
    private boolean isInitialized = false;

    private FirebaseInAppMessagingHandler() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initialize the Firebase In-App Messaging Handler
     * @param context Application context
     */
    public static void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize FirebaseInAppMessagingHandler with null context");
            return;
        }
        
        try {
            if (instance == null) {
                synchronized (FirebaseInAppMessagingHandler.class) {
                    if (instance == null) {
                        instance = new FirebaseInAppMessagingHandler();
                        instance.context = context.getApplicationContext();
                        instance.initializeInAppMessaging();
                        Log.d(TAG, "FirebaseInAppMessagingHandler initialized successfully");
                    }
                }
            } else {
                Log.d(TAG, "FirebaseInAppMessagingHandler already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseInAppMessagingHandler: " + e.getMessage(), e);
            // We don't rethrow to allow the app to continue even with initialization errors
        }
    }

    /**
     * Get the singleton instance of FirebaseInAppMessagingHandler
     * @return FirebaseInAppMessagingHandler instance
     */
    public static FirebaseInAppMessagingHandler getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FirebaseInAppMessagingHandler not initialized. Call init(Context) first.");
        }
        return instance;
    }

    /**
     * Initialize Firebase In-App Messaging
     */
    private void initializeInAppMessaging() {
        try {
            firebaseInAppMessaging = FirebaseInAppMessaging.getInstance();
            
            // Don't set this as display component since it doesn't implement the interface
            // Let Firebase handle the display automatically
            
            isInitialized = true;
            Log.d(TAG, "Firebase In-App Messaging initialized successfully");
            
            // Add more firebase initialization logic here if needed
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase In-App Messaging: " + e.getMessage(), e);
            isInitialized = false;
        }
    }

    /**
     * Set up listeners for In-App Messaging events
     */
    private void setupInAppMessagingListeners() {
        // Implementation of any required listeners
    }

    /**
     * Trigger a test event for Firebase In-App Messaging
     * @param eventName Name of the event to trigger
     */
    public void triggerTestEvent(String eventName) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot trigger event. Firebase In-App Messaging not initialized");
            return;
        }

        try {
            Log.d(TAG, "Triggering test event: " + eventName);
            Map<String, String> params = new HashMap<>();
            params.put("source", "test");
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Programmatically trigger the event that would show an in-app message
            firebaseInAppMessaging.triggerEvent(eventName);
            
            Log.d(TAG, "Test event triggered successfully: " + eventName);
        } catch (Exception e) {
            Log.e(TAG, "Error triggering test event: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Firebase In-App Messaging is initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
} 