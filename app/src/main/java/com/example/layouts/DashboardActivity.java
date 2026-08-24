package com.example.layouts;

import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.layouts.data.AchievementRepository;
import com.example.layouts.data.DashboardSummary;
import com.example.layouts.data.MediaRepository;
import com.example.layouts.data.StatsRepository;
import com.example.layouts.data.db.AppDatabase;
import com.example.layouts.data.db.Achievement;
import com.example.layouts.data.db.CleanupStats;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Muestra estadisticas, logros y almacenamiento local (RF08, RF09, RF10). */
public class DashboardActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView reviewed;
    private TextView deleted;
    private TextView bytesFreed;
    private TextView storage;
    private TextView achievementProgress;
    private LinearLayout achievementsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        reviewed = findViewById(R.id.textDashboardReviewed);
        deleted = findViewById(R.id.textDashboardDeleted);
        bytesFreed = findViewById(R.id.textDashboardBytesFreed);
        storage = findViewById(R.id.textDashboardStorage);
        achievementProgress = findViewById(R.id.textDashboardAchievementProgress);
        achievementsList = findViewById(R.id.dashboardAchievementsList);

        loadDashboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void loadDashboard() {
        executor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            StatsRepository statsRepository = new StatsRepository(database.statsDao());
            AchievementRepository achievementRepository = new AchievementRepository(database.achievementDao());
            achievementRepository.ensureSeeded();

            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            long totalBytes = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            long freeBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
            DashboardSummary summary = new DashboardSummary(
                    statsRepository.getStats(), achievementRepository.getAll(), totalBytes, freeBytes);

            runOnUiThread(() -> render(summary));
        });
    }

    private void render(DashboardSummary summary) {
        CleanupStats stats = summary.getStats();
        reviewed.setText("Revisados: " + stats.reviewedCount);
        deleted.setText("Eliminados definitivamente: " + stats.deletedCount);
        bytesFreed.setText("Espacio liberado: " + MediaRepository.humanReadableSize(stats.bytesFreed));
        storage.setText("Almacenamiento usado: " + summary.getStorageUsedPercent() + "%\n"
                + "Libre: " + MediaRepository.humanReadableSize(summary.getFreeStorageBytes())
                + " de " + MediaRepository.humanReadableSize(summary.getTotalStorageBytes()));

        Achievement next = summary.getNextAchievement();
        if (next == null) {
            achievementProgress.setText("Todos los logros desbloqueados");
        } else {
            achievementProgress.setText("Siguiente logro: " + next.thresholdFiles + " archivos");
        }

        achievementsList.removeAllViews();
        for (Achievement achievement : summary.getAchievements()) {
            TextView row = new TextView(this);
            row.setPadding(20, 14, 20, 14);
            row.setText(achievement.thresholdFiles + " archivos  -  "
                    + (achievement.unlockedAt == 0 ? "Pendiente" : "Desbloqueado"));
            row.setTextSize(15);
            achievementsList.addView(row);
        }
    }
}
