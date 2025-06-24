package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Type converter for Map<String, Object> objects in Room database
 * Handles conversion between JSON string and Map objects for metadata storage
 */
public class MapConverter {
    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

    /**
     * Convert Map<String, Object> to JSON string for storage
     */
    @TypeConverter
    public static String fromMap(Map<String, Object> map) {
        return map == null ? null : gson.toJson(map, mapType);
    }

    /**
     * Convert JSON string to Map<String, Object>
     */
    @TypeConverter
    public static Map<String, Object> toMap(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(value, mapType);
        } catch (JsonSyntaxException e) {
            // Return null if JSON is invalid
            return null;
        }
    }
} 