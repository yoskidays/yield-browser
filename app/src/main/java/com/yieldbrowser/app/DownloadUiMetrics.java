package com.yieldbrowser.app;

/** Pure calculations used by the download UI ticker and unit tests. */
final class DownloadUiMetrics {
    private static final long MAX_ETA_SECONDS = 7L * 24L * 60L * 60L;

    private DownloadUiMetrics() {
    }

    static double smoothSpeed(double previous, double sample) {
        if (sample <= 0) return previous > 0 ? previous * 0.88 : 0;
        if (previous <= 0) return sample;
        return previous * 0.78 + sample * 0.22;
    }

    static long estimateRemainingSeconds(long totalBytes, long downloadedBytes, double speedBytesPerSecond) {
        if (totalBytes <= 0 || speedBytesPerSecond <= 1 || downloadedBytes >= totalBytes) return -1L;
        long remaining = Math.max(0L, totalBytes - downloadedBytes);
        return Math.max(1L, Math.min(MAX_ETA_SECONDS,
                (long) Math.ceil(remaining / speedBytesPerSecond)));
    }

    static int progressBasisPoints(long completedBytes, long totalBytes, int fallbackPercent) {
        if (totalBytes > 0) {
            long safeCompleted = Math.max(0L, Math.min(completedBytes, totalBytes));
            return (int) Math.min(10_000L, safeCompleted * 10_000L / totalBytes);
        }
        return Math.max(0, Math.min(10_000, fallbackPercent * 100));
    }
}
