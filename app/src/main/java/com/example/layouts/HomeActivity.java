package com.example.layouts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.layouts.data.DuplicateFinder;
import com.example.layouts.data.MediaQuery;
import com.example.layouts.data.MediaRepository;
import com.example.layouts.data.SettingsRepository;
import com.example.layouts.gallery.MediaItem;
import com.example.layouts.ui.InsetsUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla principal tras login/registro (spec 2026-08-25-home-screen-design):
 * saludo + acceso a papelera, hero "Empezar a limpiar", accesos rapidos
 * (Random/Recientes/Duplicados/Capturas), un vistazo de lo mas reciente
 * agrupado por mes, y navegacion inferior a Logros/Dashboard/Configuracion.
 */
public class HomeActivity extends AppCompatActivity {

    /** Cuantos items recientes se traen como maximo para el vistazo "por mes" de Home. */
    private static final int BROWSE_PREVIEW_LIMIT = 60;

    private final MediaRepository mediaRepository = new MediaRepository();
    private final DuplicateFinder duplicateFinder = new DuplicateFinder();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, Bitmap> thumbnailCache = new LinkedHashMap<>();

    private TextView textGreeting;
    private TextView textGreetingTitle;
    private FrameLayout cardHero;
    private FrameLayout tileRandom;
    private FrameLayout tileRecent;
    private FrameLayout tileDuplicates;
    private TextView textDuplicatesSubtitle;
    private FrameLayout tileScreenshots;
    private FrameLayout buttonViewList;
    private FrameLayout buttonViewGrid;
    private TextView textMonthsEmptyState;
    private LinearLayout monthSections;

