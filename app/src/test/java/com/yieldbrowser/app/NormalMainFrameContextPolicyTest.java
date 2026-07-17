package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NormalMainFrameContextPolicyTest {
    @Test
    public void earlyAllowAcceptsSameSiteSearchOrContextNavigation() {
        assertTrue(NormalMainFrameContextPolicy.isEarlyAllowed(true, false, false));
        assertTrue(NormalMainFrameContextPolicy.isEarlyAllowed(false, true, false));
        assertTrue(NormalMainFrameContextPolicy.isEarlyAllowed(false, false, true));
        assertFalse(NormalMainFrameContextPolicy.isEarlyAllowed(false, false, false));
    }

    @Test
    public void unknownCrossSiteRequiresCleanGesture() {
        assertTrue(NormalMainFrameContextPolicy.allowUnknownCrossSite(
                true, false, false, false));
        assertFalse(NormalMainFrameContextPolicy.allowUnknownCrossSite(
                false, false, false, false));
        assertFalse(NormalMainFrameContextPolicy.allowUnknownCrossSite(
                true, true, false, false));
        assertFalse(NormalMainFrameContextPolicy.allowUnknownCrossSite(
                true, false, true, false));
        assertFalse(NormalMainFrameContextPolicy.allowUnknownCrossSite(
                true, false, false, true));
    }
}
