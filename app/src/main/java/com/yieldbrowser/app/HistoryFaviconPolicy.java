package com.yieldbrowser.app;

import java.net.URLEncoder;
import java.util.Locale;

/** Pure URL, cache-key, and recycled-target rules for favicon loading. */
final class HistoryFaviconPolicy {
    static final int ICON_SIZE_PX = 96;
    static final int CONNECT_TIMEOUT_MS = 3500;
    static final int READ_TIMEOUT_MS = 3500;
    static final int WORKER_COUNT = 3;
    static final int MEMORY_CACHE_ENTRIES = 96;

    private static final String GOOGLE_FAVICON_ENDPOINT =
            "https://www.google.com/s2/favicons?sz=" + ICON_SIZE_PX + "&domain_url=";

    private HistoryFaviconPolicy() {
    }

    static String requestKey(String pageUrl) {
        return HistoryItemPresentation.shortHost(pageUrl).toLowerCase(Locale.US);
    }

    static String requestUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.trim().isEmpty()) return "";
        try {
            return GOOGLE_FAVICON_ENDPOINT + URLEncoder.encode(pageUrl, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    static boolean matchesTarget(String requestKey, Object targetTag) {
        return requestKey != null
                && targetTag != null
                && requestKey.equals(String.valueOf(targetTag));
    }
}
