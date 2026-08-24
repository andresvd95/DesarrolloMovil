package com.example.layouts.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Fila unica (id = 0) con los contadores acumulados de limpieza (RF08).
 */
@Entity(tableName = "cleanup_stats")
public class CleanupStats {

    @PrimaryKey
    public int id = 0;

    public int reviewedCount;
    public int deletedCount;
    public long bytesFreed;
}
