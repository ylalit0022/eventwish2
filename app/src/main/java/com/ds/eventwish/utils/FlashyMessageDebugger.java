package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for debugging flashy message issues
 */
public class FlashyMessageDebugger {
    private static final String TAG = "FlashyMsgDebugger";
    
    /**
     * Dump all flashy messages to logcat for debugging
     * @param context Application context
     * @return Number of messages found
     */
    public static int dumpFlashyMessages(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                FlashyMessageManager.PREF_NAME, Context.MODE_PRIVATE);
            
            String messagesJson = prefs.getString(FlashyMessageManager.KEY_MESSAGES, "[]");
            JSONArray messagesArray = new JSONArray(messagesJson);
            
            Log.d(TAG, "Found " + messagesArray.length() + " flashy messages in storage");
            
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject message = messagesArray.getJSONObject(i);
                Log.d(TAG, "Message " + (i + 1) + ": " + message.toString(2));
            }
            
            if (context != null) {
                Toast.makeText(context, 
                    "Found " + messagesArray.length() + " flashy messages", 
                    Toast.LENGTH_SHORT).show();
            }
            
            return messagesArray.length();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing flashy messages", e);
            
            if (context != null) {
                Toast.makeText(context, 
                    "Error parsing flashy messages: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
            
            return -1;
        }
    }
    
    /**
     * Force refresh flashy messages display
     * @param context Application context
     */
    public static void forceRefreshFlashyMessages(Context context) {
        try {
            // Get current messages
            SharedPreferences prefs = context.getSharedPreferences(
                FlashyMessageManager.PREF_NAME, Context.MODE_PRIVATE);
            
            String messagesJson = prefs.getString(FlashyMessageManager.KEY_MESSAGES, "[]");
            JSONArray messagesArray = new JSONArray(messagesJson);
            
            if (messagesArray.length() == 0) {
                Log.d(TAG, "No flashy messages to refresh");
                
                if (context != null) {
                    Toast.makeText(context, 
                        "No flashy messages to refresh", 
                        Toast.LENGTH_SHORT).show();
                }
                
                return;
            }
            
            // Modify timestamps to force refresh
            JSONArray updatedArray = new JSONArray();
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject message = messagesArray.getJSONObject(i);
                
                // Update timestamp to current time
                message.put("timestamp", System.currentTimeMillis());
                
                updatedArray.put(message);
            }
            
            // Save updated messages
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(FlashyMessageManager.KEY_MESSAGES, updatedArray.toString());
            editor.apply();
            
            Log.d(TAG, "Updated timestamps for " + updatedArray.length() + " flashy messages");
            
            if (context != null) {
                Toast.makeText(context, 
                    "Refreshed " + updatedArray.length() + " flashy messages", 
                    Toast.LENGTH_SHORT).show();
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error refreshing flashy messages", e);
            
            if (context != null) {
                Toast.makeText(context, 
                    "Error refreshing flashy messages: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Check if flashy messages are being displayed
     * @param context Application context
     * @return true if messages should be displayed, false otherwise
     */
    public static boolean checkFlashyMessageDisplayState(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                FlashyMessageManager.PREF_NAME, Context.MODE_PRIVATE);
            
            // Check if messages exist
            String messagesJson = prefs.getString(FlashyMessageManager.KEY_MESSAGES, "[]");
            JSONArray messagesArray = new JSONArray(messagesJson);
            
            // Check if display flag is set
            boolean isDisplaying = prefs.getBoolean("is_displaying", false);
            
            // Log the state
            Log.d(TAG, "Flashy message state: " + messagesArray.length() + 
                " messages, displaying=" + isDisplaying);
            
            if (context != null) {
                Toast.makeText(context, 
                    "Messages: " + messagesArray.length() + ", displaying=" + isDisplaying, 
                    Toast.LENGTH_SHORT).show();
            }
            
            return isDisplaying;
        } catch (JSONException e) {
            Log.e(TAG, "Error checking flashy message state", e);
            return false;
        }
    }
    
    /**
     * Reset flashy message display state
     * @param context Application context
     */
    public static void resetFlashyMessageDisplayState(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                FlashyMessageManager.PREF_NAME, Context.MODE_PRIVATE);
            
            // Reset display flag
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_displaying", false);
            editor.apply();
            
            Log.d(TAG, "Reset flashy message display state");
            
            if (context != null) {
                Toast.makeText(context, 
                    "Reset flashy message display state", 
                    Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting flashy message state", e);
        }
    }
} 