package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TabWebViewLifecyclePolicyTest {
    @Test
    public void bindingRequiresOpenOwnerSameViewAndMatchingGeneration() {
        assertTrue(TabWebViewLifecyclePolicy.isBindingLive(
                true, false, true, 7L, 7L));
        assertFalse(TabWebViewLifecyclePolicy.isBindingLive(
                false, false, true, 7L, 7L));
        assertFalse(TabWebViewLifecyclePolicy.isBindingLive(
                true, true, true, 7L, 7L));
        assertFalse(TabWebViewLifecyclePolicy.isBindingLive(
                true, false, false, 7L, 7L));
        assertFalse(TabWebViewLifecyclePolicy.isBindingLive(
                true, false, true, 8L, 7L));
    }

    @Test
    public void recognizesOnlyRealPageUrlsAsLive() {
        assertTrue(TabWebViewLifecyclePolicy.isLivePageUrl("https://example.com/page"));
        assertTrue(TabWebViewLifecyclePolicy.isLivePageUrl("http://example.com"));
        assertFalse(TabWebViewLifecyclePolicy.isLivePageUrl(null));
        assertFalse(TabWebViewLifecyclePolicy.isLivePageUrl(""));
        assertFalse(TabWebViewLifecyclePolicy.isLivePageUrl("about:blank"));
        assertFalse(TabWebViewLifecyclePolicy.isLivePageUrl("DATA:text/html,hello"));
    }

    @Test
    public void calculatesSafeInsertionIndexAroundHomeAndOverlay() {
        assertEquals(0, TabWebViewLifecyclePolicy.insertionIndex(0, false, -1));
        assertEquals(3, TabWebViewLifecyclePolicy.insertionIndex(3, false, -1));
        assertEquals(1, TabWebViewLifecyclePolicy.insertionIndex(3, true, -1));
        assertEquals(2, TabWebViewLifecyclePolicy.insertionIndex(4, true, 2));
        assertEquals(4, TabWebViewLifecyclePolicy.insertionIndex(4, false, 9));
    }
}
