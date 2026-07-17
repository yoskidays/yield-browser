package com.yieldbrowser.app;

import java.net.URI;
import java.util.Locale;

/** Pure URL equivalence that ignores scheme and fragment. */
final class EquivalentUrlPolicy {
    private EquivalentUrlPolicy() {
    }

    static boolean equivalentIgnoringSchemeAndFragment(String first, String second) {
        try {
            URI a = new URI(first);
            URI b = new URI(second);
            String hostA = a.getHost() == null ? "" : a.getHost().toLowerCase(Locale.US);
            String hostB = b.getHost() == null ? "" : b.getHost().toLowerCase(Locale.US);
            if (!hostA.equals(hostB)) return false;

            int portA = normalizePort(a.getPort());
            int portB = normalizePort(b.getPort());
            if (portA != portB) return false;

            String pathA = normalizePath(a.getPath());
            String pathB = normalizePath(b.getPath());
            String queryA = a.getQuery() == null ? "" : a.getQuery();
            String queryB = b.getQuery() == null ? "" : b.getQuery();
            return pathA.equals(pathB) && queryA.equals(queryB);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int normalizePort(int port) {
        return port == 80 || port == 443 ? -1 : port;
    }

    private static String normalizePath(String path) {
        return path == null || path.length() == 0 ? "/" : path;
    }
}
