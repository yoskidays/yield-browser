package com.yieldbrowser.app;

/** Pure lazy decision for whether a URL requires strict compatibility handling. */
final class StrictCompatibilityUrlPolicy {
    interface HostPredicate {
        boolean test(String host);
    }

    interface ActiveEvaluator {
        boolean evaluate();
    }

    private StrictCompatibilityUrlPolicy() {
    }

    static boolean isStrict(String host,
                            HostPredicate knownHostPredicate,
                            ActiveEvaluator activeEvaluator) {
        try {
            if (host == null || host.length() == 0) return false;
            if (knownHostPredicate != null && knownHostPredicate.test(host)) return true;
            return activeEvaluator != null && activeEvaluator.evaluate();
        } catch (Exception ignored) {
            return false;
        }
    }
}
