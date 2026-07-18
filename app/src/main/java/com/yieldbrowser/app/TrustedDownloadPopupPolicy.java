package com.yieldbrowser.app;

/** Restricts trusted file-host popups to the source page and a short real-gesture window. */
final class TrustedDownloadPopupPolicy {
    static final long GESTURE_WINDOW_MS = 4000L;

    private TrustedDownloadPopupPolicy() {
    }

    static boolean canOpen(boolean sourceIsDownloadPage,
                           boolean sourceMatchesGesture,
                           boolean targetIsTrustedDownload,
                           long gestureAtMs,
                           long nowMs) {
        if (!sourceIsDownloadPage || !sourceMatchesGesture
                || !targetIsTrustedDownload || gestureAtMs <= 0L) {
            return false;
        }
        long ageMs = nowMs - gestureAtMs;
        return ageMs >= 0L && ageMs <= GESTURE_WINDOW_MS;
    }

    static boolean sameSourcePage(String gestureSource, String callbackSource) {
        String expected = withoutFragment(gestureSource);
        String actual = withoutFragment(callbackSource);
        return !expected.isEmpty() && expected.equals(actual);
    }

    private static String withoutFragment(String value) {
        if (value == null) return "";
        String clean = value.trim();
        int hash = clean.indexOf('#');
        return hash >= 0 ? clean.substring(0, hash) : clean;
    }
}
