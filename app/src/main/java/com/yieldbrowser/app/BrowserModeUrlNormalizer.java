package com.yieldbrowser.app;

/** Pure URL normalization for Desktop/Mobile browser mode changes. */
final class BrowserModeUrlNormalizer {
    interface UrlMapper {
        String map(String url);
    }

    interface UrlPredicate {
        boolean test(String url);
    }

    private BrowserModeUrlNormalizer() {
    }

    static String normalize(String url,
                            boolean desktopMode,
                            UrlMapper originalUrlMapper,
                            UrlPredicate httpOrHttpsPredicate,
                            UrlPredicate youtubePagePredicate) {
        try {
            if (url == null) return null;
            String clean = originalUrlMapper == null
                    ? url : originalUrlMapper.map(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            if (httpOrHttpsPredicate == null || !httpOrHttpsPredicate.test(clean)) {
                return clean;
            }
            if (youtubePagePredicate != null && youtubePagePredicate.test(clean)) {
                clean = normalizeYouTubeHost(clean, desktopMode);
            }
            return clean;
        } catch (Exception ignored) {
            return url;
        }
    }

    static String normalizeYouTubeHost(String url, boolean desktopMode) {
        if (url == null) return null;
        if (desktopMode) {
            return url.replace("https://m.youtube.com", "https://www.youtube.com")
                    .replace("http://m.youtube.com", "https://www.youtube.com");
        }
        return url.replace("https://www.youtube.com", "https://m.youtube.com")
                .replace("http://www.youtube.com", "https://m.youtube.com")
                .replace("https://youtube.com", "https://m.youtube.com")
                .replace("http://youtube.com", "https://m.youtube.com");
    }
}
