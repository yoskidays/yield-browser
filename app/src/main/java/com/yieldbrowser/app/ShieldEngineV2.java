package com.yieldbrowser.app;

/**
 * Stable facade for Yield Shield Engine V2.
 *
 * The implementation is split into small policy and script modules so download-page handling can
 * evolve without changing the public call sites used by MainActivity.
 */
final class ShieldEngineV2 {
    private ShieldEngineV2() {
    }

    static boolean shouldBlockMainFrameNavigation(String targetUrl,
                                                   String sourceUrl,
                                                   boolean hasGesture,
                                                   boolean compatibilityOrReaderContext,
                                                   boolean explicitlyTrusted,
                                                   boolean legacySuspicious) {
        return ShieldNavigationPolicy.shouldBlockMainFrameNavigation(
                targetUrl, sourceUrl, hasGesture, compatibilityOrReaderContext,
                explicitlyTrusted, legacySuspicious);
    }

    static boolean shouldBlockSubresource(String resourceUrl,
                                          String pageUrl,
                                          boolean legacyHardAd) {
        return ShieldNavigationPolicy.shouldBlockSubresource(
                resourceUrl, pageUrl, legacyHardAd);
    }

    static boolean isHighConfidenceSameOriginRelay(String targetUrl,
                                                    String sourceUrl,
                                                    boolean compatibilityOrReaderContext) {
        return ShieldNavigationPolicy.isHighConfidenceSameOriginRelay(
                targetUrl, sourceUrl, compatibilityOrReaderContext);
    }

    static boolean isSafeSameSiteReaderNavigation(String targetUrl, String sourceUrl) {
        return ShieldNavigationPolicy.isSafeSameSiteReaderNavigation(targetUrl, sourceUrl);
    }

    static boolean isDownloadPage(String url) {
        return ShieldNavigationPolicy.isDownloadPage(url);
    }

    static boolean isSafeDownloadNavigation(String targetUrl,
                                            String sourceUrl,
                                            boolean hasGesture) {
        return ShieldNavigationPolicy.isSafeDownloadNavigation(
                targetUrl, sourceUrl, hasGesture);
    }

    static boolean isKnownAdOrTrackerUrl(String url) {
        return ShieldNavigationPolicy.isKnownAdOrTrackerUrl(url);
    }

    static boolean isRelayPath(String url) {
        return ShieldUrlRules.isRelayPath(url);
    }

    static boolean isDirectContentAsset(String url) {
        return ShieldUrlRules.isDirectContentAsset(url);
    }

    static boolean isSearchResultsPage(String url) {
        return ShieldNavigationPolicy.isSearchResultsPage(url);
    }

    static boolean isReaderOrContentPage(String url) {
        return ShieldNavigationPolicy.isReaderOrContentPage(url);
    }

    static boolean isPopupIsolationContentPage(String url) {
        return ShieldNavigationPolicy.isPopupIsolationContentPage(url);
    }

    static boolean shouldUseLegacyClickGuard(String pageUrl, boolean clickHijackEnabled) {
        return ShieldNavigationPolicy.shouldUseLegacyClickGuard(
                pageUrl, clickHijackEnabled);
    }
}

/** Builds the universal document-start and runtime scripts for Shield Engine V2. */
final class ShieldPageScript {
    private ShieldPageScript() {
    }

    static String documentStart(boolean enabled,
                                boolean popupBlocker,
                                boolean redirectBlocker,
                                boolean scriptIframeBlocker,
                                boolean clickHijackBlocker) {
        return ShieldScriptBuilder.documentStart(
                enabled, popupBlocker, redirectBlocker,
                scriptIframeBlocker, clickHijackBlocker);
    }

    static String runtimeConfig(boolean enabled,
                                boolean popupBlocker,
                                boolean redirectBlocker,
                                boolean scriptIframeBlocker,
                                boolean clickHijackBlocker) {
        return ShieldScriptBuilder.runtimeConfig(
                enabled, popupBlocker, redirectBlocker,
                scriptIframeBlocker, clickHijackBlocker);
    }
}
