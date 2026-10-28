package com.example.layouts.data;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsRepositoryTest {

    @Test
    public void defaultsAreSystemThemeAndNothingExcluded() {
        SettingsRepository repository = new SettingsRepository(new FakeStorage());

        assertEquals(SettingsRepository.ThemeMode.SYSTEM, repository.getThemeMode());
        assertFalse(repository.isExcludeVideos());
        assertFalse(repository.isExcludeScreenshots());
        assertFalse(repository.isTutorialSeen());
    }

    @Test
    public void themeModePersists() {
        FakeStorage storage = new FakeStorage();
        SettingsRepository repository = new SettingsRepository(storage);

        repository.setThemeMode(SettingsRepository.ThemeMode.DARK);

        assertEquals(SettingsRepository.ThemeMode.DARK, new SettingsRepository(storage).getThemeMode());
    }

    @Test
    public void excludeVideosPersists() {
        FakeStorage storage = new FakeStorage();
        SettingsRepository repository = new SettingsRepository(storage);

        repository.setExcludeVideos(true);

        assertTrue(new SettingsRepository(storage).isExcludeVideos());
    }

    @Test
    public void excludeScreenshotsPersists() {
        FakeStorage storage = new FakeStorage();
        SettingsRepository repository = new SettingsRepository(storage);

        repository.setExcludeScreenshots(true);

        assertTrue(new SettingsRepository(storage).isExcludeScreenshots());
    }

    @Test
    public void tutorialSeenFlagPersists() {
        FakeStorage storage = new FakeStorage();
        SettingsRepository repository = new SettingsRepository(storage);

        repository.setTutorialSeen(true);

        assertTrue(new SettingsRepository(storage).isTutorialSeen());
    }

    private static final class FakeStorage implements SettingsRepository.Storage {
        private final Map<String, String> strings = new HashMap<>();
        private final Map<String, Boolean> booleans = new HashMap<>();

        @Override
        public String readString(String key, String defaultValue) {
            return strings.getOrDefault(key, defaultValue);
        }

        @Override
        public void writeString(String key, String value) {
            strings.put(key, value);
        }

        @Override
        public boolean readBoolean(String key, boolean defaultValue) {
            return booleans.getOrDefault(key, defaultValue);
        }

        @Override
        public void writeBoolean(String key, boolean value) {
            booleans.put(key, value);
        }
    }
}
