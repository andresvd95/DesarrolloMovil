package com.example.layouts.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Meta de limpieza (RF09). unlockedAt = 0 significa que aun no se ha
 * alcanzado; distinto de 0 guarda el momento en que se desbloqueo.
 */
@Entity(tableName = "achievement")
public class Achievement {

    @PrimaryKey
    public int thresholdFiles;

    public long unlockedAt;
}
