package com.example.layouts.data;

import com.example.layouts.data.db.CleanupStats;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatsRepositoryTest {

    @Test
    public void incrementReviewedStartsFromZero() {
        StatsRepository repository = new StatsRepository(new FakeStatsDao());

        CleanupStats stats = repository.incrementReviewed();

        assertEquals(1, stats.reviewedCount);
    }

    @Test
    public void registerDeletionAccumulatesCountAndBytes() {
        StatsRepository repository = new StatsRepository(new FakeStatsDao());

        repository.registerDeletion(1024);
        CleanupStats stats = repository.registerDeletion(2048);

        assertEquals(2, stats.deletedCount);
        assertEquals(3072, stats.bytesFreed);
    }

    @Test
    public void registerBatchDeletionAccumulatesCountAndBytes() {
        StatsRepository repository = new StatsRepository(new FakeStatsDao());

        CleanupStats stats = repository.registerDeletions(3, 4096);

        assertEquals(3, stats.deletedCount);
        assertEquals(4096, stats.bytesFreed);
    }

    @Test
    public void decrementReviewedNeverGoesNegative() {
        StatsRepository repository = new StatsRepository(new FakeStatsDao());

        CleanupStats stats = repository.decrementReviewed();

        assertEquals(0, stats.reviewedCount);
    }

    @Test
    public void incrementThenDecrementReturnsToZero() {
        StatsRepository repository = new StatsRepository(new FakeStatsDao());

        repository.incrementReviewed();
        CleanupStats stats = repository.decrementReviewed();

        assertEquals(0, stats.reviewedCount);
    }
}
