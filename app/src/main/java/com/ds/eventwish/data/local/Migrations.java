package com.ds.eventwish.data.local;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Database migrations for Room database version changes
 */
public class Migrations {
    private static final String TAG = "Migrations";
    
    /**
     * Migration from version 1 to 2
     * - Adds users table for authentication
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Migrating database from version 1 to 2");
            
            // Create users table
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `users` (" +
                "`uid` TEXT NOT NULL PRIMARY KEY, " +
                "`phoneNumber` TEXT, " +
                "`displayName` TEXT, " +
                "`email` TEXT, " +
                "`photoUrl` TEXT, " +
                "`idToken` TEXT, " +
                "`refreshToken` TEXT, " +
                "`tokenExpiryTime` INTEGER NOT NULL DEFAULT 0, " +
                "`isAuthenticated` INTEGER NOT NULL DEFAULT 0, " +
                "`lastLoginTime` INTEGER NOT NULL DEFAULT 0, " +
                "`createdAt` INTEGER NOT NULL DEFAULT 0, " +
                "`updatedAt` INTEGER NOT NULL DEFAULT 0)"
            );
            
            // Create index on phoneNumber 
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_phoneNumber` ON `users` (`phoneNumber`)");
            
            Log.d(TAG, "Migration from version 1 to 2 completed");
        }
    };
    
    /**
     * Migration from version 2 to 3
     * - Added EngagementData table for recommendation engine
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Migrating database from version 2 to 3 (adding engagement tracking)");
            
            // First drop the table if it exists to avoid migration conflicts
            database.execSQL("DROP TABLE IF EXISTS `engagement_data`");
            
            // Create the engagement_data table with proper default values
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `engagement_data` (" +
                "`id` TEXT NOT NULL, " +
                "`type` INTEGER NOT NULL, " +
                "`template_id` TEXT, " +
                "`category` TEXT, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`duration_ms` INTEGER NOT NULL DEFAULT 0, " +
                "`engagement_score` INTEGER NOT NULL DEFAULT 1, " +
                "`source` TEXT, " +
                "`synced` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`))");
                
            // Create indexes for faster queries
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_engagement_data_template_id` ON `engagement_data` (`template_id`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_engagement_data_category` ON `engagement_data` (`category`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_engagement_data_timestamp` ON `engagement_data` (`timestamp`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_engagement_data_synced` ON `engagement_data` (`synced`)");
            
            Log.d(TAG, "Migration 2 to 3 completed successfully");
        }
    };
    
    /**
     * Migration from version 3 to 4
     * - Added ad_units table for ad management
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create ad_units table
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `ad_units` (" +
                "`id` TEXT NOT NULL, " +
                "`adName` TEXT, " +
                "`adUnitCode` TEXT, " +
                "`adType` TEXT, " +
                "`status` INTEGER NOT NULL DEFAULT 0, " +
                "`targetingCriteria` TEXT, " +
                "`targetingPriority` INTEGER NOT NULL DEFAULT 1, " +
                "`parameters` TEXT, " +
                "`maxImpressionsPerDay` INTEGER NOT NULL DEFAULT 10, " +
                "`minIntervalBetweenAds` INTEGER NOT NULL DEFAULT 60, " +
                "`cooldownPeriod` INTEGER NOT NULL DEFAULT 15, " +
                "`canShow` INTEGER NOT NULL DEFAULT 0, " +
                "`reason` TEXT, " +
                "`nextAvailable` TEXT, " +
                "`lastUpdated` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`)" +
                ")"
            );
            
            // Create index on adType for faster queries
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_ad_units_adType` ON `ad_units` (`adType`)"
            );
        }
    };
    
    /**
     * Keep a reference to the expected schema for engagement_data
     * This aids in debugging migration issues
     */
    private static final String EXPECTED_ENGAGEMENT_DATA_SCHEMA = 
        "TableInfo{name='engagement_data', columns={" +
        "duration_ms=Column{name='duration_ms', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='0'}, " +
        "engagement_score=Column{name='engagement_score', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='1'}, " +
        "synced=Column{name='synced', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='0'}, " +
        "template_id=Column{name='template_id', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='undefined'}, " +
        "id=Column{name='id', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=1, defaultValue='undefined'}, " +
        "source=Column{name='source', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='undefined'}, " +
        "type=Column{name='type', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='undefined'}, " +
        "category=Column{name='category', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='undefined'}, " +
        "timestamp=Column{name='timestamp', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='undefined'}}";
} 