package com.ds.eventwish.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ds.eventwish.data.local.dao.FlashyMessageDao;
import com.ds.eventwish.data.local.entity.FlashyMessageEntity;

@Database(entities = {FlashyMessageEntity.class}, version = 1, exportSchema = false)
public abstract class FlashyMessageDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "flashy_message_db";
    private static volatile FlashyMessageDatabase instance;

    public abstract FlashyMessageDao flashyMessageDao();

    public static synchronized FlashyMessageDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    FlashyMessageDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
} 