package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuspiciousMainFrameContextPolicyTest {
    @Test
    public void nonSuspiciousNavigationIsRejectedImmediately() {
        SuspiciousMainFrameContextPolicy.Decision decision =
                SuspiciousMainFrameContextPolicy.beforeCompatibility(false, true, true);
        assertTrue(decision.resolved);
        assertFalse(decision.allow);
    }

    @Test
    public void suspiciousSameSiteOrSearchNavigationIsAllowedImmediately() {
        SuspiciousMainFrameContextPolicy.Decision sameSite =
                SuspiciousMainFrameContextPolicy.beforeCompatibility(true, true, false);
        SuspiciousMainFrameContextPolicy.Decision search =
                SuspiciousMainFrameContextPolicy.beforeCompatibility(true, false, true);
        assertTrue(sameSite.resolved);
        assertTrue(sameSite.allow);
        assertTrue(search.resolved);
        assertTrue(search.allow);
    }

    @Test
    public void suspiciousCrossSiteNavigationRequiresCompatibilityCheck() {
        SuspiciousMainFrameContextPolicy.Decision decision =
                SuspiciousMainFrameContextPolicy.beforeCompatibility(true, false, false);
        assertFalse(decision.resolved);
        assertFalse(decision.allow);
    }

    @Test
    public void crossSiteNavigationRequiresGestureAndCurrentHostOutsideCompatibility() {
        assertTrue(SuspiciousMainFrameContextPolicy.allowCrossSite(false, true, true));
        assertFalse(SuspiciousMainFrameContextPolicy.allowCrossSite(true, true, true));
        assertFalse(SuspiciousMainFrameContextPolicy.allowCrossSite(false, false, true));
        assertFalse(SuspiciousMainFrameContextPolicy.allowCrossSite(false, true, false));
    }
}
