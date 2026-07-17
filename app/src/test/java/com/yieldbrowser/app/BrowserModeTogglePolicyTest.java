package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BrowserModeTogglePolicyTest {
    @Test
    public void togglesDesktopModeBothDirections() {
        assertTrue(BrowserModeTogglePolicy.nextDesktopMode(false));
        assertFalse(BrowserModeTogglePolicy.nextDesktopMode(true));
    }

    @Test
    public void visibleWebPageWithTargetUsesHardReload() {
        BrowserModeTogglePolicy.Plan plan = BrowserModeTogglePolicy.plan(
                true, true, "https://example.com");
        assertTrue(plan.desktopMode);
        assertTrue(plan.updateAddressBar);
        assertTrue(plan.hardReload);
        assertFalse(plan.applySettings);
        assertFalse(plan.showHome);
        assertEquals("Desktop mode aktif", plan.statusMessage);
    }

    @Test
    public void visibleWebPageWithoutTargetAppliesSettingsOnly() {
        BrowserModeTogglePolicy.Plan plan = BrowserModeTogglePolicy.plan(
                false, true, "");
        assertFalse(plan.updateAddressBar);
        assertFalse(plan.hardReload);
        assertTrue(plan.applySettings);
        assertFalse(plan.showHome);
        assertEquals("Mode mobile aktif", plan.statusMessage);
    }

    @Test
    public void homeStateKeepsHomeVisibleAfterApplyingSettings() {
        BrowserModeTogglePolicy.Plan plan = BrowserModeTogglePolicy.plan(
                true, false, "https://example.com");
        assertTrue(plan.updateAddressBar);
        assertFalse(plan.hardReload);
        assertTrue(plan.applySettings);
        assertTrue(plan.showHome);
    }
}
