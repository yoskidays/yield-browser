package com.yieldbrowser.app;

/** Pure safety decisions for selecting a URL during Desktop/Mobile mode reloads. */
final class BrowserModeReloadPolicy {
    private BrowserModeReloadPolicy() {
    }

    static boolean isBaseSafe(boolean httpOrHttps,
                              boolean externalScheme,
                              boolean imageResource,
                              boolean mediaResource,
                              boolean directWebViewUrl) {
        return httpOrHttps
                && !externalScheme
                && !imageResource
                && !mediaResource
                && !directWebViewUrl;
    }

    static boolean isFallbackClassificationSafe(boolean likelyAdClick,
                                                boolean knownPopupHost,
                                                boolean adUrl) {
        return !likelyAdClick && !knownPopupHost && !adUrl;
    }

    static boolean isSafe(boolean httpOrHttps,
                          boolean externalScheme,
                          boolean imageResource,
                          boolean mediaResource,
                          boolean directWebViewUrl,
                          boolean explicitCurrentPage,
                          boolean likelyAdClick,
                          boolean knownPopupHost,
                          boolean adUrl) {
        if (!isBaseSafe(httpOrHttps, externalScheme, imageResource,
                mediaResource, directWebViewUrl)) return false;
        if (explicitCurrentPage) return true;
        return isFallbackClassificationSafe(likelyAdClick, knownPopupHost, adUrl);
    }
}
