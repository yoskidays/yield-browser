package com.yieldbrowser.app;

/** Pure mode selection for applying browser viewport settings. */
final class ViewportModeApplyPolicy {
    enum Mode {
        NONE,
        MOBILE,
        DESKTOP
    }

    private ViewportModeApplyPolicy() {
    }

    static Mode mode(boolean hasWebView, boolean desktopMode) {
        if (!hasWebView) return Mode.NONE;
        return desktopMode ? Mode.DESKTOP : Mode.MOBILE;
    }
}
