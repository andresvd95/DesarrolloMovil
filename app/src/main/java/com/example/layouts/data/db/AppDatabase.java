package com.example.layouts.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TrashEntry.class, CleanupStats.class, Achievement.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract TrashDao trashDao();

    public abstract StatsDao statsDao();

    public abstract AchievementDao achievementDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "swipeclean.db").build();
                }
            }
        }
        return instance;
    }
}
