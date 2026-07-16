package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadControlsFactoryTest {
    @Test
    public void clearAllOnlyAppearsForCompletedHistoryOutsideSelectionMode() {
        assertTrue(DownloadControlsFactory.shouldShowClearAll("Selesai", false, 3));
        assertFalse(DownloadControlsFactory.shouldShowClearAll("Selesai", true, 3));
        assertFalse(DownloadControlsFactory.shouldShowClearAll("Selesai", false, 0));
        assertFalse(DownloadControlsFactory.shouldShowClearAll("Mengunduh", false, 3));
    }

    @Test
    public void queueLabelClampsInvalidCounts() {
        assertEquals("Queue 2/4 • 3", DownloadControlsFactory.queueLabel(2, 4, 3));
        assertEquals("Queue 0/1 • 0", DownloadControlsFactory.queueLabel(-1, 0, -5));
    }
}
