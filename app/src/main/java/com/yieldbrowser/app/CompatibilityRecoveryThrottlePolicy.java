package com.yieldbrowser.app;

/** Pure five-minute retry throttle for automatic compatibility recovery. */
final class CompatibilityRecoveryThrottlePolicy {
    static final long RETRY_WINDOW_MS = 300000L;

    static final class Plan {
        final boolean retry;
        final String host;
        final String key;
        final long untilMs;

        private Plan(boolean retry, String host, String key, long untilMs) {
            this.retry = retry;
            this.host = host;
            this.key = key;
            this.untilMs = untilMs;
        }
    }

    private CompatibilityRecoveryThrottlePolicy() {
    }

    static Plan plan(String host,
                     String key,
                     String previousHost,
                     String previousKey,
                     long previousUntilMs,
                     long nowMs) {
        if (host == null || host.length() == 0) {
            return new Plan(false, "", key == null ? "" : key, 0L);
        }
        String safeKey = key == null ? "" : key;
        boolean duplicate = host.equals(previousHost)
                && safeKey.equals(previousKey)
                && nowMs < previousUntilMs;
        if (duplicate) return new Plan(false, host, safeKey, previousUntilMs);
        return new Plan(true, host, safeKey, nowMs + RETRY_WINDOW_MS);
    }
}
