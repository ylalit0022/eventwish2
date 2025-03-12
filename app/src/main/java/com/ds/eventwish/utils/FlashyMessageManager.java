package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to manage flashy messages that appear when the app is opened.
 * These messages are shown only once per message ID.
 */
public class FlashyMessageManager {
    public static final String TAG = "FlashyMessageManager";
    public static final String PREF_NAME = "flashy_message_prefs";
    public static final String KEY_MESSAGES = "flashy_messages";
    private static final String KEY_SHOWN_IDS = "shown_message_ids";
    private static final String KEY_DISPLAY_STATE = "is_displaying";
    
    /**
     * Model class for flashy messages
     */
    public static class FlashyMessage {
        private String id;
        private String title;
        private String message;
        private long timestamp;
        
        public FlashyMessage(String id, String title, String message) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Save a flashy message to be displayed when the app is opened
     */
    public static void saveFlashyMessage(Context context, String messageId, String title, String message) {
        if (context == null || messageId == null || title == null || message == null) {
            Log.e(TAG, "Cannot save flashy message with null parameters");
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Get existing messages
        List<FlashyMessage> messages = getFlashyMessages(context);
        
        // Check if message with this ID already exists
        boolean exists = false;
        for (FlashyMessage existingMessage : messages) {
            if (existingMessage.getId().equals(messageId)) {
                exists = true;
                break;
            }
        }
        
        // Add new message if it doesn't exist
        if (!exists) {
            messages.add(new FlashyMessage(messageId, title, message));
            
            // Save updated list
            Gson gson = new Gson();
            String json = gson.toJson(messages);
            editor.putString(KEY_MESSAGES, json);
            
            // Reset display state to ensure new messages are checked
            editor.putBoolean(KEY_DISPLAY_STATE, false);
            
            editor.apply();
            
            Log.d(TAG, "Saved flashy message with ID: " + messageId);
        } else {
            Log.d(TAG, "Flashy message with ID " + messageId + " already exists");
        }
    }
    
    /**
     * Get all flashy messages that haven't been shown yet
     */
    public static List<FlashyMessage> getFlashyMessages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_MESSAGES, null);
        
        if (json == null) {
            return new ArrayList<>();
        }
        
        Gson gson = new Gson();
        Type type = new TypeToken<List<FlashyMessage>>(){}.getType();
        List<FlashyMessage> messages = gson.fromJson(json, type);
        
        return messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * Get the next flashy message to show, or null if there are no messages
     * or all messages have been shown
     */
    public static FlashyMessage getNextMessage(Context context) {
        List<FlashyMessage> messages = getFlashyMessages(context);
        Set<String> shownIds = getShownMessageIds(context);
        
        // Check if we're currently displaying a message
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isDisplaying = prefs.getBoolean(KEY_DISPLAY_STATE, false);
        
        if (isDisplaying) {
            Log.d(TAG, "Already displaying a flashy message, skipping");
            return null;
        }
        
        // Find the next message that hasn't been shown
        FlashyMessage nextMessage = null;
        for (FlashyMessage message : messages) {
            if (!shownIds.contains(message.getId())) {
                nextMessage = message;
                break;
            }
        }
        
        if (nextMessage != null) {
            // Set the display state to true to prevent multiple messages from being shown
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_DISPLAY_STATE, true);
            editor.apply();
            
            Log.d(TAG, "Found next flashy message with ID: " + nextMessage.getId());
        } else {
            Log.d(TAG, "No more flashy messages to show");
        }
        
        return nextMessage;
    }
    
    /**
     * Mark a message as shown so it won't be displayed again
     */
    public static void markMessageAsShown(Context context, String messageId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> shownIds = getShownMessageIds(context);
        
        // Add the message ID to the set of shown IDs
        shownIds.add(messageId);
        
        // Save the updated set
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_SHOWN_IDS, shownIds);
        
        // Reset the display state so we can show the next message
        editor.putBoolean(KEY_DISPLAY_STATE, false);
        
        editor.apply();
        
        Log.d(TAG, "Marked message " + messageId + " as shown and reset display state");
    }
    
    /**
     * Get the set of message IDs that have already been shown
     */
    private static Set<String> getShownMessageIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_SHOWN_IDS, new HashSet<>()));
    }
    
    /**
     * Clear all flashy messages
     */
    public static void clearMessages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_MESSAGES, null);
        editor.putBoolean(KEY_DISPLAY_STATE, false);
        editor.apply();
        
        Log.d(TAG, "Cleared all flashy messages and reset display state");
    }
    
    /**
     * Reset the display state to allow showing messages again
     */
    public static void resetDisplayState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_DISPLAY_STATE, false);
        editor.apply();
        
        Log.d(TAG, "Reset flashy message display state");
    }
} 