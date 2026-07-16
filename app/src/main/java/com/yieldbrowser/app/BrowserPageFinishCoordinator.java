package com.yieldbrowser.app;

import android.os.Bundle;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;

/** Handles the isolated inactive-tab branch of WebViewClient#onPageFinished. */
final class BrowserPageFinishCoordinator {
    interface TabLookup {
        TabInfo find(WebView view);
    }

    interface UrlMapper {
        String map(String url);
    }

    interface TabCommitter {
        void commit(TabInfo tab, String url, String title);
    }

    interface TitleLookup {
        String get(WebView view);
    }

    interface WebStateSaver {
        void save(TabInfo tab, WebView view);
    }

    interface TabSupplier {
        TabInfo get();
    }

    interface NavigationSuccessHandler {
        void handle(TabInfo tab, String url);
    }

    interface UrlConsumer {
        void accept(String url);
    }

    interface UrlPredicate {
        boolean test(String url);
    }

    interface TabUrlPredicate {
        boolean test(TabInfo tab, String url);
    }

    interface BooleanSupplier {
        boolean get();
    }

    interface DelayScheduler {
        void schedule(long delayMs);
    }

    interface HistoryRecorder {
        void record(String title, String url);
    }

    interface IntSupplier {
        int get();
    }

    interface TokenDelayScheduler {
        void schedule(int token, long delayMs);
    }

    interface RunnableDelayScheduler {
        void schedule(Runnable action, long delayMs);
    }

    static final class Result {
        final TabInfo owner;
        final String finalUrl;

        private Result(TabInfo owner, String finalUrl) {
            this.owner = owner;
            this.finalUrl = finalUrl;
        }
    }

    private BrowserPageFinishCoordinator() {
    }

    static boolean handleInactive(boolean inactiveView,
                                  WebView view,
                                  String rawUrl,
                                  TabLookup tabLookup,
                                  UrlMapper originalUrlMapper,
                                  TabCommitter tabCommitter,
                                  TitleLookup titleLookup,
                                  WebStateSaver webStateSaver) {
        if (!inactiveView) return false;

        TabInfo owner = tabLookup == null ? null : tabLookup.find(view);
        if (owner != null) {
            String extractedUrl = originalUrlMapper == null
                    ? null
                    : originalUrlMapper.map(rawUrl);
            String finalUrl = BrowserPageFinishPolicy.chooseFinalUrl(
                    extractedUrl, rawUrl);
            String title = titleLookup == null ? null : titleLookup.get(view);
            if (tabCommitter != null) {
                tabCommitter.commit(owner, finalUrl, title);
            }
            if (webStateSaver != null) webStateSaver.save(owner, view);
        }
        return true;
    }

    static String getTitle(WebView view) {
        return view == null ? null : view.getTitle();
    }

    static void saveWebState(TabInfo owner, WebView view) {
        if (owner == null || view == null) return;
        try {
            Bundle state = new Bundle();
            WebBackForwardList saved = view.saveState(state);
            int savedSize = saved == null ? 0 : saved.getSize();
            if (BrowserPageFinishPolicy.shouldKeepWebState(
                    saved != null, savedSize)) {
                owner.webState = state;
            }
        } catch (Exception ignored) {
        }
    }

    static Result handleActive(WebView view,
                               String rawUrl,
                               UrlMapper originalUrlMapper,
                               TabLookup tabLookup,
                               TabSupplier currentTabSupplier,
                               NavigationSuccessHandler navigationSuccessHandler,
                               UrlConsumer currentUrlUpdater,
                               UrlConsumer horizontalGestureScheduler,
                               UrlPredicate historyPredicate,
                               TabUrlPredicate safeCommitPredicate,
                               UrlConsumer lastSafeUrlUpdater,
                               BooleanSupplier activePageVisible,
                               UrlConsumer addressBarUpdater,
                               Runnable hideProgress) {
        String extractedUrl = originalUrlMapper == null
                ? null
                : originalUrlMapper.map(rawUrl);
        String finalUrl = BrowserPageFinishPolicy.chooseFinalUrl(
                extractedUrl, rawUrl);
        TabInfo viewOwner = tabLookup == null ? null : tabLookup.find(view);
        TabInfo currentOwner = viewOwner == null && currentTabSupplier != null
                ? currentTabSupplier.get()
                : null;
        TabInfo owner = BrowserPageFinishPolicy.chooseOwner(
                viewOwner, currentOwner);

        if (navigationSuccessHandler != null) {
            navigationSuccessHandler.handle(owner, finalUrl);
        }
        if (currentUrlUpdater != null) currentUrlUpdater.accept(finalUrl);
        if (owner != null) owner.currentPageUrlForRequest = finalUrl;
        if (horizontalGestureScheduler != null) {
            horizontalGestureScheduler.accept(finalUrl);
        }

        boolean recordableHistoryUrl = historyPredicate != null
                && historyPredicate.test(finalUrl);
        boolean safeToCommit = recordableHistoryUrl
                && safeCommitPredicate != null
                && safeCommitPredicate.test(owner, finalUrl);
        if (BrowserPageFinishPolicy.shouldUpdateLastSafeUrl(
                recordableHistoryUrl, safeToCommit)
                && lastSafeUrlUpdater != null) {
            lastSafeUrlUpdater.accept(finalUrl);
        }

        boolean pageVisible = activePageVisible != null
                && activePageVisible.get();
        if (BrowserPageFinishPolicy.shouldUpdateAddressBar(pageVisible)
                && addressBarUpdater != null) {
            addressBarUpdater.accept(finalUrl);
        }
        if (hideProgress != null) hideProgress.run();
        return new Result(owner, finalUrl);
    }

