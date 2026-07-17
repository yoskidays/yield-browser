package com.yieldbrowser.app;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure request headers for minimally modified compatibility-mode loads. */
final class CompatibilityLoadRequestPolicy {
    private static final String ACCEPT_LANGUAGE =
            "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7";

    private CompatibilityLoadRequestPolicy() {
    }

    static Map<String, String> requestHeaders(boolean desktopMode,
                                              String desktopUserAgent) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (!desktopMode) return headers;
        headers.put("User-Agent", desktopUserAgent);
        headers.put("Accept-Language", ACCEPT_LANGUAGE);
        return headers;
    }
}
