package com.yieldbrowser.app;

import java.net.URI;

/** Pure exemption rules for HTTPS-First upgrades. */
final class HttpsFirstExemptionPolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface HostPredicate {
        boolean test(String host);
    }

    private HttpsFirstExemptionPolicy() {
    }

    static boolean isExempt(String url,
                            UrlPredicate httpPredicate,
                            HostPredicate localOrPrivateHostPredicate) {
        try {
            if (httpPredicate == null || !httpPredicate.test(url)) return true;
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host == null) return true;
            if (localOrPrivateHostPredicate == null
                    || localOrPrivateHostPredicate.test(host)) return true;
            int port = uri.getPort();
            return port != -1 && port != 80 && port != 443;
        } catch (Exception ignored) {
            return true;
        }
    }
}
