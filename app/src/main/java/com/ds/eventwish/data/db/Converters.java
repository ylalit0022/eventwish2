package com.ds.eventwish.data.db;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type converters for Room database.
 */
public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static JsonObject fromJsonObject(String value) {
        return value == null ? null : gson.fromJson(value, JsonObject.class);
    }

    @TypeConverter
    public static String toJsonObject(JsonObject jsonObject) {
        return jsonObject == null ? null : gson.toJson(jsonObject);
    }

    @TypeConverter
    public static Map<String, String> fromStringMap(String value) {
        if (value == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(value, mapType);
    }

    @TypeConverter
    public static String toStringMap(Map<String, String> map) {
        return map == null ? null : gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, Object> fromObjectMap(String value) {
        if (value == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(value, mapType);
    }

    @TypeConverter
    public static String toObjectMap(Map<String, Object> map) {
        return map == null ? null : gson.toJson(map);
    }

    @TypeConverter
    public static List<String> fromStringList(String value) {
        if (value == null) {
            return null;
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toStringList(List<String> list) {
        return list == null ? null : gson.toJson(list);
    }
} 