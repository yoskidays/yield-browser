package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BrowserPageFinishCoordinatorTest {
    @Test
    public void activeViewIsLeftForMainPageFinishFlow() {
        int[] callbacks = {0};

        assertFalse(BrowserPageFinishCoordinator.handleInactive(
                false,
                null,
                "https://example.com",
                view -> {
                    callbacks[0]++;
                    return null;
                },
                url -> {
                    callbacks[0]++;
                    return url;
                },
                (tab, url, title) -> callbacks[0]++,
                view -> {
                    callbacks[0]++;
                    return "Title";
                },
                (tab, view) -> callbacks[0]++));
        assertEquals(0, callbacks[0]);
    }

    @Test
    public void inactiveViewWithoutOwnerIsHandledWithoutTabMutation() {
        int[] mutations = {0};

        assertTrue(BrowserPageFinishCoordinator.handleInactive(
                true,
                null,
                "https://example.com",
                view -> null,
                url -> url,
                (tab, url, title) -> mutations[0]++,
                view -> "Title",
                (tab, view) -> mutations[0]++));
        assertEquals(0, mutations[0]);
    }

    @Test
    public void inactiveOwnerCommitsBeforeSavingItsWebState() {
        TabInfo owner = new TabInfo("Background", "", false);
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.handleInactive(
                true,
                null,
                "https://proxy.example/page",
                view -> owner,
                url -> "https://original.example/page",
                (tab, url, title) -> {
                    assertSame(owner, tab);
                    assertEquals("https://original.example/page", url);
                    assertEquals("Finished title", title);
                    calls.append("commit>");
                },
                view -> "Finished title",
                (tab, view) -> {
                    assertSame(owner, tab);
                    calls.append("state");
                }));

        assertEquals("commit>state", calls.toString());
    }
}
