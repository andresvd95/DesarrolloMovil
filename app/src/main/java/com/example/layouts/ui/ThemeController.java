package com.example.layouts.ui;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.layouts.data.SettingsRepository;

/**
 * Aplica el tema guardado en SettingsRepository via AppCompatDelegate.
 * Debe invocarse antes de setContentView() de cada Activity (aca solo en
 * SplashActivity, la primera pantalla) para evitar el parpadeo del tema
 * por defecto.
 */
public final class ThemeController {

    private ThemeController() {
    }

    public static void applySavedTheme(Context context) {
        SettingsRepository.ThemeMode mode = new SettingsRepository(context).getThemeMode();
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode));
    }

    private static int nightModeFor(SettingsRepository.ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
