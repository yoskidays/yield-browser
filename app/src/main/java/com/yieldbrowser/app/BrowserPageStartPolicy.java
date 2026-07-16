package com.yieldbrowser.app;

/** Pure decisions used while handling WebViewClient#onPageStarted. */
final class BrowserPageStartPolicy {
    enum EarlyAction {
        CONTINUE,
        RESTORE_TRANSIENT_BLANK,
        RESTORE_SHIELD_BLOCK
    }

    enum Profile {
        NORMAL,
        STRICT_COMPATIBILITY,
        GUARDED_COMPATIBILITY
    }

    private BrowserPageStartPolicy() {
    }

    static String chooseStartedUrl(String extractedUrl, String rawUrl) {
        return extractedUrl != null ? extractedUrl : rawUrl;
    }

    static String chooseSafeReference(String tabReferenceUrl, String lastSafeHttpUrl) {
        if (tabReferenceUrl != null && tabReferenceUrl.length() > 0) {
            return tabReferenceUrl;
        }
        return lastSafeHttpUrl == null ? "" : lastSafeHttpUrl;
    }

    static EarlyAction earlyAction(boolean transientBlank,
                                   boolean readerOrCompatibilityContext,
                                   boolean shieldShouldBlock) {
        if (transientBlank && readerOrCompatibilityContext) {
            return EarlyAction.RESTORE_TRANSIENT_BLANK;
        }
        if (shieldShouldBlock) {
            return EarlyAction.RESTORE_SHIELD_BLOCK;
        }
        return EarlyAction.CONTINUE;
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

    static long[] viewportDelays(Profile profile, boolean desktopMode) {
        if (profile == Profile.NORMAL) {
            return new long[]{250L};
        }
        if (desktopMode) {
            return new long[]{280L, 1200L, 2600L};
        }
        return new long[]{280L};
    }

    static boolean shouldHideProgress(Profile profile) {
        return profile != Profile.NORMAL;
    }

    static String chooseNavigationReference(String safeReferenceUrl,
                                            String tabReferenceUrl,
                                            String currentUrl) {
        if (safeReferenceUrl != null && safeReferenceUrl.length() > 0) {
            return safeReferenceUrl;
        }
        if (tabReferenceUrl != null && tabReferenceUrl.length() > 0) {
            return tabReferenceUrl;
        }
        return currentUrl == null ? "" : currentUrl;
    }

    static boolean isCompatibilityFlow(boolean referenceFlow,
                                       boolean currentFlow) {
        return referenceFlow || currentFlow;
    }

    static boolean shouldRestoreDirectImage(boolean compatibilityFlow,
                                            boolean trustedNavigation,
                                            boolean directImageNavigation) {
        return !compatibilityFlow && !trustedNavigation && directImageNavigation;
    }

    static boolean shouldRestoreExternalScheme(boolean externalScheme) {
        return externalScheme;
    }

    static boolean shouldRestoreRedirect(boolean compatibilityFlow,
                                         boolean adBlockEnabled,
                                         boolean redirectBlockerEnabled,
                                         boolean trustedNavigation,
                                         boolean searchResultNavigation,
                                         boolean suspiciousPopupNavigation,
                                         boolean likelyAdClick) {
        return !compatibilityFlow
                && adBlockEnabled
                && redirectBlockerEnabled
                && !trustedNavigation
                && !searchResultNavigation
                && (suspiciousPopupNavigation || likelyAdClick);
    }
}
