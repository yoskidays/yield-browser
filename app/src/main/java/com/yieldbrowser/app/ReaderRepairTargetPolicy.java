package com.yieldbrowser.app;

/** Pure target resolution and host guard for scheduled universal reader repairs. */
final class ReaderRepairTargetPolicy {
    interface UrlMapper {
        String map(String url);
    }

    interface UrlPredicate {
        boolean test(String url);
    }

    interface HostMapper {
        String map(String url);
    }

    interface HostRelation {
        boolean related(String firstHost, String secondHost);
    }

    private ReaderRepairTargetPolicy() {
    }

    static String resolve(String currentViewUrl,
                          String fallbackUrl,
                          String expectedHost,
                          UrlMapper originalUrlMapper,
                          UrlPredicate eligibilityPredicate,
                          HostMapper hostMapper,
                          HostRelation hostRelation) {
        try {
            String active = originalUrlMapper == null
                    ? currentViewUrl : originalUrlMapper.map(currentViewUrl);
            if (active == null || active.length() == 0) active = fallbackUrl;
            if (eligibilityPredicate == null || !eligibilityPredicate.test(active)) return null;

            String activeHost = hostMapper == null ? "" : hostMapper.map(active);
            String expected = expectedHost == null ? "" : expectedHost;
            if (expected.length() > 0 && activeHost != null && activeHost.length() > 0) {
                if (hostRelation == null) return null;
                if (!hostRelation.related(activeHost, expected)
                        && !hostRelation.related(expected, activeHost)) return null;
            }
            return active;
        } catch (Exception ignored) {
            return null;
        }
    }
}
