package com.example.layouts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.layouts.data.AchievementRepository;
import com.example.layouts.data.MediaRepository;
import com.example.layouts.data.StatsRepository;
import com.example.layouts.data.db.AppDatabase;
import com.example.layouts.data.db.TrashDao;
import com.example.layouts.gallery.MediaItem;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fase 1 (RF01, RF02) + Fase 2 (RF03, RF04): pide permisos de medios,
 * muestra un archivo real de la galeria a la vez, y lo resuelve con
 * gestos swipe (conservar / papelera / despues) con deshacer de la
 * ultima accion.
 */
public class SwipeDeckActivity extends AppCompatActivity {

    private final MediaRepository mediaRepository = new MediaRepository();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Deque<SwipeAction> undoStack = new ArrayDeque<>();

    private TrashDao trashDao;
    private StatsRepository statsRepository;
    private AchievementRepository achievementRepository;

    private FrameLayout cardMedia;
    private ImageView imageMedia;
    private TextView textVideoBadge;
    private TextView textEmptyState;
    private TextView textMediaName;
    private TextView textMediaMeta;
    private TextView textMediaCounter;
    private Button buttonUndo;

    private LinkedList<MediaItem> pendingQueue = new LinkedList<>();
    private float dragStartRawX;
    private float dragStartRawY;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (allGranted(result)) {
                    loadMedia();
                } else {
                    showPermissionDenied();
                }
            });

    private enum ActionType { KEEP, TRASH, DEFER }

    private static final class SwipeAction {
        final ActionType type;
        final MediaItem item;

        SwipeAction(ActionType type, MediaItem item) {
            this.type = type;
            this.item = item;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe_deck);

        cardMedia = findViewById(R.id.cardMedia);
        imageMedia = findViewById(R.id.imageMedia);
        textVideoBadge = findViewById(R.id.textVideoBadge);
        textEmptyState = findViewById(R.id.textEmptyState);
        textMediaName = findViewById(R.id.textMediaName);
        textMediaMeta = findViewById(R.id.textMediaMeta);
        textMediaCounter = findViewById(R.id.textMediaCounter);
        buttonUndo = findViewById(R.id.buttonUndo);

        cardMedia.setOnTouchListener(this::onCardTouch);
        buttonUndo.setOnClickListener(v -> undoLast());

        if (hasPermissions()) {
            loadMedia();
        } else {
            permissionLauncher.launch(requiredPermissions());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ---- Permisos (RF01) ----

    private String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        }
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    private boolean hasPermissions() {
        for (String permission : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean allGranted(Map<String, Boolean> result) {
        for (Boolean granted : result.values()) {
            if (granted == null || !granted) {
                return false;
            }
        }
        return true;
    }

    // ---- Carga de datos (RF01, RF02) ----

    private void loadMedia() {
        textEmptyState.setText("Cargando galería…");
        textEmptyState.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<MediaItem> loaded = mediaRepository.loadFromDevice(getContentResolver());

            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            trashDao = database.trashDao();
            statsRepository = new StatsRepository(database.statsDao());
            achievementRepository = new AchievementRepository(database.achievementDao());
            achievementRepository.ensureSeeded();

            runOnUiThread(() -> {
                pendingQueue = new LinkedList<>(loaded);
                renderCurrent();
            });
        });
    }

    private void showPermissionDenied() {
        imageMedia.setVisibility(View.GONE);
        textVideoBadge.setVisibility(View.GONE);
        textEmptyState.setText("SwipeClean necesita acceso a tus fotos y videos para funcionar.\nActiva el permiso desde los ajustes de la app.");
        textEmptyState.setVisibility(View.VISIBLE);
        textMediaName.setText("");
        textMediaMeta.setText("");
        textMediaCounter.setText("");
        buttonUndo.setEnabled(false);
    }

    private void renderCurrent() {
        MediaItem item = pendingQueue.peekFirst();

        if (item == null) {
            imageMedia.setVisibility(View.GONE);
            textVideoBadge.setVisibility(View.GONE);
            textEmptyState.setText("No quedan fotos ni videos por revisar.");
            textEmptyState.setVisibility(View.VISIBLE);
            textMediaName.setText("");
            textMediaMeta.setText("");
            textMediaCounter.setText("0 pendiente(s)");
            buttonUndo.setEnabled(!undoStack.isEmpty());
            return;
        }

        textEmptyState.setVisibility(View.GONE);
        resetCardTransform(cardMedia);

        if (item.isVideo()) {
            imageMedia.setVisibility(View.GONE);
            textVideoBadge.setVisibility(View.VISIBLE);
        } else {
            textVideoBadge.setVisibility(View.GONE);
            imageMedia.setVisibility(View.VISIBLE);
            imageMedia.setImageURI(Uri.parse(item.getUriString()));
        }

        textMediaName.setText(item.getDisplayName());
        String album = item.getAlbumName() != null ? item.getAlbumName() : "Sin álbum";
        String type = item.isVideo() ? "Video" : "Foto";
        textMediaMeta.setText(MediaRepository.humanReadableSize(item.getSizeBytes()) + " · " + album + " · " + type);
        textMediaCounter.setText(pendingQueue.size() + " pendiente(s)");
        buttonUndo.setEnabled(!undoStack.isEmpty());
    }

    // ---- Gestos swipe (RF03) ----

    private boolean onCardTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartRawX = event.getRawX();
                dragStartRawY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - dragStartRawX;
                float dy = event.getRawY() - dragStartRawY;
                view.setTranslationX(dx);
                view.setTranslationY(dy);
                view.setRotation(dx / 20f);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleRelease(view);
                return true;
            default:
                return false;
        }
    }

    private void handleRelease(View view) {
        if (pendingQueue.isEmpty()) {
            return;
        }
        float dx = view.getTranslationX();
        float dy = view.getTranslationY();
        int width = view.getWidth();
        int height = view.getHeight();

        if (width == 0 || height == 0) {
            snapBack(view);
            return;
        }

        if (dx > width * 0.4f) {
            flingAway(view, width * 1.5f, dy, ActionType.KEEP);
        } else if (dx < -width * 0.4f) {
            flingAway(view, -width * 1.5f, dy, ActionType.TRASH);
        } else if (-dy > height * 0.4f) {
            flingAway(view, dx, -height * 1.5f, ActionType.DEFER);
        } else {
            snapBack(view);
        }
    }

    private void flingAway(View view, float targetX, float targetY, ActionType type) {
        view.animate()
                .translationX(targetX)
                .translationY(targetY)
                .setDuration(220)
                .withEndAction(() -> resolveSwipe(type))
                .start();
    }

    private void snapBack(View view) {
        view.animate().translationX(0).translationY(0).rotation(0).setDuration(180).start();
    }

    private void resetCardTransform(View view) {
        view.setTranslationX(0);
        view.setTranslationY(0);
        view.setRotation(0);
        view.setAlpha(1f);
    }

    private void resolveSwipe(ActionType type) {
        MediaItem item = pendingQueue.pollFirst();
        if (item == null) {
            return;
        }
        undoStack.push(new SwipeAction(type, item));

        switch (type) {
            case KEEP:
                executor.execute(() -> statsRepository.incrementReviewed());
                break;
            case TRASH:
                executor.execute(() -> {
                    mediaRepository.moveToTrash(trashDao, item);
                    statsRepository.incrementReviewed();
                });
                break;
            case DEFER:
                pendingQueue.addLast(item);
                break;
        }

        renderCurrent();
    }

    // ---- Deshacer (RF04) ----

    private void undoLast() {
        SwipeAction action = undoStack.poll();
        if (action == null) {
            return;
        }

        switch (action.type) {
            case KEEP:
                pendingQueue.addFirst(action.item);
                executor.execute(() -> statsRepository.decrementReviewed());
                break;
            case TRASH:
                pendingQueue.addFirst(action.item);
                executor.execute(() -> {
                    mediaRepository.restoreFromTrash(trashDao, action.item);
                    statsRepository.decrementReviewed();
                });
                break;
            case DEFER:
                if (!pendingQueue.isEmpty()) {
                    pendingQueue.removeLast();
                }
                pendingQueue.addFirst(action.item);
                break;
        }

        renderCurrent();
    }
}
