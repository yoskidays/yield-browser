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
            boolean hardAdToken = hardAdTokenPredicate.test(lower)
                    || hardAdTokenPredicate.test(decoded);
            boolean suspiciousHost = suspiciousHostPredicate.test(host);
            if (hardAdToken || suspiciousHost) return false;

            // Only inspect the actual destination path for generic download signals. Ad redirects
            // often hide a real file URL inside their query string; treating that query as the
            // destination previously promoted the advertising page into a normal download tab.
            String pathOnly = stripQueryAndFragment(lower);
            String decodedPathOnly = stripQueryAndFragment(decoded);
            boolean marker = downloadMarkerPredicate.test(pathOnly)
                    || downloadMarkerPredicate.test(decodedPathOnly);
            boolean file = directFilePredicate.test(pathOnly)
                    || directFilePredicate.test(decodedPathOnly);

            // Known download hosts are allowed from a verified user download gesture. Unknown
            // hosts still work when their own path clearly represents a download or direct file.
            return trustedHost || marker || file;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String stripQueryAndFragment(String value) {
        if (value == null) return "";
        int end = value.length();
        int query = value.indexOf('?');
        int fragment = value.indexOf('#');
        if (query >= 0 && query < end) end = query;
        if (fragment >= 0 && fragment < end) end = fragment;
        return value.substring(0, end);
    }
}
