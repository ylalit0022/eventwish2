package com.ds.eventwish;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class TestNotificationActivity extends AppCompatActivity {
    private static final String TAG = "TestNotification";
    private static final String CHANNEL_ID = "test_channel";
    private static final int NOTIFICATION_ID = 1;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_notification);

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                    showTestNotification();
                } else {
                    Log.e(TAG, "Notification permission denied");
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        );

        Button testButton = findViewById(R.id.btnTestNotification);
        testButton.setOnClickListener(v -> checkAndShowNotification());
    }

    private void checkAndShowNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                showTestNotification();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            showTestNotification();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Test Channel",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for testing notifications");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setShowBadge(true);
            channel.setBypassDnd(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Created notification channel");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        }
    }

    private void showTestNotification() {
        Log.d(TAG, "Attempting to show test notification");
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test Notification")
            .setContentText("This is a test notification")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 500, 250, 500})
            .setLights(Color.RED, 1000, 1000);

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Test notification sent successfully");
            Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception showing notification: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
