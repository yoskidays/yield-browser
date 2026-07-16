package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserChromeProgressPolicyTest {
    @Test
    public void homeOrHiddenPageResetsProgress() {
        assertTrue(BrowserChromeProgressPolicy.shouldResetProgress(true, true));
        assertTrue(BrowserChromeProgressPolicy.shouldResetProgress(false, false));
        assertFalse(BrowserChromeProgressPolicy.shouldResetProgress(false, true));
    }

    @Test
    public void completedProgressIsHidden() {
        assertTrue(BrowserChromeProgressPolicy.shouldShowProgress(0));
        assertTrue(BrowserChromeProgressPolicy.shouldShowProgress(99));
        assertFalse(BrowserChromeProgressPolicy.shouldShowProgress(100));
        assertFalse(BrowserChromeProgressPolicy.shouldShowProgress(101));
    }
}
