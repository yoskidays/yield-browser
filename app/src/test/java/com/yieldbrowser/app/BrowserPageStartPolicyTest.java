package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserPageStartPolicyTest {
    @Test
    public void choosesExtractedUrlAndSafeReferenceWithoutChangingFallbackOrder() {
        assertEquals("https://original.example/page",
                BrowserPageStartPolicy.chooseStartedUrl(
                        "https://original.example/page", "https://proxy.example/page"));
        assertEquals("https://proxy.example/page",
                BrowserPageStartPolicy.chooseStartedUrl(null, "https://proxy.example/page"));

        assertEquals("https://tab.example/page",
                BrowserPageStartPolicy.chooseSafeReference(
                        "https://tab.example/page", "https://global.example/page"));
        assertEquals("https://global.example/page",
                BrowserPageStartPolicy.chooseSafeReference("", "https://global.example/page"));
        assertEquals("", BrowserPageStartPolicy.chooseSafeReference(null, null));
    }

    @Test
    public void transientBlankHasPriorityOverShieldRestore() {
        assertEquals(BrowserPageStartPolicy.EarlyAction.RESTORE_TRANSIENT_BLANK,
                BrowserPageStartPolicy.earlyAction(true, true, true));
        assertEquals(BrowserPageStartPolicy.EarlyAction.RESTORE_SHIELD_BLOCK,
                BrowserPageStartPolicy.earlyAction(false, true, true));
        assertEquals(BrowserPageStartPolicy.EarlyAction.CONTINUE,
                BrowserPageStartPolicy.earlyAction(true, false, false));
    }

    @Test
    public void selectsNormalStrictAndGuardedProfiles() {
        assertEquals(BrowserPageStartPolicy.Profile.STRICT_COMPATIBILITY,
                BrowserPageStartPolicy.profile(true, true, true));
        assertEquals(BrowserPageStartPolicy.Profile.GUARDED_COMPATIBILITY,
                BrowserPageStartPolicy.profile(false, true, false));
        assertEquals(BrowserPageStartPolicy.Profile.GUARDED_COMPATIBILITY,
                BrowserPageStartPolicy.profile(false, false, true));
        assertEquals(BrowserPageStartPolicy.Profile.NORMAL,
                BrowserPageStartPolicy.profile(false, false, false));
    }

    @Test
    public void preservesViewportRetrySchedule() {
        assertArrayEquals(new long[]{250L},
                BrowserPageStartPolicy.viewportDelays(
                        BrowserPageStartPolicy.Profile.NORMAL, false));
        assertArrayEquals(new long[]{250L},
                BrowserPageStartPolicy.viewportDelays(
                        BrowserPageStartPolicy.Profile.NORMAL, true));
        assertArrayEquals(new long[]{280L},
                BrowserPageStartPolicy.viewportDelays(
                        BrowserPageStartPolicy.Profile.GUARDED_COMPATIBILITY, false));
        assertArrayEquals(new long[]{280L, 1200L, 2600L},
                BrowserPageStartPolicy.viewportDelays(
                        BrowserPageStartPolicy.Profile.STRICT_COMPATIBILITY, true));
    }

    @Test
    public void hidesProgressOnlyForCompatibilityProfiles() {
        assertFalse(BrowserPageStartPolicy.shouldHideProgress(
                BrowserPageStartPolicy.Profile.NORMAL));
        assertTrue(BrowserPageStartPolicy.shouldHideProgress(
                BrowserPageStartPolicy.Profile.STRICT_COMPATIBILITY));
        assertTrue(BrowserPageStartPolicy.shouldHideProgress(
                BrowserPageStartPolicy.Profile.GUARDED_COMPATIBILITY));
    }

    @Test
    public void choosesNavigationReferenceWithoutChangingFallbackOrder() {
        assertEquals("https://safe.example/page",
                BrowserPageStartPolicy.chooseNavigationReference(
                        "https://safe.example/page",
                        "https://tab.example/page",
                        "https://current.example/page"));
        assertEquals("https://tab.example/page",
                BrowserPageStartPolicy.chooseNavigationReference(
                        "", "https://tab.example/page", "https://current.example/page"));
        assertEquals("https://current.example/page",
                BrowserPageStartPolicy.chooseNavigationReference(
                        null, "", "https://current.example/page"));
        assertEquals("",
                BrowserPageStartPolicy.chooseNavigationReference(null, null, null));
    }

    @Test
    public void compatibilityFlowPreservesTrustedReaderNavigation() {
        assertTrue(BrowserPageStartPolicy.isCompatibilityFlow(true, false));
        assertTrue(BrowserPageStartPolicy.isCompatibilityFlow(false, true));
        assertFalse(BrowserPageStartPolicy.isCompatibilityFlow(false, false));

        assertFalse(BrowserPageStartPolicy.shouldRestoreDirectImage(
                true, false, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreDirectImage(
                false, true, true));
        assertTrue(BrowserPageStartPolicy.shouldRestoreDirectImage(
                false, false, true));
    }

    @Test
    public void externalSchemesAlwaysRestoreTheSafePage() {
        assertTrue(BrowserPageStartPolicy.shouldRestoreExternalScheme(true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreExternalScheme(false));
    }

    @Test
    public void redirectRecoveryRequiresEveryBlockingGuard() {
        assertTrue(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, true, true, false, false, true, false));
        assertTrue(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, true, true, false, false, false, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreRedirect(
                true, true, true, false, false, true, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, false, true, false, false, true, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, true, false, false, false, true, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, true, true, true, false, true, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, true, true, false, true, true, true));
        assertFalse(BrowserPageStartPolicy.shouldRestoreRedirect(
                false, true, true, false, false, false, false));
    }
}
