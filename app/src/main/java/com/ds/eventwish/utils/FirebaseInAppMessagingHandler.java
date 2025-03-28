package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ds.eventwish.R;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks;
import com.google.firebase.inappmessaging.model.InAppMessage;
import com.google.firebase.inappmessaging.model.MessageType;

import java.util.UUID;

/**
 * Handler for Firebase In-App Messaging
 */
public class FirebaseInAppMessagingHandler {
    private static final String TAG = "FirebaseInAppMsgHandler";
    
    /**
     * Initialize Firebase In-App Messaging
     * @param context Application context
     */
    public static void initialize(Context context) {
        // Register custom display component
        FirebaseInAppMessaging.getInstance().setMessageDisplayComponent(
            (inAppMessage, callbacks) -> {
                Log.d(TAG, "Received in-app message: " + inAppMessage.getMessageType());
                
                try {
                    // Extract message details
                    String title = "New Message";
                    String message = "You have a new message from EventWish";
                    
                    // Log message details
                    Log.d(TAG, "In-app message type: " + inAppMessage.getMessageType());
                    Log.d(TAG, "In-app message data: " + inAppMessage.getData());
                    
                    // Generate a unique ID for the message
                    String messageId = UUID.randomUUID().toString();
                    
                    // Show notification instead
                    NotificationHelper.createNotification(
                        context,
                        title,
                        message
                    );
                    Log.d(TAG, "Displayed in-app message as notification");
                    
                    // Trigger impression event
                    callbacks.impressionDetected();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing in-app message", e);
                    callbacks.displayErrorEncountered(
                        FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR);
                }
            }
        );
        
        Log.d(TAG, "Firebase In-App Messaging handler initialized");
    }
    
    /**
     * Trigger a test event for in-app messaging
     * @param context Application context
     * @param eventName Name of the event to trigger
     */
    public static void triggerEvent(Context context, String eventName) {
        try {
            FirebaseInAppMessaging.getInstance().triggerEvent(eventName);
            Log.d(TAG, "Triggered in-app messaging event: " + eventName);
            
            // Show toast for confirmation
            if (context != null) {
                Toast.makeText(context, "Triggered event: " + eventName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering in-app message event", e);
            if (context != null) {
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
} 