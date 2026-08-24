package com.example.layouts.data;

import com.example.layouts.data.db.CleanupStats;
import com.example.layouts.data.db.StatsDao;

/**
 * Implementacion en memoria de StatsDao para probar StatsRepository
 * sin depender del runtime de Room/Android.
 */
class FakeStatsDao implements StatsDao {

    private CleanupStats stored;

    @Override
    public CleanupStats get() {
        return stored;
    }

    @Override
    public void upsert(CleanupStats stats) {
        stored = stats;
    }
}
