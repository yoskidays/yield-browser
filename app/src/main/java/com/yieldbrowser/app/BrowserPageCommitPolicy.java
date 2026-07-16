package com.yieldbrowser.app;

/** Pure fallback decisions used during WebViewClient#onPageCommitVisible. */
final class BrowserPageCommitPolicy {
    static final class EffectPlan {
        final boolean compatibilityPage;
        final boolean injectShieldFallback;
        final boolean injectStandardCss;
        final boolean injectCompatibilityShield;
        final boolean scheduleCompatibilityRepair;

        private EffectPlan(boolean compatibilityPage,
                           boolean injectShieldFallback,
                           boolean injectStandardCss,
                           boolean injectCompatibilityShield,
                           boolean scheduleCompatibilityRepair) {
            this.compatibilityPage = compatibilityPage;
            this.injectShieldFallback = injectShieldFallback;
            this.injectStandardCss = injectStandardCss;
            this.injectCompatibilityShield = injectCompatibilityShield;
            this.scheduleCompatibilityRepair = scheduleCompatibilityRepair;
        }
    }

    private BrowserPageCommitPolicy() {
    }

    static String chooseFinalUrl(String extractedUrl, String rawUrl) {
        return extractedUrl != null ? extractedUrl : rawUrl;
    }

    static TabInfo chooseOwner(TabInfo viewOwner, TabInfo currentOwner) {
        return viewOwner != null ? viewOwner : currentOwner;
    }

    static EffectPlan effectPlan(boolean strictCompatibility,
                                 boolean siteCompatibility,
                                 boolean adBlockEnabled) {
        boolean compatibilityPage = strictCompatibility || siteCompatibility;
        return new EffectPlan(
                compatibilityPage,
                adBlockEnabled,
                adBlockEnabled && !compatibilityPage,
                adBlockEnabled && compatibilityPage,
                compatibilityPage);
    }

    static boolean shouldApplyUserFilters(boolean filtersAvailable) {
        return filtersAvailable;
    }
}
