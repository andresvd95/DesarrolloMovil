package com.example.layouts;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.layouts.data.AchievementRepository;
import com.example.layouts.data.MediaQuery;
import com.example.layouts.data.MediaRepository;
import com.example.layouts.data.SettingsRepository;
import com.example.layouts.data.StatsRepository;
import com.example.layouts.data.db.AppDatabase;
import com.example.layouts.data.db.TrashDao;
import com.example.layouts.gallery.MediaItem;
import com.example.layouts.ui.InsetsUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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

    /** SortKey.name() opcional para arrancar el deck ya ordenado (accesos rapidos de Home). */
    public static final String EXTRA_SORT_BY = "sortBy";
    /** true para arrancar mostrando solo capturas de pantalla (acceso rapido de Home). */
    public static final String EXTRA_ONLY_SCREENSHOTS = "onlyScreenshots";
    /**
     * Lista fija de mediaUri a revisar (p.ej. duplicados ya calculados por
     * DuplicateFinder). Cuando esta presente, reemplaza por completo la
     * consulta a MediaStore via MediaQuery: no es un filtro, es el conjunto
     * exacto a mostrar.
     */
    public static final String EXTRA_DUPLICATE_URIS = "duplicateUris";

    private final MediaRepository mediaRepository = new MediaRepository();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Deque<SwipeAction> undoStack = new ArrayDeque<>();

    private ArrayList<String> duplicateUris;

    private TrashDao trashDao;
    private StatsRepository statsRepository;
    private AchievementRepository achievementRepository;

    private FrameLayout cardMedia;
    private ImageView imageMedia;
    private TextView textVideoBadge;
    private VideoView videoPlayer;
    private ImageButton buttonCloseVideo;
    private TextView textEmptyState;
    private TextView textMediaName;
    private TextView textMediaMeta;
    private TextView textMediaCounter;
    private ImageButton buttonUndo;
    private ImageButton buttonTrash;
    private ImageButton buttonKeep;
    private ImageButton buttonFilters;
    private ImageButton buttonOpenTrash;
    private ImageButton buttonHome;
    private TextView stampKeep;
    private TextView stampTrash;

    private MediaQuery activeQuery = MediaQuery.builder().build();

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

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));
        InsetsUtil.applyBottomInset(findViewById(R.id.bottomActionBar));

        cardMedia = findViewById(R.id.cardMedia);
        imageMedia = findViewById(R.id.imageMedia);
        textVideoBadge = findViewById(R.id.textVideoBadge);
        videoPlayer = findViewById(R.id.videoPlayer);
        buttonCloseVideo = findViewById(R.id.buttonCloseVideo);
        textEmptyState = findViewById(R.id.textEmptyState);
        textMediaName = findViewById(R.id.textMediaName);
        textMediaMeta = findViewById(R.id.textMediaMeta);
        textMediaCounter = findViewById(R.id.textMediaCounter);
        buttonUndo = findViewById(R.id.buttonUndo);
        buttonTrash = findViewById(R.id.buttonTrash);
        buttonKeep = findViewById(R.id.buttonKeep);
        buttonFilters = findViewById(R.id.buttonFilters);
        buttonOpenTrash = findViewById(R.id.buttonOpenTrash);
        buttonHome = findViewById(R.id.buttonHome);
        stampKeep = findViewById(R.id.stampKeep);
        stampTrash = findViewById(R.id.stampTrash);

        cardMedia.setOnTouchListener(this::onCardTouch);
        buttonUndo.setOnClickListener(v -> undoLast());
        buttonTrash.setOnClickListener(v -> triggerAction(ActionType.TRASH));
        buttonKeep.setOnClickListener(v -> triggerAction(ActionType.KEEP));
        buttonFilters.setOnClickListener(v -> showFilterDialog());
        buttonOpenTrash.setOnClickListener(v -> startActivity(new Intent(this, TrashActivity.class)));
        buttonHome.setOnClickListener(v -> goHome());
        textVideoBadge.setOnClickListener(v -> playCurrentVideo());
        buttonCloseVideo.setOnClickListener(v -> closeVideoPlayer());

        duplicateUris = getIntent().getStringArrayListExtra(EXTRA_DUPLICATE_URIS);
        activeQuery = buildQueryFromIntent();
        if (duplicateUris != null) {
            // La lista ya es el conjunto exacto a revisar: filtrar de nuevo no aplica.
            buttonFilters.setVisibility(View.GONE);
        }

        if (hasPermissions()) {
            loadMedia();
        } else {
            permissionLauncher.launch(requiredPermissions());
        }
    }

    /** Arma la consulta inicial a partir de los extras de Home (orden/screenshots) + Ajustes. */
    private MediaQuery buildQueryFromIntent() {
        MediaQuery.Builder builder = MediaQuery.builder();
        String sortByName = getIntent().getStringExtra(EXTRA_SORT_BY);
        if (sortByName != null) {
            try {
                builder.sortBy(MediaQuery.SortKey.valueOf(sortByName));
            } catch (IllegalArgumentException ignored) {
                // extra invalido: se ignora, queda el orden por defecto
            }
        }
        if (getIntent().getBooleanExtra(EXTRA_ONLY_SCREENSHOTS, false)) {
            builder.onlyScreenshots(true);
        }
        applyGlobalExclusions(builder);
        return builder.build();
    }

    /** Combina la exclusion global de video/screenshots (Ajustes) con el filtro puntual. */
    private void applyGlobalExclusions(MediaQuery.Builder builder) {
        SettingsRepository settings = new SettingsRepository(this);
        builder.excludeVideos(settings.isExcludeVideos());
        builder.excludeScreenshots(settings.isExcludeScreenshots());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoPlayer.isPlaying()) {
            videoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ---- Filtros y orden (RF06, RF07) ----

    private void showFilterDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(20), dp(24), dp(4));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout headerIcon = new FrameLayout(this);
        int headerIconSize = dp(34);
        headerIcon.setLayoutParams(new LinearLayout.LayoutParams(headerIconSize, headerIconSize));
        headerIcon.setBackgroundResource(R.drawable.bg_circle_primary_gradient);
        ImageView headerIconImage = new ImageView(this);
        int headerGlyphSize = dp(16);
        FrameLayout.LayoutParams headerGlyphParams = new FrameLayout.LayoutParams(headerGlyphSize, headerGlyphSize);
        headerGlyphParams.gravity = Gravity.CENTER;
        headerIconImage.setLayoutParams(headerGlyphParams);
        headerIconImage.setImageResource(R.drawable.ic_sliders);
        headerIconImage.setColorFilter(ContextCompat.getColor(this, R.color.white));
        headerIcon.addView(headerIconImage);
        header.addView(headerIcon);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Filtros");
        headerTitle.setTextSize(18);
        headerTitle.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_extrabold));
        headerTitle.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
        LinearLayout.LayoutParams headerTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerTitleParams.leftMargin = dp(12);
        headerTitle.setLayoutParams(headerTitleParams);
        header.addView(headerTitle);

        form.addView(header);

        List<Calendar> recentMonths = buildRecentMonths();
        Spinner typeSpinner = spinner(new String[]{"Todos", "Fotos", "Videos"});
        Spinner monthSpinner = spinner(monthLabels(recentMonths));
        Spinner sortSpinner = spinner(new String[]{"Fecha", "Peso", "Tipo", "Aleatorio"});
        typeSpinner.setSelection(typeSelectionFor(activeQuery.getType()));
        sortSpinner.setSelection(sortSelectionFor(activeQuery.getSortBy()));

        form.addView(filterGroup("Mostrar", typeSpinner));
        form.addView(filterGroup("Mes", monthSpinner));
        form.addView(filterGroup("Ordenar por", sortSpinner));

        new AlertDialog.Builder(this)
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aplicar", (dialog, which) ->
                        applyFilter(typeSpinner, monthSpinner, sortSpinner, recentMonths))
                .show();
    }

    private View filterGroup(String labelText, Spinner spinner) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        groupParams.topMargin = dp(18);
        group.setLayoutParams(groupParams);
        group.addView(label(labelText));

        FrameLayout fieldShell = new FrameLayout(this);
        fieldShell.setBackgroundResource(R.drawable.bg_field);
        LinearLayout.LayoutParams shellParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        shellParams.topMargin = dp(6);
        fieldShell.setLayoutParams(shellParams);
        fieldShell.setPaddingRelative(dp(14), 0, dp(14), 0);

        spinner.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        spinnerParams.gravity = Gravity.CENTER_VERTICAL;
        spinner.setLayoutParams(spinnerParams);
        fieldShell.addView(spinner);

        group.addView(fieldShell);
        return group;
    }

    /** Primer dia (00:00) de este mes y de los 11 anteriores, mas reciente primero. */
    private List<Calendar> buildRecentMonths() {
        List<Calendar> months = new ArrayList<>();
        Calendar cursor = Calendar.getInstance();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        cursor.set(Calendar.HOUR_OF_DAY, 0);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < 12; i++) {
            months.add((Calendar) cursor.clone());
            cursor.add(Calendar.MONTH, -1);
        }
        return months;
    }

    private String[] monthLabels(List<Calendar> months) {
        SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String[] labels = new String[months.size() + 1];
        labels[0] = "Todos los meses";
        for (int i = 0; i < months.size(); i++) {
            String label = format.format(months.get(i).getTime());
            labels[i + 1] = label.substring(0, 1).toUpperCase(Locale.getDefault()) + label.substring(1);
        }
        return labels;
    }

    private int typeSelectionFor(MediaQuery.MediaType type) {
        if (type == MediaQuery.MediaType.PHOTOS) {
            return 1;
        }
        if (type == MediaQuery.MediaType.VIDEOS) {
            return 2;
        }
        return 0;
    }

    private int sortSelectionFor(MediaQuery.SortKey sortKey) {
        if (sortKey == MediaQuery.SortKey.SIZE) {
            return 1;
        }
        if (sortKey == MediaQuery.SortKey.TYPE) {
            return 2;
        }
        if (sortKey == MediaQuery.SortKey.RANDOM) {
            return 3;
        }
        return 0;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(11);
        label.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_bold));
        label.setTextColor(ContextCompat.getColor(this, R.color.colorTextTertiary));
        label.setLetterSpacing(0.08f);
        label.setAllCaps(true);
        return label;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void applyFilter(Spinner typeSpinner, Spinner monthSpinner, Spinner sortSpinner,
                             List<Calendar> recentMonths) {
        MediaQuery.MediaType type = typeSpinner.getSelectedItemPosition() == 1
                ? MediaQuery.MediaType.PHOTOS
                : typeSpinner.getSelectedItemPosition() == 2
                ? MediaQuery.MediaType.VIDEOS : MediaQuery.MediaType.ALL;
        MediaQuery.SortKey sort = sortSpinner.getSelectedItemPosition() == 1
                ? MediaQuery.SortKey.SIZE
                : sortSpinner.getSelectedItemPosition() == 2
                ? MediaQuery.SortKey.TYPE
                : sortSpinner.getSelectedItemPosition() == 3
                ? MediaQuery.SortKey.RANDOM : MediaQuery.SortKey.DATE;

        Long fromDateAdded = null;
        Long toDateAdded = null;
        int monthPosition = monthSpinner.getSelectedItemPosition();
        if (monthPosition > 0) {
            Calendar start = recentMonths.get(monthPosition - 1);
            Calendar end = (Calendar) start.clone();
            end.add(Calendar.MONTH, 1);
            fromDateAdded = start.getTimeInMillis() / 1000;
            toDateAdded = end.getTimeInMillis() / 1000 - 1;
        }

        MediaQuery.Builder builder = MediaQuery.builder()
                .type(type)
                .sortBy(sort)
                .fromDateAdded(fromDateAdded)
                .toDateAdded(toDateAdded);
        applyGlobalExclusions(builder);
        activeQuery = builder.build();

        undoStack.clear();
        loadMedia();
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
            List<MediaItem> loaded = duplicateUris != null
                    ? mediaRepository.loadByUris(getContentResolver(), duplicateUris)
                    : mediaRepository.loadFromDevice(getContentResolver(), activeQuery);

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
        setUndoEnabled(false);
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
            setUndoEnabled(!undoStack.isEmpty());
            return;
        }

        textEmptyState.setVisibility(View.GONE);
        resetCardTransform(cardMedia);
        if (videoPlayer.getVisibility() == View.VISIBLE) {
            videoPlayer.stopPlayback();
            videoPlayer.setVisibility(View.GONE);
            buttonCloseVideo.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            imageMedia.setVisibility(View.VISIBLE);
            imageMedia.setImageResource(android.R.color.black);
            textVideoBadge.setVisibility(View.VISIBLE);
            loadVideoThumbnail(item);
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
        setUndoEnabled(!undoStack.isEmpty());
    }

    // ---- Miniatura y reproduccion de video ----

    private void loadVideoThumbnail(MediaItem item) {
        String targetUri = item.getUriString();
        executor.execute(() -> {
            Bitmap thumbnail = mediaRepository.loadThumbnail(getContentResolver(), item);
            runOnUiThread(() -> {
                MediaItem current = pendingQueue.peekFirst();
                if (current == null || !targetUri.equals(current.getUriString())) {
                    return; // el usuario ya avanzo a otro elemento
                }
                if (thumbnail != null) {
                    imageMedia.setImageBitmap(thumbnail);
                }
            });
        });
    }

    private void playCurrentVideo() {
        MediaItem item = pendingQueue.peekFirst();
        if (item == null || !item.isVideo()) {
            return;
        }

        textVideoBadge.setVisibility(View.GONE);
        videoPlayer.setVisibility(View.VISIBLE);
        buttonCloseVideo.setVisibility(View.VISIBLE);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoPlayer);
        videoPlayer.setMediaController(controller);
        videoPlayer.setVideoURI(Uri.parse(item.getUriString()));
        videoPlayer.setOnCompletionListener(mp -> closeVideoPlayer());
        videoPlayer.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "No se pudo reproducir este video", Toast.LENGTH_SHORT).show();
            closeVideoPlayer();
            return true;
        });
        videoPlayer.start();
    }

    private void closeVideoPlayer() {
        if (videoPlayer.isPlaying() || videoPlayer.getVisibility() == View.VISIBLE) {
            videoPlayer.stopPlayback();
        }
        videoPlayer.setVisibility(View.GONE);
        buttonCloseVideo.setVisibility(View.GONE);

        MediaItem item = pendingQueue.peekFirst();
        if (item != null && item.isVideo()) {
            textVideoBadge.setVisibility(View.VISIBLE);
        }
    }

    // ---- Navegacion ----

    /**
     * Vuelve a HomeActivity sin apilar otra instancia encima (CLEAR_TOP): si
     * ya existe en el back stack (siempre deberia, es la pantalla que abre
     * el deck), la trae al frente; si no, crea una nueva.
     */
    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
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
                updateStamps(dx, view.getWidth());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleRelease(view);
                return true;
            default:
                return false;
        }
    }

    private void setUndoEnabled(boolean enabled) {
        buttonUndo.setEnabled(enabled);
        buttonUndo.setAlpha(enabled ? 1f : 0.4f);
    }

    private void updateStamps(float dx, int width) {
        float threshold = width > 0 ? width * 0.35f : 260f;
        stampKeep.setAlpha(dx > 0 ? Math.min(dx / threshold, 1f) : 0f);
        stampTrash.setAlpha(dx < 0 ? Math.min(-dx / threshold, 1f) : 0f);
    }

    /** Dispara la misma resolucion que un swipe, pero desde los botones (RF03). */
    private void triggerAction(ActionType type) {
        if (pendingQueue.isEmpty()) {
            return;
        }
        int width = cardMedia.getWidth();
        if (width == 0) {
            resolveSwipe(type);
            return;
        }
        float targetX = type == ActionType.KEEP ? width * 1.5f : -width * 1.5f;
        stampKeep.setAlpha(type == ActionType.KEEP ? 1f : 0f);
        stampTrash.setAlpha(type == ActionType.TRASH ? 1f : 0f);
        flingAway(cardMedia, targetX, 0f, type);
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
        stampKeep.setAlpha(0f);
        stampTrash.setAlpha(0f);
        view.animate().translationX(0).translationY(0).rotation(0).setDuration(180).start();
    }

    private void resetCardTransform(View view) {
        view.setTranslationX(0);
        view.setTranslationY(0);
        view.setRotation(0);
        view.setAlpha(1f);
        stampKeep.setAlpha(0f);
        stampTrash.setAlpha(0f);
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
