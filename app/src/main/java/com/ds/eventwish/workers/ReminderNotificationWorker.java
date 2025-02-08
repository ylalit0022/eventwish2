package com.ds.eventwish.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import android.Manifest;

public class ReminderNotificationWorker extends Worker {
    private static final String CHANNEL_ID = "reminder_channel";
    private static final String TAG = "ReminderNotificationWorker";
    
    public ReminderNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString("title");
        String description = getInputData().getString("description");
        
        createNotificationChannel();
        
        try {
            if (checkNotificationPermission()) {
                showNotification(title, description);
                return Result.success();
            } else {
                return Result.failure();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: " + e.getMessage());
            return Result.failure();
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(getApplicationContext(), 
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            
            NotificationManager manager = getApplicationContext()
                .getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String description) throws SecurityException {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("reminderId", getInputData().getLong("reminderId", -1));
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            getApplicationContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(
                getApplicationContext().getResources(),
                R.mipmap.ic_launcher
            ))
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = 
            NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
} 