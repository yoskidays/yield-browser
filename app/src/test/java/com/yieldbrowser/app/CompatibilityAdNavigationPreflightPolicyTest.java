package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompatibilityAdNavigationPreflightPolicyTest {
    @Test
    public void externalSchemeResolvesToBlockWhenAdBlockDisabled() {
        CompatibilityAdNavigationPreflightPolicy.Decision decision =
                CompatibilityAdNavigationPreflightPolicy.initial(false, false, true);
        assertTrue(decision.resolved);
        assertTrue(decision.block);
    }

    @Test
    public void nonExternalSchemeResolvesToAllowWhenTargetIsNotWeb() {
        CompatibilityAdNavigationPreflightPolicy.Decision decision =
                CompatibilityAdNavigationPreflightPolicy.initial(true, false, false);
        assertTrue(decision.resolved);
        assertFalse(decision.block);
    }

    @Test
    public void enabledWebNavigationContinuesToHostChecks() {
        CompatibilityAdNavigationPreflightPolicy.Decision decision =
                CompatibilityAdNavigationPreflightPolicy.initial(true, true, false);
        assertFalse(decision.resolved);
        assertFalse(decision.block);
    }

    @Test
    public void trustedDownloadsAndSearchResultsAreExplicitlyAllowed() {
        assertTrue(CompatibilityAdNavigationPreflightPolicy.isExplicitlyAllowed(true, false));
        assertTrue(CompatibilityAdNavigationPreflightPolicy.isExplicitlyAllowed(false, true));
        assertFalse(CompatibilityAdNavigationPreflightPolicy.isExplicitlyAllowed(false, false));
    }
}
