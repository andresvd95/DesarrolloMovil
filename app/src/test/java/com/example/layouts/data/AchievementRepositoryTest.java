package com.example.layouts.data;

import com.example.layouts.data.db.Achievement;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AchievementRepositoryTest {

    @Test
    public void unlocksAchievementsAtOrAboveThreshold() {
        AchievementRepository repository = new AchievementRepository(new FakeAchievementDao());
        repository.ensureSeeded();

        List<Achievement> unlocked = repository.checkThresholds(50);

        assertEquals(2, unlocked.size());
        assertEquals(10, unlocked.get(0).thresholdFiles);
        assertEquals(50, unlocked.get(1).thresholdFiles);
    }

    @Test
    public void doesNotUnlockTheSameAchievementTwice() {
        AchievementRepository repository = new AchievementRepository(new FakeAchievementDao());
        repository.ensureSeeded();

        repository.checkThresholds(10);
        List<Achievement> secondCheck = repository.checkThresholds(10);

        assertTrue(secondCheck.isEmpty());
    }

    @Test
    public void ensureSeededDoesNotDuplicateExistingAchievements() {
        AchievementRepository repository = new AchievementRepository(new FakeAchievementDao());
        repository.ensureSeeded();
        repository.ensureSeeded();

        assertEquals(AchievementRepository.THRESHOLDS.length, repository.getAll().size());
    }
}
