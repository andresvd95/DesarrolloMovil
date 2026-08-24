package com.example.layouts.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.layouts.data.db.TrashDao;
import com.example.layouts.data.db.TrashEntry;
import com.example.layouts.gallery.MediaItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lee el contenido real de fotos y videos del dispositivo via MediaStore.
 * Reemplaza los datos hardcodeados de PhotoGalleryManager.createDefault().
 */
public class MediaRepository {

    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
    };

    private static final String SELECTION =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";

    private static final String[] SELECTION_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
    };

    public List<MediaItem> loadFromDevice(ContentResolver resolver) {
        List<MediaItem> items = new ArrayList<>();
        Uri collection = MediaStore.Files.getContentUri("external");

        try (Cursor cursor = resolver.query(collection, PROJECTION, SELECTION, SELECTION_ARGS,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) {
                return items;
            }

            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
            int dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
            int albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);
            int typeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                Uri itemUri = ContentUris.withAppendedId(collection, id);
                boolean isVideo = cursor.getInt(typeIndex) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

                items.add(new MediaItem(
                        itemUri.toString(),
                        cursor.getString(nameIndex),
                        cursor.getLong(sizeIndex),
                        cursor.getString(mimeIndex),
                        cursor.getLong(dateIndex),
                        cursor.getString(albumIndex),
                        isVideo
                ));
            }
        }

        return items;
    }

    /**
     * Cubre RF05 (papelera): el swipe izquierdo solo inserta un registro en
     * Room. El archivo real no se toca todavia, por lo que es reversible.
     */
    public void moveToTrash(TrashDao trashDao, MediaItem item) {
        TrashEntry entry = new TrashEntry();
        entry.mediaUri = item.getUriString();
        entry.movedToTrashAt = System.currentTimeMillis();
        entry.originalAlbum = item.getAlbumName();
        trashDao.insert(entry);
    }

    public void restoreFromTrash(TrashDao trashDao, MediaItem item) {
        trashDao.deleteByUri(item.getUriString());
    }

    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.1f GB", gb);
    }
}
