package com.yieldbrowser.app;

/** Pure bidirectional host relation used by HTTPS-First success and fallback. */
final class HttpsHostRelationPolicy {
    interface HostRelation {
        boolean related(String candidateHost, String baseHost);
    }

    private HttpsHostRelationPolicy() {
    }

    static boolean areRelated(String firstHost,
                              String secondHost,
                              HostRelation relation) {
        try {
            if (firstHost == null || secondHost == null
                    || firstHost.length() == 0 || secondHost.length() == 0
                    || relation == null) return false;
            return relation.related(firstHost, secondHost)
                    || relation.related(secondHost, firstHost);
        } catch (Exception ignored) {
            return false;
        }
    }
}
