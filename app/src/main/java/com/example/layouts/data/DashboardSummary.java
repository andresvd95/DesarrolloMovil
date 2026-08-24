package com.example.layouts.data;

import com.example.layouts.data.db.Achievement;
import com.example.layouts.data.db.CleanupStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Datos calculados que necesita el dashboard (RF08, RF09 y RF10).
 * No depende de Android para que el progreso pueda probarse como logica pura.
 */
public final class DashboardSummary {

    private final CleanupStats stats;
    private final List<Achievement> achievements;
    private final long totalStorageBytes;
    private final long freeStorageBytes;

    public DashboardSummary(CleanupStats stats, List<Achievement> achievements,
                            long totalStorageBytes, long freeStorageBytes) {
        this.stats = stats;
        this.achievements = new ArrayList<>(achievements);
        this.totalStorageBytes = totalStorageBytes;
        this.freeStorageBytes = freeStorageBytes;
    }

    public CleanupStats getStats() {
        return stats;
    }

    public List<Achievement> getAchievements() {
        return Collections.unmodifiableList(achievements);
    }

    public long getTotalStorageBytes() {
        return totalStorageBytes;
    }

    public long getFreeStorageBytes() {
        return freeStorageBytes;
    }

    public int getUnlockedAchievementCount() {
        int unlocked = 0;
        for (Achievement achievement : achievements) {
            if (achievement.unlockedAt != 0) {
                unlocked++;
            }
        }
        return unlocked;
    }

    public Achievement getNextAchievement() {
        return achievements.stream()
                .filter(achievement -> achievement.unlockedAt == 0)
                .min(Comparator.comparingInt(achievement -> achievement.thresholdFiles))
                .orElse(null);
    }

    public int getStorageUsedPercent() {
        if (totalStorageBytes <= 0) {
            return 0;
        }
        long usedBytes = Math.max(0, totalStorageBytes - freeStorageBytes);
        return (int) Math.min(100, usedBytes * 100 / totalStorageBytes);
    }
}
