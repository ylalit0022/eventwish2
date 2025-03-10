package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.ds.eventwish.data.model.CategoryIcon;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class CategoryIconConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromCategoryIcon(CategoryIcon categoryIcon) {
        return categoryIcon == null ? null : gson.toJson(categoryIcon);
    }

    @TypeConverter
    public static CategoryIcon toCategoryIcon(String value) {
        android.util.Log.d("CategoryIconConverter", "Converting to CategoryIcon, input value: " + value);
        if (value == null) return null;
        try {
            if (value.startsWith("[")) {
                android.util.Log.d("CategoryIconConverter", "Detected JSON Array format");
                CategoryIcon[] icons = gson.fromJson(value, CategoryIcon[].class);
                return icons.length > 0 ? icons[0] : null;
            } else if (value.startsWith("{")) {
                android.util.Log.d("CategoryIconConverter", "Detected JSON Object format");
                return gson.fromJson(value, CategoryIcon.class);
            } else {
                android.util.Log.d("CategoryIconConverter", "Treating as direct URL/String value");
                return new CategoryIcon(null, null, value);
            }
        } catch (JsonSyntaxException e) {
            android.util.Log.e("CategoryIconConverter", "JSON parsing error: " + e.getMessage() + ", raw value: " + value);
            return new CategoryIcon(null, null, value);
        } catch (Exception e) {
            android.util.Log.e("CategoryIconConverter", "Unexpected error: " + e.getMessage() + ", raw value: " + value);
            return new CategoryIcon(null, null, value);
        }
    }
}