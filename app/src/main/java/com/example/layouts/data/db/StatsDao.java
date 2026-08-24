package com.example.layouts.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface StatsDao {

    @Query("SELECT * FROM cleanup_stats WHERE id = 0")
    CleanupStats get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CleanupStats stats);
}
