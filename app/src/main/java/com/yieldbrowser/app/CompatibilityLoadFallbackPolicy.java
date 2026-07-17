package com.yieldbrowser.app;

/** Pure active-URL resolution and host validation for compatibility load fallback. */
final class CompatibilityLoadFallbackPolicy {
    interface UrlMapper {
        String map(String url);
    }

    interface HostMapper {
        String map(String url);
    }

    interface HostRelation {
        boolean related(String activeHost, String expectedHost);
    }

    private CompatibilityLoadFallbackPolicy() {
    }

    static String resolve(String currentViewUrl,
                          String fallbackUrl,
                          String expectedHost,
                          UrlMapper originalUrlMapper,
                          HostMapper hostMapper,
                          HostRelation hostRelation) {
        try {
            String active = originalUrlMapper == null
                    ? currentViewUrl : originalUrlMapper.map(currentViewUrl);
            if (active == null || active.length() == 0) active = fallbackUrl;
            String activeHost = hostMapper == null ? "" : hostMapper.map(active);
            if (expectedHost == null || expectedHost.length() == 0) return null;
            if (activeHost == null || activeHost.length() == 0) return null;
            if (hostRelation == null || !hostRelation.related(activeHost, expectedHost)) return null;
            return active;
        } catch (Exception ignored) {
            return null;
        }
    }
}
