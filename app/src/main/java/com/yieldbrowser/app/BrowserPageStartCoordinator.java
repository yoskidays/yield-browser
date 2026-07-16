package com.yieldbrowser.app;

import android.webkit.WebView;

/**
 * Prepares the stable, early portion of WebViewClient#onPageStarted while the
 * activity keeps history, navigation and Shield side effects that run later.
 */
final class BrowserPageStartCoordinator {
    interface TabLookup {
        TabInfo find(WebView view);
    }

    interface TabSupplier {
        TabInfo get();
    }

    interface TabUrlLookup {
        String get(TabInfo tab);
    }

    interface UrlMapper {
        String map(String url);
    }

    interface UrlPredicate {
        boolean test(String url);
    }

    interface UrlPairPredicate {
        boolean test(String targetUrl, String sourceUrl);
    }

    interface ShieldDecision {
        boolean shouldBlock(String targetUrl, String sourceUrl,
                            boolean hasGesture, boolean legacySuspicious);
    }

    interface RestoreCallback {
        void restore(WebView view, String url);
    }

    interface CompatibilityRegistrar {
        boolean register(String url);
    }

    interface DelayScheduler {
        void schedule(long delayMs);
    }

    static final class Preparation {
        final boolean inactiveView;
        final boolean restored;
        final TabInfo owner;
        final String safeReferenceUrl;
        final String startedUrl;
        final BrowserPageStartPolicy.Profile profile;

        private Preparation(boolean inactiveView,
                            boolean restored,
                            TabInfo owner,
                            String safeReferenceUrl,
                            String startedUrl,
                            BrowserPageStartPolicy.Profile profile) {
            this.inactiveView = inactiveView;
            this.restored = restored;
            this.owner = owner;
            this.safeReferenceUrl = safeReferenceUrl == null ? "" : safeReferenceUrl;
            this.startedUrl = startedUrl == null ? "" : startedUrl;
            this.profile = profile == null
                    ? BrowserPageStartPolicy.Profile.NORMAL
                    : profile;
        }
    }

    private BrowserPageStartCoordinator() {
    }

    static Preparation prepare(WebView activeWebView,
                               WebView view,
                               String rawUrl,
                               String lastSafeHttpUrl,
                               TabLookup tabLookup,
                               TabSupplier currentTabSupplier,
                               TabUrlLookup tabUrlLookup,
                               UrlMapper originalUrlMapper,
                               UrlPredicate transientBlankPredicate,
                               UrlPredicate readerOrCompatibilityPredicate,
                               UrlPredicate knownPopupPredicate,
                               UrlPredicate likelyAdClickPredicate,
                               UrlPredicate adUrlPredicate,
                               UrlPairPredicate suspiciousPopupPredicate,
                               ShieldDecision shieldDecision,
                               RestoreCallback restoreCallback,
                               UrlPredicate strictCompatibilityPredicate,
                               CompatibilityRegistrar reloadLoopRegistrar,
                               UrlPredicate siteCompatibilityPredicate) {
        String extractedUrl = originalUrlMapper == null
                ? null
                : originalUrlMapper.map(rawUrl);
        String startedUrl = BrowserPageStartPolicy.chooseStartedUrl(extractedUrl, rawUrl);

        if (view != activeWebView) {
            TabInfo inactiveOwner = tabLookup == null ? null : tabLookup.find(view);
            if (inactiveOwner != null) {
                inactiveOwner.currentPageUrlForRequest = startedUrl;
            }
            return new Preparation(true, false, inactiveOwner, "", startedUrl,
                    BrowserPageStartPolicy.Profile.NORMAL);
        }

        TabInfo owner = tabLookup == null ? null : tabLookup.find(view);
        if (owner == null && currentTabSupplier != null) {
            owner = currentTabSupplier.get();
        }
        String tabReference = tabUrlLookup == null ? "" : tabUrlLookup.get(owner);
        String safeReference = BrowserPageStartPolicy.chooseSafeReference(
                tabReference, lastSafeHttpUrl);

        boolean transientBlank = test(transientBlankPredicate, startedUrl);
        boolean readerOrCompatibility = test(
                readerOrCompatibilityPredicate, safeReference);
        boolean legacySuspicious = test(knownPopupPredicate, startedUrl)
                || test(likelyAdClickPredicate, startedUrl)
                || test(adUrlPredicate, startedUrl)
                || test(suspiciousPopupPredicate, startedUrl, safeReference);
        boolean shieldShouldBlock = shieldDecision != null
                && shieldDecision.shouldBlock(
                startedUrl, safeReference, false, legacySuspicious);

        BrowserPageStartPolicy.EarlyAction earlyAction =
                BrowserPageStartPolicy.earlyAction(
                        transientBlank,
                        readerOrCompatibility,
                        shieldShouldBlock);
        if (earlyAction != BrowserPageStartPolicy.EarlyAction.CONTINUE) {
            if (restoreCallback != null) restoreCallback.restore(view, startedUrl);
            return new Preparation(false, true, owner, safeReference, startedUrl,
                    BrowserPageStartPolicy.Profile.NORMAL);
        }

        boolean strictCompatibility = test(strictCompatibilityPredicate, rawUrl);
        boolean reloadLoopGuarded = reloadLoopRegistrar != null
                && reloadLoopRegistrar.register(rawUrl);
        boolean siteCompatibility = test(siteCompatibilityPredicate, rawUrl);
        BrowserPageStartPolicy.Profile profile = BrowserPageStartPolicy.profile(
                strictCompatibility,
                reloadLoopGuarded,
                siteCompatibility);
        return new Preparation(false, false, owner, safeReference, startedUrl, profile);
    }

    static void applyProfile(Preparation preparation,
                             boolean desktopMode,
                             Runnable enableStrictCompatibility,
                             Runnable applyPlainCompatibilitySettings,
                             Runnable scheduleCompatibilityFallback,
                             Runnable applyBrowserSettings,
                             Runnable hideProgress,
                             DelayScheduler desktopViewportScheduler,
                             DelayScheduler mobileViewportScheduler) {
        if (preparation == null || preparation.inactiveView || preparation.restored) return;

        if (preparation.profile == BrowserPageStartPolicy.Profile.STRICT_COMPATIBILITY) {
            run(enableStrictCompatibility);
            run(applyPlainCompatibilitySettings);
            run(scheduleCompatibilityFallback);
        } else {
            run(applyBrowserSettings);
        }

        if (BrowserPageStartPolicy.shouldHideProgress(preparation.profile)) {
            run(hideProgress);
        }

        long[] delays = BrowserPageStartPolicy.viewportDelays(
                preparation.profile, desktopMode);
        DelayScheduler scheduler = desktopMode
                ? desktopViewportScheduler
                : mobileViewportScheduler;
        if (scheduler == null) return;
        for (long delay : delays) {
            scheduler.schedule(delay);
        }
    }

    private static boolean test(UrlPredicate predicate, String url) {
        return predicate != null && predicate.test(url);
    }

    private static boolean test(UrlPairPredicate predicate,
                                String targetUrl,
                                String sourceUrl) {
        return predicate != null && predicate.test(targetUrl, sourceUrl);
    }

    private static void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }
}
