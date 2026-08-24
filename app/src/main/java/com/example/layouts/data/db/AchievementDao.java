package com.example.layouts.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Achievement> achievements);

    @Query("UPDATE achievement SET unlockedAt = :unlockedAt WHERE thresholdFiles = :thresholdFiles AND unlockedAt = 0")
    void unlock(int thresholdFiles, long unlockedAt);

    @Query("SELECT * FROM achievement ORDER BY thresholdFiles ASC")
    List<Achievement> getAll();
}
