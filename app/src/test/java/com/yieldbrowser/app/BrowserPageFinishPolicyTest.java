package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserPageFinishPolicyTest {
    @Test
    public void extractedUrlKeepsPriorityOverRawUrl() {
        assertEquals("https://original.example/page",
                BrowserPageFinishPolicy.chooseFinalUrl(
                        "https://original.example/page",
                        "https://proxy.example/page"));
        assertEquals("https://proxy.example/page",
                BrowserPageFinishPolicy.chooseFinalUrl(
                        null, "https://proxy.example/page"));
    }

    @Test
    public void webStateRequiresANonEmptySavedHistory() {
        assertTrue(BrowserPageFinishPolicy.shouldKeepWebState(true, 1));
        assertTrue(BrowserPageFinishPolicy.shouldKeepWebState(true, 4));
        assertFalse(BrowserPageFinishPolicy.shouldKeepWebState(true, 0));
        assertFalse(BrowserPageFinishPolicy.shouldKeepWebState(false, 4));
    }
}
