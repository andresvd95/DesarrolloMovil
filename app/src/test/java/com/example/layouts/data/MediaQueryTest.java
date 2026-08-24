package com.example.layouts.data;

import com.example.layouts.gallery.MediaItem;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MediaQueryTest {

    @Test
    public void filtersVideosAboveMinimumSizeAndSortsLargestFirst() {
        MediaItem photo = item("photo.jpg", 9_000, false, 30, "Camera");
        MediaItem smallVideo = item("small.mp4", 1_000, true, 20, "Camera");
        MediaItem largeVideo = item("large.mp4", 8_000, true, 10, "Camera");

        MediaQuery query = MediaQuery.builder()
                .type(MediaQuery.MediaType.VIDEOS)
                .minSizeBytes(5_000)
                .sortBy(MediaQuery.SortKey.SIZE)
                .build();

        List<MediaItem> result = query.apply(List.of(photo, smallVideo, largeVideo));

        assertEquals(1, result.size());
        assertEquals("large.mp4", result.get(0).getDisplayName());
    }

    @Test
    public void filtersScreenshotsByAlbumAndDateRange() {
        MediaItem screenshot = item("screen.png", 1_000, false, 20, "Screenshots");
        MediaItem oldScreenshot = item("old.png", 1_000, false, 5, "Screenshots");
        MediaItem cameraPhoto = item("camera.jpg", 1_000, false, 20, "Camera");

        MediaQuery query = MediaQuery.builder()
                .onlyScreenshots(true)
                .album("screenshots")
                .fromDateAdded(10L)
                .toDateAdded(30L)
                .build();

        List<MediaItem> result = query.apply(List.of(screenshot, oldScreenshot, cameraPhoto));

        assertEquals(1, result.size());
        assertEquals("screen.png", result.get(0).getDisplayName());
    }

    private static MediaItem item(String name, long size, boolean video, long date, String album) {
        return new MediaItem("content://media/" + name, name, size,
                video ? "video/mp4" : "image/jpeg", date, album, video);
    }
}
