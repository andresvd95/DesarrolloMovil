package com.example.layouts;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.layouts.data.AchievementRepository;
import com.example.layouts.data.StatsRepository;
import com.example.layouts.data.TrashRepository;
import com.example.layouts.data.db.AppDatabase;
import com.example.layouts.data.db.TrashEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Papelera temporal: permite recuperar entradas o solicitar su eliminacion
 * definitiva con la confirmacion nativa de Android (RF05).
 */
public class TrashActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TrashRepository trashRepository;
    private StatsRepository statsRepository;
    private AchievementRepository achievementRepository;
    private LinearLayout trashList;
    private TextView emptyState;
    private Button emptyTrashButton;
    private Button dashboardButton;
    private List<TrashEntry> pendingPermanentDeletion = new ArrayList<>();

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && !pendingPermanentDeletion.isEmpty()) {
                    List<TrashEntry> deleted = new ArrayList<>(pendingPermanentDeletion);
                    pendingPermanentDeletion.clear();
                    executor.execute(() -> {
                        trashRepository.removePermanentlyDeleted(deleted);
                        recordPermanentDeletion(deleted);
                        runOnUiThread(this::loadTrash);
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        trashList = findViewById(R.id.trashList);
        emptyState = findViewById(R.id.textTrashEmptyState);
        emptyTrashButton = findViewById(R.id.buttonEmptyTrash);
        dashboardButton = findViewById(R.id.buttonDashboard);

        AppDatabase database = AppDatabase.getInstance(getApplicationContext());
        trashRepository = new TrashRepository(database.trashDao());
        statsRepository = new StatsRepository(database.statsDao());
        achievementRepository = new AchievementRepository(database.achievementDao());
        achievementRepository.ensureSeeded();
        emptyTrashButton.setOnClickListener(v -> requestPermanentDeletion());
        dashboardButton.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        loadTrash();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void loadTrash() {
        executor.execute(() -> {
            List<TrashEntry> entries = trashRepository.getAll();
            runOnUiThread(() -> render(entries));
        });
    }

    private void render(List<TrashEntry> entries) {
        trashList.removeAllViews();
        boolean isEmpty = entries.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        emptyTrashButton.setEnabled(!isEmpty);

        for (TrashEntry entry : entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(20, 16, 20, 16);

            TextView uri = new TextView(this);
            uri.setText(entry.mediaUri);
            uri.setTextSize(14);
            row.addView(uri);

            TextView album = new TextView(this);
            album.setText(entry.originalAlbum != null ? entry.originalAlbum : "Sin álbum");
            album.setTextSize(12);
            row.addView(album);

            Button restore = new Button(this);
            restore.setText("Recuperar");
            restore.setOnClickListener(v -> restore(entry.mediaUri));
            row.addView(restore);

            trashList.addView(row);
        }
    }

    private void restore(String mediaUri) {
        executor.execute(() -> {
            trashRepository.restore(mediaUri);
            runOnUiThread(this::loadTrash);
        });
    }

    private void requestPermanentDeletion() {
        executor.execute(() -> {
            List<TrashEntry> entries = trashRepository.getAll();
            if (entries.isEmpty()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> uris = new ArrayList<>();
                for (TrashEntry entry : entries) {
                    uris.add(Uri.parse(entry.mediaUri));
                }
                pendingPermanentDeletion = entries;
                IntentSenderRequest request = new IntentSenderRequest.Builder(
                        MediaStore.createDeleteRequest(getContentResolver(), uris).getIntentSender()
                ).build();
                runOnUiThread(() -> deleteRequestLauncher.launch(request));
            } else {
                List<TrashEntry> deleted = new ArrayList<>();
                for (TrashEntry entry : entries) {
                    int deletedRows = getContentResolver().delete(
                            Uri.parse(entry.mediaUri), null, null);
                    if (deletedRows > 0) {
                        deleted.add(entry);
                    }
                }
                if (!deleted.isEmpty()) {
                    trashRepository.removePermanentlyDeleted(deleted);
                    recordPermanentDeletion(deleted);
                }
                runOnUiThread(this::loadTrash);
            }
        });
    }

    private void recordPermanentDeletion(List<TrashEntry> entries) {
        long bytesFreed = 0;
        for (TrashEntry entry : entries) {
            bytesFreed += Math.max(0, entry.sizeBytes);
        }
        com.example.layouts.data.db.CleanupStats stats =
                statsRepository.registerDeletions(entries.size(), bytesFreed);
        int unlocked = achievementRepository.checkThresholds(stats.deletedCount).size();
        if (unlocked > 0) {
            runOnUiThread(() -> Toast.makeText(this,
                    unlocked == 1 ? "Nuevo logro desbloqueado" : unlocked + " logros desbloqueados",
                    Toast.LENGTH_SHORT).show());
        }
    }
}
