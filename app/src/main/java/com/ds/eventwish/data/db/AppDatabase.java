package com.ds.eventwish.data.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;

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
    version = 3,
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create indices for like/favorite columns
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_isLiked` ON `templates` (`isLiked`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_isFavorited` ON `templates` (`isFavorited`)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new templates table with updated schema
            database.execSQL("CREATE TABLE IF NOT EXISTS `templates_new` " +
                           "(`id` TEXT NOT NULL, " +
                           "`title` TEXT, " +
                           "`categoryId` TEXT, " +
                           "`previewUrl` TEXT, " +
                           "`likeCount` INTEGER NOT NULL DEFAULT 0, " +
                           "`favoriteCount` INTEGER NOT NULL DEFAULT 0, " +
                           "`isLiked` INTEGER NOT NULL DEFAULT 0, " +
                           "`isFavorited` INTEGER NOT NULL DEFAULT 0, " +
                           "`lastUpdated` INTEGER, " +
                           "`likeChanged` INTEGER NOT NULL DEFAULT 0, " +
                           "`favoriteChanged` INTEGER NOT NULL DEFAULT 0, " +
                           "PRIMARY KEY(`id`), " +
                           "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE SET NULL)");

            // Copy data from old table
            database.execSQL("INSERT OR REPLACE INTO `templates_new` " +
                           "(id, title, categoryId, previewUrl, likeCount, favoriteCount, isLiked, isFavorited, lastUpdated) " +
                           "SELECT id, title, category_id, preview_url, like_count, favorite_count, is_liked, is_favorited, updated_at " +
                           "FROM `templates`");

            // Drop old table
            database.execSQL("DROP TABLE IF EXISTS `templates`");

            // Rename new table
            database.execSQL("ALTER TABLE `templates_new` RENAME TO `templates`");

            // Create indices
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_categoryId` ON `templates` (`categoryId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_lastUpdated` ON `templates` (`lastUpdated`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_isLiked` ON `templates` (`isLiked`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_isFavorited` ON `templates` (`isFavorited`)");
        }
    };
} 