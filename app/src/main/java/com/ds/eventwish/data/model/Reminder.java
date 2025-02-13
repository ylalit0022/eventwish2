package com.ds.eventwish.data.model;

import java.util.Calendar;

public class Reminder {
    private long id;
    private String title;
    private String description;
    private long dateTime;
    private boolean completed;
    private Priority priority;
    private boolean isRepeating;
    private int repeatInterval; // in days

    public enum Priority {
        HIGH(0),
        MEDIUM(1),
        LOW(2);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Priority fromValue(int value) {
            for (Priority priority : Priority.values()) {
                if (priority.value == value) {
                    return priority;
                }
            }
            return MEDIUM; // Default priority
        }
    }

    public Reminder(String title, String description, long dateTime) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.priority = Priority.MEDIUM;
        this.completed = false;
        this.isRepeating = false;
        this.repeatInterval = 0;
    }

    public Reminder(String title, String description, long dateTime, Priority priority, boolean isRepeating, int repeatInterval) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.priority = priority;
        this.completed = false;
        this.isRepeating = isRepeating;
        this.repeatInterval = repeatInterval;
    }

    public Reminder() {
        this.priority = Priority.MEDIUM;
        this.completed = false;
        this.isRepeating = false;
        this.repeatInterval = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isRepeating() {
        return isRepeating;
    }

    public void setRepeating(boolean repeating) {
        isRepeating = repeating;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public boolean isToday() {
        Calendar reminderCal = Calendar.getInstance();
        reminderCal.setTimeInMillis(dateTime);
        
        Calendar todayCal = Calendar.getInstance();
        
        return reminderCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
               reminderCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);
    }

    public void scheduleNextRepetition() {
        if (isRepeating && repeatInterval > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dateTime);
            calendar.add(Calendar.DAY_OF_YEAR, repeatInterval);
            dateTime = calendar.getTimeInMillis();
            completed = false;
        }
    }

    public boolean isThisWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        long weekStart = calendar.getTimeInMillis();

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        long weekEnd = calendar.getTimeInMillis();

        return dateTime >= weekStart && dateTime < weekEnd;
    }
}