package com.ds.eventwish.data.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.ds.eventwish.data.converter.CategoryIconConverter;
import com.ds.eventwish.data.converter.DateConverter;
import com.ds.eventwish.data.model.Category;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
import com.ds.eventwish.data.local.dao.AdUnitDao;

/**
 * Main database class for the application.
 * Follows singleton pattern for database access.
 */
@Database(
    entities = {
        Template.class,
        SharedWish.class,
        Category.class,
        AdUnitEntity.class
    },
    version = 1,
    exportSchema = true
)
@TypeConverters({
    DateConverter.class,
    CategoryIconConverter.class
})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "eventwish.db";
    
    // Singleton instance
    private static volatile AppDatabase instance;
    
    // DAOs
    public abstract TemplateDao templateDao();
    public abstract SharedWishDao sharedWishDao();
    public abstract CategoryDao categoryDao();
    public abstract AdUnitDao adUnitDao();
    
    /**
     * Get the singleton database instance
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
            Log.d(TAG, "Created new database instance");
        }
        return instance;
    }
    
    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(
            context.getApplicationContext(),
            AppDatabase.class,
            DATABASE_NAME)
            .addCallback(new Callback() {
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
            .addMigrations() // Will add migrations as needed
            .fallbackToDestructiveMigration() // Only during development
            .build();
    }
    
    /**
     * Clear all data in the database
     */
    public void clearAllTables() {
        if (instance != null) {
            Log.d(TAG, "Clearing all database tables");
            instance.clearAllTables();
        }
    }
} 