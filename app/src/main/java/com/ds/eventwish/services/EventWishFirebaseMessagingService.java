package com.ds.eventwish.services;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.repository.TokenRepository;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.FirebaseCrashManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Service to handle Firebase Cloud Messaging (FCM) messages and token refresh
 */
public class EventWishFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "EventWishFCM";
    
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        
        try {
            // Update token in repository
            TokenRepository tokenRepository = TokenRepository.getInstance(
                getApplicationContext(),
                ApiClient.getClient()
            );
            tokenRepository.saveToken(token);
            
            // Track token refresh for analytics
            AnalyticsUtils analytics = AnalyticsUtils.getInstance();
            Bundle params = new Bundle();
            params.putString("token", token);
            analytics.logEvent("fcm_token_refresh", params);
        } catch (Exception e) {
            Log.e(TAG, "Error handling new FCM token", e);
        }
    }
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        
        try {
            // Check if message contains a notification payload
            if (remoteMessage.getNotification() != null) {
                // Handle notification message
                handleNotificationMessage(remoteMessage);
            }
            
            // Check if message contains data payload
            if (remoteMessage.getData().size() > 0) {
                // Handle data message
                handleDataMessage(remoteMessage);
            }
            
            // Track message received for analytics
            AnalyticsUtils analytics = AnalyticsUtils.getInstance();
            Bundle params = new Bundle();
            params.putString("message_id", remoteMessage.getMessageId());
            params.putString("message_type", remoteMessage.getMessageType());
            params.putString("from", remoteMessage.getFrom());
            analytics.logEvent("fcm_message_received", params);
        } catch (Exception e) {
            Log.e(TAG, "Error processing FCM message", e);
            FirebaseCrashManager.logException(e);
        }
    }
    
    /**
     * Handle notification messages
     * @param remoteMessage The message received from FCM
     */
    private void handleNotificationMessage(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        
        if (notification != null) {
            String title = notification.getTitle();
            String body = notification.getBody();
            
            Log.d(TAG, "Message Notification Title: " + title);
            Log.d(TAG, "Message Notification Body: " + body);
            
            if (title != null && body != null) {
                // Display notification
                EventWishNotificationManager.showUpdateNotification(
                        getApplicationContext(),
                        title,
                        body);
            }
        }
    }
    
    /**
     * Handle data messages
     * @param remoteMessage The message received from FCM
     */
    private void handleDataMessage(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message data payload: " + data);
        
        // Check message type
        String type = data.get("type");
        
        if (type != null) {
            switch (type) {
                case "festival":
                    handleFestivalNotification(data);
                    break;
                case "reminder":
                    handleReminderNotification(data);
                    break;
                case "update":
                    handleUpdateNotification(data);
                    break;
                default:
                    Log.d(TAG, "Unknown message type: " + type);
                    break;
            }
        }
    }
    
    /**
     * Handle festival notifications
     * @param data Message data
     */
    private void handleFestivalNotification(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        
        if (title != null && body != null) {
            EventWishNotificationManager.showUpdateNotification(
                    getApplicationContext(),
                    title,
                    body);
        }
    }
    
    /**
     * Handle reminder notifications
     * @param data Message data
     */
    private void handleReminderNotification(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        String reminderIdStr = data.get("reminderId");
        
        if (title != null && body != null && reminderIdStr != null) {
            try {
                long reminderId = Long.parseLong(reminderIdStr);
                EventWishNotificationManager.showReminderNotification(
                        getApplicationContext(),
                        title,
                        body,
                        reminderId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reminder ID: " + reminderIdStr, e);
            }
        }
    }
    
    /**
     * Handle update notifications
     * @param data Message data
     */
    private void handleUpdateNotification(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        
        if (title != null && body != null) {
            EventWishNotificationManager.showUpdateNotification(
                    getApplicationContext(),
                    title,
                    body);
        }
    }
} 