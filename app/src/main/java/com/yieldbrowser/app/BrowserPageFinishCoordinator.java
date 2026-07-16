package com.yieldbrowser.app;

import android.os.Bundle;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;

/** Handles the isolated inactive-tab branch of WebViewClient#onPageFinished. */
final class BrowserPageFinishCoordinator {
    interface TabLookup {
        TabInfo find(WebView view);
    }

    interface UrlMapper {
        String map(String url);
    }

    interface TabCommitter {
        void commit(TabInfo tab, String url, String title);
    }

    interface TitleLookup {
        String get(WebView view);
    }

    interface WebStateSaver {
        void save(TabInfo tab, WebView view);
    }

    private BrowserPageFinishCoordinator() {
    }

    static boolean handleInactive(boolean inactiveView,
                                  WebView view,
                                  String rawUrl,
                                  TabLookup tabLookup,
                                  UrlMapper originalUrlMapper,
                                  TabCommitter tabCommitter,
                                  TitleLookup titleLookup,
                                  WebStateSaver webStateSaver) {
        if (!inactiveView) return false;

        TabInfo owner = tabLookup == null ? null : tabLookup.find(view);
        if (owner != null) {
            String extractedUrl = originalUrlMapper == null
                    ? null
                    : originalUrlMapper.map(rawUrl);
            String finalUrl = BrowserPageFinishPolicy.chooseFinalUrl(
                    extractedUrl, rawUrl);
            String title = titleLookup == null ? null : titleLookup.get(view);
            if (tabCommitter != null) {
                tabCommitter.commit(owner, finalUrl, title);
            }
            if (webStateSaver != null) webStateSaver.save(owner, view);
        }
        return true;
    }

    static String getTitle(WebView view) {
        return view == null ? null : view.getTitle();
    }

    static void saveWebState(TabInfo owner, WebView view) {
        if (owner == null || view == null) return;
        try {
            Bundle state = new Bundle();
            WebBackForwardList saved = view.saveState(state);
            int savedSize = saved == null ? 0 : saved.getSize();
            if (BrowserPageFinishPolicy.shouldKeepWebState(
                    saved != null, savedSize)) {
                owner.webState = state;
            }
        } catch (Exception ignored) {
        }
    }
}
