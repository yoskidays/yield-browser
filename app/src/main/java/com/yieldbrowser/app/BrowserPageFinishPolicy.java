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
}
