package com.ds.eventwish.services;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.FirebaseCrashManager;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks;
import com.google.firebase.inappmessaging.model.InAppMessage;
import com.google.firebase.inappmessaging.model.MessageType;

/**
 * Handler for Firebase In-App Messaging
 * This class manages the display and tracking of in-app messages
 */
public class FirebaseInAppMessagingHandler implements FirebaseInAppMessagingDisplay {
    private static final String TAG = "FirebaseInAppMsgHandler";
    
    private static FirebaseInAppMessagingHandler instance;
    
    /**
     * Get the singleton instance
     * @return Instance of FirebaseInAppMessagingHandler
     */
    public static FirebaseInAppMessagingHandler getInstance() {
        if (instance == null) {
            instance = new FirebaseInAppMessagingHandler();
        }
        return instance;
    }
    
    /**
     * Initialize the handler
     */
    public void initialize() {
        Log.d(TAG, "Firebase In-App Messaging handler initialized");
        
        try {
            // Register this handler as the display component
            FirebaseInAppMessaging.getInstance().setMessageDisplayComponent(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase In-App Messaging handler", e);
            FirebaseCrashManager.logException(e);
        }
    }
    
    @Override
    public void displayMessage(@NonNull InAppMessage inAppMessage, @NonNull FirebaseInAppMessagingDisplayCallbacks callbacks) {
        Log.d(TAG, "Received in-app message: " + inAppMessage.getCampaignMetadata().getCampaignId());
        
        try {
            // Track impression
            callbacks.impressionDetected();
            
            // Track the message impression in analytics
            AnalyticsUtils analytics = AnalyticsUtils.getInstance();
            Bundle params = new Bundle();
            params.putString("message_id", inAppMessage.getCampaignId());
            params.putString("message_name", inAppMessage.getCampaignName());
            analytics.logEvent("in_app_message_impression", params);
            
            // Handle different message types
            MessageType messageType = inAppMessage.getMessageType();
            
            switch (messageType) {
                case BANNER:
                case CARD:
                case MODAL:
                case IMAGE_ONLY:
                    // Convert in-app message to notification for banner, card, modal, and image types
                    convertToNotification(inAppMessage);
                    break;
                default:
                    Log.d(TAG, "Unsupported message type: " + messageType);
                    break;
            }
            
            // Message was successfully displayed
            callbacks.messageDismissed(FirebaseInAppMessagingDisplayCallbacks.InAppMessagingDismissType.AUTO);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying in-app message", e);
            FirebaseCrashManager.logException(e);
            
            // Report display error
            callbacks.displayErrorEncountered(FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR);
        }
    }
    
    /**
     * Convert an in-app message to a notification
     * @param message The in-app message to convert
     */
    private void convertToNotification(InAppMessage message) {
        try {
            // Extract message data
            String title = "New Message";
            String body = "You have a new message";
            
            // Try to get better title and body from message data
            if (message.getMessageType() == MessageType.BANNER || 
                message.getMessageType() == MessageType.CARD || 
                message.getMessageType() == MessageType.MODAL) {
                
                // Extract title and body if available
                if (message.getTitle() != null && !message.getTitle().getText().isEmpty()) {
                    title = message.getTitle().getText();
                }
                
                if (message.getBody() != null && !message.getBody().getText().isEmpty()) {
                    body = message.getBody().getText();
                }
            }
            
            // Display as a notification
            EventWishNotificationManager.showUpdateNotification(
                    com.ds.eventwish.EventWishApplication.getAppContext(),
                    title,
                    body);
            
            Log.d(TAG, "Converted in-app message to notification: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error converting in-app message to notification", e);
            FirebaseCrashManager.logException(e);
        }
    }
    
    /**
     * Trigger a test event to show in-app messages
     * This can be used for testing Firebase In-App Messaging campaigns
     * @param eventName The event name to trigger
     */
    public void triggerTestEvent(String eventName) {
        try {
            Log.d(TAG, "Triggering test event: " + eventName);
            AnalyticsUtils analytics = AnalyticsUtils.getInstance();
            Bundle params = new Bundle();
            params.putString("message_id", "test_message_id");
            params.putString("message_name", "Test Message");
            params.putString("button_text", "Test Button");
            analytics.logEvent(eventName, params);
        } catch (Exception e) {
            Log.e(TAG, "Error triggering test event", e);
            FirebaseCrashManager.logException(e);
        }
    }
} 