package com.example.layouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PhotoGalleryManager {

    private final List<PhotoItem> photos;

    public PhotoGalleryManager(List<PhotoItem> photos) {
        this.photos = new ArrayList<>(photos);
    }

    public static PhotoGalleryManager createDefault() {
        List<PhotoItem> initialPhotos = new ArrayList<>();
        initialPhotos.add(new PhotoItem("IMG_20240315_142233.jpg", "3.2 MB", 0xFF023e8a));
        initialPhotos.add(new PhotoItem("IMG_20240316_091045.jpg", "2.8 MB", 0xFF0077b6));
        initialPhotos.add(new PhotoItem("IMG_20240317_180532.jpg", "4.1 MB", 0xFF0096c7));
        initialPhotos.add(new PhotoItem("PHOTO_20240318_203018.jpg", "1.9 MB", 0xFF023e8a));
        initialPhotos.add(new PhotoItem("Screenshot_2024-03-19.jpg", "0.8 MB", 0xFF48cae4));
        initialPhotos.add(new PhotoItem("IMG_20240320_164520.jpg", "3.7 MB", 0xFF0077b6));
        initialPhotos.add(new PhotoItem("IMG_20240321_095811.jpg", "2.4 MB", 0xFF00b4d8));
        initialPhotos.add(new PhotoItem("PHOTO_20240322_110045.jpg", "5.1 MB", 0xFF023e8a));
        return new PhotoGalleryManager(initialPhotos);
    }

    public List<PhotoItem> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    public int getSelectedCount() {
        int count = 0;
        for (PhotoItem photo : photos) {
            if (photo.isSelected()) {
                count++;
            }
        }
        return count;
    }

    public void toggleSelection(int index) {
        PhotoItem photo = photos.get(index);
        photo.setSelected(!photo.isSelected());
    }

    public void setAllSelected(boolean selected) {
        for (PhotoItem photo : photos) {
            photo.setSelected(selected);
        }
    }

    public boolean hasUnselectedPhotos() {
        for (PhotoItem photo : photos) {
            if (!photo.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public int deleteSelected() {
        int removed = 0;
        Iterator<PhotoItem> iterator = photos.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isSelected()) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }
}
