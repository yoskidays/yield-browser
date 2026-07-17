package com.yieldbrowser.app;

/** Pure execution choices for normal and strict-compatibility browser loads. */
final class BrowserLoadExecutionPolicy {
    enum Profile {
        NORMAL,
        STRICT_COMPATIBILITY
    }

    private BrowserLoadExecutionPolicy() {
    }

    static Profile profile(boolean strictCompatibility) {
        return strictCompatibility ? Profile.STRICT_COMPATIBILITY : Profile.NORMAL;
    }

    static long[] desktopViewportDelays(Profile profile, boolean desktopMode) {
        if (profile == Profile.STRICT_COMPATIBILITY && desktopMode) {
            return new long[]{350L, 1300L, 2800L};
        }
        return new long[0];
    }

    static boolean usesCustomHeaders(Profile profile) {
        return profile == Profile.NORMAL;
    }
}
