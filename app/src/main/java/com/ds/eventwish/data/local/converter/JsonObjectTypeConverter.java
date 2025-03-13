package com.ds.eventwish.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;

/**
 * Type converter for JsonObject in Room database
 */
public class JsonObjectTypeConverter {
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
    public static String fromJsonObject(JsonObject jsonObject) {
        return jsonObject == null ? null : gson.toJson(jsonObject);
    }

    @TypeConverter
    public static JsonObject toJsonObject(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            // First try to parse as JsonObject directly
            return gson.fromJson(value, JsonObject.class);
        } catch (Exception e) {
            try {
                // If that fails, try to parse as LinkedTreeMap and convert to JsonObject
                LinkedTreeMap<String, Object> map = gson.fromJson(value, LinkedTreeMap.class);
                JsonObject jsonObject = new JsonObject();
                
                // Convert LinkedTreeMap to JsonObject
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
                    } else if (mapValue instanceof LinkedTreeMap) {
                        // Recursively convert nested maps
                        String nestedJson = gson.toJson(mapValue);
                        JsonObject nestedObject = toJsonObject(nestedJson);
                        jsonObject.add(key, nestedObject);
                    } else {
                        // For other types, convert to string
                        jsonObject.addProperty(key, mapValue.toString());
                    }
                }
                
                return jsonObject;
            } catch (Exception ex) {
                // If all else fails, return an empty JsonObject
                android.util.Log.e("JsonObjectTypeConverter", "Error converting to JsonObject: " + ex.getMessage());
                return new JsonObject();
            }
        }
    }
} 