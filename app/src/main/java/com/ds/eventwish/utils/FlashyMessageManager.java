package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;

import com.ds.eventwish.data.local.entity.FlashyMessageEntity;
import com.ds.eventwish.data.repository.FlashyMessageRepository;

/**
 * Utility class to manage flashy messages that appear when the app is opened.
 * These messages are shown only once per message ID.
 */
public class FlashyMessageManager {
    public static final String TAG = "FlashyMessageManager";
    
    // SharedPreferences constants
    public static final String PREF_NAME = "flashy_messages";
    public static final String KEY_MESSAGES = "messages";

    /**
     * Save a flashy message to be displayed when the app is opened
     */
    public static void saveFlashyMessage(Context context, String messageId, String title, String message) {
        if (context == null || messageId == null || title == null || message == null) {
            Log.e(TAG, "Cannot save flashy message with null parameters");
            return;
        }

        FlashyMessageRepository repository = FlashyMessageRepository.getInstance(context);
        repository.saveMessage(messageId, title, message);
    }

    /**
     * Get the next flashy message to show, or null if there are no messages
     * or all messages have been shown
     */
    public static void getNextMessage(Context context, MessageCallback callback) {
        FlashyMessageRepository repository = FlashyMessageRepository.getInstance(context);
        repository.getNextMessageToDisplay(new FlashyMessageRepository.MessageCallback() {
            @Override
            public void onMessageLoaded(FlashyMessageEntity message) {
                callback.onMessageLoaded(message != null ? 
                    new Message(message.getId(), message.getTitle(), message.getMessage()) : null);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting next message", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Mark a message as shown so it won't be displayed again
     */
    public static void markMessageAsShown(Context context, String messageId) {
        if (context == null || messageId == null) {
            Log.e(TAG, "Cannot mark message as shown with null parameters");
            return;
        }

        FlashyMessageRepository repository = FlashyMessageRepository.getInstance(context);
        repository.markMessageAsRead(messageId);
        repository.updateDisplayingState(messageId, false);
    }

    /**
     * Reset the display state to allow showing messages again
     */
    public static void resetDisplayState(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot reset display state with null context");
            return;
        }

        FlashyMessageRepository repository = FlashyMessageRepository.getInstance(context);
        repository.resetAllDisplayingStates();
    }

    /**
     * Clear all flashy messages
     */
    public static void clearMessages(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot clear messages with null context");
            return;
        }

        FlashyMessageRepository repository = FlashyMessageRepository.getInstance(context);
        repository.clearAllMessages();
    }

    /**
     * Model class for flashy messages
     */
    public static class Message {
        private final String id;
        private final String title;
        private final String message;

        public Message(String id, String title, String message) {
            this.id = id;
            this.title = title;
            this.message = message;
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
    }

    /**
     * Callback interface for asynchronous message operations
     */
    public interface MessageCallback {
        void onMessageLoaded(Message message);
        void onError(Exception e);
    }
} 