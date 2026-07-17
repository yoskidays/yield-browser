package com.yieldbrowser.app;

import java.net.URLDecoder;
import java.util.Locale;

/** Pure orchestration for trusted, user-intended download URLs. */
final class TrustedDownloadIntentPolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    interface HostResolver {
        String resolve(String url);
    }

    interface TextPredicate {
        boolean test(String value);
    }

    interface HostPredicate {
        boolean test(String host);
    }

    private TrustedDownloadIntentPolicy() {
    }

    static boolean isTrusted(String url,
                             UrlPredicate httpOrHttps,
                             HostResolver hostResolver,
                             HostPredicate trustedHostPredicate,
                             TextPredicate downloadMarkerPredicate,
                             TextPredicate directFilePredicate,
                             TextPredicate hardAdTokenPredicate,
                             HostPredicate suspiciousHostPredicate) {
        try {
            if (url == null || httpOrHttps == null) return false;
            String raw = url.trim();
            if (!httpOrHttps.test(raw)) return false;
            if (hostResolver == null
                    || trustedHostPredicate == null
                    || downloadMarkerPredicate == null
                    || directFilePredicate == null
                    || hardAdTokenPredicate == null
                    || suspiciousHostPredicate == null) return false;

            String lower = raw.toLowerCase(Locale.US);
            String decoded = lower;
            try {
                decoded = URLDecoder.decode(lower, "UTF-8").toLowerCase(Locale.US);
            } catch (Exception ignored) {
            }

            String host = hostResolver.resolve(raw);
            if (host == null || host.length() == 0) return false;

            boolean trustedHost = trustedHostPredicate.test(host);
            boolean marker = downloadMarkerPredicate.test(lower)
                    || downloadMarkerPredicate.test(decoded);
            boolean file = directFilePredicate.test(lower)
                    || directFilePredicate.test(decoded);
            boolean hardAdToken = hardAdTokenPredicate.test(lower)
                    || hardAdTokenPredicate.test(decoded);
            boolean suspiciousHost = suspiciousHostPredicate.test(host);
            boolean downloadSignal = marker || file;

            return downloadSignal
                    && (trustedHost || (!suspiciousHost && !hardAdToken));
        } catch (Exception ignored) {
            return false;
        }
    }
}
