package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BrowserPageCommitCoordinatorTest {
    @Test
    public void inactiveViewCommitsItsOwnTabWithoutRunningActiveSideEffects() {
        TabInfo owner = new TabInfo("Background", "", false);
        int[] commits = {0};
        int[] activeEffects = {0};

        BrowserPageCommitCoordinator.Result result =
                BrowserPageCommitCoordinator.handle(
                        false,
                        null,
                        "https://proxy.example/page",
                        url -> "https://original.example/page",
                        view -> owner,
                        () -> new TabInfo("Current", "", false),
                        (tab, url, title) -> {
                            commits[0]++;
                            assertSame(owner, tab);
                            assertEquals("https://original.example/page", url);
                            assertEquals("Background", title);
                        },
                        (tab, url) -> activeEffects[0]++,
                        url -> activeEffects[0]++,
                        url -> activeEffects[0]++,
                        url -> activeEffects[0]++);

        assertTrue(result.inactiveView);
        assertSame(owner, result.owner);
        assertEquals("https://original.example/page", result.finalUrl);
        assertEquals(1, commits[0]);
        assertEquals(0, activeEffects[0]);
    }

    @Test
    public void activeViewFallsBackToCurrentTabAndRunsStateSyncInOrder() {
        TabInfo currentOwner = new TabInfo("Current", "", false);
        StringBuilder calls = new StringBuilder();

        BrowserPageCommitCoordinator.Result result =
                BrowserPageCommitCoordinator.handle(
                        true,
                        null,
                        "https://final.example/page",
                        url -> null,
                        view -> null,
                        () -> currentOwner,
                        (tab, url, title) -> calls.append("commit>"),
                        (tab, url) -> calls.append("https>"),
                        url -> calls.append("current>"),
                        url -> calls.append("settings>"),
                        url -> calls.append("page"));

        assertFalse(result.inactiveView);
        assertSame(currentOwner, result.owner);
        assertEquals("https://final.example/page", result.finalUrl);
        assertEquals("https>current>settings>page", calls.toString());
        assertEquals("https://final.example/page",
                currentOwner.currentPageUrlForRequest);
    }

    @Test
    public void activeViewKeepsTheWebViewOwnerInsteadOfCurrentTab() {
        TabInfo viewOwner = new TabInfo("View", "", false);
        TabInfo currentOwner = new TabInfo("Current", "", false);
        int[] currentTabLookups = {0};

        BrowserPageCommitCoordinator.Result result =
                BrowserPageCommitCoordinator.handle(
                        true,
                        null,
                        "https://final.example/page",
                        url -> null,
                        view -> viewOwner,
                        () -> {
                            currentTabLookups[0]++;
                            return currentOwner;
                        },
                        null,
                        null,
                        null,
                        null,
                        null);

        assertSame(viewOwner, result.owner);
        assertEquals("https://final.example/page",
                viewOwner.currentPageUrlForRequest);
        assertEquals("", currentOwner.currentPageUrlForRequest);
        assertEquals(0, currentTabLookups[0]);
    }

    @Test
    public void normalPageRunsFallbackCssAndFiltersInOriginalOrder() {
        StringBuilder calls = new StringBuilder();

        BrowserPageCommitCoordinator.applyEffects(
                "https://normal.example/page",
                true,
                url -> false,
                url -> false,
                () -> calls.append("sync>"),
                () -> calls.append("fallback>"),
                () -> calls.append("css>"),
                () -> calls.append("compat>"),
                url -> calls.append("repair>"),
                () -> true,
                () -> calls.append("filters"));

        assertEquals("sync>fallback>css>filters", calls.toString());
    }

    @Test
    public void compatibilityPageRunsTargetedShieldAndRepair() {
        StringBuilder calls = new StringBuilder();

        BrowserPageCommitCoordinator.applyEffects(
                "https://compat.example/page",
                true,
                url -> false,
                url -> true,
                () -> calls.append("sync>"),
                () -> calls.append("fallback>"),
                () -> calls.append("css>"),
                () -> calls.append("compat>"),
                url -> calls.append("repair"),
                () -> false,
                () -> calls.append("filters"));

        assertEquals("sync>fallback>compat>repair", calls.toString());
    }

    @Test
    public void compatibilityWithoutAdBlockStillSchedulesReaderRepair() {
        StringBuilder calls = new StringBuilder();

        BrowserPageCommitCoordinator.applyEffects(
                "https://compat.example/page",
                false,
                url -> true,
                url -> {
                    throw new AssertionError("site predicate must short-circuit");
                },
                () -> calls.append("sync>"),
                () -> calls.append("fallback>"),
                () -> calls.append("css>"),
                () -> calls.append("compat>"),
                url -> calls.append("repair"),
                () -> false,
                () -> calls.append("filters"));

        assertEquals("sync>repair", calls.toString());
    }
}
