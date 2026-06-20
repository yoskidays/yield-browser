package com.yieldbrowser.app;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Low-cost URL policy for the universal image-reader compatibility repair.
 * DOM classification is performed by UniversalReaderRepairScript inside WebView.
 */
final class ReaderCompatibilityPolicy {
    private static final Pattern DIRECT_ASSET = Pattern.compile(
            ".*\\.(?:avif|bmp|gif|ico|jpe?g|png|svg|webp|mp4|m4v|mov|webm|m3u8|mpd|mp3|aac|wav|ogg|pdf|zip|rar|7z)(?:$|[?#]).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern READER_PATH_HINT = Pattern.compile(
            "(?:^|/)(?:chapter|chapitre|capitulo|episode|reader|read-online|reading|baca|ch)[-_/]?[a-z0-9.-]*(?:/|$)|(?:-chapter-|/chapter/|/episode/)",
            Pattern.CASE_INSENSITIVE);

    private ReaderCompatibilityPolicy() {
    }

    static boolean isEligiblePageUrl(String url) {
        if (url == null) return false;
        String clean = url.trim();
        if (clean.isEmpty()) return false;
        String lower = clean.toLowerCase(Locale.US);
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) return false;
        if (DIRECT_ASSET.matcher(lower).matches()) return false;
        try {
            URI parsed = URI.create(clean);
            String host = parsed.getHost();
            return host != null && !host.trim().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean hasReaderPathHint(String url) {
        if (!isEligiblePageUrl(url)) return false;
        try {
            URI parsed = URI.create(url.trim());
            String path = parsed.getPath();
            return path != null && READER_PATH_HINT.matcher(path.toLowerCase(Locale.US)).find();
        } catch (Exception ignored) {
            return false;
        }
    }

    static long[] retrySchedule(boolean compatibilityMode, boolean readerPathHint) {
        if (compatibilityMode) {
            return new long[]{0L, 300L, 1100L, 2800L, 6000L};
        }
        if (readerPathHint) {
            return new long[]{200L, 900L, 2400L, 5200L};
        }
        return new long[]{450L, 1800L};
    }
}
