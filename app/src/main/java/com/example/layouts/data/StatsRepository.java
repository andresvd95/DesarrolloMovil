package com.example.layouts.data;

import com.example.layouts.data.db.CleanupStats;
import com.example.layouts.data.db.StatsDao;

/**
 * Cubre RF08: acumula revisados, eliminados y espacio liberado.
 */
public class StatsRepository {

    private final StatsDao statsDao;

    public StatsRepository(StatsDao statsDao) {
        this.statsDao = statsDao;
    }

    public CleanupStats getStats() {
        CleanupStats stats = statsDao.get();
        return stats != null ? stats : new CleanupStats();
    }

    public CleanupStats incrementReviewed() {
        CleanupStats stats = getStats();
        stats.reviewedCount++;
        statsDao.upsert(stats);
        return stats;
    }

    public CleanupStats decrementReviewed() {
        CleanupStats stats = getStats();
        if (stats.reviewedCount > 0) {
            stats.reviewedCount--;
        }
        statsDao.upsert(stats);
        return stats;
    }

    public CleanupStats registerDeletion(long bytesFreed) {
        CleanupStats stats = getStats();
        stats.deletedCount++;
        stats.bytesFreed += bytesFreed;
        statsDao.upsert(stats);
        return stats;
    }
}
