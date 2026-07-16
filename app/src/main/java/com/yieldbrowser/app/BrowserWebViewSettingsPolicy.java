package com.yieldbrowser.app;

/** Pure decisions used while applying Android WebView settings. */
final class BrowserWebViewSettingsPolicy {
    enum CacheStrategy {
        NO_CACHE,
        CACHE_ELSE_NETWORK,
        DEFAULT
    }

    private BrowserWebViewSettingsPolicy() {
    }

    static CacheStrategy cacheStrategy(boolean privateProfile,
                                       boolean speedMode,
                                       boolean videoBufferBooster,
                                       boolean youtubePage) {
        if (privateProfile) return CacheStrategy.NO_CACHE;
        if (speedMode || (videoBufferBooster && !youtubePage)) {
            return CacheStrategy.CACHE_ELSE_NETWORK;
        }
        return CacheStrategy.DEFAULT;
    }

    static boolean mediaPlaybackRequiresUserGesture(boolean youtubePage,
                                                    boolean videoBackgroundPlay) {
        return youtubePage || !videoBackgroundPlay;
    }

    static boolean canOpenWindowsAutomatically(boolean adBlock,
                                               boolean popupBlocker) {
        return !(adBlock && popupBlocker);
    }

    static int normalizedTextZoom(int textZoom) {
        return textZoom <= 0 ? 100 : textZoom;
    }

    static boolean loadsImagesAutomatically(boolean dataSaver) {
        return !dataSaver;
    }

    static boolean saveFormData(boolean privateProfile) {
        return !privateProfile;
    }

    static boolean allowFileAccess(boolean privateProfile) {
        return !privateProfile;
    }

    static boolean useWideViewPort(boolean desktopMode) {
        return desktopMode;
    }

    static boolean loadWithOverviewMode(boolean desktopMode) {
        return desktopMode;
    }

    static int initialScale(boolean desktopMode) {
        return desktopMode ? 65 : 0;
    }

    static boolean horizontalScrollBarEnabled(boolean desktopMode) {
        return desktopMode;
    }
}
