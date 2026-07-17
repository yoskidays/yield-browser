package com.yieldbrowser.app;

/** Pure decision model for preparing an HTTPS-First navigation. */
final class HttpsNavigationPreparePolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface UrlTransformer {
        String transform(String url);
    }

    static final class Result {
        final String targetUrl;
        final boolean consumeFallbackInProgress;
        final boolean clearPendingState;
        final boolean startPendingState;

        private Result(String targetUrl,
                       boolean consumeFallbackInProgress,
                       boolean clearPendingState,
                       boolean startPendingState) {
            this.targetUrl = targetUrl;
            this.consumeFallbackInProgress = consumeFallbackInProgress;
            this.clearPendingState = clearPendingState;
            this.startPendingState = startPendingState;
        }
    }

    private HttpsNavigationPreparePolicy() {
    }

    static Result prepare(String rawUrl,
                          boolean httpsFirstEnabled,
                          boolean tabPresent,
                          boolean fallbackInProgress,
                          String pendingOriginalUrl,
                          UrlPredicate httpPredicate,
                          UrlPredicate exemptionPredicate,
                          UrlPredicate fallbackGuardPredicate,
                          UrlTransformer upgradeTransformer) {
        if (rawUrl == null) return new Result(null, false, false, false);
        String clean = rawUrl.trim();
        if (!httpsFirstEnabled
                || httpPredicate == null
                || !httpPredicate.test(clean)
                || exemptionPredicate == null
                || exemptionPredicate.test(clean)) {
            return new Result(clean, false, false, false);
        }

        boolean consumeFallback = tabPresent && fallbackInProgress;
        if (consumeFallback && pendingOriginalUrl != null && pendingOriginalUrl.equals(clean)) {
            return new Result(clean, true, true, false);
        }

        if (fallbackGuardPredicate != null && fallbackGuardPredicate.test(clean)) {
            return new Result(clean, consumeFallback, false, false);
        }

        String secure = upgradeTransformer == null ? clean : upgradeTransformer.transform(clean);
        if (secure == null || secure.equals(clean)) {
            return new Result(clean, consumeFallback, false, false);
        }
        return new Result(secure, consumeFallback, false, tabPresent);
    }
}
