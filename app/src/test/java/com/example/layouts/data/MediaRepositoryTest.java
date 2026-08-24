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
}
