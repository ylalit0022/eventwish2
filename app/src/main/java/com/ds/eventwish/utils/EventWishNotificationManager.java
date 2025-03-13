package com.ds.eventwish.utils;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Festival;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to manage notifications
 */
public class EventWishNotificationManager {
    private static final String TAG = "EventWishNotifManager";
    
    // Notification channel IDs
    public static final String CHANNEL_FESTIVALS = "channel_festivals";
    public static final String CHANNEL_REMINDERS = "channel_reminders";
    public static final String CHANNEL_UPDATES = "channel_updates";
    
    // Notification channel groups
    public static final String GROUP_EVENTS = "group_events";
    public static final String GROUP_SYSTEM = "group_system";
    
    // Notification ID generator
    private static final AtomicInteger notificationIdGenerator = new AtomicInteger(1000);
    
    private EventWishNotificationManager() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Create notification channels for Android O and above
     * @param context Application context
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels");
            
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }
            
            // Create channel groups
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Events group
                NotificationChannelGroup eventsGroup = new NotificationChannelGroup(
                        GROUP_EVENTS,
                        context.getString(R.string.notification_group_events));
                
                // System group
                NotificationChannelGroup systemGroup = new NotificationChannelGroup(
                        GROUP_SYSTEM,
                        context.getString(R.string.notification_group_system));
                
                notificationManager.createNotificationChannelGroups(
                        java.util.Arrays.asList(eventsGroup, systemGroup));
            }
            
            // Create festival channel
            NotificationChannel festivalChannel = new NotificationChannel(
                    CHANNEL_FESTIVALS,
                    context.getString(R.string.notification_channel_festivals),
                    NotificationManager.IMPORTANCE_HIGH);
            festivalChannel.setDescription(context.getString(R.string.notification_channel_festivals_desc));
            festivalChannel.enableLights(true);
            festivalChannel.setLightColor(Color.BLUE);
            festivalChannel.enableVibration(true);
            festivalChannel.setVibrationPattern(new long[]{0, 500, 250, 500});
            festivalChannel.setShowBadge(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                festivalChannel.setGroup(GROUP_EVENTS);
            }
            
            // Create reminder channel
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.notification_channel_reminders),
                    NotificationManager.IMPORTANCE_HIGH);
            reminderChannel.setDescription(context.getString(R.string.notification_channel_reminders_desc));
            reminderChannel.enableLights(true);
            reminderChannel.setLightColor(Color.RED);
            reminderChannel.enableVibration(true);
            reminderChannel.setVibrationPattern(new long[]{0, 500, 250, 500});
            reminderChannel.setShowBadge(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reminderChannel.setGroup(GROUP_EVENTS);
            }
            
            // Create updates channel
            NotificationChannel updatesChannel = new NotificationChannel(
                    CHANNEL_UPDATES,
                    context.getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT);
            updatesChannel.setDescription(context.getString(R.string.notification_channel_updates_desc));
            updatesChannel.enableLights(true);
            updatesChannel.setLightColor(Color.GREEN);
            updatesChannel.enableVibration(false);
            updatesChannel.setShowBadge(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatesChannel.setGroup(GROUP_SYSTEM);
            }
            
            // Create the channels
            notificationManager.createNotificationChannels(
                    java.util.Arrays.asList(festivalChannel, reminderChannel, updatesChannel));
            
            Log.d(TAG, "Notification channels created");
        }
    }
    
    /**
     * Show a notification for a festival
     * @param context Application context
     * @param festival Festival to show notification for
     * @param daysUntil Days until the festival
     * @return Notification ID or -1 if failed
     */
    public static int showFestivalNotification(Context context, Festival festival, int daysUntil) {
        // Check notification permission
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            Log.d(TAG, "Notification permission not granted");
            return -1;
        }
        
        try {
            Log.d(TAG, "Showing notification for festival: " + festival.getName());
            
            // Generate a unique notification ID
            int notificationId = notificationIdGenerator.getAndIncrement();
            
            // Format the date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(festival.getDate());
            
            // Create an intent for when the notification is tapped
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("FESTIVAL_ID", festival.getId());
            intent.putExtra("navigate_to", "festival");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setData(android.net.Uri.parse("festival://" + festival.getId()));
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            // Determine the notification content based on days until
            String title;
            String content;
            
            if (daysUntil == 0) {
                // Today
                title = context.getString(R.string.notification_festival_today_title, festival.getName());
                content = context.getString(R.string.notification_festival_today_content, festival.getName());
            } else if (daysUntil == 1) {
                // Tomorrow
                title = context.getString(R.string.notification_festival_tomorrow_title, festival.getName());
                content = context.getString(R.string.notification_festival_tomorrow_content, festival.getName());
            } else {
                // X days away
                title = context.getString(R.string.notification_festival_upcoming_title, festival.getName());
                content = context.getString(R.string.notification_festival_upcoming_content, 
                        festival.getName(), daysUntil, formattedDate);
            }
            
            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FESTIVALS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Show the notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification shown with ID: " + notificationId);
                return notificationId;
            } else {
                Log.e(TAG, "NotificationManager is null");
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
            return -1;
        }
    }
    
    /**
     * Show a notification for a reminder
     * @param context Application context
     * @param title Reminder title
     * @param content Reminder content
     * @param reminderId Reminder ID
     * @return Notification ID or -1 if failed
     */
    public static int showReminderNotification(Context context, String title, String content, long reminderId) {
        // Check notification permission
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            Log.d(TAG, "Notification permission not granted");
            return -1;
        }
        
        try {
            Log.d(TAG, "Showing notification for reminder: " + title);
            
            // Generate a unique notification ID
            int notificationId = notificationIdGenerator.getAndIncrement();
            
            // Create an intent for when the notification is tapped
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("REMINDER_ID", reminderId);
            intent.putExtra("navigate_to", "reminder");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setData(android.net.Uri.parse("reminder://" + reminderId));
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Show the notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification shown with ID: " + notificationId);
                return notificationId;
            } else {
                Log.e(TAG, "NotificationManager is null");
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
            return -1;
        }
    }
    
    /**
     * Show a notification for an update
     * @param context Application context
     * @param title Update title
     * @param content Update content
     * @return Notification ID or -1 if failed
     */
    public static int showUpdateNotification(Context context, String title, String content) {
        // Check notification permission
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            Log.d(TAG, "Notification permission not granted");
            return -1;
        }
        
        try {
            Log.d(TAG, "Showing notification for update: " + title);
            
            // Generate a unique notification ID
            int notificationId = notificationIdGenerator.getAndIncrement();
            
            // Create an intent for when the notification is tapped
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_UPDATES)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Show the notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification shown with ID: " + notificationId);
                return notificationId;
            } else {
                Log.e(TAG, "NotificationManager is null");
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
            return -1;
        }
    }
    
    /**
     * Cancel a notification
     * @param context Application context
     * @param notificationId Notification ID to cancel
     */
    public static void cancelNotification(Context context, int notificationId) {
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.cancel(notificationId);
                Log.d(TAG, "Notification cancelled with ID: " + notificationId);
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification", e);
        }
    }
    
    /**
     * Cancel all notifications
     * @param context Application context
     */
    public static void cancelAllNotifications(Context context) {
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.cancelAll();
                Log.d(TAG, "All notifications cancelled");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling all notifications", e);
        }
    }
} 