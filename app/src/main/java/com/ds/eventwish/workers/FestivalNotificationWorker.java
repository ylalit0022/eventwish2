package com.ds.eventwish.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.NotificationPermissionManager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Worker class to handle festival notifications
 */
public class FestivalNotificationWorker extends Worker {
    private static final String TAG = "FestivalNotifWorker";
    
    // Notification thresholds (in days)
    private static final int[] NOTIFICATION_DAYS = {0, 1, 3, 7, 14, 30};
    
    public FestivalNotificationWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting festival notification worker");
        
        // Check notification permission
        if (!NotificationPermissionManager.hasNotificationPermission(getApplicationContext())) {
            Log.d(TAG, "Notification permission not granted, skipping notifications");
            return Result.failure();
        }
        
        try {
            // Get the repository
            FestivalRepository repository = FestivalRepository.getInstance(getApplicationContext());
            
            // Get upcoming festivals
            List<Festival> festivals = repository.getUpcomingFestivalsSync();
            
            if (festivals == null || festivals.isEmpty()) {
                Log.d(TAG, "No upcoming festivals found");
                return Result.success();
            }
            
            Log.d(TAG, "Found " + festivals.size() + " upcoming festivals");
            
            // Get current date
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Date today = now.getTime();
            
            // Process each festival
            int notificationCount = 0;
            
            for (Festival festival : festivals) {
                // Calculate days until festival
                long diffInMillis = festival.getDate().getTime() - today.getTime();
                int daysUntil = (int) TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
                
                // Check if we should send a notification for this festival
                if (shouldNotify(daysUntil)) {
                    // Send notification
                    int notificationId = EventWishNotificationManager.showFestivalNotification(
                            getApplicationContext(),
                            festival,
                            daysUntil);
                    
                    if (notificationId != -1) {
                        notificationCount++;
                        
                        // Mark festival as notified for this day
                        repository.markAsNotified(festival.getId(), daysUntil);
                    }
                }
            }
            
            Log.d(TAG, "Sent " + notificationCount + " festival notifications");
            
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error processing festival notifications", e);
            return Result.retry();
        }
    }
    
    /**
     * Check if we should send a notification for a festival that is X days away
     * @param daysUntil Days until the festival
     * @return true if we should send a notification, false otherwise
     */
    private boolean shouldNotify(int daysUntil) {
        // Check if daysUntil matches any of our notification thresholds
        for (int day : NOTIFICATION_DAYS) {
            if (daysUntil == day) {
                return true;
            }
        }
        
        return false;
    }
}
