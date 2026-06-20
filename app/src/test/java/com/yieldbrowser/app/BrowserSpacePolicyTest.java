package com.yieldbrowser.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserSpacePolicyTest {
    @Test
    public void modernProfilesUseSeparateProcesses() {
        assertTrue(BrowserSpacePolicy.mustOpenOtherProcess(true, false, 35));
        assertTrue(BrowserSpacePolicy.mustOpenOtherProcess(false, true, 35));
        assertFalse(BrowserSpacePolicy.mustOpenOtherProcess(false, false, 35));
    }

    @Test
    public void legacyPrivateTabsRemainLocal() {
        assertTrue(BrowserSpacePolicy.isPrivateSpace(false, true, 27));
        assertFalse(BrowserSpacePolicy.mustOpenOtherProcess(true, false, 27));
    }

    @Test
    public void lastDedicatedPrivateTabReturnsToNormal() {
        assertTrue(BrowserSpacePolicy.shouldReturnToNormalAfterLastPrivateTab(true, 0));
        assertFalse(BrowserSpacePolicy.shouldReturnToNormalAfterLastPrivateTab(true, 1));
    }
}
