package com.yieldbrowser.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure active-state evaluation for legacy and multi-host compatibility windows. */
final class SiteCompatibilityActivePolicy {
    static final class Result {
        final boolean active;
        final List<String> expiredHosts;

        private Result(boolean active, List<String> expiredHosts) {
            this.active = active;
            this.expiredHosts = expiredHosts;
        }
    }

    private SiteCompatibilityActivePolicy() {
    }

    static Result evaluate(String host,
                           String legacyHost,
                           long legacyUntilMs,
                           Map<String, Long> activeHosts,
                           long nowMs) {
        String normalizedHost = normalizeHost(host);
        if (normalizedHost.length() == 0) {
            return new Result(false, Collections.emptyList());
        }

        String normalizedLegacy = normalizeHost(legacyHost);
        if (normalizedLegacy.length() > 0
                && nowMs <= legacyUntilMs
                && matches(normalizedHost, normalizedLegacy)) {
            return new Result(true, Collections.emptyList());
        }

        if (activeHosts == null || activeHosts.isEmpty()) {
            return new Result(false, Collections.emptyList());
        }

        ArrayList<String> expired = new ArrayList<>();
        for (Map.Entry<String, Long> entry : activeHosts.entrySet()) {
            String base = entry.getKey();
            long until = entry.getValue() == null ? 0L : entry.getValue();
            if (nowMs > until) {
                expired.add(base);
                continue;
            }
            if (base != null && base.length() > 0 && matches(normalizedHost, base)) {
                return new Result(true, Collections.emptyList());
            }
        }
        return new Result(false, expired);
    }

    static boolean matches(String host, String base) {
        return host.equals(base) || host.endsWith("." + base);
    }

    static String normalizeHost(String host) {
        if (host == null) return "";
        String normalized = host.toLowerCase(Locale.US);
        if (normalized.startsWith("www.")) normalized = normalized.substring(4);
        return normalized;
    }
}
