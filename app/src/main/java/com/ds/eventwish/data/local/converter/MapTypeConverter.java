package com.ds.eventwish.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Type converter for Map<String, String> in Room database
 */
public class MapTypeConverter {
    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, String>>(){}.getType();

    @TypeConverter
    public static String fromMap(Map<String, String> map) {
        return map == null ? null : gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, String> toMap(String value) {
        return value == null ? null : gson.fromJson(value, mapType);
    }
} 