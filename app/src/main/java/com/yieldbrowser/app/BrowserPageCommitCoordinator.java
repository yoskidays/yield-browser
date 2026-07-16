package com.yieldbrowser.app;

import android.webkit.WebView;

/** Handles stable URL and tab state updates at WebViewClient#onPageCommitVisible. */
final class BrowserPageCommitCoordinator {
    interface UrlMapper {
        String map(String url);
    }

    interface TabLookup {
        TabInfo find(WebView view);
    }

    interface TabSupplier {
        TabInfo get();
    }

    interface TabCommitter {
        void commit(TabInfo tab, String url, String title);
    }

    interface NavigationSuccessHandler {
        void handle(TabInfo tab, String url);
    }

    interface UrlConsumer {
        void accept(String url);
    }

    static final class Result {
        final boolean inactiveView;
        final TabInfo owner;
        final String finalUrl;

        private Result(boolean inactiveView, TabInfo owner, String finalUrl) {
            this.inactiveView = inactiveView;
            this.owner = owner;
            this.finalUrl = finalUrl;
        }
    }

    private BrowserPageCommitCoordinator() {
    }

    static Result handle(boolean activeView,
                         WebView view,
                         String rawUrl,
                         UrlMapper originalUrlMapper,
                         TabLookup tabLookup,
                         TabSupplier currentTabSupplier,
                         TabCommitter tabCommitter,
                         NavigationSuccessHandler navigationSuccessHandler,
                         UrlConsumer currentUrlUpdater,
                         UrlConsumer nightModeSettingsSync,
                         UrlConsumer nightModePageScheduler) {
        String extractedUrl = originalUrlMapper == null
                ? null
                : originalUrlMapper.map(rawUrl);
        String finalUrl = BrowserPageCommitPolicy.chooseFinalUrl(
                extractedUrl, rawUrl);
        TabInfo viewOwner = tabLookup == null ? null : tabLookup.find(view);

        if (!activeView) {
            if (viewOwner != null && tabCommitter != null) {
                tabCommitter.commit(viewOwner, finalUrl, viewOwner.title);
            }
            return new Result(true, viewOwner, finalUrl);
        }

        TabInfo currentOwner = viewOwner == null && currentTabSupplier != null
                ? currentTabSupplier.get()
                : null;
        TabInfo owner = BrowserPageCommitPolicy.chooseOwner(
                viewOwner, currentOwner);
        if (navigationSuccessHandler != null) {
            navigationSuccessHandler.handle(owner, finalUrl);
        }
        if (currentUrlUpdater != null) currentUrlUpdater.accept(finalUrl);
        if (owner != null) owner.currentPageUrlForRequest = finalUrl;
        if (nightModeSettingsSync != null) nightModeSettingsSync.accept(finalUrl);
        if (nightModePageScheduler != null) {
            nightModePageScheduler.accept(finalUrl);
        }
        return new Result(false, owner, finalUrl);
    }
}
