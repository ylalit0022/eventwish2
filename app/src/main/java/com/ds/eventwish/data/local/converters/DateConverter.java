package com.ds.eventwish.data.local.converters;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Type converter for Date objects in Room database
 */
public class DateConverter {
    /**
     * Convert Date to Long timestamp
     * @param date Date object
     * @return Long timestamp or null if date is null
     */
    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }
    
    /**
     * Convert Long timestamp to Date
     * @param timestamp Long timestamp
     * @return Date object or null if timestamp is null
     */
    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }
}
