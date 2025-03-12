package com.ds.eventwish.utils;

import android.util.Log;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TimeUtils {
    private static final String TAG = "TimeUtils";
    private static long serverTimeDiff = 0; // Difference between server and device time
    private static String lastSyncDate = null; // Last synced server date string

    // Update the time difference between server and device
    public static void syncWithServerTime(long serverTimestamp, String serverDate) {
        serverTimeDiff = serverTimestamp - System.currentTimeMillis();
        lastSyncDate = serverDate;
        Log.d(TAG, "Time synced with server. Difference: " + serverTimeDiff + "ms");
        Log.d(TAG, "Server date: " + serverDate);
    }

    // Get current time according to server
    public static Date getCurrentServerTime() {
        return new Date(System.currentTimeMillis() + serverTimeDiff);
    }

    // Get current server time in ISO 8601 format
    public static String getCurrentServerTimeISO() {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(getCurrentServerTime());
    }

    // Get Calendar instance with server time
    public static Calendar getServerCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(System.currentTimeMillis() + serverTimeDiff);
        return calendar;
    }

    // Add days to server time
    public static Date addDaysToServerTime(int days) {
        Calendar calendar = getServerCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    // Add months to server time
    public static Date addMonthsToServerTime(int months) {
        Calendar calendar = getServerCalendar();
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }

    // Check if time is synced with server
    public static boolean isTimeSynced() {
        return lastSyncDate != null;
    }

    // Get last sync date
    public static String getLastSyncDate() {
        return lastSyncDate;
    }

    // Get time difference with server in milliseconds
    public static long getServerTimeDifference() {
        return serverTimeDiff;
    }
} 