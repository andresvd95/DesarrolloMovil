package com.example.layouts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.layouts.data.MediaRepository;
import com.example.layouts.gallery.MediaItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fase 1 del MVP (RF01 + RF02): pide permisos de medios y muestra el
 * contenido real de la galeria del dispositivo, un archivo a la vez.
 * Los gestos de swipe (RF03/RF04) se agregan en la Fase 2.
 */
public class SwipeDeckActivity extends AppCompatActivity {

    private final MediaRepository mediaRepository = new MediaRepository();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageView imageMedia;
    private TextView textVideoBadge;
    private TextView textEmptyState;
    private TextView textMediaName;
    private TextView textMediaMeta;
    private TextView textMediaCounter;
    private Button buttonPrevious;
    private Button buttonNext;

    private List<MediaItem> mediaItems = Collections.emptyList();
    private int currentIndex = 0;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (allGranted(result)) {
                    loadMedia();
                } else {
                    showPermissionDenied();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe_deck);

        imageMedia = findViewById(R.id.imageMedia);
        textVideoBadge = findViewById(R.id.textVideoBadge);
        textEmptyState = findViewById(R.id.textEmptyState);
        textMediaName = findViewById(R.id.textMediaName);
        textMediaMeta = findViewById(R.id.textMediaMeta);
        textMediaCounter = findViewById(R.id.textMediaCounter);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonNext = findViewById(R.id.buttonNext);

        buttonPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                renderCurrent();
            }
        });
        buttonNext.setOnClickListener(v -> {
            if (currentIndex < mediaItems.size() - 1) {
                currentIndex++;
                renderCurrent();
            }
        });

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

    private void loadMedia() {
        textEmptyState.setText("Cargando galería…");
        textEmptyState.setVisibility(TextView.VISIBLE);
        executor.execute(() -> {
            List<MediaItem> loaded = mediaRepository.loadFromDevice(getContentResolver());
            runOnUiThread(() -> {
                mediaItems = loaded;
                currentIndex = 0;
                renderCurrent();
            });
        });
    }

    private void showPermissionDenied() {
        imageMedia.setVisibility(ImageView.GONE);
        textVideoBadge.setVisibility(TextView.GONE);
        textEmptyState.setText("SwipeClean necesita acceso a tus fotos y videos para funcionar.\nActiva el permiso desde los ajustes de la app.");
        textEmptyState.setVisibility(TextView.VISIBLE);
        textMediaName.setText("");
        textMediaMeta.setText("");
        textMediaCounter.setText("");
        buttonPrevious.setEnabled(false);
        buttonNext.setEnabled(false);
    }

    private void renderCurrent() {
        if (mediaItems.isEmpty()) {
            imageMedia.setVisibility(ImageView.GONE);
            textVideoBadge.setVisibility(TextView.GONE);
            textEmptyState.setText("No se encontraron fotos ni videos en la galería.");
            textEmptyState.setVisibility(TextView.VISIBLE);
            textMediaName.setText("");
            textMediaMeta.setText("");
            textMediaCounter.setText("0 / 0");
            buttonPrevious.setEnabled(false);
            buttonNext.setEnabled(false);
            return;
        }

        textEmptyState.setVisibility(TextView.GONE);
        MediaItem item = mediaItems.get(currentIndex);

        if (item.isVideo()) {
            imageMedia.setVisibility(ImageView.GONE);
            textVideoBadge.setVisibility(TextView.VISIBLE);
        } else {
            textVideoBadge.setVisibility(TextView.GONE);
            imageMedia.setVisibility(ImageView.VISIBLE);
            imageMedia.setImageURI(Uri.parse(item.getUriString()));
        }

        textMediaName.setText(item.getDisplayName());
        String album = item.getAlbumName() != null ? item.getAlbumName() : "Sin álbum";
        String type = item.isVideo() ? "Video" : "Foto";
        textMediaMeta.setText(MediaRepository.humanReadableSize(item.getSizeBytes()) + " · " + album + " · " + type);
        textMediaCounter.setText((currentIndex + 1) + " / " + mediaItems.size());

        buttonPrevious.setEnabled(currentIndex > 0);
        buttonNext.setEnabled(currentIndex < mediaItems.size() - 1);
    }
}
