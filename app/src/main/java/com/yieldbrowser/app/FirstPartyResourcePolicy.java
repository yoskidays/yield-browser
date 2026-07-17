package com.yieldbrowser.app;

/** Pure, dependency-driven first-party resource classification. */
final class FirstPartyResourcePolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface HostResolver {
        String resolve(String url);
    }

    interface HostRelation {
        boolean related(String candidateHost, String pageHost);
    }

    private FirstPartyResourcePolicy() {
    }

    static boolean isFirstParty(String resourceUrl,
                                String pageUrl,
                                UrlPredicate httpOrHttps,
                                HostResolver hostResolver,
                                HostRelation hostRelation) {
        try {
            if (httpOrHttps == null
                    || !httpOrHttps.test(resourceUrl)
                    || !httpOrHttps.test(pageUrl)) return false;
            if (hostResolver == null || hostRelation == null) return false;

            String resourceHost = hostResolver.resolve(resourceUrl);
            String pageHost = hostResolver.resolve(pageUrl);
            if (resourceHost == null || resourceHost.length() == 0) return false;
            if (pageHost == null || pageHost.length() == 0) return false;
            return hostRelation.related(resourceHost, pageHost);
        } catch (Exception ignored) {
            return false;
        }
    }
}
