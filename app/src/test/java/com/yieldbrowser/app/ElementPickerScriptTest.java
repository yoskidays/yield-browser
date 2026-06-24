package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElementPickerScriptTest {
    @Test
    public void pickerHasPersistentModeCloseButtonAndSafeEventHandling() {
        String script = ElementPickerScript.build();
        assertTrue(script.contains("__yield_picker_close"));
        assertTrue(script.contains("__yieldPickerContinue"));
        assertTrue(script.contains("onElementPickedV2"));
        assertTrue(script.contains("window.PointerEvent"));
        assertTrue(script.contains("Elemen penting tidak dapat diblokir"));
        assertFalse(script.contains("komiku.org"));
    }
}
