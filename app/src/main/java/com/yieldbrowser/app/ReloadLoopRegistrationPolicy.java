package com.yieldbrowser.app;

/** Pure state transition for repeated-navigation reload-loop detection. */
final class ReloadLoopRegistrationPolicy {
    static final long COUNT_WINDOW_MS = 12000L;
    static final int TRIGGER_COUNT = 4;
    static final long GUARD_DURATION_MS = 120000L;
    static final long TOAST_INTERVAL_MS = 6000L;

    static final class Plan {
        final String lastKey;
        final long windowStartMs;
        final int count;
        final boolean guardTriggered;
        final long guardUntilMs;
        final boolean showToast;

        private Plan(String lastKey,
                     long windowStartMs,
                     int count,
                     boolean guardTriggered,
                     long guardUntilMs,
                     boolean showToast) {
            this.lastKey = lastKey;
            this.windowStartMs = windowStartMs;
            this.count = count;
            this.guardTriggered = guardTriggered;
            this.guardUntilMs = guardUntilMs;
            this.showToast = showToast;
        }
    }

    private ReloadLoopRegistrationPolicy() {
    }

    static Plan plan(String key,
                     String previousKey,
                     long previousWindowStartMs,
                     int previousCount,
                     long lastToastMs,
                     long nowMs) {
        String safeKey = key == null ? "" : key;
        boolean sameWindow = safeKey.equals(previousKey)
                && (nowMs - previousWindowStartMs) <= COUNT_WINDOW_MS;

        String nextKey = sameWindow ? previousKey : safeKey;
        long nextWindowStart = sameWindow ? previousWindowStartMs : nowMs;
        int nextCount = sameWindow ? previousCount + 1 : 1;

        if (nextCount < TRIGGER_COUNT) {
            return new Plan(nextKey, nextWindowStart, nextCount, false, 0L, false);
        }
        return new Plan(
                nextKey,
                nextWindowStart,
                0,
                true,
                nowMs + GUARD_DURATION_MS,
                (nowMs - lastToastMs) > TOAST_INTERVAL_MS);
    }
}
