package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowserWebErrorPolicyTest {
    @Test
    public void subresourceErrorsRemainWithWebView() {
        assertEquals(BrowserWebErrorPolicy.Action.DELEGATE_TO_WEBVIEW,
                BrowserWebErrorPolicy.decide(
                        false, true, true, true, "unknown_url_scheme",
                        true, false, true, true, true, true));
    }

    @Test
    public void compatibilityMainFrameKeepsOriginalErrorPage() {
        assertEquals(BrowserWebErrorPolicy.Action.KEEP_CURRENT_PAGE,
                BrowserWebErrorPolicy.decide(
                        true, true, true, true, "unknown_url_scheme",
                        true, false, true, true, true, true));
    }

    @Test
    public void unsupportedAndUnknownSchemesRestoreSafePage() {
        assertEquals(BrowserWebErrorPolicy.Action.RESTORE_BLOCKED_NAVIGATION,
                BrowserWebErrorPolicy.decide(
                        true, false, false, true, "",
                        false, true, false, false, false, false));
        assertEquals(BrowserWebErrorPolicy.Action.RESTORE_BLOCKED_NAVIGATION,
                BrowserWebErrorPolicy.decide(
                        true, false, false, false, "  UNKNOWN_URL_SCHEME  ",
                        false, true, false, false, false, false));
    }

    @Test
    public void suspiciousAdFailureRestoresOnlyWhenUntrustedAndAdBlockEnabled() {
        assertEquals(BrowserWebErrorPolicy.Action.RESTORE_BLOCKED_NAVIGATION,
                BrowserWebErrorPolicy.decide(
                        true, false, false, false, "",
                        true, false, true, false, false, false));
        assertEquals(BrowserWebErrorPolicy.Action.DELEGATE_TO_WEBVIEW,
                BrowserWebErrorPolicy.decide(
                        true, false, false, false, "",
                        true, true, true, true, true, true));
        assertEquals(BrowserWebErrorPolicy.Action.DELEGATE_TO_WEBVIEW,
                BrowserWebErrorPolicy.decide(
                        true, false, false, false, "",
                        false, false, true, true, true, true));
    }

    @Test
    public void ordinaryMainFrameFailureIsDelegated() {
        assertEquals(BrowserWebErrorPolicy.Action.DELEGATE_TO_WEBVIEW,
                BrowserWebErrorPolicy.decide(
                        true, false, false, false, "net::err_name_not_resolved",
                        true, false, false, false, false, false));
    }
}
