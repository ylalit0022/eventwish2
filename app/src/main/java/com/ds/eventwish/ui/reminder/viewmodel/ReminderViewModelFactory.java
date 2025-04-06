package com.ds.eventwish.ui.reminder.viewmodel;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.data.local.ReminderDao;

public class ReminderViewModelFactory implements ViewModelProvider.Factory {
    private final ReminderDao reminderDao;
    private final Context context;

    public ReminderViewModelFactory(ReminderDao reminderDao, Context context) {
        this.reminderDao = reminderDao;
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ReminderViewModel.class)) {
            return (T) new ReminderViewModel(reminderDao, context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
} 