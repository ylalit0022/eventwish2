package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.ds.eventwish.data.model.CategoryIcon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import android.util.Log;

/**
 * Room database converter for CategoryIcon objects with enhanced parsing capabilities
 * and fallback mechanisms for handling various JSON formats.
 */
public class CategoryIconConverter {
    private static final String TAG = "CategoryIconConverter";
    
    // Use GsonBuilder to create a configurable Gson instance
    private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();
    
    // Cache for repeated conversions
    private static final java.util.Map<String, CategoryIcon> parseCache = 
            new java.util.concurrent.ConcurrentHashMap<>(32);
    private static final int MAX_CACHE_SIZE = 100;
    
    /**
     * Convert CategoryIcon object to String for storage in database
     * @param categoryIcon The CategoryIcon to convert
     * @return JSON string representation
     */
    @TypeConverter
    public static String fromCategoryIcon(CategoryIcon categoryIcon) {
        if (categoryIcon == null) return null;
        
        try {
            // Add emoji indicator for better log scanning
            Log.d(TAG, "🔄 Converting CategoryIcon to JSON: " + categoryIcon);
            return gson.toJson(categoryIcon);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting CategoryIcon to JSON: " + e.getMessage(), e);
            // Return direct URL if available as fallback
            if (categoryIcon.getCategoryIcon() != null) {
                Log.d(TAG, "⚠️ Using URL fallback: " + categoryIcon.getCategoryIcon());
                return categoryIcon.getCategoryIcon();
            }
            return null;
        }
    }

    /**
     * Convert String to CategoryIcon for retrieval from database with enhanced format support
     * @param value The string value to convert
     * @return Parsed CategoryIcon object or null
     */
    @TypeConverter
    public static CategoryIcon toCategoryIcon(String value) {
        if (value == null) return null;
        
        // Trim value to avoid whitespace issues
        value = value.trim();
        if (value.isEmpty()) return null;
        
        // Check cache first for performance
        CategoryIcon cachedResult = parseCache.get(value);
        if (cachedResult != null) {
            Log.d(TAG, "✅ Found in cache: " + value.substring(0, Math.min(20, value.length())) + "...");
            return cachedResult;
        }
        
        Log.d(TAG, "🔍 Converting to CategoryIcon: " + 
              (value.length() > 100 ? value.substring(0, 100) + "..." : value));
        
        CategoryIcon result = parseValue(value);
        
        // Add to cache if successful and not too large
        if (result != null && parseCache.size() < MAX_CACHE_SIZE) {
            parseCache.put(value, result);
            
            // Clean cache if it gets too large
            if (parseCache.size() >= MAX_CACHE_SIZE) {
                Log.d(TAG, "⚠️ Parse cache full, clearing older entries");
                // Simple strategy: just clear half the entries
                java.util.Iterator<String> it = parseCache.keySet().iterator();
                for (int i = 0; i < MAX_CACHE_SIZE / 2 && it.hasNext(); i++) {
                    it.next();
                    it.remove();
                }
            }
        }
        
        return result;
    }
    
