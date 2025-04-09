package com.ds.eventwish.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * Type converter for Map types in Room database
 */
public class MapTypeConverter {
    private static final String TAG = "MapTypeConverter";
    
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(JsonObject.class, (JsonDeserializer<JsonObject>) (json, typeOfT, context) -> {
                if (json.isJsonObject()) {
                    return json.getAsJsonObject();
                } else if (json.isJsonArray()) {
                    JsonObject obj = new JsonObject();
                    obj.add("array", json.getAsJsonArray());
                    return obj;
                } else if (json.isJsonNull()) {
                    return new JsonObject();
                } else {
                    JsonObject obj = new JsonObject();
                    obj.add("value", json);
                    return obj;
                }
            })
            .create();

    @TypeConverter
    public static String fromStringMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        try {
            return gson.toJson(map);
        } catch (Exception e) {
            Log.e(TAG, "Error converting Map<String, String> to String: " + e.getMessage(), e);
            return null;
        }
    }

    @TypeConverter
    public static Map<String, String> toStringMap(String value) {
        if (value == null) {
            return null;
        }
        try {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            return gson.fromJson(value, mapType);
        } catch (Exception e) {
            Log.e(TAG, "Error converting String to Map<String, String>: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @TypeConverter
    public static String fromObjectMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return gson.toJson(map);
        } catch (Exception e) {
            Log.e(TAG, "Error converting Map<String, Object> to String: " + e.getMessage(), e);
            return null;
        }
    }

    @TypeConverter
    public static Map<String, Object> toObjectMap(String value) {
        if (value == null) {
            return null;
        }
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(value, mapType);
        } catch (Exception e) {
            Log.e(TAG, "Error converting String to Map<String, Object>: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }
} 