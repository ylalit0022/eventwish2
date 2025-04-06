package com.ds.eventwish.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;

import android.util.Log;

/**
 * Type converter for Object in Room database
 */
public class ObjectTypeConverter {
    private static final String TAG = "ObjectTypeConverter";
    
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
    public static String fromObject(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            Log.e(TAG, "Error converting Object to String: " + e.getMessage(), e);
            return null;
        }
    }

    @TypeConverter
    public static Object toObject(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            // First try to parse as JsonObject
            if (value.trim().startsWith("{") && value.trim().endsWith("}")) {
                try {
                    return gson.fromJson(value, JsonObject.class);
                } catch (Exception e) {
                    Log.d(TAG, "Failed to parse as JsonObject, trying LinkedTreeMap: " + e.getMessage());
                }
            }
            
            // If that fails, try to parse as LinkedTreeMap
            return gson.fromJson(value, Object.class);
        } catch (Exception e) {
            Log.e(TAG, "Error converting String to Object: " + e.getMessage(), e);
            return null;
        }
    }
} 