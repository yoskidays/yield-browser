package com.yieldbrowser.app;

import java.util.Locale;

/** Pure policy helpers for progressive video playback and multipart byte availability. */
final class ProgressivePlaybackPolicy {
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
