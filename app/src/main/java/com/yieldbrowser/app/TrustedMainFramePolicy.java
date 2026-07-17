package com.yieldbrowser.app;

/** Pure ten-second trust window for explicit main-frame navigations. */
final class TrustedMainFramePolicy {
    static final long TRUST_DURATION_MS = 10000L;

    static final class Activation {
        final boolean activate;
        final String host;
        final long untilMs;

        private Activation(boolean activate, String host, long untilMs) {
            this.activate = activate;
            this.host = host;
            this.untilMs = untilMs;
        }
    }

    interface HostRelation {
        boolean related(String candidateHost, String trustedHost);
    }

    private TrustedMainFramePolicy() {
    }

    static Activation activation(boolean httpOrHttps, String host, long nowMs) {
        if (!httpOrHttps || host == null || host.length() == 0) {
            return new Activation(false, "", 0L);
        }
        return new Activation(true, host, nowMs + TRUST_DURATION_MS);
    }

    static boolean isTrusted(String trustedHost,
                             long trustedUntilMs,
                             long nowMs,
                             String candidateHost,
                             HostRelation hostRelation) {
        try {
            if (trustedHost == null || trustedHost.length() == 0) return false;
            if (nowMs > trustedUntilMs) return false;
            if (candidateHost == null || candidateHost.length() == 0) return false;
            return hostRelation != null && hostRelation.related(candidateHost, trustedHost);
        } catch (Exception ignored) {
            return false;
        }
    }
}
