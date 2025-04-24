package com.ds.eventwish.data.converter;

import android.util.Log;
import androidx.room.TypeConverter;
import com.ds.eventwish.data.model.CategoryIcon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Type converter for CategoryIcon objects in Room database
 */
public class CategoryIconConverter {
    private static final String TAG = "CategoryIconConverter";
    
    // Use GsonBuilder to match the same adapter configuration as the model
    // This ensures we use the same serialization/deserialization logic
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(CategoryIcon.class, new CategoryIcon.CategoryIconTypeAdapter())
            .create();
    
    @TypeConverter
    public static CategoryIcon fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            Type type = new TypeToken<CategoryIcon>() {}.getType();
            CategoryIcon icon = gson.fromJson(value, type);
            
            // Validate the deserialized object
            if (icon != null) {
                // Check if icon URL is valid
                if (icon.getCategoryIcon() == null || icon.getCategoryIcon().isEmpty()) {
                    Log.w(TAG, "Deserialized icon has null or empty URL: " + value);
                    // Return the object anyway, as null URL will be handled by UI
                }
                return icon;
            } else {
                Log.e(TAG, "Deserialized to null object from: " + value);
                return null;
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error deserializing CategoryIcon: " + e.getMessage() + ", value: " + value, e);
            // Create a fallback icon with diagnostic info
            return new CategoryIcon(null, "Error", null);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error deserializing CategoryIcon: " + e.getMessage(), e);
            return null;
        }
    }

    @TypeConverter
    public static String toString(CategoryIcon icon) {
        if (icon == null) {
            return null;
        }
        
        try {
            String json = gson.toJson(icon);
            // Validate the JSON before returning
            if (json == null || json.isEmpty() || json.equals("null")) {
                Log.w(TAG, "Serialized icon is null or empty");
                return null;
            }
            return json;
        } catch (Exception e) {
            Log.e(TAG, "Error serializing CategoryIcon: " + e.getMessage(), e);
            // Return a minimal valid JSON object
            return "{\"category\":\"Error\"}";
        }
    }
}