package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.ds.eventwish.data.local.FlashyMessageDatabase;
import com.ds.eventwish.data.local.dao.FlashyMessageDao;
import com.ds.eventwish.data.local.entity.FlashyMessageEntity;
import com.ds.eventwish.utils.AppExecutors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class FlashyMessageRepository {
    private static final String TAG = "FlashyMessageRepo";
    private static final String PREF_NAME = "flashy_message_prefs";
    private static final String KEY_MESSAGES = "flashy_messages";

    private final FlashyMessageDao flashyMessageDao;
    private final Context context;
    private final Executor diskIO;

    private static volatile FlashyMessageRepository instance;

    private FlashyMessageRepository(Context context) {
        this.context = context.getApplicationContext();
        FlashyMessageDatabase database = FlashyMessageDatabase.getInstance(context);
        this.flashyMessageDao = database.flashyMessageDao();
        this.diskIO = AppExecutors.getInstance().diskIO();
        
        // Migrate existing data from SharedPreferences
        migrateFromSharedPreferences();
    }

    public static FlashyMessageRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FlashyMessageRepository.class) {
                if (instance == null) {
                    instance = new FlashyMessageRepository(context);
                }
            }
        }
        return instance;
    }

    public void saveMessage(String id, String title, String message) {
        diskIO.execute(() -> {
            try {
                FlashyMessageEntity entity = new FlashyMessageEntity(id, title, message);
                flashyMessageDao.insert(entity);
                Log.d(TAG, "Saved flashy message with ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error saving flashy message", e);
            }
        });
    }

    public LiveData<List<FlashyMessageEntity>> getUnreadMessages() {
        return flashyMessageDao.getUnreadMessages();
    }

    public LiveData<List<FlashyMessageEntity>> getAllMessages() {
        return flashyMessageDao.getAllMessages();
    }

    public void markMessageAsRead(String messageId) {
        diskIO.execute(() -> {
            try {
                flashyMessageDao.markAsRead(messageId);
                Log.d(TAG, "Marked message as read: " + messageId);
            } catch (Exception e) {
                Log.e(TAG, "Error marking message as read", e);
            }
        });
    }

    public void updateDisplayingState(String messageId, boolean isDisplaying) {
        diskIO.execute(() -> {
            try {
                flashyMessageDao.updateDisplayingState(messageId, isDisplaying);
                Log.d(TAG, "Updated displaying state for message " + messageId + ": " + isDisplaying);
            } catch (Exception e) {
                Log.e(TAG, "Error updating displaying state", e);
            }
        });
    }

    public void resetAllDisplayingStates() {
        diskIO.execute(() -> {
            try {
                flashyMessageDao.resetAllDisplayingStates();
                Log.d(TAG, "Reset all displaying states");
            } catch (Exception e) {
                Log.e(TAG, "Error resetting displaying states", e);
            }
        });
    }

    public void getNextMessageToDisplay(MessageCallback callback) {
        diskIO.execute(() -> {
            try {
                FlashyMessageEntity message = flashyMessageDao.getNextMessageToDisplay();
                if (message != null) {
                    updateDisplayingState(message.getId(), true);
                }
                AppExecutors.getInstance().mainThread().execute(() -> callback.onMessageLoaded(message));
            } catch (Exception e) {
                Log.e(TAG, "Error getting next message", e);
                AppExecutors.getInstance().mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void clearAllMessages() {
        diskIO.execute(() -> {
            try {
                flashyMessageDao.deleteAll();
                Log.d(TAG, "Cleared all flashy messages");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing messages", e);
            }
        });
    }

    private void migrateFromSharedPreferences() {
        diskIO.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String messagesJson = prefs.getString(KEY_MESSAGES, "[]");
                JSONArray messagesArray = new JSONArray(messagesJson);

                List<FlashyMessageEntity> messages = new ArrayList<>();
                for (int i = 0; i < messagesArray.length(); i++) {
                    JSONObject messageObj = messagesArray.getJSONObject(i);
                    FlashyMessageEntity entity = new FlashyMessageEntity(
                            messageObj.getString("id"),
                            messageObj.getString("title"),
                            messageObj.getString("message")
                    );
                    entity.setTimestamp(messageObj.optLong("timestamp", System.currentTimeMillis()));
                    messages.add(entity);
                }

                if (!messages.isEmpty()) {
                    flashyMessageDao.insertAll(messages);
                    Log.d(TAG, "Migrated " + messages.size() + " messages from SharedPreferences");

                    // Clear SharedPreferences after successful migration
                    prefs.edit().remove(KEY_MESSAGES).apply();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error migrating from SharedPreferences", e);
            }
        });
    }

    public interface MessageCallback {
        void onMessageLoaded(FlashyMessageEntity message);
        void onError(Exception e);
    }
} 