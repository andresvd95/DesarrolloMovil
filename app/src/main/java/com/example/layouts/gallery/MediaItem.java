package com.example.layouts.gallery;

/**
 * Representa un archivo real de la galeria del dispositivo (foto o video),
 * leido desde MediaStore. Sustituye el modelo de datos de ejemplo del MVP.
 */
public class MediaItem {

    private final String uriString;
    private final String displayName;
    private final long sizeBytes;
    private final String mimeType;
    private final long dateAdded;
    private final String albumName;
    private final boolean video;

    public MediaItem(String uriString, String displayName, long sizeBytes, String mimeType,
                      long dateAdded, String albumName, boolean video) {
        this.uriString = uriString;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
        this.dateAdded = dateAdded;
        this.albumName = albumName;
        this.video = video;
    }

    public String getUriString() {
        return uriString;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public String getAlbumName() {
        return albumName;
    }

    public boolean isVideo() {
        return video;
    }
}
