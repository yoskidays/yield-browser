package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TabsPanelControllerTest {
    @Test
    public void visibleCountsIgnoreClosedAndOtherSpaceTabs() {
        TabInfo normal = new TabInfo("One", "https://one.test", false);
        TabInfo closed = new TabInfo("Closed", "https://closed.test", false);
        closed.closed = true;
        TabInfo privateTab = new TabInfo("Private", "https://private.test", true);

        assertEquals(1, TabsPanelController.countVisible(
                Arrays.asList(normal, closed, privateTab), false));
        assertEquals(1, TabsPanelController.countVisible(
                Arrays.asList(normal, closed, privateTab), true));
    }

    @Test
    public void fallbackTitlesAndUrlsMatchSpace() {
        TabInfo blank = new TabInfo("", "", false);
        assertEquals("Tab baru", TabsPanelController.displayTitle(blank, false));
        assertEquals("Tab privat", TabsPanelController.displayTitle(blank, true));
        assertEquals("Halaman awal", TabsPanelController.displayUrl(blank, false));
        assertEquals("Privat • Halaman awal", TabsPanelController.displayUrl(blank, true));
    }

    @Test
    public void explicitTitlesAndUrlsArePreserved() {
        TabInfo tab = new TabInfo("Example", "https://example.test", false);
        assertEquals("Example", TabsPanelController.displayTitle(tab, false));
        assertEquals("https://example.test", TabsPanelController.displayUrl(tab, false));
    }
}
