package com.example.layouts.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferencias de la app: tema y exclusion global de video/screenshots
 * (Plan A). Unica fuente de verdad, consumida por ThemeController y por
 * MediaQuery (combinada con los filtros puntuales de la sesion).
 */
public final class SettingsRepository {

    private static final String PREFERENCES_NAME = "swipe_cleaner_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_EXCLUDE_VIDEOS = "exclude_videos";
    private static final String KEY_EXCLUDE_SCREENSHOTS = "exclude_screenshots";
    private static final String KEY_TUTORIAL_SEEN = "tutorial_seen";

    public enum ThemeMode { LIGHT, DARK, SYSTEM }

    private final Storage storage;

    public SettingsRepository(Context context) {
        this(new SharedPreferencesStorage(
                context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ));
    }

    SettingsRepository(Storage storage) {
        this.storage = storage;
    }

    public ThemeMode getThemeMode() {
        String stored = storage.readString(KEY_THEME_MODE, ThemeMode.SYSTEM.name());
        try {
            return ThemeMode.valueOf(stored);
        } catch (IllegalArgumentException exception) {
            return ThemeMode.SYSTEM;
        }
    }

    public void setThemeMode(ThemeMode mode) {
        storage.writeString(KEY_THEME_MODE, (mode != null ? mode : ThemeMode.SYSTEM).name());
    }

    public boolean isExcludeVideos() {
        return storage.readBoolean(KEY_EXCLUDE_VIDEOS, false);
    }

    public void setExcludeVideos(boolean exclude) {
        storage.writeBoolean(KEY_EXCLUDE_VIDEOS, exclude);
    }

    public boolean isExcludeScreenshots() {
        return storage.readBoolean(KEY_EXCLUDE_SCREENSHOTS, false);
    }

    public void setExcludeScreenshots(boolean exclude) {
        storage.writeBoolean(KEY_EXCLUDE_SCREENSHOTS, exclude);
    }

    public boolean isTutorialSeen() {
        return storage.readBoolean(KEY_TUTORIAL_SEEN, false);
    }

    public void setTutorialSeen(boolean seen) {
        storage.writeBoolean(KEY_TUTORIAL_SEEN, seen);
    }

    interface Storage {
        String readString(String key, String defaultValue);

        void writeString(String key, String value);

        boolean readBoolean(String key, boolean defaultValue);

        void writeBoolean(String key, boolean value);
    }

    private static final class SharedPreferencesStorage implements Storage {
        private final SharedPreferences preferences;

        SharedPreferencesStorage(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public String readString(String key, String defaultValue) {
            return preferences.getString(key, defaultValue);
        }

        @Override
        public void writeString(String key, String value) {
            preferences.edit().putString(key, value).apply();
        }

        @Override
        public boolean readBoolean(String key, boolean defaultValue) {
            return preferences.getBoolean(key, defaultValue);
        }

        @Override
        public void writeBoolean(String key, boolean value) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }
}
