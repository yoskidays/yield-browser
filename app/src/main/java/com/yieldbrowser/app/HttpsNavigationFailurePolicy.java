package com.yieldbrowser.app;

/** Pure preflight and pending-host validation for HTTPS-First failures. */
final class HttpsNavigationFailurePolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface ErrorPredicate {
        boolean test(int errorCode);
    }

    interface HostResolver {
        String resolve(String url);
    }

    private HttpsNavigationFailurePolicy() {
    }

    static boolean passesPreflight(boolean httpsFirstEnabled,
                                   boolean viewPresent,
                                   String failedUrl,
                                   int errorCode,
                                   UrlPredicate httpsPredicate,
                                   ErrorPredicate eligibleErrorPredicate) {
        try {
            if (!httpsFirstEnabled || !viewPresent || httpsPredicate == null
                    || !httpsPredicate.test(failedUrl)) return false;
            return eligibleErrorPredicate != null && eligibleErrorPredicate.test(errorCode);
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean hasRelatedPendingFailure(String pendingOriginalUrl,
                                            String pendingUpgradeUrl,
                                            String failedUrl,
                                            HostResolver hostResolver,
                                            HttpsHostRelationPolicy.HostRelation hostRelation) {
        try {
            if (pendingOriginalUrl == null || pendingOriginalUrl.length() == 0) return false;
            if (hostResolver == null || hostRelation == null) return false;
            String expectedHost = hostResolver.resolve(pendingUpgradeUrl);
            String failedHost = hostResolver.resolve(failedUrl);
            return HttpsHostRelationPolicy.areRelated(
                    expectedHost,
                    failedHost,
                    hostRelation);
        } catch (Exception ignored) {
            return false;
        }
    }
}
