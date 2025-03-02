package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.ds.eventwish.data.model.CategoryIcon;
import com.google.gson.Gson;

public class CategoryIconConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromCategoryIcon(CategoryIcon categoryIcon) {
        return categoryIcon == null ? null : gson.toJson(categoryIcon);
    }

    @TypeConverter
    public static CategoryIcon toCategoryIcon(String value) {
        return value == null ? null : gson.fromJson(value, CategoryIcon.class);
    }
}