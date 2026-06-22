package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniversalReaderRepairScriptTest {
    @Test
    public void scriptIsDomainIndependentAndSupportsCommonLazyPatterns() {
        String script = UniversalReaderRepairScript.build();
        assertFalse(script.contains("komiku.org"));
        assertTrue(script.contains("data-lazy-src"));
        assertTrue(script.contains("data-original-srcset"));
        assertTrue(script.contains("data-background-image"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("AD_URL"));
    }
}
