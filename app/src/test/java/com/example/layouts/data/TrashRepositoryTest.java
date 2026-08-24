package com.example.layouts.data;

import com.example.layouts.data.db.TrashDao;
import com.example.layouts.data.db.TrashEntry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TrashRepositoryTest {

    @Test
    public void restoreRemovesTheSelectedEntryFromTrash() {
        FakeTrashDao dao = new FakeTrashDao();
        TrashEntry entry = new TrashEntry();
        entry.mediaUri = "content://media/external/images/media/42";
        dao.insert(entry);

        TrashRepository repository = new TrashRepository(dao);

        repository.restore(entry.mediaUri);

        assertEquals(0, repository.getAll().size());
    }

    @Test
    public void removePermanentlyDeletedEntriesClearsOnlyConfirmedUris() {
        FakeTrashDao dao = new FakeTrashDao();
        TrashEntry confirmed = entry("content://media/1");
        TrashEntry stillPending = entry("content://media/2");
        dao.insert(confirmed);
        dao.insert(stillPending);

        TrashRepository repository = new TrashRepository(dao);

        repository.removePermanentlyDeleted(List.of(confirmed));

        assertEquals(1, repository.getAll().size());
        assertEquals(stillPending.mediaUri, repository.getAll().get(0).mediaUri);
    }

    private static TrashEntry entry(String mediaUri) {
        TrashEntry entry = new TrashEntry();
        entry.mediaUri = mediaUri;
        return entry;
    }

    private static class FakeTrashDao implements TrashDao {
        private final List<TrashEntry> entries = new ArrayList<>();

        @Override
        public void insert(TrashEntry entry) {
            entries.add(entry);
        }

        @Override
        public void deleteByUri(String mediaUri) {
            entries.removeIf(entry -> mediaUri.equals(entry.mediaUri));
        }

        @Override
        public List<TrashEntry> getAll() {
            return new ArrayList<>(entries);
        }

        @Override
        public int count() {
            return entries.size();
        }
    }
}
