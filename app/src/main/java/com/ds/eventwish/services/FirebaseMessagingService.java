package com.ds.eventwish.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";
    private static final String CHANNEL_ID = "fcm_channel";
    private static final String PREF_NAME = "fcm_preferences";
    private static final String KEY_TOKEN = "fcm_token";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            Log.d(TAG, "From: " + remoteMessage.getFrom());
    
            // Check if message contains a data payload
            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessage.getData());
                
                // Handle message type
                handleDataMessage(remoteMessage.getData());
            }
    
            // Check if message contains a notification payload
            if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
                
                // Send as regular notification
                sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling FCM message", e);
        }
    }
    
    /**
     * Handle data message from FCM
     * @param data Message data
     */
    private void handleDataMessage(Map<String, String> data) {
        try {
            // Handle message type
            String messageType = data.get("type");
            
            if (messageType != null) {
                switch (messageType) {
                    case "notification":
                        // Show regular notification
                        sendNotification(
                            data.get("title"),
                            data.get("message")
                        );
                        break;
                        
                    default:
                        // Default to notification
                        sendNotification(
                            data.get("title"),
                            data.get("message")
                        );
                        break;
                }
            } else {
                // No message type, just show notification with whatever data is available
                sendNotification(
                    data.get("title"),
                    data.get("message")
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling data message", e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        try {
            Log.d(TAG, "Refreshed token: " + token);
            
            // Save the token to shared preferences
            saveTokenToPrefs(token);
            
            // Send token to backend server
            sendRegistrationToServer(token);
        } catch (Exception e) {
            Log.e(TAG, "Error handling FCM token refresh", e);
        }
    }

    private void saveTokenToPrefs(String token) {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_TOKEN, token);
            editor.apply();
            Log.d(TAG, "FCM token saved to preferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving FCM token to preferences", e);
        }
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement API call to send token to backend
        Log.d(TAG, "FCM token registration to server not implemented yet");
    }

    private void sendNotification(String title, String messageBody) {
        try {
            // Check for null values
            if (title == null) title = getString(R.string.app_name);
            if (messageBody == null) messageBody = "";
            
            // Create intent for notification click
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent,
                PendingIntent.FLAG_IMMUTABLE
            );
    
            // Set notification sound
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            
            // Build notification
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(messageBody)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentIntent(pendingIntent);
    
            // Get notification manager
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    
            // Since android Oreo notification channel is needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Firebase Notifications",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications from EventWish");
                channel.enableLights(true);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
    
            // Show notification
            if (notificationManager != null) {
                // Use message hash as notification ID to prevent duplicates
                int notificationId = (title + messageBody).hashCode();
                notificationManager.notify(notificationId, notificationBuilder.build());
                Log.d(TAG, "Notification sent with ID: " + notificationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification", e);
        }
    }
} 