package com.yieldbrowser.app;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import java.util.List;

/**
 * Owns the mechanical lifecycle of WebViews attached to browser tabs.
 *
 * Navigation policy, WebViewClient/WebChromeClient configuration, Shield rules, and URL loading
 * remain in MainActivity and are supplied through narrow callbacks.
 */
final class TabWebViewLifecycle {
    interface Factory {
        WebView create(TabInfo tab, int visibility);
    }

    interface Attacher {
        void attach(WebView candidate);
    }

    interface ReuseHandler {
        void onReuse(WebView candidate);
    }

    interface UrlExtractor {
        String extract(String url);
    }

    interface ShieldRemover {
        void remove(WebView candidate);
    }

    static final class Binding {
        final TabInfo tab;
        final long generation;

        Binding(TabInfo tab, long generation) {
            this.tab = tab;
            this.generation = generation;
        }
    }

    private TabWebViewLifecycle() {
    }

    static TabInfo findOwner(List<TabInfo> tabs, WebView candidate) {
        if (candidate == null) return null;
        try {
            Object tag = candidate.getTag();
            if (tag instanceof Binding) {
                Binding binding = (Binding) tag;
                TabInfo owner = binding.tab;
                boolean live = TabWebViewLifecyclePolicy.isBindingLive(
                        owner != null,
                        owner != null && owner.closed,
                        owner != null && owner.webView == candidate,
                        owner == null ? -1L : owner.webViewGeneration,
                        binding.generation);
                return live ? owner : null;
            }

            if (tabs != null) {
                for (TabInfo tab : tabs) {
                    if (tab != null && !tab.closed && tab.webView == candidate) return tab;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    static boolean isLive(List<TabInfo> tabs,
                          TabInfo tab,
                          WebView candidate,
                          long generation) {
        return TabWebViewLifecyclePolicy.isBindingLive(
                tab != null,
                tab != null && tab.closed,
                tab != null && tab.webView == candidate,
                tab == null ? -1L : tab.webViewGeneration,
                generation)
                && findOwner(tabs, candidate) == tab;
    }

    static boolean isPrivate(boolean dedicatedPrivateProfile,
                             List<TabInfo> tabs,
                             WebView candidate) {
        if (dedicatedPrivateProfile) return true;
        if (candidate == null) return false;
        try {
            Object tag = candidate.getTag();
            if (tag instanceof Binding) {
                TabInfo owner = ((Binding) tag).tab;
                if (owner != null) return owner.privateTab;
            }
        } catch (Exception ignored) {
        }
        TabInfo owner = findOwner(tabs, candidate);
        return owner != null && owner.privateTab;
    }

    static void attach(ViewGroup contentFrame,
                       ScrollView homeScroll,
                       View navigationLoadingOverlay,
                       WebView candidate) {
        if (candidate == null || contentFrame == null) return;
        try {
            if (candidate.getParent() == null) {
                boolean homeAttached = homeScroll != null && homeScroll.getParent() == contentFrame;
                int overlayIndex = -1;
                if (navigationLoadingOverlay != null
                        && navigationLoadingOverlay.getParent() == contentFrame) {
                    overlayIndex = contentFrame.indexOfChild(navigationLoadingOverlay);
                }
                int insertIndex = TabWebViewLifecyclePolicy.insertionIndex(
                        contentFrame.getChildCount(), homeAttached, overlayIndex);
                contentFrame.addView(candidate, insertIndex,
                        new FrameLayout.LayoutParams(-1, -1));
            }
            if (navigationLoadingOverlay != null) navigationLoadingOverlay.bringToFront();
        } catch (Exception ignored) {
        }
    }

    static WebView ensure(TabInfo tab,
                          int visibility,
                          WebView fallback,
                          Factory factory,
                          Attacher attacher,
                          ReuseHandler reuseHandler) {
        if (tab == null) return fallback;
        if (tab.closed) return null;
        try {
            if (tab.webView == null) {
                WebView created = factory == null ? null : factory.create(tab, visibility);
                tab.webView = created;
                if (attacher != null) attacher.attach(created);
            } else {
                WebView existing = tab.webView;
                if (attacher != null) attacher.attach(existing);
                if (reuseHandler != null) reuseHandler.onReuse(existing);
                existing.setVisibility(visibility);
            }
            return tab.webView;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static void hideInactive(List<TabInfo> tabs, WebView active) {
        if (tabs == null) return;
        try {
            for (TabInfo tab : tabs) {
                if (tab != null && tab.webView != null && tab.webView != active) {
                    tab.webView.setVisibility(View.GONE);
                }
            }
        } catch (Exception ignored) {
        }
    }

    static void activateSurface(ScrollView homeScroll,
                                View navigationLoadingOverlay,
                                WebView target,
                                boolean showWebPage) {
        try {
            if (homeScroll != null) {
                homeScroll.setVisibility(showWebPage ? View.GONE : View.VISIBLE);
            }
            if (target != null) target.setVisibility(showWebPage ? View.VISIBLE : View.GONE);
            if (navigationLoadingOverlay != null) navigationLoadingOverlay.bringToFront();
        } catch (Exception ignored) {
        }
    }

    static WebView destroy(TabInfo tab,
                           WebView current,
                           ViewGroup contentFrame,
                           ShieldRemover shieldRemover) {
        if (tab == null) return current;
        WebView doomed = tab.webView;
        tab.webView = null;
        tab.webViewGeneration++;
        if (doomed == null) return current;

        WebView nextCurrent = current == doomed ? null : current;
        if (tab.privateTab) {
            try { doomed.clearHistory(); } catch (Exception ignored) {}
            try { doomed.clearFormData(); } catch (Exception ignored) {}
            try { doomed.clearSslPreferences(); } catch (Exception ignored) {}
            try { doomed.clearCache(true); } catch (Exception ignored) {}
        }
        if (shieldRemover != null) {
            try { shieldRemover.remove(doomed); } catch (Exception ignored) {}
        }
        try { doomed.setTag(null); } catch (Exception ignored) {}
        try { doomed.setDownloadListener(null); } catch (Exception ignored) {}
        try { doomed.removeJavascriptInterface("YieldVideoBridge"); } catch (Exception ignored) {}
        try { doomed.removeJavascriptInterface("YieldAdBlockBridge"); } catch (Exception ignored) {}
        try { doomed.removeJavascriptInterface("YieldTranslateBridge"); } catch (Exception ignored) {}
        try { doomed.setWebViewClient(new WebViewClient()); } catch (Exception ignored) {}
        try { doomed.setWebChromeClient(new WebChromeClient()); } catch (Exception ignored) {}
        try { doomed.stopLoading(); } catch (Exception ignored) {}
        try { doomed.loadUrl("about:blank"); } catch (Exception ignored) {}
        try { if (contentFrame != null) contentFrame.removeView(doomed); } catch (Exception ignored) {}
        try { doomed.removeAllViews(); } catch (Exception ignored) {}
        try { doomed.destroy(); } catch (Exception ignored) {}
        return nextCurrent;
    }

    static boolean hasLivePage(WebView candidate, UrlExtractor extractor) {
        if (candidate == null) return false;
        try {
            String raw = candidate.getUrl();
            String clean = extractor == null ? raw : extractor.extract(raw);
            return TabWebViewLifecyclePolicy.isLivePageUrl(clean);
        } catch (Exception ignored) {
            return false;
        }
    }
}
