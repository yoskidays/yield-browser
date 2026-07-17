package com.yieldbrowser.app;

/** Pure preflight gate for compatibility-mode third-party resource classification. */
final class CompatibilityThirdPartyResourcePreflightPolicy {
    private CompatibilityThirdPartyResourcePreflightPolicy() {
    }

    static boolean shouldClassify(boolean adBlockEnabled,
                                  boolean resourceHttpOrHttps,
                                  boolean pageHttpOrHttps,
                                  boolean trustedDownloadIntent,
                                  boolean youtubeCoreResource) {
        return adBlockEnabled
                && resourceHttpOrHttps
                && pageHttpOrHttps
                && !trustedDownloadIntent
                && !youtubeCoreResource;
    }
}
