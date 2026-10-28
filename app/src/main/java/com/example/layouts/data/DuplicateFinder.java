package com.example.layouts.data;

import com.example.layouts.gallery.MediaItem;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deteccion de duplicados exactos, sin IA: agrupa por tamano de archivo
 * (barato, MediaStore ya lo trae) y solo lee contenido para confirmar con
 * un hash MD5 dentro de cada grupo con 2+ candidatos. Es una clase pura
 * (igual que MediaQuery) para poder testearla sin ContentResolver real,
 * inyectando un ContentReader.
 */
public final class DuplicateFinder {

    /** Abstrae ContentResolver.openInputStream() para poder fakearlo en tests. */
    public interface ContentReader {
        InputStream open(String uriString) throws IOException;
    }

    /**
     * Devuelve los MediaItem duplicados a revisar: todos los items de cada
     * grupo con hash identico, EXCEPTO el de fecha mas antigua (se conserva
     * como "original"). Un fallo de lectura puntual excluye solo ese item,
     * no aborta el resto.
     */
    public List<MediaItem> findDuplicates(List<MediaItem> items, ContentReader reader) {
        List<MediaItem> result = new ArrayList<>();
        for (List<MediaItem> sizeGroup : groupBySize(items).values()) {
            if (sizeGroup.size() < 2) {
                continue;
            }
            for (List<MediaItem> hashGroup : groupByHash(sizeGroup, reader).values()) {
                if (hashGroup.size() < 2) {
                    continue;
                }
                hashGroup.sort(Comparator.comparingLong(MediaItem::getDateAdded));
                // el primero (mas antiguo) es el "original" y se preserva
                result.addAll(hashGroup.subList(1, hashGroup.size()));
            }
        }
        return result;
    }

    private Map<Long, List<MediaItem>> groupBySize(List<MediaItem> items) {
        Map<Long, List<MediaItem>> groups = new HashMap<>();
        for (MediaItem item : items) {
            groups.computeIfAbsent(item.getSizeBytes(), key -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    private Map<String, List<MediaItem>> groupByHash(List<MediaItem> candidates, ContentReader reader) {
        Map<String, List<MediaItem>> groups = new HashMap<>();
        for (MediaItem item : candidates) {
            String hash = hashOf(item, reader);
            if (hash != null) {
                groups.computeIfAbsent(hash, key -> new ArrayList<>()).add(item);
            }
        }
        return groups;
    }

    private String hashOf(MediaItem item, ContentReader reader) {
        try (InputStream input = reader.open(item.getUriString())) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (IOException | SecurityException | NoSuchAlgorithmException exception) {
            // fallo de lectura puntual: se excluye este item, no aborta el resto
            return null;
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
