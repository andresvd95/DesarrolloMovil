package com.example.layouts.data;

import com.example.layouts.gallery.MediaItem;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DuplicateFinderTest {

    private final DuplicateFinder finder = new DuplicateFinder();
    private final Map<String, String> contentByUri = new HashMap<>();

    @Test
    public void ignoresGroupsWithUniqueSize() {
        MediaItem a = item("a.jpg", 1000, 10, "AAAA");
        MediaItem b = item("b.jpg", 2000, 20, "BBBB");

        List<MediaItem> duplicates = finder.findDuplicates(List.of(a, b), fakeReader());

        assertTrue(duplicates.isEmpty());
    }

    @Test
    public void sameSizeButDifferentContentIsNotADuplicate() {
        MediaItem a = item("a.jpg", 1000, 10, "AAAA");
        MediaItem b = item("b.jpg", 1000, 20, "BBBB");

        List<MediaItem> duplicates = finder.findDuplicates(List.of(a, b), fakeReader());

        assertTrue(duplicates.isEmpty());
    }

    @Test
    public void identicalContentKeepsOldestAsOriginal() {
        MediaItem original = item("original.jpg", 1000, 10, "SAME_BYTES");
        MediaItem copy1 = item("copy1.jpg", 1000, 30, "SAME_BYTES");
        MediaItem copy2 = item("copy2.jpg", 1000, 20, "SAME_BYTES");

        List<MediaItem> duplicates = finder.findDuplicates(
                List.of(original, copy1, copy2), fakeReader());

        assertEquals(2, duplicates.size());
        assertTrue(duplicates.contains(copy1));
        assertTrue(duplicates.contains(copy2));
        assertTrue(duplicates.stream().noneMatch(candidate -> candidate == original));
    }

    @Test
    public void skipsItemsThatFailToRead() {
        MediaItem readable = item("a.jpg", 1000, 10, "SAME_BYTES");
        MediaItem broken = item("b.jpg", 1000, 20, "SAME_BYTES");

        DuplicateFinder.ContentReader reader = uri -> {
            if (uri.equals(broken.getUriString())) {
                throw new IOException("boom");
            }
            return new ByteArrayInputStream(contentByUri.get(uri).getBytes(StandardCharsets.UTF_8));
        };

        List<MediaItem> duplicates = finder.findDuplicates(List.of(readable, broken), reader);

        assertTrue(duplicates.isEmpty());
    }

    /** Registra el contenido simulado de cada item y lo guarda en contentByUri. */
    private MediaItem item(String name, long sizeBytes, long dateAdded, String content) {
        MediaItem mediaItem = new MediaItem(
                "content://" + name, name, sizeBytes, "image/jpeg", dateAdded, "Camera", false);
        contentByUri.put(mediaItem.getUriString(), content);
        return mediaItem;
    }

    private DuplicateFinder.ContentReader fakeReader() {
        return uri -> new ByteArrayInputStream(contentByUri.get(uri).getBytes(StandardCharsets.UTF_8));
    }
}
