package com.ds.eventwish.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ds.eventwish.data.model.Reminder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReminderDao {
    private static final String TAG = "ReminderDao";
    private static final String PREF_NAME = "reminders";
    private static final String KEY_REMINDERS = "reminder_list";
    private final SharedPreferences prefs;
    private final Gson gson;

    public ReminderDao(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addReminder(Reminder reminder) {
        Log.d(TAG, "Adding reminder: " + reminder.getTitle());
        List<Reminder> reminders = getAllReminders();
        if (reminders == null) {
            reminders = new ArrayList<>();
        }
        reminder.setId(System.currentTimeMillis()); // Use timestamp as ID
        reminders.add(0, reminder); // Add to start of list (newest first)
        saveReminderList(reminders);
    }

    public void updateReminder(Reminder reminder) {
        Log.d(TAG, "Updating reminder: " + reminder.getId());
        List<Reminder> reminders = getAllReminders();
        int index = -1;
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId() == reminder.getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            reminders.set(index, reminder);
            saveReminderList(reminders);
        } else {
            Log.w(TAG, "Reminder not found for update: " + reminder.getId());
        }
    }

    public void deleteReminder(long reminderId) {
        Log.d(TAG, "Deleting reminder: " + reminderId);
        List<Reminder> reminders = getAllReminders();
        reminders.removeIf(reminder -> reminder.getId() == reminderId);
        saveReminderList(reminders);
    }

    public List<Reminder> getAllReminders() {
        String json = prefs.getString(KEY_REMINDERS, "[]");
        Type type = new TypeToken<List<Reminder>>(){}.getType();
        List<Reminder> reminders = gson.fromJson(json, type);
        Log.d(TAG, "Retrieved " + reminders.size() + " reminders");
        return reminders;
    }

    private void saveReminderList(List<Reminder> reminders) {
        Log.d(TAG, "Saving " + reminders.size() + " reminders");
        String json = gson.toJson(reminders);
        prefs.edit().putString(KEY_REMINDERS, json).apply();
    }

    public void clearAllReminders() {
        Log.d(TAG, "Clearing all reminders");
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_REMINDERS);
        editor.apply();
    }

    public Reminder getReminderById(long reminderId) {
        List<Reminder> reminders = getAllReminders();
        if (reminders != null) {
            for (Reminder reminder : reminders) {
                if (reminder.getId() == reminderId) {
                    return reminder;
                }
            }
        }
        Log.w(TAG, "Reminder not found: " + reminderId);
        return null;
    }
}