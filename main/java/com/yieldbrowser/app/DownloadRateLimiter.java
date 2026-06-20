package com.yieldbrowser.app;

/** Aggregate speed limiter shared by every worker of one download. */
final class DownloadRateLimiter {
    private long nextAvailableNanos;

    synchronized void reset() {
        nextAvailableNanos = 0L;
    }

    void acquire(int bytes, int limitKbps) throws InterruptedException {
        if (bytes <= 0 || limitKbps <= 0) return;
        long now = System.nanoTime();
        long durationNanos = Math.max(1L,
                (long) ((bytes * 1_000_000_000.0) / (limitKbps * 1024.0)));
        long sleepNanos;
        synchronized (this) {
            long start = Math.max(now, nextAvailableNanos);
            nextAvailableNanos = start + durationNanos;
            sleepNanos = start - now;
        }
        if (sleepNanos <= 0) return;
        long millis = sleepNanos / 1_000_000L;
        int nanos = (int) (sleepNanos % 1_000_000L);
        Thread.sleep(millis, nanos);
    }
}
