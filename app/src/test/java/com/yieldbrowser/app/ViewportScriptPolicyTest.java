package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewportScriptPolicyTest {
    @Test
    public void mobileScriptCreatesResponsiveViewportAndClearsForcedWidths() {
        String script = ViewportScriptPolicy.mobileResetScript();
        assertTrue(script.startsWith("javascript:"));
        assertTrue(script.contains("width=device-width"));
        assertTrue(script.contains("maximum-scale=5.0"));
        assertTrue(script.contains("removeProperty('min-width')"));
        assertTrue(script.contains("removeProperty('width')"));
        assertTrue(script.contains("dispatchEvent(new Event('resize'))"));
        assertFalse(script.contains("var w=1200"));
    }

    @Test
    public void desktopScriptLocksViewportToTwelveHundredPixels() {
        String script = ViewportScriptPolicy.desktopLockScript();
        assertTrue(script.startsWith("javascript:"));
        assertTrue(script.contains("var w=1200"));
        assertTrue(script.contains("width='+w+"));
        assertTrue(script.contains("style.minWidth=w+'px'"));
        assertFalse(script.contains("width=device-width"));
        assertFalse(script.contains("removeProperty('min-width')"));
    }

    @Test
    public void bothScriptsCreateViewportMetaWhenMissing() {
        String expected = "if(!m){m=document.createElement('meta');m.name='viewport';h.appendChild(m);}";
        assertTrue(ViewportScriptPolicy.mobileResetScript().contains(expected));
        assertTrue(ViewportScriptPolicy.desktopLockScript().contains(expected));
    }
}
