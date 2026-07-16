package com.yieldbrowser.app;

import android.webkit.WebView;

/** Keeps YouTube-specific page handling outside MainActivity. */
final class YouTubeAdGuardController {
    private YouTubeAdGuardController() {
    }

    static void installIfNeeded(WebView webView, String pageUrl) {
        if (webView == null || !YouTubeAdGuardScript.shouldInstall(pageUrl)) return;
        try {
            webView.evaluateJavascript(YouTubeAdGuardScript.script(), null);
        } catch (Exception ignored) {
        }
    }
}
