package com.ds.eventwish.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.ds.eventwish.data.model.Reminder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ReminderDao {
    private static final String PREF_NAME = "reminders";
    private static final String KEY_REMINDERS = "reminder_list";
    private final SharedPreferences prefs;
    private final Gson gson;

    public ReminderDao(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveReminder(Reminder reminder) {
        List<Reminder> reminders = getAllReminders();
        reminder.setId(System.currentTimeMillis()); // Use timestamp as ID
        reminders.add(0, reminder); // Add to start of list (newest first)
        saveReminderList(reminders);
    }

    public void updateReminder(Reminder reminder) {
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
        }
    }

    public void deleteReminder(long reminderId) {
        List<Reminder> reminders = getAllReminders();
        reminders.removeIf(reminder -> reminder.getId() == reminderId);
        saveReminderList(reminders);
    }

    public List<Reminder> getAllReminders() {
        String json = prefs.getString(KEY_REMINDERS, "[]");
        Type type = new TypeToken<List<Reminder>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void saveReminderList(List<Reminder> reminders) {
        String json = gson.toJson(reminders);
        prefs.edit().putString(KEY_REMINDERS, json).apply();
    }
} 