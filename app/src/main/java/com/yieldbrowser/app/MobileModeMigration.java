package com.yieldbrowser.app;

import android.content.SharedPreferences;

/** Applies the existing one-time mobile-mode preference migration. */
final class MobileModeMigration {
    private MobileModeMigration() {
    }

    static boolean apply(SharedPreferences preferences, Runnable disableDesktopMode) {
        try {
            if (preferences == null) return false;
            boolean alreadyApplied = preferences.getBoolean(
                    MobileModeMigrationPolicy.MIGRATION_MARKER_KEY, false);
            if (!MobileModeMigrationPolicy.shouldMigrate(alreadyApplied)) return false;

            if (disableDesktopMode != null) disableDesktopMode.run();
            preferences.edit()
                    .putBoolean(MobileModeMigrationPolicy.DESKTOP_MODE_KEY, false)
                    .putBoolean(MobileModeMigrationPolicy.MIGRATION_MARKER_KEY, true)
                    .apply();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
