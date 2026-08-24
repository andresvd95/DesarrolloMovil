package com.example.layouts.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Estado de la papelera temporal (RF05): un MediaItem marcado para borrar
 * pero recuperable hasta que se vacia la papelera.
 */
@Entity(tableName = "trash_entry")
public class TrashEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String mediaUri;
    public long movedToTrashAt;
    public String originalAlbum;
}
