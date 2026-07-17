package com.yieldbrowser.app;

/** Pure early decisions for compatibility-mode main-frame ad navigation checks. */
final class CompatibilityAdNavigationPreflightPolicy {
    static final class Decision {
        final boolean resolved;
        final boolean block;

        private Decision(boolean resolved, boolean block) {
            this.resolved = resolved;
            this.block = block;
        }
    }

    private CompatibilityAdNavigationPreflightPolicy() {
    }

    static Decision initial(boolean adBlockEnabled,
                            boolean targetHttpOrHttps,
                            boolean externalScheme) {
        if (!adBlockEnabled || !targetHttpOrHttps) {
            return new Decision(true, externalScheme);
        }
        return new Decision(false, false);
    }

    static boolean isExplicitlyAllowed(boolean trustedDownloadIntent,
                                       boolean searchResultNavigation) {
        return trustedDownloadIntent || searchResultNavigation;
    }
}
