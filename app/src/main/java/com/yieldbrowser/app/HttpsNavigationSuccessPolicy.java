package com.yieldbrowser.app;

/** Pure action selection for HTTPS-First navigation completion. */
final class HttpsNavigationSuccessPolicy {
    enum Action {
        IGNORE,
        KEEP_FALLBACK,
        CLEAR_FALLBACK,
        COMPLETE_RELATED,
        COMPLETE_UNRELATED
    }

    interface UrlPredicate {
        boolean test(String url);
    }

    interface HostResolver {
        String resolve(String url);
    }

    private HttpsNavigationSuccessPolicy() {
    }

    static Action evaluate(String finalUrl,
                           String originalHttpUrl,
                           String pendingHttpsUrl,
                           String fallbackHost,
                           UrlPredicate httpsPredicate,
                           HostResolver hostResolver,
                           HttpsHostRelationPolicy.HostRelation hostRelation) {
        try {
            if (httpsPredicate == null || !httpsPredicate.test(finalUrl)) return Action.IGNORE;
            if (hostResolver == null) return Action.IGNORE;

            if (originalHttpUrl == null || originalHttpUrl.length() == 0) {
                String finalHost = hostResolver.resolve(finalUrl);
                return finalHost != null && finalHost.equals(fallbackHost)
                        ? Action.CLEAR_FALLBACK
                        : Action.KEEP_FALLBACK;
            }

            String expectedHost = hostResolver.resolve(pendingHttpsUrl);
            String finalHost = hostResolver.resolve(finalUrl);
            boolean related = HttpsHostRelationPolicy.areRelated(
                    expectedHost, finalHost, hostRelation);
            return related ? Action.COMPLETE_RELATED : Action.COMPLETE_UNRELATED;
        } catch (Exception ignored) {
            return Action.IGNORE;
        }
    }
}