    static BrowserPageFinishPolicy.Profile prepareProfile(
            String finalUrl,
            UrlPredicate strictCompatibilityPredicate,
            UrlPredicate reloadLoopPredicate,
            UrlPredicate siteCompatibilityPredicate,
            Runnable applyPlainCompatibilitySettings,
            Runnable cancelSmoothTransition) {
        boolean strictCompatibility = test(
                strictCompatibilityPredicate, finalUrl);
        boolean reloadLoopGuarded = false;
        boolean siteCompatibilityActive = false;
        if (strictCompatibility) {
            run(applyPlainCompatibilitySettings);
            run(cancelSmoothTransition);
        } else {
            reloadLoopGuarded = test(reloadLoopPredicate, finalUrl);
            if (!reloadLoopGuarded) {
                siteCompatibilityActive = test(
                        siteCompatibilityPredicate, finalUrl);
            }
        }
        return BrowserPageFinishPolicy.profile(
                strictCompatibility,
                reloadLoopGuarded,
                siteCompatibilityActive);
    }

    static boolean applyGuardedEffects(
            BrowserPageFinishPolicy.Profile profile,
            String finalUrl,
            boolean adBlockEnabled,
            boolean desktopMode,
            Runnable applyPlainCompatibilitySettings,
            UrlConsumer scheduleNightModeSync,
            Runnable injectCompatibilityShield,
            DelayScheduler compatibilityShieldScheduler,
            UrlConsumer scheduleReaderRepair,
            DelayScheduler desktopViewportScheduler,
            DelayScheduler mobileViewportScheduler) {
        if (!BrowserPageFinishPolicy.isReloadGuarded(profile)) return false;

        run(applyPlainCompatibilitySettings);
        if (scheduleNightModeSync != null) {
            scheduleNightModeSync.accept(finalUrl);
        }
        if (adBlockEnabled) {
            run(injectCompatibilityShield);
            if (compatibilityShieldScheduler != null) {
                for (long delay : BrowserPageFinishPolicy
                        .guardedShieldRetryDelays(true)) {
                    compatibilityShieldScheduler.schedule(delay);
                }
            }
        }
        if (scheduleReaderRepair != null) {
            scheduleReaderRepair.accept(finalUrl);
        }

        DelayScheduler viewportScheduler = desktopMode
                ? desktopViewportScheduler
                : mobileViewportScheduler;
        if (viewportScheduler != null) {
            for (long delay : BrowserPageFinishPolicy
                    .guardedViewportDelays(desktopMode)) {
                viewportScheduler.schedule(delay);
            }
        }
        return true;
    }

    static boolean applyNormalEffects(
            BrowserPageFinishPolicy.Profile profile,
            String finalUrl,
            boolean desktopMode,
            boolean readerMode,
            boolean adBlockEnabled,
            Runnable applyViewport,
            DelayScheduler viewportScheduler,
            DelayScheduler desktopViewportScheduler,
            Runnable injectReaderMode,
            Runnable injectInitialAdBlock,
            DelayScheduler adBlockRetryScheduler,
            UrlConsumer scheduleBlankRecovery,
            UrlConsumer scheduleReaderRepair,
            Runnable updateVideoControls) {
        if (BrowserPageFinishPolicy.isReloadGuarded(profile)) return false;

        run(applyViewport);
        if (viewportScheduler != null) {
            for (long delay : BrowserPageFinishPolicy
                    .normalViewportRetryDelays()) {
                viewportScheduler.schedule(delay);
            }
        }
        if (desktopViewportScheduler != null) {
            for (long delay : BrowserPageFinishPolicy
                    .normalDesktopViewportDelays(desktopMode)) {
                desktopViewportScheduler.schedule(delay);
            }
        }
        if (readerMode) run(injectReaderMode);
        if (adBlockEnabled) {
            run(injectInitialAdBlock);
            if (adBlockRetryScheduler != null) {
                for (long delay : BrowserPageFinishPolicy
                        .normalAdBlockRetryDelays(true)) {
                    adBlockRetryScheduler.schedule(delay);
                }
            }
        }
        if (scheduleBlankRecovery != null) {
            scheduleBlankRecovery.accept(finalUrl);
        }
        if (scheduleReaderRepair != null) {
            scheduleReaderRepair.accept(finalUrl);
        }
        run(updateVideoControls);
        return true;
    }

