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
import com.ds.eventwish.data.local.dao.AdUnitDao;
import com.ds.eventwish.data.local.dao.CategoryClickDao;
import com.ds.eventwish.data.local.dao.EngagementDataDao;
import com.ds.eventwish.data.local.dao.FestivalDao;
import com.ds.eventwish.data.local.dao.ResourceDao;
import com.ds.eventwish.data.local.dao.SponsoredAdDao;
import com.ds.eventwish.data.local.dao.UserDao;
import com.ds.eventwish.data.local.entity.AdUnitEntity;
import com.ds.eventwish.data.local.entity.CategoryClickEntity;
import com.ds.eventwish.data.local.entity.ResourceEntity;
import com.ds.eventwish.data.local.entity.SponsoredAdEntity;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.data.model.EngagementData;
import com.ds.eventwish.data.model.Festival;

@Database(
    entities = {
        Festival.class,
        ResourceEntity.class,
        UserEntity.class,
        EngagementData.class,
        AdUnitEntity.class,
        CategoryClickEntity.class,
        SponsoredAdEntity.class
    },
    version = 6,
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
    public abstract UserDao userDao();
    public abstract EngagementDataDao engagementDataDao();
    public abstract AdUnitDao adUnitDao();
    public abstract CategoryClickDao categoryClickDao();
    public abstract SponsoredAdDao sponsoredAdDao();
    
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
                
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    Log.d(TAG, "Database opened");
                }
            })
            .addMigrations(
                Migrations.MIGRATION_1_2, 
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6
            )
            .fallbackToDestructiveMigration()
            .build();
            Log.d(TAG, "Database instance created");
        }
        return instance;
    }
}
