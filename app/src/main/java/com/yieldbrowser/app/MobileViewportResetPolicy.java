package com.yieldbrowser.app;

/** Pure timing and cancellation rules for post-mode-change mobile viewport resets. */
final class MobileViewportResetPolicy {
    private static final long[] DELAYS = {120L, 500L, 1200L};

    private MobileViewportResetPolicy() {
    }

    static boolean shouldSchedule(boolean desktopMode) {
        return !desktopMode;
    }

    static long[] delays() {
        return DELAYS.clone();
    }

    static boolean shouldApply(int capturedToken,
                               int currentToken,
                               boolean desktopMode) {
        return capturedToken == currentToken && !desktopMode;
    }
}
