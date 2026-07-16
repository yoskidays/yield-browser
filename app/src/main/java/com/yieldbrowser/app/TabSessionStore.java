package com.yieldbrowser.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Small persistence boundary for the serialized browser tab session. */
final class TabSessionStore {
    static final class StoredSession {
        final String raw;
        final int requestedIndex;

        StoredSession(String raw, int requestedIndex) {
            this.raw = raw == null ? "" : raw;
            this.requestedIndex = Math.max(0, requestedIndex);
        }
    }

    private TabSessionStore() {
    }

    static StoredSession read(Context context) {
        if (context == null) return new StoredSession("", 0);
        try {
            SharedPreferences preferences = context.getSharedPreferences(
                    BrowserConstants.PREFS, Context.MODE_PRIVATE);
            return new StoredSession(
                    preferences.getString(BrowserConstants.KEY_TABS_SESSION_V1, ""),
                    preferences.getInt(BrowserConstants.KEY_TABS_CURRENT_INDEX_V1, 0));
        } catch (Exception ignored) {
            return new StoredSession("", 0);
        }
    }

    static void write(Context context, String raw, int selectedIndex) {
        if (context == null) return;
        try {
            context.getSharedPreferences(BrowserConstants.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(BrowserConstants.KEY_TABS_SESSION_V1, raw == null ? "" : raw)
                    .putInt(BrowserConstants.KEY_TABS_CURRENT_INDEX_V1,
                            Math.max(0, selectedIndex))
                    .apply();
        } catch (Exception ignored) {
        }
    }
}
