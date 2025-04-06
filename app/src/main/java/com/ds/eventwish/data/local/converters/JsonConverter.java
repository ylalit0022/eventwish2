package com.ds.eventwish.data.local.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type converter for JSON objects in Room database
 */
public class JsonConverter {
    private static final Gson gson = new Gson();
    
    /**
     * Convert JsonObject to String
     * @param jsonObject JsonObject
     * @return String representation of JsonObject or null if jsonObject is null
     */
    @TypeConverter
    public static String fromJsonObject(JsonObject jsonObject) {
        return jsonObject == null ? null : gson.toJson(jsonObject);
    }
    
    /**
     * Convert String to JsonObject
     * @param jsonString String representation of JsonObject
     * @return JsonObject or null if jsonString is null
     */
    @TypeConverter
    public static JsonObject toJsonObject(String jsonString) {
        if (jsonString == null) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(jsonString);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert List<String> to String
     * @param list List of strings
     * @return String representation of list or null if list is null
     */
    @TypeConverter
    public static String fromStringList(List<String> list) {
        return list == null ? null : gson.toJson(list);
    }
    
    /**
     * Convert String to List<String>
     * @param jsonString String representation of list
     * @return List of strings or null if jsonString is null
     */
    @TypeConverter
    public static List<String> toStringList(String jsonString) {
        if (jsonString == null) {
            return null;
        }
        try {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            return gson.fromJson(jsonString, type);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert Map<String, String> to String
     * @param map Map of string to string
     * @return String representation of map or null if map is null
     */
    @TypeConverter
    public static String fromStringMap(Map<String, String> map) {
        return map == null ? null : gson.toJson(map);
    }
    
    /**
     * Convert String to Map<String, String>
     * @param jsonString String representation of map
     * @return Map of string to string or null if jsonString is null
     */
    @TypeConverter
    public static Map<String, String> toStringMap(String jsonString) {
        if (jsonString == null) {
            return null;
        }
        try {
            Type type = new TypeToken<HashMap<String, String>>() {}.getType();
            return gson.fromJson(jsonString, type);
        } catch (Exception e) {
            return null;
        }
    }
} 