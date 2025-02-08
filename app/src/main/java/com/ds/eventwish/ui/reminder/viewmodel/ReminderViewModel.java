package com.ds.eventwish.ui.reminder.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.local.ReminderDao;

import java.util.ArrayList;
import java.util.List;

public class ReminderViewModel extends ViewModel {
    private final MutableLiveData<List<Reminder>> reminders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ReminderDao reminderDao;

    public ReminderViewModel(ReminderDao reminderDao) {
        this.reminderDao = reminderDao;
        loadReminders();
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadReminders() {
        List<Reminder> allReminders = reminderDao.getAllReminders();
        reminders.setValue(allReminders);
    }

    public void addReminder(Reminder reminder) {
        reminderDao.saveReminder(reminder);
        loadReminders();
    }

    public void updateReminder(Reminder reminder) {
        reminderDao.updateReminder(reminder);
        loadReminders();
    }

    public void deleteReminder(long reminderId) {
        reminderDao.deleteReminder(reminderId);
        loadReminders();
    }

    public LiveData<Reminder> getReminder(long reminderId) {
        MutableLiveData<Reminder> reminderLiveData = new MutableLiveData<>();
        List<Reminder> allReminders = reminderDao.getAllReminders();
        for (Reminder reminder : allReminders) {
            if (reminder.getId() == reminderId) {
                reminderLiveData.setValue(reminder);
                break;
            }
        }
        return reminderLiveData;
    }
} 