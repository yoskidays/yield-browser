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

    @Test
    public void activeStateUpdatesKeepOriginalOrderAndFallbackOwner() {
        TabInfo currentOwner = new TabInfo("Current", "", false);
        StringBuilder calls = new StringBuilder();

        BrowserPageFinishCoordinator.Result result =
                BrowserPageFinishCoordinator.handleActive(
                        null,
                        "https://proxy.example/page",
                        url -> "https://original.example/page",
                        view -> null,
                        () -> currentOwner,
                        (tab, url) -> calls.append("https>"),
                        url -> calls.append("current>"),
                        url -> calls.append("gesture>"),
                        url -> {
                            calls.append("history>");
                            return true;
                        },
                        (tab, url) -> {
                            calls.append("safe>");
                            return true;
                        },
                        url -> calls.append("lastSafe>"),
                        () -> {
                            calls.append("visible>");
                            return true;
                        },
                        url -> calls.append("address>"),
                        () -> calls.append("progress"));

        assertSame(currentOwner, result.owner);
        assertEquals("https://original.example/page", result.finalUrl);
        assertEquals("https://original.example/page",
                currentOwner.currentPageUrlForRequest);
        assertEquals(
                "https>current>gesture>history>safe>lastSafe>visible>address>progress",
                calls.toString());
    }

    @Test
    public void nonRecordableInvisiblePageSkipsSafeAndAddressUpdates() {
        TabInfo viewOwner = new TabInfo("View", "", false);
        int[] currentTabLookups = {0};
        StringBuilder calls = new StringBuilder();

        BrowserPageFinishCoordinator.Result result =
                BrowserPageFinishCoordinator.handleActive(
                        null,
                        "https://example.com/page",
                        url -> null,
                        view -> viewOwner,
                        () -> {
                            currentTabLookups[0]++;
                            return null;
                        },
                        null,
                        null,
                        null,
                        url -> false,
                        (tab, url) -> {
                            calls.append("safe>");
                            return true;
                        },
                        url -> calls.append("lastSafe>"),
                        () -> false,
                        url -> calls.append("address>"),
                        () -> calls.append("progress"));

        assertSame(viewOwner, result.owner);
        assertEquals(0, currentTabLookups[0]);
        assertEquals("progress", calls.toString());
    }

    @Test
    public void strictProfileRunsPlainSettingsAndCancelsTransition() {
        StringBuilder calls = new StringBuilder();

        BrowserPageFinishPolicy.Profile profile =
                BrowserPageFinishCoordinator.prepareProfile(
                        "https://strict.example/page",
                        url -> {
                            calls.append("strict>");
                            return true;
                        },
                        url -> {
                            throw new AssertionError("reload predicate must short-circuit");
                        },
                        url -> {
                            throw new AssertionError("site predicate must short-circuit");
                        },
                        () -> calls.append("plain>"),
                        () -> calls.append("cancel"));

        assertEquals(BrowserPageFinishPolicy.Profile.STRICT_COMPATIBILITY,
                profile);
        assertEquals("strict>plain>cancel", calls.toString());
    }

    @Test
    public void reloadGuardShortCircuitsSitePredicateWithoutStrictEffects() {
        StringBuilder calls = new StringBuilder();

        BrowserPageFinishPolicy.Profile profile =
                BrowserPageFinishCoordinator.prepareProfile(
                        "https://guarded.example/page",
                        url -> {
                            calls.append("strict>");
                            return false;
                        },
                        url -> {
                            calls.append("reload");
                            return true;
                        },
                        url -> {
                            throw new AssertionError("site predicate must short-circuit");
                        },
                        () -> calls.append("plain>"),
                        () -> calls.append("cancel"));

        assertEquals(BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                profile);
        assertEquals("strict>reload", calls.toString());
    }

    @Test
    public void ordinaryPageUsesNormalProfileWithoutCompatibilityEffects() {
        BrowserPageFinishPolicy.Profile profile =
                BrowserPageFinishCoordinator.prepareProfile(
                        "https://normal.example/page",
                        url -> false,
                        url -> false,
                        url -> false,
                        () -> {
                            throw new AssertionError("plain settings must not run");
                        },
                        () -> {
                            throw new AssertionError("transition must not cancel");
                        });

        assertEquals(BrowserPageFinishPolicy.Profile.NORMAL, profile);
    }
}
