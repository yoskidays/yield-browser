package com.yieldbrowser.app;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickMenuControllerTest {
    @Test
    public void visibleItemsPreserveMenuOrderAndStateLabels() {
        QuickMenuController.State state = new QuickMenuController.State(
                true, true, true, true, true,
                true, true, true, true, true,
                true, true, true, true, true,
                false, true, false, true, "Auto ikut sistem");

        List<QuickMenuController.Item> items = QuickMenuController.buildItems(state);

        assertEquals(15, items.size());
        assertEquals(QuickMenuController.Action.DOWNLOADS, items.get(0).action);
        assertEquals("Buka ruang privat", items.get(2).label);
        assertEquals("AdBlock ON", items.get(3).label);
        assertEquals("Reader Mode OFF", items.get(4).label);
        assertEquals("Mode Malam: Auto ikut sistem", items.get(5).label);
        assertEquals("Kontrol video ON", items.get(11).label);
        assertEquals(QuickMenuController.Action.SITE_FILTER, items.get(14).action);
        assertTrue(items.get(0).switchDialog);
        assertFalse(items.get(2).switchDialog);
    }

    @Test
    public void hiddenShortcutsAreOmittedAndPrivateLabelChanges() {
        QuickMenuController.State state = new QuickMenuController.State(
                false, false, true, false, false,
                false, false, false, false, false,
                false, false, false, false, false,
                true, false, false, false, null);

        List<QuickMenuController.Item> items = QuickMenuController.buildItems(state);

        assertEquals(1, items.size());
        assertEquals(QuickMenuController.Action.PROFILE, items.get(0).action);
        assertEquals("Beralih ke tab umum", items.get(0).label);
    }
}