    private boolean gridView = true;
    private List<MediaItem> lastLoadedItems = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (allGranted(result)) {
                    loadBrowsePreview();
                } else {
                    showPermissionDenied();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        InsetsUtil.applyTopInset(findViewById(R.id.topBar));
        InsetsUtil.applyBottomInset(findViewById(R.id.bottomNav));

        textGreeting = findViewById(R.id.textGreeting);
        textGreetingTitle = findViewById(R.id.textGreetingTitle);
        ImageButton buttonOpenTrash = findViewById(R.id.buttonOpenTrash);
        cardHero = findViewById(R.id.cardHero);
        tileRandom = findViewById(R.id.tileRandom);
        tileRecent = findViewById(R.id.tileRecent);
        tileDuplicates = findViewById(R.id.tileDuplicates);
        textDuplicatesSubtitle = findViewById(R.id.textDuplicatesSubtitle);
        tileScreenshots = findViewById(R.id.tileScreenshots);
        buttonViewList = findViewById(R.id.buttonViewList);
        buttonViewGrid = findViewById(R.id.buttonViewGrid);
        textMonthsEmptyState = findViewById(R.id.textMonthsEmptyState);
        monthSections = findViewById(R.id.monthSections);
        View navHome = findViewById(R.id.navHome);
        View navAchievements = findViewById(R.id.navAchievements);
        View navDashboard = findViewById(R.id.navDashboard);
        View navSettings = findViewById(R.id.navSettings);

        setGreeting();

        buttonOpenTrash.setOnClickListener(v -> startActivity(new Intent(this, TrashActivity.class)));
        cardHero.setOnClickListener(v -> openDeck(MediaQuery.SortKey.RANDOM, false));
        tileRandom.setOnClickListener(v -> openDeck(MediaQuery.SortKey.RANDOM, false));
        tileRecent.setOnClickListener(v -> openDeck(MediaQuery.SortKey.DATE, false));
        tileScreenshots.setOnClickListener(v -> openDeck(MediaQuery.SortKey.DATE, true));
        tileDuplicates.setOnClickListener(v -> findDuplicatesAndOpen());
        buttonViewList.setOnClickListener(v -> setGridView(false));
        buttonViewGrid.setOnClickListener(v -> setGridView(true));
        navHome.setOnClickListener(v -> { /* ya estamos en Home */ });
        navAchievements.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        navDashboard.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        navSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        if (hasPermissions()) {
            loadBrowsePreview();
        } else {
            permissionLauncher.launch(requiredPermissions());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // el usuario puede haber borrado/restaurado items en otra pantalla
        if (hasPermissions()) {
            loadBrowsePreview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String salutation = hour < 12 ? "Buenos días," : hour < 19 ? "Buenas tardes," : "Buenas noches,";
        textGreeting.setText(salutation);
        textGreetingTitle.setText("¿Listos para limpiar?");
    }

    private void openDeck(MediaQuery.SortKey sortKey, boolean onlyScreenshots) {
        Intent intent = new Intent(this, SwipeDeckActivity.class);
        intent.putExtra(SwipeDeckActivity.EXTRA_SORT_BY, sortKey.name());
        intent.putExtra(SwipeDeckActivity.EXTRA_ONLY_SCREENSHOTS, onlyScreenshots);
        startActivity(intent);
    }

    // ---- Duplicados (sin IA) ----

    private void findDuplicatesAndOpen() {
        tileDuplicates.setEnabled(false);
        textDuplicatesSubtitle.setText("buscando…");
        executor.execute(() -> {
            MediaQuery.Builder builder = MediaQuery.builder();
            applyGlobalExclusions(builder);
            List<MediaItem> all = mediaRepository.loadFromDevice(getContentResolver(), builder.build());
            List<MediaItem> duplicates = duplicateFinder.findDuplicates(all,
                    uri -> getContentResolver().openInputStream(Uri.parse(uri)));

            runOnUiThread(() -> {
                tileDuplicates.setEnabled(true);
                textDuplicatesSubtitle.setText("buscar coincidencias");
                if (duplicates.isEmpty()) {
                    Toast.makeText(this, "Sin duplicados por ahora", Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<String> uris = new ArrayList<>();
                for (MediaItem item : duplicates) {
                    uris.add(item.getUriString());
                }
                Intent intent = new Intent(this, SwipeDeckActivity.class);
                intent.putStringArrayListExtra(SwipeDeckActivity.EXTRA_DUPLICATE_URIS, uris);
                startActivity(intent);
            });
        });
    }

    // ---- Explorar por mes ----

    private void setGridView(boolean grid) {
        gridView = grid;
        buttonViewGrid.setBackgroundResource(grid ? R.drawable.bg_pill_primary_gradient : 0);
        buttonViewList.setBackgroundResource(!grid ? R.drawable.bg_pill_primary_gradient : 0);
        ((ImageView) buttonViewGrid.getChildAt(0)).setColorFilter(
                ContextCompat.getColor(this, grid ? R.color.white : R.color.colorTextSecondary));
        ((ImageView) buttonViewList.getChildAt(0)).setColorFilter(
                ContextCompat.getColor(this, !grid ? R.color.white : R.color.colorTextSecondary));
        renderMonths(lastLoadedItems);
    }

    private void loadBrowsePreview() {
        textMonthsEmptyState.setText("Cargando…");
        textMonthsEmptyState.setVisibility(View.VISIBLE);
        monthSections.removeAllViews();
        executor.execute(() -> {
            MediaQuery.Builder builder = MediaQuery.builder().sortBy(MediaQuery.SortKey.DATE);
            applyGlobalExclusions(builder);
            List<MediaItem> loaded = mediaRepository.loadFromDevice(getContentResolver(), builder.build());
            List<MediaItem> preview = loaded.size() > BROWSE_PREVIEW_LIMIT
                    ? loaded.subList(0, BROWSE_PREVIEW_LIMIT) : loaded;
            runOnUiThread(() -> {
                lastLoadedItems = preview;
                renderMonths(preview);
            });
        });
    }

    private void renderMonths(List<MediaItem> items) {
        monthSections.removeAllViews();
        if (items.isEmpty()) {
            textMonthsEmptyState.setText("Todavía no hay fotos ni videos en el dispositivo.");
            textMonthsEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        textMonthsEmptyState.setVisibility(View.GONE);

        Map<String, List<MediaItem>> byMonth = new LinkedHashMap<>();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        for (MediaItem item : items) {
            String key = keyFormat.format(item.getDateAdded() * 1000L);
            byMonth.computeIfAbsent(key, unused -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<MediaItem>> entry : byMonth.entrySet()) {
            monthSections.addView(monthHeader(labelFor(entry.getValue().get(0), labelFormat)));
            monthSections.addView(gridView ? buildGrid(entry.getValue()) : buildList(entry.getValue()));
        }
    }

    private String labelFor(MediaItem item, SimpleDateFormat labelFormat) {
        String label = labelFormat.format(item.getDateAdded() * 1000L);
        return label.substring(0, 1).toUpperCase(Locale.getDefault()) + label.substring(1);
    }

    private View monthHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(13);
        header.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(20);
        params.bottomMargin = dp(8);
        header.setLayoutParams(params);
        return header;
    }

    private View buildGrid(List<MediaItem> monthItems) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = null;
        for (int i = 0; i < monthItems.size(); i++) {
            int column = i % 3;
            if (column == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    rowParams.topMargin = dp(8);
                }
                row.setLayoutParams(rowParams);
                container.addView(row);
            }
            row.addView(gridTile(monthItems.get(i), column));
        }
        return container;
    }

    private View gridTile(MediaItem item, int column) {
        int size = dp(104);
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
        loadThumbnailInto(thumb, item);

        if (item.isVideo()) {
            ImageView playBadge = new ImageView(this);
            int badgeSize = dp(24);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            badgeParams.gravity = Gravity.TOP | Gravity.END;
            badgeParams.setMargins(0, dp(6), dp(6), 0);
            playBadge.setLayoutParams(badgeParams);
            playBadge.setBackgroundResource(R.drawable.bg_checkbox_circle);
            playBadge.setSelected(true);
            playBadge.setPadding(dp(5), dp(5), dp(5), dp(5));
            playBadge.setImageResource(R.drawable.ic_play);
            tile.addView(playBadge);
        }

        tile.setOnClickListener(v -> openDeck(MediaQuery.SortKey.DATE, false));
        return tile;
    }

    private View buildList(List<MediaItem> monthItems) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        for (MediaItem item : monthItems) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> openDeck(MediaQuery.SortKey.DATE, false));

            FrameLayout thumbHolder = new FrameLayout(this);
            int size = dp(56);
            thumbHolder.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            thumbHolder.setBackgroundResource(R.drawable.bg_tile_thumb);
            thumbHolder.setClipToOutline(true);
            thumbHolder.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            ImageView thumb = new ImageView(this);
            thumb.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbHolder.addView(thumb);
            loadThumbnailInto(thumb, item);
            row.addView(thumbHolder);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textsParams.leftMargin = dp(12);
            texts.setLayoutParams(textsParams);

            TextView name = new TextView(this);
            name.setText(item.getDisplayName());
            name.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            texts.addView(name);

            TextView meta = new TextView(this);
            meta.setText(MediaRepository.humanReadableSize(item.getSizeBytes())
                    + (item.isVideo() ? " · Video" : " · Foto"));
            meta.setTextSize(12.5f);
            meta.setTextColor(ContextCompat.getColor(this, R.color.colorTextTertiary));
            texts.addView(meta);

            row.addView(texts);
            container.addView(row);
        }
        return container;
    }

    private void loadThumbnailInto(ImageView target, MediaItem item) {
        Bitmap cached = thumbnailCache.get(item.getUriString());
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        target.setImageDrawable(null);
        String targetUri = item.getUriString();
        target.setTag(targetUri);
        executor.execute(() -> {
            Bitmap bitmap = mediaRepository.loadThumbnail(getContentResolver(), item.getUriString(),
                    item.isVideo(), new Size(300, 300));
            if (bitmap != null) {
                thumbnailCache.put(targetUri, bitmap);
            }
            runOnUiThread(() -> {
                if (bitmap != null && targetUri.equals(target.getTag())) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    // ---- Ajustes globales ----

    private void applyGlobalExclusions(MediaQuery.Builder builder) {
        SettingsRepository settings = new SettingsRepository(this);
        builder.excludeVideos(settings.isExcludeVideos());
        builder.excludeScreenshots(settings.isExcludeScreenshots());
    }

    // ---- Permisos ----

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

    private void showPermissionDenied() {
        textMonthsEmptyState.setText("SwipeCleaner necesita acceso a tus fotos y videos.\nActivalo desde los ajustes del sistema.");
        textMonthsEmptyState.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
