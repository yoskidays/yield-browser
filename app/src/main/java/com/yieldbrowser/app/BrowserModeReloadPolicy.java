package com.yieldbrowser.app;

/** Pure safety decisions for selecting a URL during Desktop/Mobile mode reloads. */
final class BrowserModeReloadPolicy {
    private BrowserModeReloadPolicy() {
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
        if (!httpOrHttps) return false;
        if (externalScheme || imageResource || mediaResource || directWebViewUrl) return false;
        if (explicitCurrentPage) return true;
        return !likelyAdClick && !knownPopupHost && !adUrl;
    }
}
