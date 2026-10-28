package com.example.layouts.auth;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {

    private static final String PREFERENCES_NAME = "swipe_cleaner_session";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final Storage storage;

    public SessionManager(Context context) {
        this(new SharedPreferencesStorage(
                context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ));
    }

    SessionManager(Storage storage) {
        this.storage = storage;
    }

    public boolean isLoggedIn() {
        return storage.readLoggedIn();
    }

    public void markLoggedIn() {
        storage.writeLoggedIn(true);
    }

    public void logout() {
        storage.writeLoggedIn(false);
    }

    interface Storage {
        boolean readLoggedIn();

        void writeLoggedIn(boolean loggedIn);
    }

    private static final class SharedPreferencesStorage implements Storage {
        private final SharedPreferences preferences;

        SharedPreferencesStorage(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public boolean readLoggedIn() {
            return preferences.getBoolean(KEY_LOGGED_IN, false);
        }

        @Override
        public void writeLoggedIn(boolean loggedIn) {
            preferences.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
        }
    }
}
