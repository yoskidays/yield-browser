package com.yieldbrowser.app;

/** Pure one-time migration decision for the legacy forced-mobile-mode marker. */
final class MobileModeMigrationPolicy {
    static final String DESKTOP_MODE_KEY = "desktopMode";
    static final String MIGRATION_MARKER_KEY = "forceMobileModeV0939";

    private MobileModeMigrationPolicy() {
    }

    static boolean shouldMigrate(boolean migrationAlreadyApplied) {
        return !migrationAlreadyApplied;
    }
}
