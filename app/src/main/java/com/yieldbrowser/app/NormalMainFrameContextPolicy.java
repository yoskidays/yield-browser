package com.yieldbrowser.app;

/** Pure final context decision for normal user main-frame navigations. */
final class NormalMainFrameContextPolicy {
    private NormalMainFrameContextPolicy() {
    }

    static boolean allowUnknownCrossSite(boolean hasGesture,
                                         boolean knownPopupHost,
                                         boolean adUrl,
                                         boolean likelyAdClickUrl) {
        return hasGesture && !knownPopupHost && !adUrl && !likelyAdClickUrl;
    }
}
