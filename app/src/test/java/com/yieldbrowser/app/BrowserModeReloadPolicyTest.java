package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserModeReloadPolicyTest {
    @Test
    public void rejectsNonWebExternalImageMediaAndDirectUrls() {
        assertFalse(BrowserModeReloadPolicy.isSafe(
                false, false, false, false, false, true, false, false, false));
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, true, false, false, false, true, false, false, false));
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, false, true, false, false, true, false, false, false));
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, false, false, true, false, true, false, false, false));
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, false, false, false, true, true, false, false, false));
    }

    @Test
    public void explicitCurrentPageIgnoresAdAndPopupClassification() {
        assertTrue(BrowserModeReloadPolicy.isSafe(
                true, false, false, false, false, true, true, true, true));
    }

    @Test
    public void fallbackUrlRejectsAdAndPopupClassification() {
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, false, false, false, false, false, true, false, false));
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, false, false, false, false, false, false, true, false));
        assertFalse(BrowserModeReloadPolicy.isSafe(
                true, false, false, false, false, false, false, false, true));
        assertTrue(BrowserModeReloadPolicy.isSafe(
                true, false, false, false, false, false, false, false, false));
    }
}
