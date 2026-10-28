package com.example.layouts.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SessionManagerTest {

    @Test
    public void newSessionStartsLoggedOut() {
        FakeSessionStorage storage = new FakeSessionStorage();

        SessionManager sessionManager = new SessionManager(storage);

        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void successfulAuthenticationPersistsSession() {
        FakeSessionStorage storage = new FakeSessionStorage();
        SessionManager sessionManager = new SessionManager(storage);

        sessionManager.markLoggedIn();

        assertTrue(new SessionManager(storage).isLoggedIn());
    }

    @Test
    public void logoutClearsPersistedSession() {
        FakeSessionStorage storage = new FakeSessionStorage();
        SessionManager sessionManager = new SessionManager(storage);
        sessionManager.markLoggedIn();

        sessionManager.logout();

        assertFalse(new SessionManager(storage).isLoggedIn());
    }

    private static final class FakeSessionStorage implements SessionManager.Storage {
        private boolean loggedIn;

        @Override
        public boolean readLoggedIn() {
            return loggedIn;
        }

        @Override
        public void writeLoggedIn(boolean loggedIn) {
            this.loggedIn = loggedIn;
        }
    }
}
