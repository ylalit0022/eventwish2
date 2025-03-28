package com.ds.eventwish.data.local;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.ds.eventwish.data.converter.CategoryIconConverter;
import com.ds.eventwish.data.local.converters.DateConverter;
import com.ds.eventwish.data.local.converters.TemplateListConverter;
import com.ds.eventwish.data.local.converter.JsonObjectTypeConverter;
import com.ds.eventwish.data.local.converter.MapTypeConverter;
import com.ds.eventwish.data.local.converter.ObjectTypeConverter;
import com.ds.eventwish.data.local.dao.FestivalDao;
import com.ds.eventwish.data.local.dao.ResourceDao;
import com.ds.eventwish.data.local.entity.ResourceEntity;
import com.ds.eventwish.data.local.entity.CoinsEntity;
import com.ds.eventwish.data.local.entity.AdMobEntity;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.local.dao.CoinsDao;
import com.ds.eventwish.data.local.dao.AdMobDao;

@Database(
    entities = {
        Festival.class,
        ResourceEntity.class,
        CoinsEntity.class,
        AdMobEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({
    DateConverter.class,
    TemplateListConverter.class,
    CategoryIconConverter.class,
    JsonObjectTypeConverter.class,
    MapTypeConverter.class,
    ObjectTypeConverter.class
})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "eventwish_db";
    private static volatile AppDatabase instance;
    
    public abstract FestivalDao festivalDao();
    public abstract ResourceDao resourceDao();
    public abstract CoinsDao coinsDao();
    public abstract AdMobDao adMobDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    Log.d(TAG, "Database created");
                    // Initialize with default data if needed
                }
            })
            .fallbackToDestructiveMigration()
            .build();
            Log.d(TAG, "Database instance created");
        }
        return instance;
    }
}
