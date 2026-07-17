package com.yieldbrowser.app;

/** Pure context decisions for normal user main-frame navigations. */
final class NormalMainFrameContextPolicy {
    private NormalMainFrameContextPolicy() {
    }

    static boolean isEarlyAllowed(boolean sameSite,
                                  boolean fromSearchResults,
                                  boolean contextAllowedSuspicious) {
        return sameSite || fromSearchResults || contextAllowedSuspicious;
    }

    static boolean allowUnknownCrossSite(boolean hasGesture,
                                         boolean knownPopupHost,
                                         boolean adUrl,
                                         boolean likelyAdClickUrl) {
        return hasGesture && !knownPopupHost && !adUrl && !likelyAdClickUrl;
    }
}
