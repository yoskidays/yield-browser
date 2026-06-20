package com.yieldbrowser.app;

import android.os.Bundle;
import android.webkit.WebView;

import java.util.UUID;

/** Runtime and persisted state belonging to exactly one browser tab. */
final class TabInfo {
    /** Stable runtime/session identity; never inferred from a mutable list index. */
    final String id;

    String title;
    String url;
    final boolean privateTab;
    final boolean adTab;

    /** Each tab owns one live WebView at most. */
    transient WebView webView;
    Bundle webState;

    /** Invalidates callbacks from a WebView that has already been replaced or destroyed. */
    long webViewGeneration;
    boolean closed;

    String lastSafeUrl = "";
    String lastSafeTitle = "";
    String isolationHost = "";
    String currentPageUrlForRequest = "";
    String trustedNavigationHost = "";
    long trustedNavigationUntilMs;
    long domainSwitchAllowedUntilMs;

    TabInfo(String title, String url, boolean privateTab) {
        this(null, title, url, privateTab, false);
    }

    TabInfo(String title, String url, boolean privateTab, boolean adTab) {
        this(null, title, url, privateTab, adTab);
    }

    TabInfo(String id, String title, String url, boolean privateTab, boolean adTab) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id.trim();
        this.title = title;
        this.url = url == null ? "" : url;
        this.privateTab = privateTab;
        this.adTab = adTab;

        if (!this.url.isEmpty() && !adTab) {
            lastSafeUrl = this.url;
            lastSafeTitle = title != null ? title : this.url;
            isolationHost = BrowserUrlUtils.safeHostForTabIsolation(this.url);
            currentPageUrlForRequest = this.url;
        }
    }

    /** Clears all navigational state so Home cannot resurrect a previous URL. */
    void resetToHome() {
        title = privateTab ? "Tab privat" : "Tab baru";
        url = "";
        webState = null;
        lastSafeUrl = "";
        lastSafeTitle = "";
        isolationHost = "";
        currentPageUrlForRequest = "";
        trustedNavigationHost = "";
        trustedNavigationUntilMs = 0L;
        domainSwitchAllowedUntilMs = 0L;
        webViewGeneration++;
    }
}
