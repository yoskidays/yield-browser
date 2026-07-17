package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserLoadExecutionPolicyTest {
    @Test
    public void selectsNormalAndStrictCompatibilityProfiles() {
        assertEquals(BrowserLoadExecutionPolicy.Profile.NORMAL,
                BrowserLoadExecutionPolicy.profile(false));
        assertEquals(BrowserLoadExecutionPolicy.Profile.STRICT_COMPATIBILITY,
                BrowserLoadExecutionPolicy.profile(true));
    }

    @Test
    public void preservesCompatibilityDesktopViewportDelays() {
        assertArrayEquals(new long[]{350L, 1300L, 2800L},
                BrowserLoadExecutionPolicy.desktopViewportDelays(
                        BrowserLoadExecutionPolicy.Profile.STRICT_COMPATIBILITY, true));
        assertArrayEquals(new long[0],
                BrowserLoadExecutionPolicy.desktopViewportDelays(
                        BrowserLoadExecutionPolicy.Profile.STRICT_COMPATIBILITY, false));
        assertArrayEquals(new long[0],
                BrowserLoadExecutionPolicy.desktopViewportDelays(
                        BrowserLoadExecutionPolicy.Profile.NORMAL, true));
    }

    @Test
    public void usesCustomHeadersOnlyForNormalProfile() {
        assertTrue(BrowserLoadExecutionPolicy.usesCustomHeaders(
                BrowserLoadExecutionPolicy.Profile.NORMAL));
        assertFalse(BrowserLoadExecutionPolicy.usesCustomHeaders(
                BrowserLoadExecutionPolicy.Profile.STRICT_COMPATIBILITY));
    }
}
