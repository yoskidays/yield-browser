package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class TabInfoTest {
    @Test
    public void resetToHomeRemovesAllRestorableNavigationState() {
        TabInfo tab = new TabInfo("Page", "https://example.com/path", false);
        tab.webState = null;
        tab.trustedNavigationHost = "example.com";
        tab.trustedNavigationUntilMs = 99L;
        tab.domainSwitchAllowedUntilMs = 99L;

        tab.resetToHome();

        assertEquals("", tab.url);
        assertEquals("", tab.lastSafeUrl);
        assertEquals("", tab.currentPageUrlForRequest);
        assertEquals("", tab.isolationHost);
        assertEquals("", tab.trustedNavigationHost);
        assertEquals(0L, tab.trustedNavigationUntilMs);
        assertEquals(0L, tab.domainSwitchAllowedUntilMs);
        assertNull(tab.webState);
        assertFalse(tab.closed);
    }
}
