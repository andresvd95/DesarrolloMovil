package com.example.layouts.data;

import com.example.layouts.gallery.MediaItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Filtros y ordenamiento de la cola de revision (RF06 y RF07).
 * Es una clase pura para poder probar las reglas sin depender de MediaStore.
 */
public final class MediaQuery {

    public enum MediaType { ALL, PHOTOS, VIDEOS }

    public enum SortKey { RANDOM, SIZE, DATE, TYPE }

    private final MediaType type;
    private final boolean onlyScreenshots;
    private final Long minSizeBytes;
    private final Long maxSizeBytes;
    private final Long fromDateAdded;
    private final Long toDateAdded;
    private final String album;
    private final SortKey sortBy;

    private MediaQuery(Builder builder) {
        type = builder.type;
        onlyScreenshots = builder.onlyScreenshots;
        minSizeBytes = builder.minSizeBytes;
        maxSizeBytes = builder.maxSizeBytes;
        fromDateAdded = builder.fromDateAdded;
        toDateAdded = builder.toDateAdded;
        album = builder.album;
        sortBy = builder.sortBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<MediaItem> apply(List<MediaItem> source) {
        List<MediaItem> result = new ArrayList<>();
        for (MediaItem item : source) {
            if (matches(item)) {
                result.add(item);
            }
        }

        if (sortBy == SortKey.RANDOM) {
            Collections.shuffle(result);
        } else if (sortBy == SortKey.SIZE) {
            result.sort(Comparator.comparingLong(MediaItem::getSizeBytes).reversed());
        } else if (sortBy == SortKey.DATE) {
            result.sort(Comparator.comparingLong(MediaItem::getDateAdded).reversed());
        } else if (sortBy == SortKey.TYPE) {
            result.sort(Comparator.comparing(MediaItem::isVideo));
        }
        return result;
    }

    private boolean matches(MediaItem item) {
        if (type == MediaType.PHOTOS && item.isVideo()) {
            return false;
        }
        if (type == MediaType.VIDEOS && !item.isVideo()) {
            return false;
        }
        if (onlyScreenshots && !isScreenshot(item)) {
            return false;
        }
        if (minSizeBytes != null && item.getSizeBytes() < minSizeBytes) {
            return false;
        }
        if (maxSizeBytes != null && item.getSizeBytes() > maxSizeBytes) {
            return false;
        }
        if (fromDateAdded != null && item.getDateAdded() < fromDateAdded) {
            return false;
        }
        if (toDateAdded != null && item.getDateAdded() > toDateAdded) {
            return false;
        }
        if (album != null && !album.equalsIgnoreCase(item.getAlbumName())) {
            return false;
        }
        return true;
    }

    private boolean isScreenshot(MediaItem item) {
        return containsScreenshotWord(item.getDisplayName())
                || containsScreenshotWord(item.getAlbumName());
    }

    private boolean containsScreenshotWord(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.US);
        return normalized.contains("screenshot") || normalized.contains("captura");
    }

    public static final class Builder {
        private MediaType type = MediaType.ALL;
        private boolean onlyScreenshots;
        private Long minSizeBytes;
        private Long maxSizeBytes;
        private Long fromDateAdded;
        private Long toDateAdded;
        private String album;
        private SortKey sortBy = SortKey.DATE;

        public Builder type(MediaType type) {
            this.type = type != null ? type : MediaType.ALL;
            return this;
        }

        public Builder onlyScreenshots(boolean onlyScreenshots) {
            this.onlyScreenshots = onlyScreenshots;
            return this;
        }

        public Builder minSizeBytes(Long minSizeBytes) {
            this.minSizeBytes = minSizeBytes;
            return this;
        }

        public Builder maxSizeBytes(Long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        public Builder fromDateAdded(Long fromDateAdded) {
            this.fromDateAdded = fromDateAdded;
            return this;
        }

        public Builder toDateAdded(Long toDateAdded) {
            this.toDateAdded = toDateAdded;
            return this;
        }

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder sortBy(SortKey sortBy) {
            this.sortBy = sortBy != null ? sortBy : SortKey.DATE;
            return this;
        }

        public MediaQuery build() {
            return new MediaQuery(this);
        }
    }
}
