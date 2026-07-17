package com.yieldbrowser.app;

import java.util.Map;

/** Preserves the ordered side effects used by MainActivity#loadBrowserUrl. */
final class BrowserLoadExecutionCoordinator {
    interface UrlAction {
        void run(String url) throws Exception;
    }

    interface HeaderSupplier {
        Map<String, String> get() throws Exception;
    }

    interface HeaderLoadAction {
        void run(String url, Map<String, String> headers) throws Exception;
    }

    interface DelayScheduler {
        void schedule(long delayMs);
    }

    private BrowserLoadExecutionCoordinator() {
    }

    static void execute(String cleanUrl,
                        boolean strictCompatibility,
                        boolean desktopMode,
                        Runnable updateRequestState,
                        Runnable markTrustedNavigation,
                        Runnable prepareTabNavigation,
                        Runnable enableCompatibilityMode,
                        Runnable applyPlainCompatibilitySettings,
                        UrlAction compatibilityLoader,
                        DelayScheduler desktopViewportScheduler,
                        UrlAction compatibilityFallbackScheduler,
                        Runnable applyBrowserSettings,
                        HeaderSupplier headerSupplier,
                        HeaderLoadAction normalLoader,
                        UrlAction fallbackLoader) {
        try {
            run(updateRequestState);
            run(markTrustedNavigation);
            run(prepareTabNavigation);

            BrowserLoadExecutionPolicy.Profile profile =
                    BrowserLoadExecutionPolicy.profile(strictCompatibility);
            if (profile == BrowserLoadExecutionPolicy.Profile.STRICT_COMPATIBILITY) {
                run(enableCompatibilityMode);
                run(applyPlainCompatibilitySettings);
                if (compatibilityLoader != null) compatibilityLoader.run(cleanUrl);
                if (desktopViewportScheduler != null) {
                    for (long delay : BrowserLoadExecutionPolicy.desktopViewportDelays(
                            profile, desktopMode)) {
                        desktopViewportScheduler.schedule(delay);
                    }
                }
                if (compatibilityFallbackScheduler != null) {
                    compatibilityFallbackScheduler.run(cleanUrl);
                }
                return;
            }

            run(applyBrowserSettings);
            if (normalLoader != null) {
                Map<String, String> headers = headerSupplier == null
                        ? null : headerSupplier.get();
                normalLoader.run(cleanUrl, headers);
            }
        } catch (Exception ignored) {
            try {
                if (fallbackLoader != null) fallbackLoader.run(cleanUrl);
            } catch (Exception ignoredAgain) {
            }
        }
    }

    private static void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }
}
