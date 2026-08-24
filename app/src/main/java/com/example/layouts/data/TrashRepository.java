package com.example.layouts.data;

import com.example.layouts.data.db.TrashDao;
import com.example.layouts.data.db.TrashEntry;

import java.util.List;

/**
 * Acceso a la papelera temporal de Room.
 * La eliminacion definitiva del archivo se mantiene fuera de este repositorio:
 * Android debe solicitarla mediante MediaStore.createDeleteRequest().
 */
public class TrashRepository {

    private final TrashDao trashDao;

    public TrashRepository(TrashDao trashDao) {
        this.trashDao = trashDao;
    }

    public List<TrashEntry> getAll() {
        return trashDao.getAll();
    }

    public void restore(String mediaUri) {
        trashDao.deleteByUri(mediaUri);
    }

    /**
     * Elimina de Room solo las entradas cuyo borrado en MediaStore ya fue
     * confirmado por el sistema.
     */
    public void removePermanentlyDeleted(List<TrashEntry> entries) {
        for (TrashEntry entry : entries) {
            trashDao.deleteByUri(entry.mediaUri);
        }
    }
}
