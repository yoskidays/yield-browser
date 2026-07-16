package com.yieldbrowser.app;

import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

/** Applies WebChrome progress decisions to the Android progress bar. */
final class BrowserChromeProgressHandler {
    private BrowserChromeProgressHandler() {
    }

    static boolean handle(WebView activeWebView,
                          WebView view,
                          int newProgress,
                          boolean homeVisible,
                          ProgressBar progressBar) {
        boolean activeView = view != null && view == activeWebView;
        boolean webViewVisible = view != null && view.getVisibility() == View.VISIBLE;
        BrowserChromeProgressPolicy.State state = BrowserChromeProgressPolicy.decide(
                activeView, homeVisible, webViewVisible, newProgress);
        if (!state.handled) return false;
        if (progressBar != null) {
            progressBar.setProgress(state.progress);
            progressBar.setVisibility(state.visible ? View.VISIBLE : View.GONE);
        }
        return true;
    }
}
