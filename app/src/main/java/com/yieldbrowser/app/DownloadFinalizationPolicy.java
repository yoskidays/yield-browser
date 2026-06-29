package com.yieldbrowser.app;

/**
 * Central timing and buffer policy for exporting completed downloads.
 *
 * Large files are first written to app-private staging storage and then copied to the user's
 * Downloads folder. These limits keep that second I/O pass cooperative so it cannot flood the
 * main-thread UI queue or repeatedly query storage while the device is under heavy write load.
 */
final class DownloadFinalizationPolicy {
    static final int COPY_BUFFER_BYTES = 1024 * 1024;
    static final long PROGRESS_UPDATE_INTERVAL_MS = 750L;
    static final long HISTORY_PERSIST_INTERVAL_MS = 5_000L;
    static final long NOTIFICATION_UPDATE_INTERVAL_MS = 2_000L;
    static final long UI_TICK_NORMAL_MS = 300L;
    static final long UI_TICK_FINALIZING_MS = 800L;
    static final long STORAGE_QUERY_INTERVAL_MS = 5_000L;
    static final long COOPERATIVE_PAUSE_EVERY_BYTES = 16L * 1024L * 1024L;
    static final long COOPERATIVE_PAUSE_MS = 2L;

    private DownloadFinalizationPolicy() {
    }

    static int progressPercent(long copiedBytes, long totalBytes) {
        if (totalBytes <= 0L || copiedBytes <= 0L) return 0;
        if (copiedBytes >= totalBytes) return 100;
        return (int) Math.min(99L, copiedBytes * 100L / totalBytes);
    }

    static boolean isUpdateDue(long nowMs, long lastUpdateMs, long intervalMs,
                               long copiedBytes, long totalBytes) {
        return copiedBytes >= totalBytes || lastUpdateMs <= 0L
                || nowMs - lastUpdateMs >= Math.max(1L, intervalMs);
    }
}
