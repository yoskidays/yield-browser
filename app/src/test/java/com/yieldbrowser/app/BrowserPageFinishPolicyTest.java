package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BrowserPageFinishPolicyTest {
    @Test
    public void extractedUrlKeepsPriorityOverRawUrl() {
        assertEquals("https://original.example/page",
                BrowserPageFinishPolicy.chooseFinalUrl(
                        "https://original.example/page",
                        "https://proxy.example/page"));
        assertEquals("https://proxy.example/page",
                BrowserPageFinishPolicy.chooseFinalUrl(
                        null, "https://proxy.example/page"));
    }

    @Test
    public void webStateRequiresANonEmptySavedHistory() {
        assertTrue(BrowserPageFinishPolicy.shouldKeepWebState(true, 1));
        assertTrue(BrowserPageFinishPolicy.shouldKeepWebState(true, 4));
        assertFalse(BrowserPageFinishPolicy.shouldKeepWebState(true, 0));
        assertFalse(BrowserPageFinishPolicy.shouldKeepWebState(false, 4));
    }

    @Test
    public void activeOwnerAndVisibleStatePreserveFallbackRules() {
        TabInfo viewOwner = new TabInfo("View", "", false);
        TabInfo currentOwner = new TabInfo("Current", "", false);

        assertSame(viewOwner,
                BrowserPageFinishPolicy.chooseOwner(viewOwner, currentOwner));
        assertSame(currentOwner,
                BrowserPageFinishPolicy.chooseOwner(null, currentOwner));
        assertTrue(BrowserPageFinishPolicy.shouldUpdateLastSafeUrl(true, true));
        assertFalse(BrowserPageFinishPolicy.shouldUpdateLastSafeUrl(true, false));
        assertFalse(BrowserPageFinishPolicy.shouldUpdateLastSafeUrl(false, true));
        assertTrue(BrowserPageFinishPolicy.shouldUpdateAddressBar(true));
        assertFalse(BrowserPageFinishPolicy.shouldUpdateAddressBar(false));
    }

    @Test
    public void pageFinishProfilesKeepStrictAndGuardedPriority() {
        assertEquals(BrowserPageFinishPolicy.Profile.STRICT_COMPATIBILITY,
                BrowserPageFinishPolicy.profile(true, true, true));
        assertEquals(BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                BrowserPageFinishPolicy.profile(false, true, false));
        assertEquals(BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                BrowserPageFinishPolicy.profile(false, false, true));
        assertEquals(BrowserPageFinishPolicy.Profile.NORMAL,
                BrowserPageFinishPolicy.profile(false, false, false));

        assertTrue(BrowserPageFinishPolicy.isReloadGuarded(
                BrowserPageFinishPolicy.Profile.STRICT_COMPATIBILITY));
        assertTrue(BrowserPageFinishPolicy.isReloadGuarded(
                BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY));
        assertFalse(BrowserPageFinishPolicy.isReloadGuarded(
                BrowserPageFinishPolicy.Profile.NORMAL));
    }

    @Test
    public void guardedEffectRetrySchedulesRemainStable() {
        assertArrayEquals(new long[]{900L, 2600L},
                BrowserPageFinishPolicy.guardedShieldRetryDelays(true));
        assertArrayEquals(new long[0],
                BrowserPageFinishPolicy.guardedShieldRetryDelays(false));
        assertArrayEquals(new long[]{350L, 1200L, 2600L},
                BrowserPageFinishPolicy.guardedViewportDelays(true));
        assertArrayEquals(new long[]{350L},
                BrowserPageFinishPolicy.guardedViewportDelays(false));
    }

    @Test
    public void normalEffectRetrySchedulesRemainStable() {
        assertArrayEquals(new long[]{600L, 1800L},
                BrowserPageFinishPolicy.normalViewportRetryDelays());
        assertArrayEquals(new long[]{350L, 1200L, 2600L},
                BrowserPageFinishPolicy.normalDesktopViewportDelays(true));
        assertArrayEquals(new long[0],
                BrowserPageFinishPolicy.normalDesktopViewportDelays(false));
        assertArrayEquals(new long[]{1800L, 5200L},
                BrowserPageFinishPolicy.normalAdBlockRetryDelays(true));
        assertArrayEquals(new long[0],
                BrowserPageFinishPolicy.normalAdBlockRetryDelays(false));
    }

    @Test
    public void userFilterRetryScheduleRemainsStable() {
        assertArrayEquals(new long[]{350L, 1400L},
                BrowserPageFinishPolicy.userFilterRetryDelays(true));
        assertArrayEquals(new long[0],
                BrowserPageFinishPolicy.userFilterRetryDelays(false));
    }

    @Test
    public void historyIsAddedOnlyForRecordablePublicTabs() {
        TabInfo publicTab = new TabInfo("Public", "", false);
        TabInfo privateTab = new TabInfo("Private", "", true);

        assertTrue(BrowserPageFinishPolicy.shouldAddHistory(true, publicTab));
        assertFalse(BrowserPageFinishPolicy.shouldAddHistory(true, privateTab));
        assertFalse(BrowserPageFinishPolicy.shouldAddHistory(true, null));
        assertFalse(BrowserPageFinishPolicy.shouldAddHistory(false, publicTab));
    }
}
