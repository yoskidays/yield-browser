package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserUtilityDialogsTest {
    @Test
    public void searchEngineIndexUsesGoogleFallback() {
        assertEquals(0, BrowserUtilityDialogs.selectedEngineIndex(null));
        assertEquals(0, BrowserUtilityDialogs.selectedEngineIndex("Unknown"));
        assertEquals(2, BrowserUtilityDialogs.selectedEngineIndex("DuckDuckGo"));
        assertEquals(4, BrowserUtilityDialogs.selectedEngineIndex("Yandex"));
    }

    @Test
    public void qrValuesAreTrimmed() {
        assertEquals("", BrowserUtilityDialogs.normalizeQrValue(null));
        assertEquals("https://example.test", BrowserUtilityDialogs.normalizeQrValue(
                "  https://example.test  "));
    }

    @Test
    public void minimizeNormalDisplayIsInverseOfFloatingState() {
        VideoOptimizationDialogController.State floating =
                new VideoOptimizationDialogController.State(true, true, true, true);
        VideoOptimizationDialogController.State normal =
                new VideoOptimizationDialogController.State(true, true, false, true);
        assertFalse(VideoOptimizationDialogController.displayedMinimizeNormal(floating));
        assertTrue(VideoOptimizationDialogController.displayedMinimizeNormal(normal));
    }
}
