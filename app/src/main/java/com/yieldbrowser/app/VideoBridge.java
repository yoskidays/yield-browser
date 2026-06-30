package com.yieldbrowser.app;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge for in-page video control, exposed to web pages as {@code YieldVideoBridge}.
 *
 * <p>This used to be a private inner class of {@code MainActivity}. It is now a top-level class that
 * forwards every {@code @JavascriptInterface} call to a {@link Callback}. The JavaScript-visible
 * method names and the registered interface name are unchanged, so page scripts behave exactly as
 * before; only the Java structure is decoupled. All UI-thread posting and control-bar logic remain
 * on the {@code MainActivity} side, inside the callback implementation.</p>
 */
final class VideoBridge {

    /**
     * Receives video events from the page. Methods are invoked on whatever thread the WebView
     * delivers the JavaScript call on; the implementation is responsible for any threading.
     */
    interface Callback {
        void onVideoPlaying();

        void onVideoTapped();

        void onVideoStopped();

        void tapAtRatio(double xRatio, double yRatio);
    }

    private final Callback callback;

    VideoBridge(Callback callback) {
        this.callback = callback;
    }

    @JavascriptInterface
    public void onVideoPlaying() {
        callback.onVideoPlaying();
    }

    @JavascriptInterface
    public void onVideoTapped() {
        callback.onVideoTapped();
    }

    @JavascriptInterface
    public void onVideoStopped() {
        callback.onVideoStopped();
    }

    @JavascriptInterface
    public void tapAtRatio(double xRatio, double yRatio) {
        callback.tapAtRatio(xRatio, yRatio);
    }
}
