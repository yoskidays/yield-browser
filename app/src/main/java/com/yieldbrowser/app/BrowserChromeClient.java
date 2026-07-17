package com.yieldbrowser.app;

import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/** Thin WebChromeClient adapter that keeps browser policy and UI mechanics in tested handlers. */
final class BrowserChromeClient extends WebChromeClient {
    interface ProgressCallback {
        void onProgress(WebView view, int newProgress);
    }

    interface ShowCustomViewCallback {
        void onShow(View view, CustomViewCallback callback);
    }

    private final ProgressCallback progressCallback;
    private final ShowCustomViewCallback showCustomViewCallback;
    private final Runnable hideCustomViewCallback;

    BrowserChromeClient(ProgressCallback progressCallback,
                        ShowCustomViewCallback showCustomViewCallback,
                        Runnable hideCustomViewCallback) {
        this.progressCallback = progressCallback;
        this.showCustomViewCallback = showCustomViewCallback;
        this.hideCustomViewCallback = hideCustomViewCallback;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        progressCallback.onProgress(view, newProgress);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        showCustomViewCallback.onShow(view, callback);
    }

    @Override
    public void onHideCustomView() {
        hideCustomViewCallback.run();
    }
}
