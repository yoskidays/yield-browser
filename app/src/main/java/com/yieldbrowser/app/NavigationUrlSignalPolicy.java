package com.yieldbrowser.app;

import java.util.Locale;

/** Pure classification for external schemes and likely advertising clicks. */
final class NavigationUrlSignalPolicy {
    interface UrlPredicate {
        boolean test(String url);
    }

    private NavigationUrlSignalPolicy() {
    }

    static boolean isExternalScheme(String url) {
        if (url == null || url.trim().length() == 0) return false;
        String normalized = url.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) return false;
        if (normalized.startsWith("about:")
                || normalized.startsWith("javascript:")
                || normalized.startsWith("data:")
                || normalized.startsWith("blob:")
                || normalized.startsWith("file:")) return false;
        return normalized.matches("^[a-z][a-z0-9+.-]*:.*");
    }

    static boolean isLikelyAdClick(String url,
                                   UrlPredicate mediaPredicate,
                                   UrlPredicate youtubePredicate,
                                   UrlPredicate trustedDownloadPredicate) {
        try {
            if (url == null) return false;
            String normalized = url.toLowerCase(Locale.US);
            if (mediaPredicate != null && mediaPredicate.test(normalized)) return false;
            if (youtubePredicate != null && youtubePredicate.test(normalized)) return false;
            if (trustedDownloadPredicate != null && trustedDownloadPredicate.test(normalized)) return false;

            return isExternalScheme(normalized)
                    || normalized.contains("utm_medium=affiliates")
                    || normalized.contains("utm_source=an_")
                    || normalized.contains("affiliate")
                    || normalized.contains("aff_sub")
                    || normalized.contains("deep_and_deferred")
                    || normalized.contains("navigate_url=")
                    || normalized.contains("reactpath")
                    || normalized.contains("click_id")
                    || normalized.contains("adclick")
                    || normalized.contains("ad_click")
                    || normalized.contains("adurl=")
                    || normalized.contains("af_click")
                    || normalized.contains("tracking_id")
                    || normalized.contains("campaign_id")
                    || normalized.startsWith("shopeeid:")
                    || normalized.startsWith("lazada:")
                    || normalized.startsWith("tokopedia:")
                    || normalized.startsWith("intent:")
                    || normalized.startsWith("market:");
        } catch (Exception ignored) {
            return false;
        }
    }
}
