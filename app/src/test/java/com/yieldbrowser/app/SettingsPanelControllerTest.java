package com.yieldbrowser.app;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsPanelControllerTest {
    @Test
    public void mainItemsPreserveSectionsLabelsAndBehaviors() {
        SettingsPanelController.MainState state = new SettingsPanelController.MainState(
                false, "DuckDuckGo", true, true, false, true,
                "Auto ikut sistem", false, true, "Shield aktif",
                false, true, 125);

        List<SettingsPanelController.MainItem> items =
                SettingsPanelController.buildMainItems(state);

        assertEquals(33, items.size());
        assertEquals("Pusat fitur", items.get(1).section);
        assertEquals("Ruang privat", items.get(4).title);
        assertEquals(SettingsPanelController.Behavior.DISMISS, items.get(4).behavior);
        assertEquals("Search engine: DuckDuckGo", items.get(7).title);
        assertEquals(SettingsPanelController.MainAction.VIDEO_CONTROLS,
                items.get(17).action);
        assertTrue(items.get(17).setting);
        assertTrue(items.get(17).enabled);
        assertEquals("Mode Malam: Auto ikut sistem", items.get(24).title);
        assertEquals("AdBlock: ON", items.get(26).title);
        assertFalse(items.get(26).setting);
        assertEquals("Ukuran teks: 125%", items.get(29).title);
        assertEquals("Informasi", items.get(31).section);
    }

    @Test
    public void privateProfileUsesGeneralProfileCopy() {
        SettingsPanelController.MainState state = new SettingsPanelController.MainState(
                true, "Google", false, false, false, false,
                "OFF", false, false, "Off", false, false, 100);

        SettingsPanelController.MainItem profile =
                SettingsPanelController.buildMainItems(state).get(4);

        assertEquals("Tab umum", profile.title);
        assertTrue(profile.description.contains("profil umum"));
    }
}
