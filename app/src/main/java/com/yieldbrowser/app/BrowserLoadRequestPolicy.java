package com.yieldbrowser.app;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Pure preparation for direct WebView URLs and mobile/desktop request headers. */
final class BrowserLoadRequestPolicy {
    private static final String ACCEPT_LANGUAGE =
            "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7";

    private BrowserLoadRequestPolicy() {
    }

    static String trimInput(String url) {
        return url == null ? "" : url.trim();
    }

    static boolean isDirectWebViewUrl(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.US);
        return lower.startsWith("javascript:")
                || lower.startsWith("about:")
                || lower.startsWith("data:");
    }

    static Map<String, String> requestHeaders(boolean desktopMode,
                                              String mobileUserAgent,
                                              String desktopUserAgent) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (desktopMode) {
            headers.put("User-Agent", desktopUserAgent == null ? "" : desktopUserAgent);
            headers.put("Sec-CH-UA-Mobile", "?0");
            headers.put("Sec-CH-UA-Platform", "\"Windows\"");
        } else {
            headers.put("User-Agent", mobileUserAgent == null ? "" : mobileUserAgent);
            headers.put("Sec-CH-UA-Mobile", "?1");
            headers.put("Sec-CH-UA-Platform", "\"Android\"");
        }
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Accept-Language", ACCEPT_LANGUAGE);
        return headers;
    }
}
