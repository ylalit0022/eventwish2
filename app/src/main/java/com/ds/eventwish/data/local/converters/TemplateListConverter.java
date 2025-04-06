package com.ds.eventwish.data.local.converters;

import androidx.room.TypeConverter;

import com.ds.eventwish.data.model.FestivalTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Type converter for Room database to convert between List<FestivalTemplate> and String
 */
public class TemplateListConverter {
    
    private static final Gson gson = new Gson();
    
    @TypeConverter
    public static List<FestivalTemplate> fromString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        
        Type listType = new TypeToken<List<FestivalTemplate>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String fromList(List<FestivalTemplate> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
}
