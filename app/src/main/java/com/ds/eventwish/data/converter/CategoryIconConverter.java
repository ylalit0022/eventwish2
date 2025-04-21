package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.ds.eventwish.data.model.CategoryIcon;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Type converter for CategoryIcon objects in Room database
 */
public class CategoryIconConverter {
    private static final Gson gson = new Gson();
    
    @TypeConverter
    public static CategoryIcon fromString(String value) {
        if (value == null) {
            return null;
        }
        Type type = new TypeToken<CategoryIcon>() {}.getType();
        return gson.fromJson(value, type);
    }

    @TypeConverter
    public static String toString(CategoryIcon icon) {
        if (icon == null) {
            return null;
        }
        return gson.toJson(icon);
    }
}