package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class StringListConverter {
    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<List<String>>(){}.getType();

    @TypeConverter
    public static String fromStringList(List<String> list) {
        return list == null ? null : gson.toJson(list, type);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        return value == null ? null : gson.fromJson(value, type);
    }
} 