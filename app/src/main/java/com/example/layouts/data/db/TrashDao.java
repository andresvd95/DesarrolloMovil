package com.example.layouts.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TrashDao {

    @Insert
    void insert(TrashEntry entry);

    @Query("DELETE FROM trash_entry WHERE mediaUri = :mediaUri")
    void deleteByUri(String mediaUri);

    @Query("SELECT * FROM trash_entry ORDER BY movedToTrashAt DESC")
    List<TrashEntry> getAll();

    @Query("SELECT COUNT(*) FROM trash_entry")
    int count();
}
