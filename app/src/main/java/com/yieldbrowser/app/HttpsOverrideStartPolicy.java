package com.yieldbrowser.app;

/** Pure target resolution for starting an HTTPS-First override. */
final class HttpsOverrideStartPolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface UrlTransformer {
        String transform(String url);
    }

    private HttpsOverrideStartPolicy() {
    }

    static String resolve(boolean viewPresent,
                          String targetUrl,
                          UrlPredicate httpPredicate,
                          UrlTransformer prepareTransformer) {
        try {
            if (!viewPresent || httpPredicate == null || !httpPredicate.test(targetUrl)) return null;
            if (prepareTransformer == null) return null;
            String secure = prepareTransformer.transform(targetUrl);
            return secure == null || secure.equals(targetUrl) ? null : secure;
        } catch (Exception ignored) {
            return null;
        }
    }
}
