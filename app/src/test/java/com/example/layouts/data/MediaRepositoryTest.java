package com.example.layouts.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MediaRepositoryTest {

    @Test
    public void humanReadableSizeFormatsBytes() {
        assertEquals("512 B", MediaRepository.humanReadableSize(512));
    }

    @Test
    public void humanReadableSizeFormatsKilobytes() {
        assertEquals("2.0 KB", MediaRepository.humanReadableSize(2048));
    }

    @Test
    public void humanReadableSizeFormatsMegabytes() {
        assertEquals("3.0 MB", MediaRepository.humanReadableSize(3L * 1024 * 1024));
    }

    @Test
    public void humanReadableSizeFormatsGigabytes() {
        assertEquals("2.0 GB", MediaRepository.humanReadableSize(2L * 1024 * 1024 * 1024));
    }

    @Test
    public void typedMediaUriUsesImagesCollectionForPhotos() {
        assertEquals("content://media/external/images/media/526272",
                MediaRepository.typedMediaUri(526272, false).toString());
    }

    @Test
    public void typedMediaUriUsesVideoCollectionForVideos() {
        assertEquals("content://media/external/video/media/526496",
                MediaRepository.typedMediaUri(526496, true).toString());
    }

    @Test
    public void typedMediaUriNeverUsesGenericFilesCollection() {
        // MediaStore.createDeleteRequest()/createWriteRequest() reject
        // content://media/external/file/<id> with "All requested items
        // must be Media items" — regression test for that bug.
        assertEquals(-1, MediaRepository.typedMediaUri(1, false).toString().indexOf("/file/"));
        assertEquals(-1, MediaRepository.typedMediaUri(1, true).toString().indexOf("/file/"));
    }
}
