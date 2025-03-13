package com.ds.eventwish.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Utility class for JSON serialization and deserialization
 */
public class JsonUtils {
    private static final String TAG = "JsonUtils";
    private static final Gson gson = new GsonBuilder().create();
    
    /**
     * Convert an object to JSON string
     * @param object The object to convert
     * @return The JSON string
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            Log.e(TAG, "Error converting object to JSON", e);
            return null;
        }
    }
    
    /**
     * Convert a JSON string to an object
     * @param json The JSON string
     * @param classOfT The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing JSON: " + json, e);
            return null;
        }
    }
    
    /**
     * Parse a JSON string to a JsonObject
     * @param json The JSON string
     * @return The JsonObject
     */
    public static JsonObject parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON to JsonObject: " + json, e);
            return null;
        }
    }
    
    /**
     * Check if a string is valid JSON
     * @param json The string to check
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        
        try {
            JsonParser.parseString(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 