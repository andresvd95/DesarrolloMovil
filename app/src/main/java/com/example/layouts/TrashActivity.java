package com.example.layouts;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.layouts.data.AchievementRepository;
import com.example.layouts.data.MediaRepository;
import com.example.layouts.data.StatsRepository;
import com.example.layouts.data.TrashRepository;
import com.example.layouts.data.db.AppDatabase;
import com.example.layouts.data.db.TrashEntry;
import com.example.layouts.ui.InsetsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Papelera temporal: muestra miniaturas de lo eliminado, permite
 * seleccionar cada elemento y recuperarlo o solicitar su eliminacion
 * definitiva con la confirmacion nativa de Android (RF05).
 */
public class TrashActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MediaRepository mediaRepository = new MediaRepository();
    private final Set<String> selectedUris = new HashSet<>();
    private final Map<String, CachedThumb> thumbnailCache = new HashMap<>();

    private TrashRepository trashRepository;
    private StatsRepository statsRepository;
    private AchievementRepository achievementRepository;

    private LinearLayout trashList;
    private TextView emptyState;
    private TextView textTrashCount;
    private TextView textSelectionStatus;
    private TextView buttonSelectAction;
    private TextView emptyTrashButton;
    private LinearLayout selectionActionBar;

    private List<TrashEntry> currentEntries = new ArrayList<>();
    private List<TrashEntry> pendingPermanentDeletion = new ArrayList<>();

    private static final class CachedThumb {
        final Bitmap bitmap;
        final boolean isVideo;

        CachedThumb(Bitmap bitmap, boolean isVideo) {
            this.bitmap = bitmap;
            this.isVideo = isVideo;
        }
    }

    private static final String TAG = "TrashActivity";

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                Log.d(TAG, "deleteRequestLauncher resultCode=" + result.getResultCode()
                        + " (RESULT_OK=" + RESULT_OK + ") pendingCount=" + pendingPermanentDeletion.size());
                Toast.makeText(this, "Sistema devolvió resultCode=" + result.getResultCode(), Toast.LENGTH_LONG).show();
                if (result.getResultCode() == RESULT_OK && !pendingPermanentDeletion.isEmpty()) {
                    List<TrashEntry> deleted = new ArrayList<>(pendingPermanentDeletion);
                    pendingPermanentDeletion.clear();
                    executor.execute(() -> {
                        trashRepository.removePermanentlyDeleted(deleted);
                        recordPermanentDeletion(deleted);
                        Log.d(TAG, "Room actualizado: " + deleted.size() + " entrada(s) removida(s) de trash_entry");
                        runOnUiThread(() -> {
                            Toast.makeText(this, deleted.size() + " elemento(s) eliminado(s) de la papelera (app)", Toast.LENGTH_SHORT).show();
                            loadTrash();
                        });
                    });
                } else if (result.getResultCode() != RESULT_OK) {
                    Log.w(TAG, "El usuario canceló o el sistema rechazó el pedido de borrado");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        trashList = findViewById(R.id.trashList);
        emptyState = findViewById(R.id.textTrashEmptyState);
        textTrashCount = findViewById(R.id.textTrashCount);
        textSelectionStatus = findViewById(R.id.textSelectionStatus);
        buttonSelectAction = findViewById(R.id.buttonSelectAction);
        emptyTrashButton = findViewById(R.id.buttonEmptyTrash);
        selectionActionBar = findViewById(R.id.selectionActionBar);
        InsetsUtil.applyBottomInset(selectionActionBar);
        Button buttonRestoreSelected = findViewById(R.id.buttonRestoreSelected);
        Button buttonDeleteSelected = findViewById(R.id.buttonDeleteSelected);

        AppDatabase database = AppDatabase.getInstance(getApplicationContext());
        trashRepository = new TrashRepository(database.trashDao());
        statsRepository = new StatsRepository(database.statsDao());
        achievementRepository = new AchievementRepository(database.achievementDao());

        emptyTrashButton.setOnClickListener(v -> confirmEmptyTrash());
        buttonSelectAction.setOnClickListener(v -> onSelectActionClick());
        buttonRestoreSelected.setOnClickListener(v -> restoreSelected());
        buttonDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());

        executor.execute(achievementRepository::ensureSeeded);
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
        currentEntries = entries;

        Set<String> validUris = new HashSet<>();
        for (TrashEntry entry : entries) {
            validUris.add(entry.mediaUri);
        }
        selectedUris.retainAll(validUris);

        boolean isEmpty = entries.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        emptyTrashButton.setEnabled(!isEmpty);
        emptyTrashButton.setAlpha(isEmpty ? 0.4f : 1f);
        textTrashCount.setText(isEmpty ? "Sin elementos" : entries.size() + " elementos");

        trashList.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < entries.size(); i++) {
            int column = i % 3;
            if (column == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    rowParams.topMargin = dp(10);
                }
                row.setLayoutParams(rowParams);
                trashList.addView(row);
            }
            row.addView(buildTile(entries.get(i), column));
        }

        updateSelectionUi(isEmpty);
    }

    private void updateSelectionUi(boolean isEmpty) {
        boolean hasSelection = !selectedUris.isEmpty();
        selectionActionBar.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
        buttonSelectAction.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (hasSelection) {
            textSelectionStatus.setText(selectedUris.size() + " seleccionados");
            buttonSelectAction.setText("Cancelar");
        } else {
            textSelectionStatus.setText(isEmpty ? "" : "Toca una foto para seleccionarla");
            buttonSelectAction.setText("Seleccionar todo");
        }
    }

    private View buildTile(TrashEntry entry, int column) {
        int size = dp(108);
        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, size, 1f);
        if (column > 0) {
            tileParams.leftMargin = dp(8);
        }
        tile.setLayoutParams(tileParams);
        tile.setBackgroundResource(R.drawable.bg_tile_thumb);
        tile.setClipToOutline(true);
        tile.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);

        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(thumb);

        ImageView playBadge = new ImageView(this);
        int badgeSize = dp(26);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(badgeSize, badgeSize);
        badgeParams.gravity = Gravity.TOP | Gravity.END;
        badgeParams.setMargins(0, dp(6), dp(6), 0);
        playBadge.setLayoutParams(badgeParams);
        playBadge.setBackgroundResource(R.drawable.bg_checkbox_circle);
        playBadge.setPadding(dp(6), dp(6), dp(6), dp(6));
        playBadge.setImageResource(R.drawable.ic_play);
        playBadge.setSelected(true);
        playBadge.setVisibility(View.GONE);
        tile.addView(playBadge);

        FrameLayout checkbox = new FrameLayout(this);
        int checkboxSize = dp(22);
        FrameLayout.LayoutParams checkboxParams = new FrameLayout.LayoutParams(checkboxSize, checkboxSize);
        checkboxParams.gravity = Gravity.TOP | Gravity.START;
        checkboxParams.setMargins(dp(6), dp(6), 0, 0);
        checkbox.setLayoutParams(checkboxParams);
        checkbox.setBackgroundResource(R.drawable.bg_checkbox_circle);

        ImageView check = new ImageView(this);
        int checkIconSize = dp(11);
        FrameLayout.LayoutParams checkIconParams = new FrameLayout.LayoutParams(checkIconSize, checkIconSize);
        checkIconParams.gravity = Gravity.CENTER;
        check.setLayoutParams(checkIconParams);
        check.setImageResource(R.drawable.ic_check);
        check.setColorFilter(ContextCompat.getColor(this, R.color.white));
        checkbox.addView(check);

        boolean selected = selectedUris.contains(entry.mediaUri);
        checkbox.setSelected(selected);
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
        tile.setForeground(selected
                ? ContextCompat.getDrawable(this, R.drawable.bg_tile_ring_selected)
                : null);
        tile.addView(checkbox);

        CachedThumb cached = thumbnailCache.get(entry.mediaUri);
        if (cached != null) {
            thumb.setImageBitmap(cached.bitmap);
            playBadge.setVisibility(cached.isVideo ? View.VISIBLE : View.GONE);
        } else {
            executor.execute(() -> {
                boolean isVideo = MediaRepository.isVideoUri(getContentResolver(), entry.mediaUri);
                Bitmap bitmap = mediaRepository.loadThumbnail(
                        getContentResolver(), entry.mediaUri, isVideo, new Size(400, 400));
                if (bitmap != null) {
                    thumbnailCache.put(entry.mediaUri, new CachedThumb(bitmap, isVideo));
                }
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        thumb.setImageBitmap(bitmap);
                    }
                    playBadge.setVisibility(isVideo ? View.VISIBLE : View.GONE);
                });
            });
        }

        tile.setOnClickListener(v -> toggleSelect(entry.mediaUri));
        return tile;
    }

    private void toggleSelect(String mediaUri) {
        if (!selectedUris.add(mediaUri)) {
            selectedUris.remove(mediaUri);
        }
        render(currentEntries);
    }

    private void onSelectActionClick() {
        if (selectedUris.isEmpty()) {
            for (TrashEntry entry : currentEntries) {
                selectedUris.add(entry.mediaUri);
            }
        } else {
            selectedUris.clear();
        }
        render(currentEntries);
    }

    private List<TrashEntry> entriesMatchingSelection() {
        List<TrashEntry> result = new ArrayList<>();
        for (TrashEntry entry : currentEntries) {
            if (selectedUris.contains(entry.mediaUri)) {
                result.add(entry);
            }
        }
        return result;
    }

    private void restoreSelected() {
        List<TrashEntry> toRestore = entriesMatchingSelection();
        if (toRestore.isEmpty()) {
            return;
        }
        selectedUris.clear();
        executor.execute(() -> {
            for (TrashEntry entry : toRestore) {
                trashRepository.restore(entry.mediaUri);
            }
            runOnUiThread(this::loadTrash);
        });
    }

    /**
     * El propio dialogo de confirmacion se saca a proposito: Android ya
     * exige su propio dialogo nativo por cada MediaStore.createDeleteRequest
     * (ver requestPermanentDeletion) y no se puede omitir ni recordar la
     * respuesta. Confirmar dos veces la misma accion es friccion redundante,
     * asi que esta pantalla dispara el pedido directo y deja que el sistema
     * sea la unica confirmacion.
     */
    private void confirmDeleteSelected() {
        List<TrashEntry> toDelete = entriesMatchingSelection();
        if (toDelete.isEmpty()) {
            return;
        }
        selectedUris.clear();
        requestPermanentDeletion(toDelete);
    }

    private void confirmEmptyTrash() {
        if (currentEntries.isEmpty()) {
            return;
        }
        selectedUris.clear();
        requestPermanentDeletion(new ArrayList<>(currentEntries));
    }

    private void requestPermanentDeletion(List<TrashEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        Log.d(TAG, "requestPermanentDeletion: pidiendo borrado de " + entries.size() + " elemento(s), sdk=" + Build.VERSION.SDK_INT);
        executor.execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> uris = new ArrayList<>();
                for (TrashEntry entry : entries) {
                    Uri uri = Uri.parse(entry.mediaUri);
                    Log.d(TAG, "  uri a borrar: " + uri);
                    uris.add(uri);
                }
                pendingPermanentDeletion = entries;
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(
                            MediaStore.createDeleteRequest(getContentResolver(), uris).getIntentSender()
                    ).build();
                    Log.d(TAG, "createDeleteRequest OK, lanzando dialogo del sistema");
                    runOnUiThread(() -> deleteRequestLauncher.launch(request));
                } catch (Exception exception) {
                    Log.e(TAG, "createDeleteRequest lanzo una excepcion", exception);
                    pendingPermanentDeletion = new ArrayList<>();
                    runOnUiThread(() -> Toast.makeText(this,
                            "No se pudo pedir el borrado: " + exception.getMessage(), Toast.LENGTH_LONG).show());
                }
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

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
