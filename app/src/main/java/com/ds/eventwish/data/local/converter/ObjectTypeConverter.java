package com.ds.eventwish.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;
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
            if (object instanceof JsonObject) {
                return gson.toJson(object);
            } else if (object instanceof Map) {
                JsonObject jsonObject = convertMapToJsonObject((Map<String, Object>) object);
                return gson.toJson(jsonObject);
            }
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
                    try {
                        // If that fails, try to parse as LinkedTreeMap and convert to JsonObject
                        LinkedTreeMap<String, Object> map = gson.fromJson(value, LinkedTreeMap.class);
                        return convertMapToJsonObject(map);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to convert LinkedTreeMap to JsonObject: " + ex.getMessage());
                    }
                }
            }
            
            // If all else fails, try to parse as Object
            return gson.fromJson(value, Object.class);
        } catch (Exception e) {
            Log.e(TAG, "Error converting String to Object: " + e.getMessage(), e);
            return null;
        }
    }

    private static JsonObject convertMapToJsonObject(Map<String, Object> map) {
        JsonObject jsonObject = new JsonObject();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object mapValue = entry.getValue();
            
            if (mapValue == null) {
                jsonObject.addProperty(key, (String) null);
            } else if (mapValue instanceof String) {
                jsonObject.addProperty(key, (String) mapValue);
            } else if (mapValue instanceof Number) {
                jsonObject.addProperty(key, (Number) mapValue);
            } else if (mapValue instanceof Boolean) {
                jsonObject.addProperty(key, (Boolean) mapValue);
            } else if (mapValue instanceof Map) {
                // Recursively convert nested maps
                JsonObject nestedObject = convertMapToJsonObject((Map<String, Object>) mapValue);
                jsonObject.add(key, nestedObject);
            } else {
                // For other types, convert to string
                jsonObject.addProperty(key, mapValue.toString());
            }
        }
        
        return jsonObject;
    }
} 