package com.example.layouts.data;

import com.example.layouts.data.db.Achievement;
import com.example.layouts.data.db.AchievementDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementacion en memoria de AchievementDao para probar
 * AchievementRepository sin depender del runtime de Room/Android.
 */
class FakeAchievementDao implements AchievementDao {

    private final List<Achievement> achievements = new ArrayList<>();

    @Override
    public void insertAll(List<Achievement> newAchievements) {
        for (Achievement candidate : newAchievements) {
            boolean exists = false;
            for (Achievement existing : achievements) {
                if (existing.thresholdFiles == candidate.thresholdFiles) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                achievements.add(candidate);
            }
        }
    }

    @Override
    public void unlock(int thresholdFiles, long unlockedAt) {
        for (Achievement achievement : achievements) {
            if (achievement.thresholdFiles == thresholdFiles && achievement.unlockedAt == 0) {
                achievement.unlockedAt = unlockedAt;
            }
        }
    }

    @Override
    public List<Achievement> getAll() {
        return achievements;
    }
}
