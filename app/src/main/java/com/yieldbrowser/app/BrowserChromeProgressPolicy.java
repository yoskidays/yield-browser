package com.yieldbrowser.app;

/** Pure decision table for WebChromeClient progress updates. */
final class BrowserChromeProgressPolicy {
    static final class State {
        final boolean handled;
        final int progress;
        final boolean visible;

        State(boolean handled, int progress, boolean visible) {
            this.handled = handled;
            this.progress = progress;
            this.visible = visible;
        }
    }

    private BrowserChromeProgressPolicy() {
    }

    static State decide(boolean activeView,
                        boolean homeVisible,
                        boolean webViewVisible,
                        int newProgress) {
        if (!activeView) return new State(false, 0, false);
        if (homeVisible || !webViewVisible) {
            return new State(true, 0, false);
        }
        return new State(true, newProgress, newProgress < 100);
    }
}
