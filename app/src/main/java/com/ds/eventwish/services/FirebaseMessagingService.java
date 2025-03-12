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
import com.ds.eventwish.utils.FlashyMessageManager;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";
    private static final String CHANNEL_ID = "fcm_channel";
    private static final String PREF_NAME = "fcm_preferences";
    private static final String KEY_TOKEN = "fcm_token";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            
            // Handle message type
            String messageType = remoteMessage.getData().get("type");
            
            if (messageType != null) {
                switch (messageType) {
                    case "flashy_message":
                        // Store flashy message to be shown when app is opened
                        String title = remoteMessage.getData().get("title");
                        String message = remoteMessage.getData().get("message");
                        String messageId = remoteMessage.getData().get("message_id");
                        
                        // Save the flashy message to be displayed when app is opened
                        FlashyMessageManager.saveFlashyMessage(this, messageId, title, message);
                        break;
                    
                    case "notification":
                        // Show regular notification
                        sendNotification(
                            remoteMessage.getData().get("title"),
                            remoteMessage.getData().get("message")
                        );
                        break;
                        
                    default:
                        // Default to notification
                        if (remoteMessage.getNotification() != null) {
                            sendNotification(
                                remoteMessage.getNotification().getTitle(),
                                remoteMessage.getNotification().getBody()
                            );
                        }
                        break;
                }
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            
            // Convert Firebase Console notification to flashy message
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            String messageId = remoteMessage.getMessageId();
            
            if (title != null && body != null && messageId != null) {
                // Save as flashy message
                FlashyMessageManager.saveFlashyMessage(this, messageId, title, body);
                Log.d(TAG, "Converted Firebase Console notification to flashy message");
            } else {
                // Fall back to regular notification
                sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
                );
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save the token to shared preferences
        saveTokenToPrefs(token);
        
        // Send token to backend server
        sendRegistrationToServer(token);
    }

    private void saveTokenToPrefs(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement API call to send token to backend
        // This will be implemented in the TokenRepository
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Firebase Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
} 