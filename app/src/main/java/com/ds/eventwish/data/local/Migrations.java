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
} 