package com.example.layouts.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.example.layouts.data.db.TrashDao;
import com.example.layouts.data.db.TrashEntry;
import com.example.layouts.gallery.MediaItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee el contenido real de fotos y videos del dispositivo via MediaStore.
 * Reemplaza los datos hardcodeados de la primera version del prototipo.
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
        return loadFromDevice(resolver, MediaQuery.builder().build());
    }

    /**
     * Carga los medios del dispositivo y aplica los filtros/orden solicitados
     * antes de entregarlos a la cola de revision.
     */
    public List<MediaItem> loadFromDevice(ContentResolver resolver, MediaQuery query) {
        List<MediaItem> items = new ArrayList<>();
        Uri collection = MediaStore.Files.getContentUri("external");

        try (Cursor cursor = resolver.query(collection, PROJECTION, SELECTION, SELECTION_ARGS,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
            readRows(cursor, items);
        }

        return query == null ? items : query.apply(items);
    }

    /**
     * Carga solo los items cuyo _id coincide con los uriString dados,
     * preservando el orden de la lista de entrada. Se usa para revisar un
     * conjunto ya calculado (p.ej. duplicados de DuplicateFinder), que no es
     * expresable como un MediaQuery sobre MediaStore.
     */
    public List<MediaItem> loadByUris(ContentResolver resolver, List<String> uriStrings) {
        if (uriStrings.isEmpty()) {
            return new ArrayList<>();
        }
        StringBuilder selection = new StringBuilder(MediaStore.Files.FileColumns._ID).append(" IN (");
        String[] selectionArgs = new String[uriStrings.size()];
        for (int i = 0; i < uriStrings.size(); i++) {
            selection.append(i == 0 ? "?" : ",?");
            selectionArgs[i] = String.valueOf(ContentUris.parseId(Uri.parse(uriStrings.get(i))));
        }
        selection.append(")");

        Map<Long, MediaItem> byId = new HashMap<>();
        Uri collection = MediaStore.Files.getContentUri("external");
        try (Cursor cursor = resolver.query(collection, PROJECTION, selection.toString(), selectionArgs, null)) {
            List<MediaItem> loaded = new ArrayList<>();
            readRows(cursor, loaded);
            for (MediaItem item : loaded) {
                byId.put(ContentUris.parseId(Uri.parse(item.getUriString())), item);
            }
        }

        List<MediaItem> ordered = new ArrayList<>();
        for (String uriString : uriStrings) {
            MediaItem item = byId.get(ContentUris.parseId(Uri.parse(uriString)));
            if (item != null) {
                ordered.add(item);
            }
        }
        return ordered;
    }

    private void readRows(Cursor cursor, List<MediaItem> destination) {
        if (cursor == null) {
            return;
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
            boolean isVideo = cursor.getInt(typeIndex) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
            Uri itemUri = typedMediaUri(id, isVideo);

            destination.add(new MediaItem(
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

    /**
     * URI de la coleccion tipada (Images/Video) para un _id de MediaStore.
     * MediaStore.Files (content://media/external/file/id) sirve para
     * consultar y para leer miniaturas, pero MediaStore.createDeleteRequest()
     * / createWriteRequest() lo rechazan con "All requested items must be
     * Media items": exigen la coleccion especifica por tipo aunque el _id
     * sea el mismo. Por eso todo mediaUri que se guarda (incluida la
     * papelera) debe construirse con este metodo, no con MediaStore.Files.
     */
    static Uri typedMediaUri(long id, boolean isVideo) {
        Uri collection = isVideo
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return ContentUris.withAppendedId(collection, id);
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
        entry.sizeBytes = item.getSizeBytes();
        trashDao.insert(entry);
    }

    public void restoreFromTrash(TrashDao trashDao, MediaItem item) {
        trashDao.deleteByUri(item.getUriString());
    }

    /**
     * Genera la caratula (foto o video) que se muestra en la tarjeta de swipe.
     * Debe llamarse fuera del hilo principal: hace I/O sobre MediaStore.
     */
    public Bitmap loadThumbnail(ContentResolver resolver, MediaItem item) {
        return loadThumbnail(resolver, item.getUriString(), item.isVideo(), new Size(1024, 1024));
    }

    /**
     * Genera la caratula a partir de un uri y su tipo (foto/video). Se usa
     * tanto para la tarjeta de swipe como para las miniaturas de la papelera.
     * Debe llamarse fuera del hilo principal: hace I/O sobre MediaStore.
     */
    public Bitmap loadThumbnail(ContentResolver resolver, String uriString, boolean isVideo, Size targetSize) {
        Uri uri = Uri.parse(uriString);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return resolver.loadThumbnail(uri, targetSize, null);
            }
            long id = ContentUris.parseId(uri);
            int kind = MediaStore.Images.Thumbnails.MINI_KIND;
            return isVideo
                    ? MediaStore.Video.Thumbnails.getThumbnail(resolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null)
                    : MediaStore.Images.Thumbnails.getThumbnail(resolver, id, kind, null);
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            Log.w("MediaRepository", "No se pudo generar la caratula de " + uri, exception);
            return null;
        }
    }

    /** Determina si un uri de MediaStore corresponde a un video, via su MIME type. */
    public static boolean isVideoUri(ContentResolver resolver, String uriString) {
        String mimeType = resolver.getType(Uri.parse(uriString));
        return mimeType != null && mimeType.startsWith("video/");
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
