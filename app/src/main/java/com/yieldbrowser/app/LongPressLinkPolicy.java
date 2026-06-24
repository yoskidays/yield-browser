package com.yieldbrowser.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Resolves a long-pressed WebView anchor while rejecting non-web schemes. */
final class LongPressLinkPolicy {
    private LongPressLinkPolicy() {
        // Utility class.
    }

    static String resolveHttpUrl(String rawHref, String pageUrl) {
        if (rawHref == null) return null;
        String href = rawHref.trim();
        if (href.isEmpty()) return null;

        try {
            URI resolved;
            if (href.startsWith("//")) {
                String baseScheme = httpScheme(pageUrl);
                resolved = new URI((baseScheme == null ? "https" : baseScheme) + ":" + href);
            } else {
                URI hrefUri = new URI(href);
                if (hrefUri.isAbsolute()) {
                    resolved = hrefUri;
                } else {
                    URI baseUri = validHttpBase(pageUrl);
                    if (baseUri == null) return null;
                    resolved = baseUri.resolve(hrefUri);
                }
            }

            String scheme = resolved.getScheme();
            if (scheme == null) return null;
            scheme = scheme.toLowerCase(Locale.US);
            if (!"http".equals(scheme) && !"https".equals(scheme)) return null;
            if (resolved.getRawAuthority() == null || resolved.getRawAuthority().trim().isEmpty()) {
                return null;
            }
            return resolved.toString();
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return null;
        }
    }

    private static URI validHttpBase(String pageUrl) throws URISyntaxException {
        if (pageUrl == null || pageUrl.trim().isEmpty()) return null;
        URI base = new URI(pageUrl.trim());
        String scheme = base.getScheme();
        if (scheme == null) return null;
        scheme = scheme.toLowerCase(Locale.US);
        if (!"http".equals(scheme) && !"https".equals(scheme)) return null;
        if (base.getRawAuthority() == null || base.getRawAuthority().trim().isEmpty()) return null;
        return base;
    }

    private static String httpScheme(String pageUrl) {
        try {
            URI base = validHttpBase(pageUrl);
            return base != null ? base.getScheme().toLowerCase(Locale.US) : null;
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}
