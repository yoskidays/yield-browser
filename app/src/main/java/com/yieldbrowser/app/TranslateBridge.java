package com.yieldbrowser.app;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge for the compatible-translation feature, exposed to web pages as
 * {@code YieldTranslateBridge}.
 *
 * <p>This used to be a private inner class of {@code MainActivity}. It is now a top-level class that
 * forwards every {@code @JavascriptInterface} call to a {@link Callback}. The JavaScript-visible
 * method names and the registered interface name are unchanged, so page scripts behave exactly as
 * before; only the Java structure is decoupled. Threading, session-token checks, and UI work all
 * remain on the {@code MainActivity} side, inside the callback implementation.</p>
 */
final class TranslateBridge {

    /**
     * Receives translation events from the page. Methods are invoked on whatever thread the WebView
     * delivers the JavaScript call on; the implementation is responsible for any threading.
     */
    interface Callback {
        void translateText(int index, String text);

        void onCollected(int count);
    }

    private final Callback callback;

    TranslateBridge(Callback callback) {
        this.callback = callback;
    }

    @JavascriptInterface
    public void translateText(int index, String text) {
        callback.translateText(index, text);
    }

    @JavascriptInterface
    public void onCollected(int count) {
        callback.onCollected(count);
    }
}
