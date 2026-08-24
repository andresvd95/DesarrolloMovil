package com.example.layouts.data;

import com.example.layouts.data.db.Achievement;
import com.example.layouts.data.db.AchievementDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Cubre RF09: metas de limpieza (10/50/100/200/500/1000 archivos).
 */
public class AchievementRepository {

    public static final int[] THRESHOLDS = {10, 50, 100, 200, 500, 1000};

    private final AchievementDao achievementDao;

    public AchievementRepository(AchievementDao achievementDao) {
        this.achievementDao = achievementDao;
    }

    public void ensureSeeded() {
        List<Achievement> seed = new ArrayList<>();
        for (int threshold : THRESHOLDS) {
            Achievement achievement = new Achievement();
            achievement.thresholdFiles = threshold;
            achievement.unlockedAt = 0;
            seed.add(achievement);
        }
        achievementDao.insertAll(seed);
    }

    /**
     * Marca como desbloqueadas las metas que deletedCount alcanza o supera
     * y que aun no estaban desbloqueadas. Devuelve las recien desbloqueadas.
     */
    public List<Achievement> checkThresholds(int deletedCount) {
        List<Achievement> newlyUnlocked = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Achievement achievement : achievementDao.getAll()) {
            if (achievement.unlockedAt == 0 && deletedCount >= achievement.thresholdFiles) {
                achievementDao.unlock(achievement.thresholdFiles, now);
                achievement.unlockedAt = now;
                newlyUnlocked.add(achievement);
            }
        }
        return newlyUnlocked;
    }

    public List<Achievement> getAll() {
        return achievementDao.getAll();
    }
}
