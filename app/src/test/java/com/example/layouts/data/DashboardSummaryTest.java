package com.example.layouts.data;

import com.example.layouts.data.db.Achievement;
import com.example.layouts.data.db.CleanupStats;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DashboardSummaryTest {

    @Test
    public void exposesProgressAndNextAchievement() {
        CleanupStats stats = new CleanupStats();
        stats.reviewedCount = 80;
        stats.deletedCount = 50;
        stats.bytesFreed = 12_000;

        Achievement first = achievement(10, 1);
        Achievement second = achievement(50, 1);
        Achievement next = achievement(100, 0);
        List<Achievement> achievements = Arrays.asList(first, second, next);

        DashboardSummary summary = new DashboardSummary(stats, achievements, 1_000, 250);

        assertEquals(2, summary.getUnlockedAchievementCount());
        assertEquals(100, summary.getNextAchievement().thresholdFiles);
        assertEquals(75, summary.getStorageUsedPercent());
    }

    private static Achievement achievement(int threshold, long unlockedAt) {
        Achievement achievement = new Achievement();
        achievement.thresholdFiles = threshold;
        achievement.unlockedAt = unlockedAt;
        return achievement;
    }
}
