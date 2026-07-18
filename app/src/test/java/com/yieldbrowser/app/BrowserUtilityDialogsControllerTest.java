package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowserUtilityDialogsControllerTest {
    @Test
    public void zoomIsClampedToSupportedRange() {
        assertEquals(70, BrowserUtilityDialogsController.clampZoom(10));
        assertEquals(100, BrowserUtilityDialogsController.clampZoom(100));
        assertEquals(150, BrowserUtilityDialogsController.clampZoom(999));
    }

    @Test
    public void folderNameUsesDefaultAndRemovesPathSeparators() {
        assertEquals("Download", BrowserUtilityDialogsController.sanitizeSubfolder("  "));
        assertEquals("Anime-2026-HD",
                BrowserUtilityDialogsController.sanitizeSubfolder("Anime/2026\\HD"));
    }
}
