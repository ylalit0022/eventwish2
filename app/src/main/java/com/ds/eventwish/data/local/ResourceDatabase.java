package com.ds.eventwish.data.local;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.ds.eventwish.data.local.converters.DateConverter;
import com.ds.eventwish.data.local.converters.JsonConverter;
import com.ds.eventwish.data.local.dao.ResourceDao;
import com.ds.eventwish.data.local.entity.ResourceEntity;
import com.ds.eventwish.utils.AppExecutors;

/**
 * Room database for storing resources for offline access
 */
@Database(entities = {ResourceEntity.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, JsonConverter.class})
public abstract class ResourceDatabase extends RoomDatabase {
    private static final String TAG = "ResourceDatabase";
    private static final String DATABASE_NAME = "resource_db";
    
    // Singleton instance
    private static volatile ResourceDatabase instance;
    
    /**
     * Get the singleton instance of ResourceDatabase
     * @param context Application context
     * @return ResourceDatabase instance
     */
    public static ResourceDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ResourceDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ResourceDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d(TAG, "Database created");
                                }
                                
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    Log.d(TAG, "Database opened");
                                }
                            })
                            .build();
                    Log.d(TAG, "ResourceDatabase instance created");
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the ResourceDao for accessing resource data
     * @return ResourceDao
     */
    public abstract ResourceDao resourceDao();
} 