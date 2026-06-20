package com.yieldbrowser.app;

import android.net.Uri;

import java.util.Locale;

/** Stateless URL helpers shared by browser state classes. */
final class BrowserUrlUtils {
    private BrowserUrlUtils() {
        // Utility class.
    }

    static String safeHostForTabIsolation(String url) {
        try {
            if (url == null) return "";
            String clean = url.trim();
            if (clean.isEmpty()) return "";

            Uri uri = Uri.parse(clean);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return "";
            }

            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
