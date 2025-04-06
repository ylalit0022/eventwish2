package com.ds.eventwish.ui.reminder.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.utils.ReminderException;
import com.ds.eventwish.utils.ReminderScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import android.util.Log;

// Create a stub for ShortcutBadger
class ShortcutBadger {
    public static boolean applyCount(Context context, int count) {
        Log.d("ShortcutBadger", "Applying badge count: " + count + " (stub implementation)");
        return true;
    }
}

public class ReminderViewModel extends ViewModel {
    private final ReminderDao reminderDao;
    private final Context context;
    private final MutableLiveData<List<Reminder>> reminders;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> error;
    private final MutableLiveData<Integer> badgeCount;
    private final MutableLiveData<Integer> todayRemindersCount = new MutableLiveData<>(0);
    private Filter currentFilter;
    private boolean isAppInForeground = false;

    public static class Factory implements ViewModelProvider.Factory {
        private final Context context;

        public Factory(Context context) {
            this.context = context.getApplicationContext();
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ReminderViewModel.class)) {
                return (T) new ReminderViewModel(new ReminderDao(context), context);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    public enum Filter {
        ALL,
        TODAY,
        UPCOMING,
        COMPLETED
    }

    public ReminderViewModel(ReminderDao reminderDao, Context context) {
        this.reminderDao = reminderDao;
        this.context = context.getApplicationContext();
        this.reminders = new MutableLiveData<>(new ArrayList<>());
        this.isLoading = new MutableLiveData<>(false);
        this.error = new MutableLiveData<>();
        this.badgeCount = new MutableLiveData<>(0);
        this.currentFilter = Filter.ALL;
        loadReminders();
    }

    public void loadReminders() {
        isLoading.setValue(true);
        try {
            List<Reminder> allReminders = reminderDao.getAllReminders();
            if (allReminders != null) {
                applyFilter(allReminders);
                updateBadgeCount(allReminders);
                updateTodayRemindersCount(allReminders);
            } else {
                reminders.setValue(new ArrayList<>());
                badgeCount.setValue(0);
                todayRemindersCount.setValue(0);
            }
        } catch (Exception e) {
            error.setValue("Failed to load reminders");
            Log.e("ReminderViewModel", "Error loading reminders: " + e.getMessage(), e);
        } finally {
            isLoading.setValue(false);
        }
    }

    private void applyFilter(List<Reminder> allReminders) {
        if (allReminders == null) {
            reminders.setValue(new ArrayList<>());
            return;
        }

        List<Reminder> filteredList;
        long now = System.currentTimeMillis();

        switch (currentFilter) {
            case TODAY:
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.setTimeInMillis(today.getTimeInMillis());
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                
                filteredList = allReminders.stream()
                    .filter(r -> {
                        long reminderTime = r.getDateTime();
                        return reminderTime >= today.getTimeInMillis() && 
                               reminderTime < tomorrow.getTimeInMillis();
                    })
                    .collect(Collectors.toList());
                break;
            case UPCOMING:
                filteredList = allReminders.stream()
                    .filter(r -> r.getDateTime() > now && !r.isCompleted())
                    .sorted((r1, r2) -> Long.compare(r1.getDateTime(), r2.getDateTime()))
                    .collect(Collectors.toList());
                break;
            case COMPLETED:
                filteredList = allReminders.stream()
                    .filter(Reminder::isCompleted)
                    .sorted((r1, r2) -> Long.compare(r2.getDateTime(), r1.getDateTime()))
                    .collect(Collectors.toList());
                break;
            default:
                filteredList = new ArrayList<>(allReminders);
                Collections.sort(filteredList, 
                    (r1, r2) -> Long.compare(r1.getDateTime(), r2.getDateTime()));
        }

        reminders.setValue(filteredList);
    }

    private void updateBadgeCount(List<Reminder> allReminders) {
        if (allReminders == null) {
            badgeCount.setValue(0);
            return;
        }

        int count = (int) allReminders.stream()
                .filter(Reminder::isUnread)
                .count();
        badgeCount.setValue(count);
        
        // Update app icon badge (optional)
        try {
            ShortcutBadger.applyCount(context, count);
        } catch (Exception e) {
            Log.e("ReminderViewModel", "Error updating badge: " + e.getMessage(), e);
        }
    }

    public void saveReminder(Reminder reminder) {
        isLoading.setValue(true);
        try {
            // If the reminder has an ID, check if it's a restore operation
            if (reminder.getId() > 0) {
                Reminder existingReminder = reminderDao.getReminderById(reminder.getId());
                if (existingReminder == null) {
                    // This is a restore operation, preserve the original ID
                    reminderDao.addReminder(reminder);
                } else {
                    // This is an update operation
                    reminderDao.updateReminder(reminder);
                }
            } else {
                // This is a new reminder
                reminderDao.addReminder(reminder);
            }
            
            // Schedule notification if needed
            if (!reminder.isCompleted() && reminder.getDateTime() > System.currentTimeMillis()) {
                ReminderScheduler.scheduleReminder(context, reminder);
            }
            
            // Get the updated list directly from DAO
            List<Reminder> allReminders = reminderDao.getAllReminders();
            applyFilter(allReminders);
            updateBadgeCount(allReminders);
        } catch (ReminderException e) {
            error.setValue(e.getUserFriendlyMessage());
            Log.e("ReminderViewModel", e.getErrorMessage(), e);
        } catch (Exception e) {
            error.setValue("Failed to save reminder");
            Log.e("ReminderViewModel", "Error saving reminder: " + e.getMessage(), e);
        } finally {
            isLoading.setValue(false);
        }
    }

    public void deleteReminder(Reminder reminder) {
        isLoading.setValue(true);
        try {
            reminderDao.deleteReminder(reminder.getId());
            ReminderScheduler.cancelReminder(context, reminder.getId());
            // Get the updated list directly from DAO
            List<Reminder> allReminders = reminderDao.getAllReminders();
            applyFilter(allReminders);
            updateBadgeCount(allReminders);
        } catch (Exception e) {
            error.setValue("Failed to delete reminder");
            Log.e("ReminderViewModel", "Error deleting reminder: " + e.getMessage(), e);
        } finally {
            isLoading.setValue(false);
        }
    }

    public void updateReminder(Reminder reminder) {
        isLoading.setValue(true);
        try {
            reminderDao.updateReminder(reminder);
            ReminderScheduler.cancelReminder(context, reminder.getId());
            if (!reminder.isCompleted() && reminder.getDateTime() > System.currentTimeMillis()) {
                ReminderScheduler.scheduleReminder(context, reminder);
            }
            // Get the updated list directly from DAO
            List<Reminder> allReminders = reminderDao.getAllReminders();
            applyFilter(allReminders);
            updateBadgeCount(allReminders);
        } catch (ReminderException e) {
            error.setValue(e.getUserFriendlyMessage());
            Log.e("ReminderViewModel", e.getErrorMessage(), e);
        } catch (Exception e) {
            error.setValue("Failed to update reminder");
            Log.e("ReminderViewModel", "Error updating reminder: " + e.getMessage(), e);
        } finally {
            isLoading.setValue(false);
        }
    }

    public void toggleReminderCompleted(Reminder reminder) {
        reminder.setCompleted(!reminder.isCompleted());
        updateReminder(reminder);
    }

    public void clearAllReminders() {
        isLoading.setValue(true);
        try {
            List<Reminder> allReminders = reminderDao.getAllReminders();
            if (allReminders != null) {
                for (Reminder reminder : allReminders) {
                    ReminderScheduler.cancelReminder(context, reminder.getId());
                }
            }
            reminderDao.clearAllReminders();
            reminders.setValue(new ArrayList<>());
            badgeCount.setValue(0);
        } catch (Exception e) {
            error.setValue("Failed to clear reminders");
            Log.e("ReminderViewModel", "Error clearing reminders: " + e.getMessage(), e);
        } finally {
            isLoading.setValue(false);
        }
    }

    public void checkOverdueReminders() {
        List<Reminder> allReminders = reminderDao.getAllReminders();
        if (allReminders == null) return;

        long now = System.currentTimeMillis();
        boolean hasChanges = false;

        for (Reminder reminder : allReminders) {
            try {
                if (!reminder.isCompleted() && reminder.getDateTime() <= now) {
                    if (reminder.isRepeating()) {
                        // Create next reminder
                        Reminder nextReminder = new Reminder();
                        nextReminder.setId(System.currentTimeMillis());
                        nextReminder.setTitle(reminder.getTitle());
                        nextReminder.setDescription(reminder.getDescription());
                        nextReminder.setPriority(reminder.getPriority());
                        nextReminder.setRepeating(true);
                        nextReminder.setRepeatInterval(reminder.getRepeatInterval());
                        nextReminder.setDateTime(reminder.getDateTime() + 
                            TimeUnit.DAYS.toMillis(reminder.getRepeatInterval()));
                        nextReminder.setCompleted(false);
                        saveReminder(nextReminder);
                    }
                    
                    // Mark original reminder as completed
                    reminder.setCompleted(true);
                    reminderDao.updateReminder(reminder);
                    ReminderScheduler.cancelReminder(context, reminder.getId());
                    hasChanges = true;
                }

            } catch (Exception e) {
                Log.e("ReminderViewModel", "Error processing overdue reminder " + 
                    reminder.getId() + ": " + e.getMessage(), e);
            }
        }

        if (hasChanges) {
            loadReminders();
        }
    }

    public void markAllAsRead() {
        reminderDao.markAllAsRead();
        loadReminders();
    }

    public void markAsRead(long reminderId) {
        reminderDao.markAsRead(reminderId);
        loadReminders();
    }

    public LiveData<Integer> getBadgeCount() {
        return badgeCount;
    }

    public ReminderDao getReminderDao() {
        return reminderDao;
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setAppInForeground(boolean isInForeground) {
        this.isAppInForeground = isInForeground;
        if (isInForeground) {
            clearBadgeCount(); // Clear badge count when app is visible
        }
    }

    public boolean isAppInForeground() {
        return isAppInForeground;
    }

    public void clearBadgeCount() {
        badgeCount.setValue(0);
    }

    public void setFilter(Filter filter) {
        if (this.currentFilter != filter) {
            this.currentFilter = filter;
            loadReminders();
        }
    }

    public Filter getCurrentFilter() {
        return currentFilter;
    }

    public void onResume() {
        isAppInForeground = true;
        checkOverdueReminders();
    }

    public void onPause() {
        isAppInForeground = false;
    }

    /**
     * Update the count of today's reminders
     */
    private void updateTodayRemindersCount(List<Reminder> allReminders) {
        if (allReminders == null) {
            todayRemindersCount.setValue(0);
            return;
        }
        
        // Get today's start and end time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();
        
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long tomorrowStart = calendar.getTimeInMillis();
        
        // Count reminders for today
        int count = 0;
        for (Reminder reminder : allReminders) {
            if (!reminder.isCompleted() && 
                reminder.getDateTime() >= todayStart && 
                reminder.getDateTime() < tomorrowStart) {
                count++;
            }
        }
        
        todayRemindersCount.setValue(count);
        Log.d("ReminderViewModel", "Today's reminders count: " + count);
    }

    /**
     * Get the LiveData for today's reminders count
     */
    public LiveData<Integer> getTodayRemindersCount() {
        return todayRemindersCount;
    }
}