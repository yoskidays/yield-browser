package com.yieldbrowser.app;

import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Handles the mechanical WebView error flow while MainActivity supplies browser policies. */
final class BrowserWebErrorHandler {
    interface HttpsFailureHandler {
        boolean handle(WebView view, String failedUrl, int errorCode);
    }

    interface UrlPredicate {
        boolean test(String url);
    }

    interface UrlPairPredicate {
        boolean test(String targetUrl, String sourceUrl);
    }

    interface BlockedNavigationRestorer {
        void restore(WebView view, String failedUrl);
    }

    interface BooleanValue {
        boolean get();
    }

    private BrowserWebErrorHandler() {
    }

    static boolean handle(WebView activeWebView,
                          WebView view,
                          WebResourceRequest request,
                          WebResourceError error,
                          boolean adBlockEnabled,
                          String lastSafeHttpUrl,
                          HttpsFailureHandler httpsFailureHandler,
                          UrlPredicate compatibilityModePredicate,
                          UrlPredicate externalSchemePredicate,
                          UrlPredicate trustedNavigationPredicate,
                          UrlPredicate knownPopupPredicate,
                          UrlPredicate likelyAdClickPredicate,
                          UrlPredicate adUrlPredicate,
                          UrlPairPredicate suspiciousPopupPredicate,
                          BlockedNavigationRestorer restorer,
                          BooleanValue smoothTransitionActive,
                          Runnable finishSmoothTransition) {
        if (view != activeWebView) return false;

        String failedUrl = "";
        int errorCode = 0;
        String errorText = "";
        boolean mainFrame = false;
        try {
            failedUrl = request != null && request.getUrl() != null
                    ? request.getUrl().toString()
                    : "";
            mainFrame = request != null && request.isForMainFrame();
            if (Build.VERSION.SDK_INT >= 23 && error != null) {
                errorCode = error.getErrorCode();
            }
            if (error != null && error.getDescription() != null) {
                errorText = String.valueOf(error.getDescription());
            }
        } catch (Exception ignored) {
        }

        if (mainFrame && httpsFailureHandler != null
                && httpsFailureHandler.handle(view, failedUrl, errorCode)) {
            return true;
        }

        boolean compatibilityMode = test(compatibilityModePredicate, failedUrl);
        boolean externalScheme = test(externalSchemePredicate, failedUrl);
        boolean trustedNavigation = test(trustedNavigationPredicate, failedUrl);
        boolean knownPopup = test(knownPopupPredicate, failedUrl);
        boolean likelyAdClick = test(likelyAdClickPredicate, failedUrl);
        boolean adUrl = test(adUrlPredicate, failedUrl);
        boolean suspiciousPopup = suspiciousPopupPredicate != null
                && suspiciousPopupPredicate.test(failedUrl,
                lastSafeHttpUrl == null ? "" : lastSafeHttpUrl);

        BrowserWebErrorPolicy.Action action = BrowserWebErrorPolicy.decide(
                mainFrame,
                compatibilityMode,
                externalScheme,
                errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME,
                errorText,
                adBlockEnabled,
                trustedNavigation,
                knownPopup,
                likelyAdClick,
                adUrl,
                suspiciousPopup);

        if (action == BrowserWebErrorPolicy.Action.KEEP_CURRENT_PAGE) {
            return true;
        }
        if (action == BrowserWebErrorPolicy.Action.RESTORE_BLOCKED_NAVIGATION) {
            if (restorer != null) restorer.restore(view, failedUrl);
            return true;
        }

        if (smoothTransitionActive != null && smoothTransitionActive.get()
                && finishSmoothTransition != null) {
            finishSmoothTransition.run();
        }
        return false;
    }

    private static boolean test(UrlPredicate predicate, String url) {
        try {
            return predicate != null && predicate.test(url);
        } catch (Exception ignored) {
            return false;
        }
    }
}
