package com.yieldbrowser.app;

/** Pure font-asset detection for compatibility-mode resource filtering. */
final class CompatibilityContentAssetPolicy {
    private CompatibilityContentAssetPolicy() {
    }

    static boolean isFontAsset(String normalizedUrl) {
        if (normalizedUrl == null) return false;
        return normalizedUrl.matches(".*\\.(?:woff2?|ttf|otf)(?:$|[?#]).*");
    }
}
