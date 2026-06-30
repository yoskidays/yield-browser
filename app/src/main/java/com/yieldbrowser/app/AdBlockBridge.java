package com.yieldbrowser.app;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge for ad-redirect capture and the element picker, exposed to web pages as
 * {@code YieldAdBlockBridge}.
 *
 * <p>This used to be a private inner class of {@code MainActivity}. It is now a top-level class that
 * forwards every {@code @JavascriptInterface} call to a {@link Callback}. The JavaScript-visible
 * method names and the registered interface name are unchanged, so page scripts behave exactly as
 * before; only the Java structure is decoupled. Tab capture and picker-dialog logic remain on the
 * {@code MainActivity} side, inside the callback implementation. Origin handling is unchanged.</p>
 */
final class AdBlockBridge {

    /**
     * Receives ad-redirect and element-picker events from the page. Methods are invoked on whatever
     * thread the WebView delivers the JavaScript call on; the implementation handles any threading.
     */
    interface Callback {
        void onAdRedirect(String url);

        void onElementPicked(String selector, String preview);

        void onElementPickedV2(String selector, String preview, int matchCount, String tagName);

        void onPickerExited();
    }

    private final Callback callback;

    AdBlockBridge(Callback callback) {
        this.callback = callback;
    }

    @JavascriptInterface
    public void onAdRedirect(String url) {
        callback.onAdRedirect(url);
    }

    @JavascriptInterface
    public void onElementPicked(String selector, String preview) {
        callback.onElementPicked(selector, preview);
    }

    @JavascriptInterface
    public void onElementPickedV2(String selector, String preview, int matchCount, String tagName) {
        callback.onElementPickedV2(selector, preview, matchCount, tagName);
    }

    @JavascriptInterface
    public void onPickerExited() {
        callback.onPickerExited();
    }
}
