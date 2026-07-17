package com.yieldbrowser.app;

/** Pure active-URL, host, and navigation-key guard for blank-page recovery. */
final class BlankRecoveryTargetPolicy {
    interface UrlMapper {
        String map(String url);
    }

    interface ValueMapper {
        String map(String url);
    }

    interface HostRelation {
        boolean related(String activeHost, String expectedHost);
    }

    private BlankRecoveryTargetPolicy() {
    }

    static String resolve(String currentViewUrl,
                          String fallbackUrl,
                          String expectedHost,
                          String expectedKey,
                          UrlMapper originalUrlMapper,
                          ValueMapper hostMapper,
                          ValueMapper keyMapper,
                          HostRelation hostRelation) {
        try {
            String active = originalUrlMapper == null
                    ? currentViewUrl : originalUrlMapper.map(currentViewUrl);
            if (active == null || active.length() == 0) active = fallbackUrl;

            String activeHost = hostMapper == null ? "" : hostMapper.map(active);
            String activeKey = keyMapper == null ? "" : keyMapper.map(active);
            if (hostRelation == null || !hostRelation.related(activeHost, expectedHost)) return null;
            if (expectedKey == null || !expectedKey.equals(activeKey)) return null;
            return active;
        } catch (Exception ignored) {
            return null;
        }
    }
}
