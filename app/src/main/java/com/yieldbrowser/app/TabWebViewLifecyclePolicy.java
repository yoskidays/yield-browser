package com.yieldbrowser.app;

import java.util.Locale;

/** Pure decisions used by the per-tab WebView lifecycle boundary. */
final class TabWebViewLifecyclePolicy {
    private TabWebViewLifecyclePolicy() {
    }

    static boolean isBindingLive(boolean ownerPresent,
                                 boolean ownerClosed,
                                 boolean sameView,
                                 long ownerGeneration,
                                 long bindingGeneration) {
        return ownerPresent
                && !ownerClosed
                && sameView
                && ownerGeneration == bindingGeneration;
    }

    static boolean isLivePageUrl(String url) {
        if (url == null) return false;
        String clean = url.trim();
        if (clean.isEmpty()) return false;
        String lower = clean.toLowerCase(Locale.US);
        return !"about:blank".equals(lower) && !lower.startsWith("data:");
    }

    static int insertionIndex(int childCount,
                              boolean homeAttached,
                              int overlayIndex) {
        int safeCount = Math.max(0, childCount);
        int candidate = homeAttached ? 1 : safeCount;
        if (overlayIndex >= 0) candidate = overlayIndex;
        return Math.max(0, Math.min(candidate, safeCount));
    }
}
