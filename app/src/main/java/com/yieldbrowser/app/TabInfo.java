package com.yieldbrowser.app;

import android.os.Bundle;
import android.webkit.WebView;

/** Runtime and persisted state belonging to one browser tab. */
final class TabInfo {
    String title;
    String url;
    boolean privateTab;
    boolean adTab;

    /** Each tab owns its WebView so switching tabs does not overwrite another tab's page state. */
    transient WebView webView;
    Bundle webState;

    String lastSafeUrl = "";
    String lastSafeTitle = "";
    String isolationHost = "";
    String currentPageUrlForRequest = "";
    String trustedNavigationHost = "";
    long trustedNavigationUntilMs;
    long domainSwitchAllowedUntilMs;

    TabInfo(String title, String url, boolean privateTab) {
        this(title, url, privateTab, false);
    }

    TabInfo(String title, String url, boolean privateTab, boolean adTab) {
        this.title = title;
        this.url = url;
        this.privateTab = privateTab;
        this.adTab = adTab;

        if (url != null && !url.isEmpty() && !adTab) {
            lastSafeUrl = url;
            lastSafeTitle = title != null ? title : url;
            isolationHost = BrowserUrlUtils.safeHostForTabIsolation(url);
            currentPageUrlForRequest = url;
        }
    }
}
