package com.yieldbrowser.app;

import java.util.Locale;

/** Pure HTTP/HTTPS scheme predicates with legacy trimming behavior. */
final class UrlSchemePolicy {
    private UrlSchemePolicy() {
    }

    static boolean isHttpOrHttps(String url) {
        String normalized = normalize(url);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    static boolean isHttp(String url) {
        return normalize(url).startsWith("http://");
    }

    static boolean isHttps(String url) {
        return normalize(url).startsWith("https://");
    }

    private static String normalize(String url) {
        return url == null ? "" : url.trim().toLowerCase(Locale.US);
    }
}
