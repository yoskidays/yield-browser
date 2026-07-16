package com.yieldbrowser.app;

import java.util.Locale;

/** Pure decision table for main-frame WebView errors. */
final class BrowserWebErrorPolicy {
    enum Action {
        KEEP_CURRENT_PAGE,
        RESTORE_BLOCKED_NAVIGATION,
        DELEGATE_TO_WEBVIEW
    }

    private BrowserWebErrorPolicy() {
    }

    static Action decide(boolean mainFrame,
                         boolean compatibilityModeActive,
                         boolean externalScheme,
                         boolean unsupportedSchemeError,
                         String errorText,
                         boolean adBlockEnabled,
                         boolean trustedNavigation,
                         boolean knownPopupHost,
                         boolean likelyAdClick,
                         boolean adUrl,
                         boolean suspiciousPopupNavigation) {
        if (!mainFrame) return Action.DELEGATE_TO_WEBVIEW;
        if (compatibilityModeActive) return Action.KEEP_CURRENT_PAGE;

        boolean unknownScheme = normalizeErrorText(errorText)
                .contains("unknown_url_scheme");
        boolean blockedAdNavigation = adBlockEnabled
                && !trustedNavigation
                && (knownPopupHost || likelyAdClick || adUrl || suspiciousPopupNavigation);

        if (externalScheme || unsupportedSchemeError || unknownScheme || blockedAdNavigation) {
            return Action.RESTORE_BLOCKED_NAVIGATION;
        }
        return Action.DELEGATE_TO_WEBVIEW;
    }

    static String normalizeErrorText(String errorText) {
        return errorText == null ? "" : errorText.trim().toLowerCase(Locale.US);
    }
}