    static boolean applyUserFilterEffects(
            boolean hasUserFilters,
            Runnable applyUserFilters,
            DelayScheduler userFilterScheduler) {
        if (!hasUserFilters) return false;

        run(applyUserFilters);
        if (userFilterScheduler != null) {
            for (long delay : BrowserPageFinishPolicy
                    .userFilterRetryDelays(true)) {
                userFilterScheduler.schedule(delay);
            }
        }
        return true;
    }

    static TabInfo finalizeHistory(
            WebView view,
            TabInfo owner,
            TabSupplier currentTabSupplier,
            String finalUrl,
            UrlPredicate historyPredicate,
            HistoryRecorder historyRecorder,
            TitleLookup titleLookup,
            TabCommitter tabCommitter,
            Runnable saveTabsSession) {
        TabInfo currentOwner = owner == null && currentTabSupplier != null
                ? currentTabSupplier.get()
                : null;
        TabInfo resolvedOwner = BrowserPageFinishPolicy.chooseOwner(
                owner, currentOwner);

        boolean recordableForHistory = test(historyPredicate, finalUrl);
        if (BrowserPageFinishPolicy.shouldAddHistory(
                recordableForHistory, resolvedOwner)
                && historyRecorder != null) {
            String title = titleLookup == null ? null : titleLookup.get(view);
            historyRecorder.record(title, finalUrl);
        }

        boolean recordableForSession = test(historyPredicate, finalUrl);
        if (recordableForSession) {
            String title = titleLookup == null ? null : titleLookup.get(view);
            if (tabCommitter != null) {
                tabCommitter.commit(resolvedOwner, finalUrl, title);
            }
            run(saveTabsSession);
        }
        return resolvedOwner;
    }

    static boolean applyNormalCompletionEffects(
            BrowserPageFinishPolicy.Profile profile,
            String finalUrl,
            String rawUrl,
            Runnable resetManualVideoControls,
            Runnable injectVideoPlaybackWatcher,
            UrlConsumer scheduleNightModeSync,
            UrlConsumer detectTranslateProxyBlocked) {
        if (BrowserPageFinishPolicy.isReloadGuarded(profile)) return false;

        run(resetManualVideoControls);
        run(injectVideoPlaybackWatcher);
        if (scheduleNightModeSync != null) {
            scheduleNightModeSync.accept(finalUrl);
        }
        if (detectTranslateProxyBlocked != null) {
            detectTranslateProxyBlocked.accept(rawUrl);
        }
        return true;
    }

    static boolean applyTranslateToolbarEffects(
            BrowserPageFinishPolicy.Profile profile,
            boolean hideGoogleTranslateBar,
            String rawUrl,
            UrlPredicate translatedUrlPredicate,
            DelayScheduler hideToolbarScheduler) {
        if (BrowserPageFinishPolicy.isReloadGuarded(profile)
                || !hideGoogleTranslateBar
                || !test(translatedUrlPredicate, rawUrl)) {
            return false;
        }

        if (hideToolbarScheduler != null) {
            for (long delay : BrowserPageFinishPolicy
                    .translateToolbarHideDelays(true)) {
                hideToolbarScheduler.schedule(delay);
            }
        }
        return true;
    }

    static boolean applyCompatibleTranslateEffects(
            BrowserPageFinishPolicy.Profile profile,
            boolean translateEnabled,
            boolean compatibleTranslateActive,
            boolean translateManuallyDisabled,
            String rawUrl,
            UrlPredicate translatedUrlPredicate,
            IntSupplier translateSessionTokenSupplier,
            TokenDelayScheduler translateScheduler) {
        if (BrowserPageFinishPolicy.isReloadGuarded(profile)
                || !translateEnabled
                || !compatibleTranslateActive
                || translateManuallyDisabled
                || test(translatedUrlPredicate, rawUrl)) {
            return false;
        }

        int token = translateSessionTokenSupplier == null
                ? 0
                : translateSessionTokenSupplier.get();
        if (translateScheduler != null) {
            for (long delay : BrowserPageFinishPolicy
                    .compatibleTranslateRetryDelays(true)) {
                translateScheduler.schedule(token, delay);
            }
        }
        return true;
    }

    static boolean applyKeyboardEffects(
            boolean pendingHideKeyboard,
            Runnable blurAndHideKeyboard,
            Runnable clearPendingHide,
            RunnableDelayScheduler keyboardScheduler) {
        if (!pendingHideKeyboard) return false;

        run(blurAndHideKeyboard);
        if (keyboardScheduler != null) {
            long[] delays = BrowserPageFinishPolicy
                    .keyboardHideRetryDelays(true);
            for (int index = 0; index < delays.length; index++) {
                boolean clearAfterRun = index == delays.length - 1;
                keyboardScheduler.schedule(() -> {
                    run(blurAndHideKeyboard);
                    if (clearAfterRun) run(clearPendingHide);
                }, delays[index]);
            }
        }
        return true;
    }

    private static boolean test(UrlPredicate predicate, String url) {
        return predicate != null && predicate.test(url);
    }

    private static void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }
}
