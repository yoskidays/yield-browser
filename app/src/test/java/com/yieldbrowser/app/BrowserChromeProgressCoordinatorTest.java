package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserChromeProgressCoordinatorTest {
    @Test
    public void inactiveViewDoesNotReadOrMutateProgressState() {
        int[] calls = {0};

        assertFalse(BrowserChromeProgressCoordinator.handle(
                false,
                42,
                () -> {
                    calls[0]++;
                    return false;
                },
                () -> {
                    calls[0]++;
                    return true;
                },
                value -> calls[0]++,
                visible -> calls[0]++));

        assertEquals(0, calls[0]);
    }

    @Test
    public void visibleHomeResetsAndShortCircuitsPageVisibility() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserChromeProgressCoordinator.handle(
                true,
                42,
                () -> {
                    calls.append("home>");
                    return true;
                },
                () -> {
                    throw new AssertionError("page visibility must short-circuit");
                },
                value -> calls.append("progress").append(value).append('>'),
                visible -> calls.append("visible").append(visible)));

        assertEquals("home>progress0>visiblefalse", calls.toString());
    }

    @Test
    public void visiblePageShowsInProgressAndHidesCompletion() {
        StringBuilder calls = new StringBuilder();

        assertTrue(BrowserChromeProgressCoordinator.handle(
                true,
                42,
                () -> false,
                () -> true,
                value -> calls.append("progress").append(value).append('>'),
                visible -> calls.append("visible").append(visible).append('>')));
        assertTrue(BrowserChromeProgressCoordinator.handle(
                true,
                100,
                () -> false,
                () -> true,
                value -> calls.append("progress").append(value).append('>'),
                visible -> calls.append("visible").append(visible)));

        assertEquals(
                "progress42>visibletrue>progress100>visiblefalse",
                calls.toString());
    }
}
