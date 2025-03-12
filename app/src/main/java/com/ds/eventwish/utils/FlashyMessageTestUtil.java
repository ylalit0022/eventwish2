package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Utility class for testing flashy messages manually without using Firebase
 */
public class FlashyMessageTestUtil {
    private static final String TAG = "FlashyMessageTestUtil";
    
    /**
     * Create a test flashy message that will be shown when the app is opened
     * @param context Application context
     * @param title Message title
     * @param message Message content
     */
    public static void createTestFlashyMessage(Context context, String title, String message) {
        String messageId = UUID.randomUUID().toString();
        
        // Save the flashy message using the FlashyMessageManager
        FlashyMessageManager.saveFlashyMessage(context, messageId, title, message);
        
        Log.d(TAG, "Created test flashy message with ID: " + messageId);
    }
    
    /**
     * Create a default test flashy message
     * @param context Application context
     */
    public static void createDefaultTestFlashyMessage(Context context) {
        createTestFlashyMessage(
            context,
            "Welcome to EventWish!",
            "Thank you for using our app. We hope you enjoy creating and sharing beautiful greeting cards!"
        );
        
        Log.d(TAG, "Created default test flashy message");
    }
    
    /**
     * Clear all flashy messages
     * @param context Application context
     */
    public static void clearAllFlashyMessages(Context context) {
        FlashyMessageManager.clearMessages(context);
        Log.d(TAG, "Cleared all flashy messages");
    }
} 