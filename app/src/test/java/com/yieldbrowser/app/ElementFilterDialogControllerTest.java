package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElementFilterDialogControllerTest {
    @Test
    public void pickerMessageIncludesSelectorCountAndPreview() {
        String message = ElementFilterDialogController.buildPickerMessage(
                ".ad-card", "Sponsored result", 3);
        assertTrue(message.contains("Selector:\n.ad-card"));
        assertTrue(message.contains("3 elemen pada halaman ini"));
        assertTrue(message.contains("Pratinjau:\nSponsored result"));
    }

    @Test
    public void pickerMessageNormalizesNegativeCountAndTruncatesPreview() {
        StringBuilder preview = new StringBuilder();
        for (int index = 0; index < 220; index++) preview.append('x');
        String message = ElementFilterDialogController.buildPickerMessage(
                "#overlay", preview.toString(), -4);
        assertFalse(message.contains("Akan menyembunyikan"));
        assertTrue(message.endsWith("…"));
    }
}
