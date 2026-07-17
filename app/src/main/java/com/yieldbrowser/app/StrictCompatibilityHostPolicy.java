package com.yieldbrowser.app;

import java.util.Locale;

/** Pure matching against the built-in strict compatibility host list. */
final class StrictCompatibilityHostPolicy {
    private StrictCompatibilityHostPolicy() {
    }

    static boolean isKnownHost(String host, Iterable<String> strictHosts) {
        try {
            if (host == null || host.length() == 0 || strictHosts == null) return false;
            String normalized = host.toLowerCase(Locale.US);
            if (normalized.startsWith("www.")) normalized = normalized.substring(4);
            for (String base : strictHosts) {
                if (normalized.equals(base) || normalized.endsWith("." + base)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
