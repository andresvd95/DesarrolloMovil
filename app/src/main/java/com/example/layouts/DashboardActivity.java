package com.example.layouts;

import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.layouts.data.AchievementRepository;
import com.example.layouts.data.DashboardSummary;
import com.example.layouts.data.MediaRepository;
import com.example.layouts.data.StatsRepository;
import com.example.layouts.data.db.AppDatabase;
import com.example.layouts.data.db.Achievement;
import com.example.layouts.data.db.CleanupStats;
import com.example.layouts.ui.InsetsUtil;

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
    private ProgressBar progressStorage;
    private TextView storageDetail;
    private TextView achievementProgress;
    private TextView achievementDetail;
    private ProgressBar progressAchievement;
    private LinearLayout achievementsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        reviewed = findViewById(R.id.textDashboardReviewed);
        deleted = findViewById(R.id.textDashboardDeleted);
        bytesFreed = findViewById(R.id.textDashboardBytesFreed);
        storage = findViewById(R.id.textDashboardStorage);
        progressStorage = findViewById(R.id.progressDashboardStorage);
        storageDetail = findViewById(R.id.textDashboardStorageDetail);
        achievementProgress = findViewById(R.id.textDashboardAchievementProgress);
        achievementDetail = findViewById(R.id.textDashboardAchievementDetail);
        progressAchievement = findViewById(R.id.progressDashboardAchievement);
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
        reviewed.setText(String.valueOf(stats.reviewedCount));
        deleted.setText(String.valueOf(stats.deletedCount));
        bytesFreed.setText(MediaRepository.humanReadableSize(stats.bytesFreed));

        int storagePercent = summary.getStorageUsedPercent();
        storage.setText(storagePercent + "%");
        progressStorage.setProgress(storagePercent);
        storageDetail.setText(MediaRepository.humanReadableSize(summary.getFreeStorageBytes())
                + " libres de " + MediaRepository.humanReadableSize(summary.getTotalStorageBytes()));

        Achievement next = summary.getNextAchievement();
        if (next == null) {
            achievementProgress.setText("Todos los logros desbloqueados");
            achievementDetail.setText("¡Has llegado al final de la lista! Sigue así.");
            progressAchievement.setProgress(100);
        } else {
            achievementProgress.setText("Siguiente logro: " + next.thresholdFiles + " archivos");
            int missing = Math.max(0, next.thresholdFiles - stats.deletedCount);
            achievementDetail.setText("Te faltan " + missing + " fotos eliminadas para desbloquearlo.");
            int progressPct = (int) Math.min(100, stats.deletedCount * 100L / next.thresholdFiles);
            progressAchievement.setProgress(progressPct);
        }

        achievementsList.removeAllViews();
        List<Achievement> achievements = summary.getAchievements();
        LinearLayout row = null;
        for (int i = 0; i < achievements.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                if (i > 0) {
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.topMargin = dp(18);
                    row.setLayoutParams(rowParams);
                }
                achievementsList.addView(row);
            }
            Achievement achievement = achievements.get(i);
            boolean isNext = next != null && achievement.thresholdFiles == next.thresholdFiles;
            row.addView(buildAchievementBadge(achievement, isNext));
        }
    }

    private View buildAchievementBadge(Achievement achievement, boolean isNext) {
        boolean unlocked = achievement.unlockedAt != 0;

        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        badge.setLayoutParams(badgeParams);

        FrameLayout circle = new FrameLayout(this);
        int size = dp(52);
        circle.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        circle.setBackgroundResource(unlocked
                ? R.drawable.bg_circle_primary_gradient
                : isNext ? R.drawable.bg_circle_next_outline : R.drawable.bg_circle_locked);

        ImageView icon = new ImageView(this);
        int iconSize = dp(unlocked ? 22 : 18);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER;
        icon.setLayoutParams(iconParams);
        if (unlocked) {
            icon.setImageResource(R.drawable.ic_star);
        } else {
            icon.setImageResource(isNext ? R.drawable.ic_star : R.drawable.ic_lock);
            icon.setColorFilter(ContextCompat.getColor(this,
                    isNext ? R.color.colorAccent : R.color.colorTextTertiary));
        }
        circle.addView(icon);
        badge.addView(circle);

        TextView label = new TextView(this);
        label.setText(achievement.thresholdFiles + " archivos");
        label.setTextSize(13);
        label.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_bold));
        label.setTextColor(ContextCompat.getColor(this,
                unlocked || isNext ? R.color.colorTextPrimary : R.color.colorTextTertiary));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(8);
        label.setLayoutParams(labelParams);
        badge.addView(label);

        TextView status = new TextView(this);
        status.setTextSize(11);
        status.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_semibold));
        if (unlocked) {
            status.setText("Desbloqueado");
            status.setTextColor(ContextCompat.getColor(this, R.color.colorAccentLight));
        } else if (isNext) {
            status.setText("Próximo");
            status.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        } else {
            status.setText("Bloqueado");
            status.setTextColor(ContextCompat.getColor(this, R.color.colorTextTertiary));
        }
        badge.addView(status);

        return badge;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
