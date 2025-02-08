package com.ds.eventwish.ui.reminder.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.data.local.ReminderDao;

public class ReminderViewModelFactory implements ViewModelProvider.Factory {
    private final ReminderDao reminderDao;

    public ReminderViewModelFactory(ReminderDao reminderDao) {
        this.reminderDao = reminderDao;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ReminderViewModel.class)) {
            return (T) new ReminderViewModel(reminderDao);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
} 