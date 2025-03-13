package com.ds.eventwish.data.local;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ds.eventwish.data.local.dao.FlashyMessageDao;
import com.ds.eventwish.data.local.entity.FlashyMessageEntity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FlashyMessageDatabaseMigrationTest {
    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public FlashyMessageDatabaseMigrationTest() {
        helper = new MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                FlashyMessageDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrateAll() throws IOException {
        // Create earliest version of the database
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        db.close();

        // Open latest version of the database
        FlashyMessageDatabase database = Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FlashyMessageDatabase.class,
                TEST_DB)
                .fallbackToDestructiveMigration()
                .build();
        database.getOpenHelper().getWritableDatabase();
        database.close();
    }

    @Test
    public void verifyDbCreation() {
        // Create a new database
        Context context = ApplicationProvider.getApplicationContext();
        FlashyMessageDatabase db = Room.inMemoryDatabaseBuilder(context, FlashyMessageDatabase.class)
                .allowMainThreadQueries()
                .build();

        // Verify the database is created with the correct version
        assertEquals("Database should be at version 1", 1, db.getOpenHelper().getWritableDatabase().getVersion());

        // Close the database
        db.close();
    }

    @Test
    public void verifyTableStructure() {
        // Create a new database
        Context context = ApplicationProvider.getApplicationContext();
        FlashyMessageDatabase db = Room.inMemoryDatabaseBuilder(context, FlashyMessageDatabase.class)
                .allowMainThreadQueries()
                .build();

        // Get the database
        SupportSQLiteDatabase database = db.getOpenHelper().getWritableDatabase();

        // Verify the flashy_messages table exists
        Cursor cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='flashy_messages'");
        assertTrue("Table flashy_messages should exist", cursor.moveToFirst());
        cursor.close();

        // Verify table structure
        cursor = database.query("PRAGMA table_info(flashy_messages)");
        assertTrue("Table should have columns", cursor.moveToFirst());
        
        // Expected columns
        String[] expectedColumns = {"id", "title", "message", "timestamp", "priority", "isRead", "isDisplaying"};
        int columnCount = 0;
        
        do {
            String columnName = cursor.getString(cursor.getColumnIndex("name"));
            assertTrue("Column " + columnName + " should be expected", 
                Arrays.asList(expectedColumns).contains(columnName));
            columnCount++;
        } while (cursor.moveToNext());
        
        assertEquals("Table should have correct number of columns", 
            expectedColumns.length, columnCount);
        
        cursor.close();
        db.close();
    }

    @Test
    public void verifyDatabaseOperations() {
        // Create a new database
        Context context = ApplicationProvider.getApplicationContext();
        FlashyMessageDatabase db = Room.inMemoryDatabaseBuilder(context, FlashyMessageDatabase.class)
                .allowMainThreadQueries()
                .build();

        // Get the DAO
        FlashyMessageDao dao = db.flashyMessageDao();

        // Create a test message
        FlashyMessageEntity message = new FlashyMessageEntity("test_id", "Test Title", "Test Message");

        // Insert the message
        dao.insert(message);

        // Verify the message was inserted
        FlashyMessageEntity loaded = dao.getById("test_id");
        assertNotNull("Message should be loaded", loaded);
        assertEquals("Message ID should match", message.getId(), loaded.getId());
        assertEquals("Message title should match", message.getTitle(), loaded.getTitle());
        assertEquals("Message content should match", message.getMessage(), loaded.getMessage());

        // Close the database
        db.close();
    }
} 