package com.example.layouts;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PhotoGalleryManagerTest {

    @Test
    public void deleteSelectedRemovesOnlyCheckedPhotos() {
        PhotoGalleryManager manager = PhotoGalleryManager.createDefault();

        manager.toggleSelection(1);
        manager.toggleSelection(4);

        int deletedCount = manager.deleteSelected();
        List<PhotoItem> remainingPhotos = manager.getPhotos();

        assertEquals(2, deletedCount);
        assertEquals(6, remainingPhotos.size());
        assertEquals("IMG_20240315_142233.jpg", remainingPhotos.get(0).getName());
        assertEquals("IMG_20240317_180532.jpg", remainingPhotos.get(1).getName());
        assertEquals("IMG_20240320_164520.jpg", remainingPhotos.get(3).getName());
        assertTrue(remainingPhotos.stream().noneMatch(PhotoItem::isSelected));
    }
}
