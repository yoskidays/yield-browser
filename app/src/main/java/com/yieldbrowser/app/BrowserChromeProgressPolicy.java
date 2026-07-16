package com.yieldbrowser.app;

/** Pure visibility decisions for WebChromeClient progress updates. */
final class BrowserChromeProgressPolicy {
    private BrowserChromeProgressPolicy() {
    }

    static boolean shouldResetProgress(boolean homeVisible,
                                       boolean pageVisible) {
        return homeVisible || !pageVisible;
    }

    static boolean shouldShowProgress(int progress) {
        return progress < 100;
    }
}
