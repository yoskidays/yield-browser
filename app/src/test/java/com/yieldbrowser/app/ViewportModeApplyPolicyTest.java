package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ViewportModeApplyPolicyTest {
    @Test
    public void missingWebViewSelectsNone() {
        assertEquals(
                ViewportModeApplyPolicy.Mode.NONE,
                ViewportModeApplyPolicy.mode(false, false));
        assertEquals(
                ViewportModeApplyPolicy.Mode.NONE,
                ViewportModeApplyPolicy.mode(false, true));
    }

    @Test
    public void availableWebViewSelectsMobileOrDesktop() {
        assertEquals(
                ViewportModeApplyPolicy.Mode.MOBILE,
                ViewportModeApplyPolicy.mode(true, false));
        assertEquals(
                ViewportModeApplyPolicy.Mode.DESKTOP,
                ViewportModeApplyPolicy.mode(true, true));
    }
}
