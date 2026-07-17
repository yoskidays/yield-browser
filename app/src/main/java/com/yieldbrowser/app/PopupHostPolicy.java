package com.yieldbrowser.app;

import java.util.Locale;

/** Pure classification for known popup/ad navigation hosts and URL shapes. */
final class PopupHostPolicy {
    private static final String[] HOST_PATTERNS = new String[]{
            "hotterydiseur", "sewarsremeets", "sewarsremeet", "onclickads", "clickadu", "popads", "popcash",
            "popunder", "adsterra", "propellerads", "hilltopads", "exoclick", "trafficjunky", "juicyads",
            "admaven", "pushpush", "pushengage", "pushwoosh", "realsrv", "invest-tracing", "highperformanceformat",
            "highperformancedisplayformat", "xmladfeed", "rotator", "smartlink", "adnxs", "rubiconproject",
            "taboola", "outbrain", "mgid", "revcontent", "doubleclick", "googlesyndication", "googleadservices"
    };

    interface UrlPredicate {
        boolean test(String url);
    }

    private PopupHostPolicy() {
    }

    static boolean isKnown(String url, String normalizedHost, UrlPredicate trustedDownloadPredicate) {
        try {
            if (normalizedHost == null || normalizedHost.length() == 0) return false;
            if (trustedDownloadPredicate != null && trustedDownloadPredicate.test(url)) return false;

            for (String pattern : HOST_PATTERNS) {
                if (normalizedHost.contains(pattern)) return true;
            }

            if (normalizedHost.endsWith(".cfd")
                    || normalizedHost.endsWith(".click")
                    || normalizedHost.endsWith(".cam")
                    || normalizedHost.endsWith(".monster")
                    || normalizedHost.endsWith(".quest")
                    || normalizedHost.endsWith(".buzz")
                    || normalizedHost.endsWith(".icu")
                    || normalizedHost.endsWith(".cyou")) return true;

            if (url == null) return false;
            String lower = url.toLowerCase(Locale.US);
            return lower.contains("/popunder")
                    || lower.contains("/popup")
                    || lower.contains("/redirect")
                    || lower.contains("/push/")
                    || lower.contains("?utm_source=ad")
                    || lower.contains("&ad_id=")
                    || lower.contains("?ad_id=")
                    || lower.contains("/prebid")
                    || lower.contains("/vast")
                    || lower.contains("/vpaid");
        } catch (Exception ignored) {
            return false;
        }
    }
}
