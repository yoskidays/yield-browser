package com.yieldbrowser.app;

import java.util.Locale;

/** Pure activation decisions for temporary per-site compatibility mode. */
final class SiteCompatibilityActivationPolicy {
    static final long ACTIVE_DURATION_MS = 300000L;
    static final long TOAST_INTERVAL_MS = 8000L;

    static final class Plan {
        final boolean activate;
        final String host;
        final long untilMs;
        final boolean showToast;

        private Plan(boolean activate, String host, long untilMs, boolean showToast) {
            this.activate = activate;
            this.host = host;
            this.untilMs = untilMs;
            this.showToast = showToast;
        }
    }

    private SiteCompatibilityActivationPolicy() {
    }

    static Plan plan(boolean httpOrHttps,
                     String host,
                     long activationNowMs,
                     long toastNowMs,
                     long lastToastMs) {
        String normalizedHost = normalizeHost(host);
        if (!httpOrHttps || normalizedHost.length() == 0) {
            return new Plan(false, "", 0L, false);
        }
        return new Plan(
                true,
                normalizedHost,
                activationNowMs + ACTIVE_DURATION_MS,
                toastNowMs - lastToastMs > TOAST_INTERVAL_MS);
    }

    static String normalizeHost(String host) {
        if (host == null) return "";
        String normalized = host.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("www.")) normalized = normalized.substring(4);
        return normalized;
    }
}
