package com.yieldbrowser.app;

import java.net.URI;

/** Pure construction of HTTPS-First upgrade destinations. */
final class HttpsFirstUpgradePolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    private HttpsFirstUpgradePolicy() {
    }

    static String upgrade(String httpUrl,
                          UrlPredicate httpPredicate,
                          UrlPredicate exemptionPredicate) {
        try {
            if (httpPredicate == null
                    || !httpPredicate.test(httpUrl)
                    || exemptionPredicate == null
                    || exemptionPredicate.test(httpUrl)) return httpUrl;

            URI source = new URI(httpUrl.trim());
            int sourcePort = source.getPort();
            int securePort = sourcePort == 80 ? -1 : sourcePort;
            URI secure = new URI(
                    "https",
                    source.getUserInfo(),
                    source.getHost(),
                    securePort,
                    source.getPath(),
                    source.getQuery(),
                    source.getFragment());
            return secure.toASCIIString();
        } catch (Exception ignored) {
            return httpUrl;
        }
    }
}
