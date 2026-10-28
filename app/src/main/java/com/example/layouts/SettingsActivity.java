package com.example.layouts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.layouts.auth.SessionManager;
import com.example.layouts.data.SettingsRepository;
import com.example.layouts.ui.InsetsUtil;
import com.example.layouts.ui.ThemeController;

/**
 * Tema (Claro/Oscuro/Sistema) y exclusion global de video/screenshots
 * (spec 2026-08-25-home-screen-design, Plan A). Unica fuente de verdad en
 * SettingsRepository, consumida ademas por SwipeDeckActivity y HomeActivity.
 */
public class SettingsActivity extends AppCompatActivity {

    private SettingsRepository settings;
    private TextView optionThemeLight;
    private TextView optionThemeDark;
    private TextView optionThemeSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));

        settings = new SettingsRepository(this);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        optionThemeLight = findViewById(R.id.optionThemeLight);
        optionThemeDark = findViewById(R.id.optionThemeDark);
        optionThemeSystem = findViewById(R.id.optionThemeSystem);
        Switch switchIncludeVideos = findViewById(R.id.switchIncludeVideos);
        Switch switchIncludeScreenshots = findViewById(R.id.switchIncludeScreenshots);
        androidx.appcompat.widget.AppCompatButton buttonLogout = findViewById(R.id.buttonLogout);

        buttonBack.setOnClickListener(v -> finish());

        // "Incluir X" en la UI es lo inverso de "excluir X" en el repositorio.
        switchIncludeVideos.setChecked(!settings.isExcludeVideos());
        switchIncludeScreenshots.setChecked(!settings.isExcludeScreenshots());
        switchIncludeVideos.setOnCheckedChangeListener((button, checked) -> settings.setExcludeVideos(!checked));
        switchIncludeScreenshots.setOnCheckedChangeListener((button, checked) -> settings.setExcludeScreenshots(!checked));

        optionThemeLight.setOnClickListener(v -> selectTheme(SettingsRepository.ThemeMode.LIGHT));
        optionThemeDark.setOnClickListener(v -> selectTheme(SettingsRepository.ThemeMode.DARK));
        optionThemeSystem.setOnClickListener(v -> selectTheme(SettingsRepository.ThemeMode.SYSTEM));
        renderThemeSelection(settings.getThemeMode());

        buttonLogout.setOnClickListener(v -> logout());
    }

    private void selectTheme(SettingsRepository.ThemeMode mode) {
        settings.setThemeMode(mode);
        ThemeController.applySavedTheme(this);
        renderThemeSelection(mode);
    }

    private void renderThemeSelection(SettingsRepository.ThemeMode mode) {
        highlight(optionThemeLight, mode == SettingsRepository.ThemeMode.LIGHT);
        highlight(optionThemeDark, mode == SettingsRepository.ThemeMode.DARK);
        highlight(optionThemeSystem, mode == SettingsRepository.ThemeMode.SYSTEM);
    }

    private void highlight(TextView option, boolean selected) {
        option.setBackgroundResource(selected ? R.drawable.bg_pill_primary_gradient : 0);
        option.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.colorTextSecondary));
    }

    private void logout() {
        new SessionManager(this).logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
