package com.yieldbrowser.app;

/** Pure eligibility and per-bookmark decisions after HTTPS-First success. */
final class HttpsBookmarkUpgradePolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface UrlTransformer {
        String transform(String url);
    }

    interface UrlRelation {
        boolean related(String first, String second);
    }

    private HttpsBookmarkUpgradePolicy() {
    }

    static boolean isEligible(String originalHttpUrl,
                              String finalHttpsUrl,
                              UrlPredicate httpPredicate,
                              UrlPredicate httpsPredicate) {
        try {
            return httpPredicate != null
                    && httpsPredicate != null
                    && httpPredicate.test(originalHttpUrl)
                    && httpsPredicate.test(finalHttpsUrl);
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean shouldUpgrade(String bookmarkUrl,
                                 String originalHttpUrl,
                                 String finalHttpsUrl,
                                 UrlPredicate httpPredicate,
                                 UrlTransformer upgradeTransformer,
                                 UrlRelation equivalentRelation) {
        try {
            if (bookmarkUrl == null || httpPredicate == null
                    || !httpPredicate.test(bookmarkUrl)) return false;
            if (upgradeTransformer == null || equivalentRelation == null) return false;
            String candidate = upgradeTransformer.transform(bookmarkUrl);
            return equivalentRelation.related(bookmarkUrl, originalHttpUrl)
                    || equivalentRelation.related(candidate, finalHttpsUrl);
        } catch (Exception ignored) {
            return false;
        }
    }
}
