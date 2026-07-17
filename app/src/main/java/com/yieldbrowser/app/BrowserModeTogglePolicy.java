package com.yieldbrowser.app;

/** Pure presentation and reload decisions for Desktop/Mobile mode toggles. */
final class BrowserModeTogglePolicy {
    static final class Plan {
        final boolean desktopMode;
        final boolean updateAddressBar;
        final boolean hardReload;
        final boolean applySettings;
        final boolean showHome;
        final String statusMessage;

        private Plan(boolean desktopMode,
                     boolean updateAddressBar,
                     boolean hardReload,
                     boolean applySettings,
                     boolean showHome,
                     String statusMessage) {
            this.desktopMode = desktopMode;
            this.updateAddressBar = updateAddressBar;
            this.hardReload = hardReload;
            this.applySettings = applySettings;
            this.showHome = showHome;
            this.statusMessage = statusMessage;
        }
    }

    private BrowserModeTogglePolicy() {
    }

    static boolean nextDesktopMode(boolean currentDesktopMode) {
        return !currentDesktopMode;
    }

    static Plan plan(boolean desktopMode,
                     boolean wasShowingWeb,
                     String normalizedTargetUrl) {
        boolean hasTargetUrl = normalizedTargetUrl != null
                && normalizedTargetUrl.length() > 0;
        boolean hardReload = wasShowingWeb && hasTargetUrl;
        return new Plan(
                desktopMode,
                hasTargetUrl,
                hardReload,
                !hardReload,
                !hardReload && !wasShowingWeb,
                desktopMode ? "Desktop mode aktif" : "Mode mobile aktif");
    }
}
