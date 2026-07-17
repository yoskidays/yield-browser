package com.yieldbrowser.app;

import java.util.Locale;

/** Pure host normalization and domain-relation rules used by ad-block navigation. */
final class AdBlockHostPolicy {
    interface HostResolver {
        String resolve(String url);
    }

    private AdBlockHostPolicy() {
    }

    static String normalize(String url, HostResolver resolver) {
        try {
            if (url == null || url.length() == 0 || resolver == null) return "";
            String host = resolver.resolve(url);
            if (host == null || host.length() == 0) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception ignored) {
            return "";
        }
    }

    static boolean sameOrSubDomain(String host, String baseHost) {
        if (host == null || baseHost == null
                || host.length() == 0 || baseHost.length() == 0) return false;
        return host.equals(baseHost) || host.endsWith("." + baseHost);
    }
}
