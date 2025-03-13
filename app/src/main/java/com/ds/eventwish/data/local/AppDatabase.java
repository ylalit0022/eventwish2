package com.ds.eventwish.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.converter.CategoryIconConverter;
import com.ds.eventwish.data.local.converters.DateConverter;
import com.ds.eventwish.data.local.converters.TemplateListConverter;
import com.ds.eventwish.data.local.converter.JsonObjectTypeConverter;
import com.ds.eventwish.data.local.converter.MapTypeConverter;
import com.ds.eventwish.data.local.converter.ObjectTypeConverter;
import com.ds.eventwish.data.local.dao.FestivalDao;
import com.ds.eventwish.data.local.dao.ResourceDao;
import com.ds.eventwish.data.local.entity.ResourceEntity;
import com.ds.eventwish.data.model.Festival;

@Database(entities = {Festival.class, ResourceEntity.class}, version = 3, exportSchema = false)
@TypeConverters({
    DateConverter.class,
    TemplateListConverter.class,
    CategoryIconConverter.class,
    JsonObjectTypeConverter.class,
    MapTypeConverter.class,
    ObjectTypeConverter.class
})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "eventwish_db";
    private static volatile AppDatabase INSTANCE;
    
    public abstract FestivalDao festivalDao();
    public abstract ResourceDao resourceDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
