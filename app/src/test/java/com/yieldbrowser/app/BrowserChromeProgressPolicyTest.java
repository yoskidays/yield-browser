package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserChromeProgressPolicyTest {
    @Test
    public void ignoresCallbacksFromInactiveWebView() {
        BrowserChromeProgressPolicy.State state = BrowserChromeProgressPolicy.decide(
                false, false, true, 45);
        assertFalse(state.handled);
        assertEquals(0, state.progress);
        assertFalse(state.visible);
    }

    @Test
    public void resetsAndHidesProgressOnHomeOrHiddenWebView() {
        BrowserChromeProgressPolicy.State home = BrowserChromeProgressPolicy.decide(
                true, true, true, 45);
        assertTrue(home.handled);
        assertEquals(0, home.progress);
        assertFalse(home.visible);

        BrowserChromeProgressPolicy.State hidden = BrowserChromeProgressPolicy.decide(
                true, false, false, 70);
        assertTrue(hidden.handled);
        assertEquals(0, hidden.progress);
        assertFalse(hidden.visible);
    }

    @Test
    public void showsIncompleteProgressAndHidesCompletedProgress() {
        BrowserChromeProgressPolicy.State loading = BrowserChromeProgressPolicy.decide(
                true, false, true, 73);
        assertTrue(loading.handled);
        assertEquals(73, loading.progress);
        assertTrue(loading.visible);

        BrowserChromeProgressPolicy.State complete = BrowserChromeProgressPolicy.decide(
                true, false, true, 100);
        assertTrue(complete.handled);
        assertEquals(100, complete.progress);
        assertFalse(complete.visible);
    }
}
