package com.yieldbrowser.app;

import java.util.Locale;

/** Pure policy helpers for progressive video playback and multipart byte availability. */
final class ProgressivePlaybackPolicy {
    static final long MIN_STARTUP_BYTES = 2L * 1024L * 1024L;
    static final long ORIGIN_RANGE_WINDOW_BYTES = 4L * 1024L * 1024L;

    private ProgressivePlaybackPolicy() {
    }

    static boolean supportsContainer(String fileName, String url, boolean hlsDownload) {
        if (hlsDownload) return false;
        String combined = (safe(fileName) + " " + safe(url)).toLowerCase(Locale.US);
        return combined.contains(".mp4")
                || combined.contains(".m4v")
                || combined.contains(".3gp")
                || combined.contains(".webm")
                || combined.contains(".mov")
                || combined.contains("videoplayback");
    }

    static long availableEndExclusive(
            long position,
            long total,
            int connectionCount,
            long sequentialAvailable,
            boolean fullyAvailable,
            long[] partStarts,
            long[] partEnds,
            long[] partDone
    ) {
        if (total <= 0L || position < 0L || position >= total) return position;
        if (fullyAvailable) return total;
        if (connectionCount <= 1) {
            long ready = Math.max(0L, Math.min(total, sequentialAvailable));
            return position < ready ? ready : position;
        }
        if (partStarts == null || partEnds == null || partDone == null) return position;
        int count = Math.min(connectionCount,
                Math.min(partStarts.length, Math.min(partEnds.length, partDone.length)));
        for (int index = 0; index < count; index++) {
            long start = partStarts[index];
            long end = partEnds[index];
            long done = partDone[index];
            if (end < start || done <= 0L) continue;
            long readyEnd = Math.min(end + 1L, start + done);
            if (position >= start && position < readyEnd) return readyEnd;
        }
        return position;
    }

    static String selectOriginUrl(String resolvedUrl, String originalUrl) {
        String resolved = safe(resolvedUrl).trim();
        if (isHttpUrl(resolved)) return resolved;
        String original = safe(originalUrl).trim();
        return isHttpUrl(original) ? original : "";
    }

    static long minimumStartupBytes(long totalBytes) {
        if (totalBytes <= 0L) return MIN_STARTUP_BYTES;
        return Math.min(totalBytes, MIN_STARTUP_BYTES);
    }

    static long capOriginRangeEnd(long start, long requestedEnd) {
        if (start < 0L || requestedEnd < start) return requestedEnd;
        long capped;
        try {
            capped = Math.addExact(start, ORIGIN_RANGE_WINDOW_BYTES - 1L);
        } catch (ArithmeticException overflow) {
            capped = Long.MAX_VALUE;
        }
        return Math.min(requestedEnd, capped);
    }

    static boolean shouldFallbackToOrigin(boolean alreadyUsingOrigin,
                                          boolean originAvailable,
                                          int localErrors,
                                          boolean preparationTimedOut) {
        if (alreadyUsingOrigin || !originAvailable) return false;
        return preparationTimedOut || localErrors >= 1;
    }

    private static boolean isHttpUrl(String value) {
        String lower = safe(value).toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
