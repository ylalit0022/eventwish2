package com.ds.eventwish.workers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.R;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.utils.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import me.leolin.shortcutbadger.ShortcutBadger;

public class FestivalNotificationWorker extends Worker {
    private static final String TAG = "FestivalNotifWorker";
    private static final String NOTIFICATION_CHANNEL_ID = "festival_channel";
    private static final int NOTIFICATION_ID = 200;
    
    public FestivalNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Checking for upcoming festivals...");
        
        // Get the repository instance
        FestivalRepository repository = FestivalRepository.getInstance(getApplicationContext());
        
        // Get unnotified upcoming festivals
        List<Festival> upcomingFestivals = repository.getUnnotifiedUpcomingFestivals();
        
        if (upcomingFestivals != null && !upcomingFestivals.isEmpty()) {
            Log.d(TAG, "Found " + upcomingFestivals.size() + " upcoming festivals to notify");
            
            // Create notifications for each festival
            for (Festival festival : upcomingFestivals) {
                createFestivalNotification(festival);
                
                // Mark the festival as notified
                repository.markAsNotified(festival.getId());
            }
            
            // Update badge count
            updateBadgeCount(getApplicationContext(), upcomingFestivals.size());
        } else {
            Log.d(TAG, "No upcoming festivals to notify");
        }
        
        return Result.success();
    }
    
    private void createFestivalNotification(Festival festival) {
        Context context = getApplicationContext();
        
        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(festival.getDate());
        
        // Create an intent for when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("FESTIVAL_ID", festival.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Upcoming Festival: " + festival.getName())
                .setContentText(festival.getName() + " is coming up on " + formattedDate)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(festival.getDescription() != null ? 
                                festival.getDescription() : 
                                "Get ready for " + festival.getName() + " on " + formattedDate))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID + festival.hashCode(), builder.build());
    }
    
    private void updateBadgeCount(Context context, int newFestivalsCount) {
        // Get the current unread count from the repository
        FestivalRepository repository = FestivalRepository.getInstance(context);
        int unreadCount = 0;
        
        try {
            // This is a synchronous call to get the current value from LiveData
            // In a real app, you might want to use a different approach
            unreadCount = repository.getUnreadCount().getValue() != null ? 
                    repository.getUnreadCount().getValue() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unread count", e);
        }
        
        // Update the badge count
        ShortcutBadger.applyCount(context, unreadCount);
    }
}
