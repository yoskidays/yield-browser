package com.yieldbrowser.app;

import java.util.Locale;

/** Pure final URL and host classification for universal compatibility recovery. */
final class UniversalCompatibilityCandidatePolicy {
    private UniversalCompatibilityCandidatePolicy() {
    }

    static boolean isCandidate(String url, String host) {
        try {
            String lowerUrl = url == null ? "" : url.toLowerCase(Locale.US);
            if (lowerUrl.contains(".pdf") || lowerUrl.contains("application/pdf")) return false;
            if (host == null || host.length() == 0) return false;

            String normalized = host.toLowerCase(Locale.US);
            if (normalized.startsWith("www.")) normalized = normalized.substring(4);
            if (matches(normalized, "youtube.com") || normalized.equals("youtu.be")) return false;
            if (matches(normalized, "google.com") || matches(normalized, "google.co.id")) return false;
            if (matches(normalized, "bing.com")) return false;
            if (matches(normalized, "duckduckgo.com")) return false;
            if (matches(normalized, "startpage.com")) return false;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean matches(String host, String base) {
        return host.equals(base) || host.endsWith("." + base);
    }
}
