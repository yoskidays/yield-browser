package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistoryPanelPresentationTest {
    @Test
    public void normalizesQueriesAndSelectsEmptyMessages() {
        assertEquals("", HistoryPanelPresentation.normalizeQuery(null));
        assertEquals("komiku", HistoryPanelPresentation.normalizeQuery("  komiku  "));
        assertEquals("Riwayat masih kosong.", HistoryPanelPresentation.emptyMessage(""));
        assertEquals("Tidak ada riwayat yang cocok.", HistoryPanelPresentation.emptyMessage(" manga "));
    }

    @Test
    public void controlsInitialLoadingAndEmptyVisibility() {
        assertTrue(HistoryPanelPresentation.shouldShowInitialLoading(true, true));
        assertFalse(HistoryPanelPresentation.shouldShowInitialLoading(true, false));
        assertTrue(HistoryPanelPresentation.shouldShowEmpty(false, true));
        assertFalse(HistoryPanelPresentation.shouldShowEmpty(true, true));
    }

    @Test
    public void loadsOnlyNearBottomDuringForwardScroll() {
        assertTrue(HistoryPanelPresentation.shouldLoadNextPage(4, false, false, 42, 50));
        assertFalse(HistoryPanelPresentation.shouldLoadNextPage(0, false, false, 49, 50));
        assertFalse(HistoryPanelPresentation.shouldLoadNextPage(4, true, false, 49, 50));
        assertFalse(HistoryPanelPresentation.shouldLoadNextPage(4, false, true, 49, 50));
        assertFalse(HistoryPanelPresentation.shouldLoadNextPage(4, false, false, 10, 50));
    }

    @Test
    public void detectsLastPageFromReturnedCount() {
        assertTrue(HistoryPanelPresentation.isEndReached(49, 50));
        assertFalse(HistoryPanelPresentation.isEndReached(50, 50));
    }
}
