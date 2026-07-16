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

    @Test
    public void guardedDesktopEffectsKeepShieldAndViewportRetryOrder() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyGuardedEffects(
                BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                "https://guarded.example/page",
                true,
                true,
                () -> calls.append("plain>"),
                url -> calls.append("night>"),
                () -> calls.append("shield>"),
                delay -> calls.append("shield").append(delay).append('>'),
                url -> calls.append("repair>"),
                delay -> calls.append("desktop").append(delay).append('>'),
                delay -> calls.append("mobile").append(delay).append('>')));

        assertEquals(
                "plain>night>shield>shield900>shield2600>repair>"
                        + "desktop350>desktop1200>desktop2600>",
                calls.toString());
    }

    @Test
    public void guardedMobileWithoutAdBlockSkipsShieldButKeepsRepair() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyGuardedEffects(
                BrowserPageFinishPolicy.Profile.STRICT_COMPATIBILITY,
                "https://strict.example/page",
                false,
                false,
                () -> calls.append("plain>"),
                url -> calls.append("night>"),
                () -> calls.append("shield>"),
                delay -> calls.append("shieldRetry>"),
                url -> calls.append("repair>"),
                delay -> calls.append("desktop>"),
                delay -> calls.append("mobile").append(delay)));

        assertEquals("plain>night>repair>mobile350", calls.toString());
    }

    @Test
    public void normalProfileDoesNotRunGuardedEffects() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyGuardedEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                "https://normal.example/page",
                true,
                true,
                () -> calls[0]++,
                url -> calls[0]++,
                () -> calls[0]++,
                delay -> calls[0]++,
                url -> calls[0]++,
                delay -> calls[0]++,
                delay -> calls[0]++));
        assertEquals(0, calls[0]);
    }

    @Test
    public void normalDesktopEffectsKeepViewportReaderAndAdBlockOrder() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyNormalEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                "https://normal.example/page",
                true,
                true,
                true,
                () -> calls.append("viewport>"),
                delay -> calls.append("viewport").append(delay).append('>'),
                delay -> calls.append("desktop").append(delay).append('>'),
                () -> calls.append("reader>"),
                () -> calls.append("adInitial>"),
                delay -> calls.append("adRetry").append(delay).append('>'),
                url -> calls.append("blank>"),
                url -> calls.append("repair>"),
                () -> calls.append("video")));

        assertEquals(
                "viewport>viewport600>viewport1800>"
                        + "desktop350>desktop1200>desktop2600>"
                        + "reader>adInitial>adRetry1800>adRetry5200>"
                        + "blank>repair>video",
                calls.toString());
    }

    @Test
    public void normalMobileWithoutOptionalModesKeepsCoreEffects() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyNormalEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                "https://normal.example/page",
                false,
                false,
                false,
                () -> calls.append("viewport>"),
                delay -> calls.append("viewport").append(delay).append('>'),
                delay -> calls.append("desktop>"),
                () -> calls.append("reader>"),
                () -> calls.append("adInitial>"),
                delay -> calls.append("adRetry>"),
                url -> calls.append("blank>"),
                url -> calls.append("repair>"),
                () -> calls.append("video")));

        assertEquals(
                "viewport>viewport600>viewport1800>blank>repair>video",
                calls.toString());
    }

    @Test
    public void guardedProfileDoesNotRunNormalEffects() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyNormalEffects(
                BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                "https://guarded.example/page",
                true,
                true,
                true,
                () -> calls[0]++,
                delay -> calls[0]++,
                delay -> calls[0]++,
                () -> calls[0]++,
                () -> calls[0]++,
                delay -> calls[0]++,
                url -> calls[0]++,
                url -> calls[0]++,
                () -> calls[0]++));
        assertEquals(0, calls[0]);
    }

    @Test
    public void userFiltersRunImmediatelyAndAtBothRetryDelays() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyUserFilterEffects(
                true,
                () -> calls.append("apply>"),
                delay -> calls.append(delay).append('>')));

        assertEquals("apply>350>1400>", calls.toString());
    }

    @Test
    public void absentUserFiltersDoNotRunEffects() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyUserFilterEffects(
                false,
                () -> calls[0]++,
                delay -> calls[0]++));

        assertEquals(0, calls[0]);
    }

    @Test
    public void publicOwnerAddsHistoryBeforeCommitAndSessionSave() {
        TabInfo owner = new TabInfo("Public", "", false);
        StringBuilder calls = new StringBuilder();

        TabInfo result = BrowserPageFinishCoordinator.finalizeHistory(
                null,
                owner,
                () -> {
                    throw new AssertionError("current tab fallback must not run");
                },
                "https://example.com/page",
                url -> {
                    calls.append("record?>");
                    return true;
                },
                (title, url) -> calls.append("add>"),
                view -> {
                    calls.append("title>");
                    return "Example";
                },
                (tab, url, title) -> {
                    assertSame(owner, tab);
                    calls.append("commit>");
                },
                () -> calls.append("save"));

        assertSame(owner, result);
        assertEquals(
                "record?>title>add>record?>title>commit>save",
                calls.toString());
    }

    @Test
    public void privateFallbackSkipsHistoryButStillCommitsSession() {
        TabInfo privateTab = new TabInfo("Private", "", true);
        StringBuilder calls = new StringBuilder();

        TabInfo result = BrowserPageFinishCoordinator.finalizeHistory(
                null,
                null,
                () -> {
                    calls.append("current>");
                    return privateTab;
                },
                "https://example.com/private",
                url -> {
                    calls.append("record?>");
                    return true;
                },
                (title, url) -> calls.append("add>"),
                view -> {
                    calls.append("title>");
                    return "Private";
                },
                (tab, url, title) -> {
                    assertSame(privateTab, tab);
                    calls.append("commit>");
                },
                () -> calls.append("save"));

        assertSame(privateTab, result);
        assertEquals(
                "current>record?>record?>title>commit>save",
                calls.toString());
    }

    @Test
    public void unrecordableUrlKeepsBothHistoryChecksWithoutMutation() {
        TabInfo owner = new TabInfo("Public", "", false);
        StringBuilder calls = new StringBuilder();

        assertSame(owner, BrowserPageFinishCoordinator.finalizeHistory(
                null,
                owner,
                () -> null,
                "about:blank",
                url -> {
                    calls.append("record?>");
                    return false;
                },
                (title, url) -> calls.append("add>"),
                view -> {
                    calls.append("title>");
                    return "Blank";
                },
                (tab, url, title) -> calls.append("commit>"),
                () -> calls.append("save>")));

        assertEquals("record?>record?>", calls.toString());
    }

    @Test
    public void normalCompletionEffectsKeepVideoNightAndTranslateOrder() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyNormalCompletionEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                "https://original.example/page",
                "https://proxy.example/page",
                () -> calls.append("reset>"),
                () -> calls.append("watcher>"),
                url -> calls.append("night:").append(url).append('>'),
                url -> calls.append("translate:").append(url)));

        assertEquals(
                "reset>watcher>night:https://original.example/page>"
                        + "translate:https://proxy.example/page",
                calls.toString());
    }

    @Test
    public void guardedCompletionSkipsNormalEffects() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyNormalCompletionEffects(
                BrowserPageFinishPolicy.Profile.STRICT_COMPATIBILITY,
                "https://strict.example/page",
                "https://strict.example/page",
                () -> calls[0]++,
                () -> calls[0]++,
                url -> calls[0]++,
                url -> calls[0]++));

        assertEquals(0, calls[0]);
    }

    @Test
    public void translatedPageSchedulesEveryToolbarHideRetry() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyTranslateToolbarEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                true,
                "https://translate.example/page",
                url -> {
                    calls.append("translated?>");
                    return true;
                },
                delay -> calls.append(delay).append('>')));

        assertEquals(
                "translated?>250>800>1800>3500>6000>",
                calls.toString());
    }

    @Test
    public void disabledOrGuardedToolbarEffectsShortCircuitUrlCheck() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyTranslateToolbarEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                false,
                "https://translate.example/page",
                url -> {
                    calls[0]++;
                    return true;
                },
                delay -> calls[0]++));
        assertFalse(BrowserPageFinishCoordinator.applyTranslateToolbarEffects(
                BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                true,
                "https://translate.example/page",
                url -> {
                    calls[0]++;
                    return true;
                },
                delay -> calls[0]++));

        assertEquals(0, calls[0]);
    }

    @Test
    public void compatibleTranslateRetriesUseOneSessionTokenSnapshot() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyCompatibleTranslateEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                true,
                true,
                false,
                "https://example.com/page",
                url -> {
                    calls.append("translated?>");
                    return false;
                },
                () -> {
                    calls.append("token>");
                    return 42;
                },
                (token, delay) -> calls.append(token).append(':')
                        .append(delay).append('>')));

        assertEquals(
                "translated?>token>42:600>42:2200>",
                calls.toString());
    }

    @Test
    public void disabledOrGuardedCompatibleTranslateShortCircuitsUrlCheck() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyCompatibleTranslateEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                false,
                true,
                false,
                "https://example.com/page",
                url -> {
                    calls[0]++;
                    return false;
                },
                () -> ++calls[0],
                (token, delay) -> calls[0]++));
        assertFalse(BrowserPageFinishCoordinator.applyCompatibleTranslateEffects(
                BrowserPageFinishPolicy.Profile.GUARDED_COMPATIBILITY,
                true,
                true,
                false,
                "https://example.com/page",
                url -> {
                    calls[0]++;
                    return false;
                },
                () -> ++calls[0],
                (token, delay) -> calls[0]++));

        assertEquals(0, calls[0]);
    }

    @Test
    public void alreadyTranslatedPageDoesNotCaptureSessionToken() {
        int[] tokenCalls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyCompatibleTranslateEffects(
                BrowserPageFinishPolicy.Profile.NORMAL,
                true,
                true,
                false,
                "https://translate.example/page",
                url -> true,
                () -> ++tokenCalls[0],
                (token, delay) -> tokenCalls[0]++));

        assertEquals(0, tokenCalls[0]);
    }

    @Test
    public void keyboardPendingFlagClearsOnlyAfterFinalBlurRetry() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserPageFinishCoordinator.applyKeyboardEffects(
                true,
                () -> calls.append("blur>"),
                () -> calls.append("clear>"),
                (action, delay) -> {
                    calls.append("schedule").append(delay).append('>');
                    action.run();
                }));

        assertEquals(
                "blur>schedule250>blur>schedule900>blur>clear>",
                calls.toString());
    }

    @Test
    public void noPendingKeyboardHideSkipsAllEffects() {
        int[] calls = {0};

        assertFalse(BrowserPageFinishCoordinator.applyKeyboardEffects(
                false,
                () -> calls[0]++,
                () -> calls[0]++,
                (action, delay) -> calls[0]++));

        assertEquals(0, calls[0]);
    }

    @Test
    public void finalEffectsScheduleTransitionBeforeCloseAndTopActions() {
        StringBuilder calls = new StringBuilder();

        BrowserPageFinishCoordinator.applyFinalEffects(
                true,
                delay -> calls.append("transition").append(delay).append('>'),
                () -> calls.append("close>"),
                () -> calls.append("top"));

        assertEquals("transition220>close>top", calls.toString());
    }

    @Test
    public void finalEffectsAlwaysCloseAdTabsAndUpdateTopActions() {
        StringBuilder calls = new StringBuilder();

        BrowserPageFinishCoordinator.applyFinalEffects(
                false,
                delay -> calls.append("transition>"),
                () -> calls.append("close>"),
                () -> calls.append("top"));

        assertEquals("close>top", calls.toString());
    }
}
