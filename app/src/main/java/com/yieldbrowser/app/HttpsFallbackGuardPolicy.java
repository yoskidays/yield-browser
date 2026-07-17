package com.yieldbrowser.app;

/** Pure, lazy decision for the temporary HTTP fallback guard. */
final class HttpsFallbackGuardPolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface HostResolver {
        String resolve(String url);
    }

    private HttpsFallbackGuardPolicy() {
    }

    static boolean isActive(String httpUrl,
                            long allowedUntilMs,
                            long nowMs,
                            String fallbackHost,
                            UrlPredicate httpPredicate,
                            HostResolver hostResolver) {
        try {
            if (httpPredicate == null || !httpPredicate.test(httpUrl)) return false;
            if (nowMs > allowedUntilMs) return false;
            if (hostResolver == null) return false;
            String host = hostResolver.resolve(httpUrl);
            return host != null
                    && host.length() > 0
                    && fallbackHost != null
                    && host.equals(fallbackHost);
        } catch (Exception ignored) {
            return false;
        }
    }
}
