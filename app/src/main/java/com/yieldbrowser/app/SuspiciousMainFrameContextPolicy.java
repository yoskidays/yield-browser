package com.yieldbrowser.app;

/** Pure context decisions for suspicious main-frame navigations. */
final class SuspiciousMainFrameContextPolicy {
    static final class Decision {
        final boolean resolved;
        final boolean allow;

        private Decision(boolean resolved, boolean allow) {
            this.resolved = resolved;
            this.allow = allow;
        }
    }

    private SuspiciousMainFrameContextPolicy() {
    }

    static Decision beforeCompatibility(boolean suspicious,
                                        boolean sameSite,
                                        boolean fromSearchResults) {
        if (!suspicious) return new Decision(true, false);
        if (sameSite || fromSearchResults) return new Decision(true, true);
        return new Decision(false, false);
    }

    static boolean allowCrossSite(boolean compatibilitySource,
                                  boolean hasGesture,
                                  boolean currentHostPresent) {
        return !compatibilitySource && hasGesture && currentHostPresent;
    }
}
