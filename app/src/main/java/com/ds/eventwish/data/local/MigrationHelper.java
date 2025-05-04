package com.ds.eventwish.data.local;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Helper utility for database migrations
 */
public class MigrationHelper {
    private static final String TAG = "MigrationHelper";
    
    /**
     * Check if a column exists in a table
     * @param db The database to check
     * @param tableName The table name
     * @param columnName The column name
     * @return true if the column exists
     */
    public static boolean columnExists(@NonNull SupportSQLiteDatabase db, String tableName, String columnName) {
        boolean result = false;
        try {
            db.execSQL("SELECT " + columnName + " FROM " + tableName + " LIMIT 0");
            result = true;
        } catch (Exception e) {
            Log.d(TAG, "Column " + columnName + " does not exist in table " + tableName);
        }
        return result;
    }
    
    /**
     * Safely add a column to a table if it doesn't exist
     * @param db The database to modify
     * @param tableName The table name
     * @param columnName The column name
     * @param columnType The column type (e.g., "INTEGER NOT NULL DEFAULT 0")
     * @return true if the column was added
     */
    public static boolean safeAddColumn(@NonNull SupportSQLiteDatabase db, String tableName, 
                                       String columnName, String columnType) {
        try {
            if (!columnExists(db, tableName, columnName)) {
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
                db.execSQL(sql);
                Log.d(TAG, "Added column " + columnName + " to table " + tableName);
                return true;
            } else {
                Log.d(TAG, "Column " + columnName + " already exists in table " + tableName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding column " + columnName + " to table " + tableName, e);
        }
        return false;
    }
} 