    /**
     * Core parsing logic extracted to a separate method for better organization
     */
    private static CategoryIcon parseValue(String value) {
        try {
            // Handle different JSON formats
            if (value.startsWith("[")) {
                return parseArrayFormat(value);
            } else if (value.startsWith("{")) {
                return parseObjectFormat(value);
            } else {
                return parseStringValue(value);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "❌ JSON parsing error: " + e.getMessage() + ", raw value: " + 
                  (value.length() > 50 ? value.substring(0, 50) + "..." : value), e);
            return new CategoryIcon(null, null, value);
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected error: " + e.getMessage() + ", raw value: " + 
                  (value.length() > 50 ? value.substring(0, 50) + "..." : value), e);
            return new CategoryIcon(null, null, value);
        }
    }
    
    /**
     * Parse JSON array format
     */
    private static CategoryIcon parseArrayFormat(String value) {
        Log.d(TAG, "📊 Detected JSON Array format");
        try {
            CategoryIcon[] icons = gson.fromJson(value, CategoryIcon[].class);
            if (icons != null && icons.length > 0) {
                Log.d(TAG, "✅ Successfully parsed array with " + icons.length + " icons");
                return icons[0];
            } else {
                Log.w(TAG, "⚠️ Empty array or null result from JSON parsing");
                return new CategoryIcon(null, null, value);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "❌ Error parsing JSON array: " + e.getMessage() + ", trying as JsonElement");
            // Try parsing as JsonElement and extract the first element
            try {
                JsonElement element = JsonParser.parseString(value);
                if (element.isJsonArray() && element.getAsJsonArray().size() > 0) {
                    JsonElement firstElement = element.getAsJsonArray().get(0);
                    if (firstElement.isJsonObject()) {
                        return extractFromJsonObject(firstElement.getAsJsonObject());
                    } else if (firstElement.isJsonPrimitive()) {
                        String iconUrl = firstElement.getAsString();
                        Log.d(TAG, "📝 Extracted URL from array element: " + iconUrl);
                        return new CategoryIcon(null, null, iconUrl);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "❌ Failed second attempt to parse JSON array: " + ex.getMessage());
            }
            return new CategoryIcon(null, null, value);
        }
    }
    
    /**
     * Parse JSON object format
     */
    private static CategoryIcon parseObjectFormat(String value) {
        Log.d(TAG, "📋 Detected JSON Object format");
        try {
            // First try standard parsing using the enhanced TypeAdapter
            CategoryIcon icon = gson.fromJson(value, CategoryIcon.class);
            if (icon != null && icon.getCategoryIcon() != null) {
                Log.d(TAG, "✅ Successfully parsed JSON object to CategoryIcon");
                return icon;
            } else {
                Log.w(TAG, "⚠️ Null or incomplete result from standard parsing, trying manual extraction");
                // Try manually extracting fields
                JsonObject jsonObject = JsonParser.parseString(value).getAsJsonObject();
                return extractFromJsonObject(jsonObject);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing JSON object: " + e.getMessage() + ", trying manual extraction");
            try {
                JsonObject jsonObject = JsonParser.parseString(value).getAsJsonObject();
                return extractFromJsonObject(jsonObject);
            } catch (Exception ex) {
                Log.e(TAG, "❌ Failed manual JSON extraction: " + ex.getMessage());
                return new CategoryIcon(null, null, value);
            }
        }
    }
    
    /**
     * Parse string value (URL or simple identifier)
     */
    private static CategoryIcon parseStringValue(String value) {
        // Check if it's a URL (basic check)
        if (value.startsWith("http://") || value.startsWith("https://") || 
            value.startsWith("file://") || value.startsWith("/") || 
            value.contains(".jpg") || value.contains(".png") || 
            value.contains(".gif") || value.contains(".svg")) {
            
            Log.d(TAG, "🔗 Treating as direct URL value: " + value);
            return new CategoryIcon(null, null, value);
        } else {
            // Try to parse as JSON before giving up
            try {
                JsonElement element = JsonParser.parseString(value);
                if (element.isJsonObject()) {
                    Log.d(TAG, "📋 Parsed string as JSON object");
                    return extractFromJsonObject(element.getAsJsonObject());
                }
            } catch (Exception e) {
                // Not JSON, treat as string value
                Log.d(TAG, "📝 Not a URL or JSON, treating as string identifier: " + value);
            }
            return new CategoryIcon(null, null, value);
        }
    }
    
    /**
     * Helper method to extract CategoryIcon fields from a JsonObject with enhanced field checking
     */
    private static CategoryIcon extractFromJsonObject(JsonObject jsonObject) {
        String id = null;
        String category = null;
        String iconUrl = null;
        
        try {
            // Extract ID from various possible field names
            for (String idField : new String[]{"_id", "id", "ID", "categoryId"}) {
                if (jsonObject.has(idField) && !jsonObject.get(idField).isJsonNull()) {
                    id = getStringFromElement(jsonObject.get(idField));
                    if (id != null) break;
                }
            }
            
            // Extract category from various possible field names
            for (String catField : new String[]{"category", "categoryName", "name", "type"}) {
                if (jsonObject.has(catField) && !jsonObject.get(catField).isJsonNull()) {
                    category = getStringFromElement(jsonObject.get(catField));
                    if (category != null) break;
                }
            }
            
            // Check all possible field names for icon URL
            String[] iconFields = {
                "categoryIcon", "icon", "url", "src", "uri", "imageUrl", 
                "image", "iconUrl", "thumbnail", "href", "link"
            };
            
            for (String field : iconFields) {
                if (jsonObject.has(field) && !jsonObject.get(field).isJsonNull()) {
                    JsonElement iconElement = jsonObject.get(field);
                    if (iconElement.isJsonPrimitive()) {
                        iconUrl = iconElement.getAsString();
                        break;
                    } else if (iconElement.isJsonObject()) {
                        // The icon itself might be a nested object with a URL field
                        JsonObject iconObject = iconElement.getAsJsonObject();
                        for (String urlField : new String[]{"url", "src", "uri", "href", "link"}) {
                            if (iconObject.has(urlField) && !iconObject.get(urlField).isJsonNull()) {
                                iconUrl = iconObject.get(urlField).getAsString();
                                if (iconUrl != null) break;
                            }
                        }
                        if (iconUrl != null) break;
                    }
                }
            }
            
            Log.d(TAG, "📝 Manually extracted from JSON: id=" + id + ", category=" + category + ", iconUrl=" + iconUrl);
            
            // If no icon URL found but we have a category, we might want to generate a fallback URL
            if (iconUrl == null && category != null) {
                // This could be replaced with actual fallback logic to generate URLs based on category
                Log.d(TAG, "⚠️ No icon URL found for category: " + category);
            }
            
            return new CategoryIcon(id, category, iconUrl);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting fields from JsonObject: " + e.getMessage(), e);
            return new CategoryIcon(null, null, jsonObject.toString());
        }
    }
    
    /**
     * Helper method to safely extract string from JsonElement regardless of underlying type
     */
    private static String getStringFromElement(JsonElement element) {
        try {
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            } else if (element.isJsonObject() || element.isJsonArray()) {
                return element.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting element to string: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Clear the parse cache - useful for testing or when memory pressure is high
     */
    public static void clearCache() {
        parseCache.clear();
        Log.d(TAG, "🧹 Parse cache cleared");
    }
}