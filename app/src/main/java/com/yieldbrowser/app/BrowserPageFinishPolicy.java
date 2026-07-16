package com.yieldbrowser.app;

/** Pure fallback decisions used during WebViewClient#onPageFinished. */
final class BrowserPageFinishPolicy {
    enum Profile {
        NORMAL,
        STRICT_COMPATIBILITY,
        GUARDED_COMPATIBILITY
    }

    private BrowserPageFinishPolicy() {
    }

    static String chooseFinalUrl(String extractedUrl, String rawUrl) {
        return extractedUrl != null ? extractedUrl : rawUrl;
    }

    static boolean shouldKeepWebState(boolean savedListAvailable,
                                      int savedListSize) {
        return savedListAvailable && savedListSize > 0;
    }

    static TabInfo chooseOwner(TabInfo viewOwner, TabInfo currentOwner) {
        return viewOwner != null ? viewOwner : currentOwner;
    }

    static boolean shouldUpdateLastSafeUrl(boolean recordableHistoryUrl,
                                           boolean safeToCommit) {
        return recordableHistoryUrl && safeToCommit;
    }

    static boolean shouldUpdateAddressBar(boolean activePageVisible) {
        return activePageVisible;
    }

    static Profile profile(boolean strictCompatibility,
                           boolean reloadLoopGuarded,
                           boolean siteCompatibilityActive) {
        if (strictCompatibility) return Profile.STRICT_COMPATIBILITY;
        if (reloadLoopGuarded || siteCompatibilityActive) {
            return Profile.GUARDED_COMPATIBILITY;
        }
        return Profile.NORMAL;
    }

    static boolean isReloadGuarded(Profile profile) {
        return profile != Profile.NORMAL;
    }

    static long[] guardedShieldRetryDelays(boolean adBlockEnabled) {
        return adBlockEnabled ? new long[]{900L, 2600L} : new long[0];
    }

    static long[] guardedViewportDelays(boolean desktopMode) {
        return desktopMode
                ? new long[]{350L, 1200L, 2600L}
                : new long[]{350L};
    }

    static long[] normalViewportRetryDelays() {
        return new long[]{600L, 1800L};
    }

    static long[] normalDesktopViewportDelays(boolean desktopMode) {
        return desktopMode
                ? new long[]{350L, 1200L, 2600L}
                : new long[0];
    }

    static long[] normalAdBlockRetryDelays(boolean adBlockEnabled) {
        return adBlockEnabled ? new long[]{1800L, 5200L} : new long[0];
    }

    static long[] userFilterRetryDelays(boolean hasUserFilters) {
        return hasUserFilters ? new long[]{350L, 1400L} : new long[0];
    }

    static boolean shouldAddHistory(boolean recordableHistoryUrl,
                                    TabInfo owner) {
        return recordableHistoryUrl && owner != null && !owner.privateTab;
    }

    static long[] translateToolbarHideDelays(boolean shouldHideToolbar) {
        return shouldHideToolbar
                ? new long[]{250L, 800L, 1800L, 3500L, 6000L}
                : new long[0];
    }

    static long[] compatibleTranslateRetryDelays(boolean shouldTranslate) {
        return shouldTranslate ? new long[]{600L, 2200L} : new long[0];
    }

    static long[] keyboardHideRetryDelays(boolean pendingHide) {
        return pendingHide ? new long[]{250L, 900L} : new long[0];
    }
}
