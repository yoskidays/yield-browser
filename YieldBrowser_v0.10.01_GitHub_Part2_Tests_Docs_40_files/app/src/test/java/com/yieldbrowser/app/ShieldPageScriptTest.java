package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldPageScriptTest {
    @Test
    public void buildsSilentUniversalDocumentStartScript() {
        String script = ShieldPageScript.documentStart(true, true, true, true, true);
        assertTrue(script.contains("__yieldShieldV2State"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("sameRelay"));
        assertTrue(script.contains("safeReaderNav"));
        assertTrue(script.contains("navControl"));
        assertFalse(script.contains("Toast"));
        assertFalse(script.contains("blockedCount"));
    }

    @Test
    public void runtimeConfigCanDisableProtectionWithoutReload() {
        String script = ShieldPageScript.runtimeConfig(false, false, false, false, false);
        assertTrue(script.contains("enabled:false"));
        assertTrue(script.contains("__yieldShieldV2SetConfig"));
    }
}
