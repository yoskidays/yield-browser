package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BrowserShellUiTest {
    @Test
    public void shortcutInitialHandlesBlankAcronymAndNormalLabels() {
        assertEquals("?", BrowserShellUi.shortcutInitial(null));
        assertEquals("?", BrowserShellUi.shortcutInitial("   "));
        assertEquals("YO", BrowserShellUi.shortcutInitial("YOUTUBE"));
        assertEquals("G", BrowserShellUi.shortcutInitial("GitHub"));
    }

    @Test
    public void shortcutColorIndexIsStableAndWithinPalette() {
        int first = BrowserShellUi.shortcutColorIndex("GitHub", 7);
        int second = BrowserShellUi.shortcutColorIndex("GitHub", 7);
        assertEquals(first, second);
        assertTrue(first >= 0 && first < 7);
        assertEquals(0, BrowserShellUi.shortcutColorIndex("GitHub", 0));
    }
}
