package com.yieldbrowser.app;

/** Restricts trusted file-host popups to a short window after a real download-control gesture. */
final class TrustedDownloadPopupPolicy {
    static final long GESTURE_WINDOW_MS = 4000L;

    private TrustedDownloadPopupPolicy() {
    }

    static boolean canOpen(boolean sourceIsDownloadPage,
                           boolean targetIsTrustedDownload,
                           long gestureAtMs,
                           long nowMs) {
        if (!sourceIsDownloadPage || !targetIsTrustedDownload || gestureAtMs <= 0L) {
            return false;
        }
        long ageMs = nowMs - gestureAtMs;
        return ageMs >= 0L && ageMs <= GESTURE_WINDOW_MS;
    }
}
