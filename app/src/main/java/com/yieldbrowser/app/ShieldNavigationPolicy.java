package com.yieldbrowser.app;

final class ShieldNavigationPolicy {
    private ShieldNavigationPolicy() {
    }

    static boolean shouldBlockMainFrameNavigation(String targetUrl,
                                                   String sourceUrl,
                                                   boolean hasGesture,
                                                   boolean compatibilityOrReaderContext,
                                                   boolean explicitlyTrusted,
                                                   boolean legacySuspicious) {
        if (explicitlyTrusted) return false;
        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)) {
            return ShieldUrlRules.isDangerousExternalScheme(targetUrl);
        }
        if (ShieldUrlRules.isDirectContentAsset(targetUrl)) return false;

        String targetHost = ShieldUrlRules.hostOf(targetUrl);
        String sourceHost = ShieldUrlRules.hostOf(sourceUrl);
        if (targetHost.isEmpty()) return false;

        boolean sameSite = !sourceHost.isEmpty()
                && ShieldUrlRules.sameSite(targetHost, sourceHost);
        if (sameSite) {
            if (isSafeSameSiteReaderNavigation(targetUrl, sourceUrl)) return false;
            return isHighConfidenceSameOriginRelay(
                    targetUrl, sourceUrl, compatibilityOrReaderContext);
        }

        boolean hardSignal = ShieldUrlRules.isKnownAdHost(targetHost)
                || ShieldUrlRules.hasHardAdToken(targetUrl)
                || legacySuspicious;
        if (hardSignal) return true;

        if (isDownloadPage(sourceUrl)
                && isSafeDownloadNavigation(targetUrl, sourceUrl, hasGesture)) {
            return false;
        }

        if (compatibilityOrReaderContext && isPopupIsolationContentPage(sourceUrl)) {
            return true;
        }

        return compatibilityOrReaderContext
                && ShieldUrlRules.isCheapAdHost(targetHost)
                && (ShieldUrlRules.hasOpaquePath(targetUrl)
                || ShieldUrlRules.hasRedirectParameter(targetUrl));
    }

    static boolean shouldBlockSubresource(String resourceUrl,
                                          String pageUrl,
                                          boolean legacyHardAd) {
        if (!ShieldUrlRules.isHttpOrHttps(resourceUrl)
                || !ShieldUrlRules.isHttpOrHttps(pageUrl)) return false;

        String resourceHost = ShieldUrlRules.hostOf(resourceUrl);
        String pageHost = ShieldUrlRules.hostOf(pageUrl);
        if (resourceHost.isEmpty() || pageHost.isEmpty()) return false;

        boolean directAsset = ShieldUrlRules.isDirectContentAsset(resourceUrl);
        if (ShieldUrlRules.sameSite(resourceHost, pageHost)) {
            return ShieldUrlRules.hasHardAdToken(resourceUrl)
                    && ShieldUrlRules.isRelayPath(resourceUrl);
        }

        if (directAsset) {
            return ShieldUrlRules.isKnownAdHost(resourceHost)
                    || ShieldUrlRules.hasHardAdToken(resourceUrl);
        }

        return legacyHardAd
                || ShieldUrlRules.isKnownAdHost(resourceHost)
                || ShieldUrlRules.hasHardAdToken(resourceUrl);
    }

    static boolean isHighConfidenceSameOriginRelay(String targetUrl,
                                                    String sourceUrl,
                                                    boolean compatibilityOrReaderContext) {
        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)
                || !ShieldUrlRules.isHttpOrHttps(sourceUrl)) return false;

        String targetHost = ShieldUrlRules.hostOf(targetUrl);
        String sourceHost = ShieldUrlRules.hostOf(sourceUrl);
        if (targetHost.isEmpty() || sourceHost.isEmpty()
                || !ShieldUrlRules.sameSite(targetHost, sourceHost)) return false;
        if (ShieldUrlRules.isDirectContentAsset(targetUrl)) return false;

        String targetPath = ShieldUrlRules.pathOf(targetUrl);
        if (isSafeSameSiteReaderNavigation(targetUrl, sourceUrl)) return false;
        if (ShieldUrlRules.SAFE_CONTENT_PATH.matcher(targetPath).find()) return false;
        if (isDownloadPage(sourceUrl)
                && isCleanDownloadTarget(targetUrl)
                && !ShieldUrlRules.isRelayPath(targetUrl)) return false;

        int score = 0;
        if (ShieldUrlRules.RELAY_SEGMENT.matcher(targetPath).find()) score += 3;
        if (ShieldUrlRules.hasRedirectParameter(targetUrl)) score += 2;
        if (ShieldUrlRules.hasHardAdToken(targetUrl)) score += 2;
        if (ShieldUrlRules.OPAQUE_SEGMENT.matcher(targetPath).find()) score += 1;
        if (compatibilityOrReaderContext) score += 2;
        if (ReaderCompatibilityPolicy.hasReaderPathHint(sourceUrl)) score += 1;

        return score >= (compatibilityOrReaderContext ? 4 : 6);
    }

    static boolean isSafeSameSiteReaderNavigation(String targetUrl, String sourceUrl) {
        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)
                || !ShieldUrlRules.isHttpOrHttps(sourceUrl)
                || isDownloadPage(sourceUrl)) return false;

        String targetHost = ShieldUrlRules.hostOf(targetUrl);
        String sourceHost = ShieldUrlRules.hostOf(sourceUrl);
        if (targetHost.isEmpty() || sourceHost.isEmpty()
                || !ShieldUrlRules.sameSite(targetHost, sourceHost)) return false;
        if (ShieldUrlRules.isDirectContentAsset(targetUrl)
                || ShieldUrlRules.isRelayPath(targetUrl)
                || ShieldUrlRules.hasHardAdToken(targetUrl)) return false;

        String targetPath = ShieldUrlRules.pathOf(targetUrl);
        boolean targetReader = ReaderCompatibilityPolicy.hasReaderPathHint(targetUrl)
                || ShieldUrlRules.SAFE_CONTENT_PATH.matcher(targetPath).find();
        if (!targetReader) return false;

        String sourcePath = ShieldUrlRules.pathOf(sourceUrl);
        return ReaderCompatibilityPolicy.hasReaderPathHint(sourceUrl)
                || ShieldUrlRules.SAFE_CONTENT_PATH.matcher(sourcePath).find();
    }

    static boolean isDownloadPage(String url) {
        return ShieldUrlRules.isHttpOrHttps(url)
                && !isSearchResultsPage(url)
                && ShieldUrlRules.DOWNLOAD_PAGE_PATH
                .matcher(ShieldUrlRules.pathOf(url)).find();
    }

    static boolean isSafeDownloadNavigation(String targetUrl,
                                            String sourceUrl,
                                            boolean hasGesture) {
        if (!hasGesture || !isDownloadPage(sourceUrl)
                || !ShieldUrlRules.isHttpOrHttps(targetUrl)
                || !isCleanDownloadTarget(targetUrl)) return false;

        String targetHost = ShieldUrlRules.hostOf(targetUrl);
        String sourceHost = ShieldUrlRules.hostOf(sourceUrl);
        if (targetHost.isEmpty() || sourceHost.isEmpty()) return false;
        if (ShieldUrlRules.sameSite(targetHost, sourceHost)) return true;

        return ShieldUrlRules.DOWNLOAD_TARGET_HINT
                .matcher(ShieldUrlRules.decodedLower(targetUrl)).find();
    }

    private static boolean isCleanDownloadTarget(String targetUrl) {
        String host = ShieldUrlRules.hostOf(targetUrl);
        return ShieldUrlRules.isHttpOrHttps(targetUrl)
                && !host.isEmpty()
                && !ShieldUrlRules.isKnownAdHost(host)
                && !ShieldUrlRules.isCheapAdHost(host)
                && !ShieldUrlRules.hasHardAdToken(targetUrl)
                && !ShieldUrlRules.hasRedirectParameter(targetUrl)
                && !ShieldUrlRules.isRelayPath(targetUrl);
    }

    static boolean isKnownAdOrTrackerUrl(String url) {
        String host = ShieldUrlRules.hostOf(url);
        return (!host.isEmpty() && ShieldUrlRules.isKnownAdHost(host))
                || ShieldUrlRules.hasHardAdToken(url);
    }

    static boolean isSearchResultsPage(String url) {
        if (!ShieldUrlRules.isHttpOrHttps(url)) return false;
        String host = ShieldUrlRules.hostOf(url);
        if (host.isEmpty()) return false;

        String path = ShieldUrlRules.pathOf(url);
        String query = ShieldUrlRules.queryOf(url);
        boolean hasSearchQuery = ShieldUrlRules.SEARCH_QUERY_HINT.matcher(query).find();
        boolean hasSearchPath = ShieldUrlRules.SEARCH_PATH_HINT.matcher(path).find();
        boolean knownSearchHost = ShieldUrlRules.KNOWN_SEARCH_HOST.matcher(host).find();

        return (knownSearchHost && (hasSearchQuery || hasSearchPath))
                || (hasSearchPath && hasSearchQuery);
    }

    static boolean isReaderOrContentPage(String url) {
        if (!ShieldUrlRules.isHttpOrHttps(url)
                || isSearchResultsPage(url)
                || isDownloadPage(url)) return false;

        String path = ShieldUrlRules.pathOf(url);
        return ShieldUrlRules.SAFE_CONTENT_PATH.matcher(path).find()
                || ReaderCompatibilityPolicy.hasReaderPathHint(url);
    }

    static boolean isPopupIsolationContentPage(String url) {
        if (!ShieldUrlRules.isHttpOrHttps(url) || isSearchResultsPage(url)) return false;

        String host = ShieldUrlRules.hostOf(url);
        if (host.isEmpty()) return false;
        if (ShieldUrlRules.TRUSTED_VIDEO_HOST.matcher(host).find()) return false;
        if (isDownloadPage(url) || isReaderOrContentPage(url)) return true;

        return ShieldUrlRules.POPUP_ISOLATED_VIDEO_PATH
                .matcher(ShieldUrlRules.pathOf(url)).find();
    }

    static boolean shouldUseLegacyClickGuard(String pageUrl, boolean clickHijackEnabled) {
        return clickHijackEnabled && !isPopupIsolationContentPage(pageUrl);
    }
}
