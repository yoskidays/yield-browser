package com.yieldbrowser.app;

/** Pure final decision for navigation inside an active compatibility flow. */
final class CompatibilityNavigationPolicy {
    interface HostRelation {
        boolean related(String firstHost, String secondHost);
    }

    private CompatibilityNavigationPolicy() {
    }

    static boolean isFlow(boolean compatibility,
                          String targetHost,
                          String sourceHost,
                          HostRelation hostRelation) {
        if (!compatibility) return false;
        if (targetHost == null || sourceHost == null) return false;
        if (targetHost.length() == 0 || sourceHost.length() == 0) return true;
        if (hostRelation == null) return false;
        return hostRelation.related(targetHost, sourceHost)
                || hostRelation.related(sourceHost, targetHost);
    }
}
