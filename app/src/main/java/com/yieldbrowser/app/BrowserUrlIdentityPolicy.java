package com.yieldbrowser.app;

import java.util.Locale;

/** Pure URL identity helpers used by navigation and compatibility guards. */
final class BrowserUrlIdentityPolicy {
    interface UrlMapper {
        String map(String url);
    }

    interface HostParser {
        String parseHost(String url);
    }

    private BrowserUrlIdentityPolicy() {
    }

    static String normalizedHost(String url,
                                 UrlMapper originalUrlMapper,
                                 HostParser hostParser) {
        try {
            String clean = mapOrFallback(url, originalUrlMapper);
            if (hostParser == null) return "";
            String host = hostParser.parseHost(clean);
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception ignored) {
            return "";
        }
    }

    static String navigationLoopKey(String url, UrlMapper originalUrlMapper) {
        try {
            String clean = mapOrFallback(url, originalUrlMapper);
            if (clean == null) return "";
            int hash = clean.indexOf('#');
            if (hash >= 0) clean = clean.substring(0, hash);
            return clean.trim().toLowerCase(Locale.US);
        } catch (Exception ignored) {
            return url == null ? "" : url.trim().toLowerCase(Locale.US);
        }
    }

    private static String mapOrFallback(String url, UrlMapper originalUrlMapper) {
        String clean = originalUrlMapper == null ? url : originalUrlMapper.map(url);
        if (clean == null || clean.trim().length() == 0) clean = url;
        return clean;
    }
}
