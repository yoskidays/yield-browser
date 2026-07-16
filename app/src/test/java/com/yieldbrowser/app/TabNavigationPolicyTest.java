package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TabNavigationPolicyTest {
    @Test
    public void clampsIndexesAndKeepsOneVisibleCount() {
        assertEquals(0, TabNavigationPolicy.clampIndex(-4, 3));
        assertEquals(2, TabNavigationPolicy.clampIndex(9, 3));
        assertEquals(0, TabNavigationPolicy.clampIndex(2, 0));
        assertEquals(1, TabNavigationPolicy.countForUi(0));
        assertEquals(4, TabNavigationPolicy.countForUi(4));
    }

    @Test
    public void validatesCurrentTabByIdentity() {
        Object first = new Object();
        Object second = new Object();
        ArrayList<Object> tabs = new ArrayList<>(Arrays.asList(first, second));

        assertTrue(TabNavigationPolicy.isCurrentTab(tabs, 1, second, false));
        assertFalse(TabNavigationPolicy.isCurrentTab(tabs, 1, first, false));
        assertFalse(TabNavigationPolicy.isCurrentTab(tabs, 1, second, true));
        assertFalse(TabNavigationPolicy.isCurrentTab(tabs, 5, second, false));
    }

    @Test
    public void choosesStableIndexAfterClosingTabs() {
        assertEquals(1, TabNavigationPolicy.indexAfterClosingCurrent(1, 1, 2));
        assertEquals(1, TabNavigationPolicy.indexAfterClosingCurrent(5, -1, 2));
        assertEquals(1, TabNavigationPolicy.indexAfterRemovingTab(2, 0, 2));
        assertEquals(0, TabNavigationPolicy.indexAfterRemovingTab(0, 1, 2));
        assertEquals(0, TabNavigationPolicy.indexAfterClosingAdTab(2, 2, 0, 2, true));
        assertEquals(1, TabNavigationPolicy.indexAfterClosingAdTab(2, 0, 0, 2, false));
        assertEquals(1, TabNavigationPolicy.indexAfterDetectedAdRemoval(2, 0, 2, false));
        assertEquals(0, TabNavigationPolicy.indexAfterDetectedAdRemoval(1, 1, 1, true));
    }

    @Test
    public void derivesBackAndForwardActionsFromVisibleSurface() {
        assertEquals(TabNavigationPolicy.BackAction.RESTORE_PAGE,
                TabNavigationPolicy.backAction(true, false, false));
        assertEquals(TabNavigationPolicy.BackAction.WEB_BACK,
                TabNavigationPolicy.backAction(false, true, true));
        assertEquals(TabNavigationPolicy.BackAction.SHOW_HOME,
                TabNavigationPolicy.backAction(false, true, false));

        assertEquals(TabNavigationPolicy.ForwardAction.WEB_FORWARD,
                TabNavigationPolicy.forwardAction(false, true, true));
        assertEquals(TabNavigationPolicy.ForwardAction.RESTORE_AND_FORWARD,
                TabNavigationPolicy.forwardAction(true, false, true));
        assertEquals(TabNavigationPolicy.ForwardAction.RESTORE_PAGE,
                TabNavigationPolicy.forwardAction(true, false, false));
        assertEquals(TabNavigationPolicy.ForwardAction.NONE,
                TabNavigationPolicy.forwardAction(false, true, false));
    }

    @Test
    public void keepsSwitchAndPageVisibilityRulesExplicit() {
        assertTrue(TabNavigationPolicy.changesTab(2, 1));
        assertFalse(TabNavigationPolicy.changesTab(1, 1));
        assertTrue(TabNavigationPolicy.shouldSaveBeforeSwitch(false));
        assertFalse(TabNavigationPolicy.shouldSaveBeforeSwitch(true));
        assertTrue(TabNavigationPolicy.shouldShowPage("https://example.com", false));
        assertFalse(TabNavigationPolicy.shouldShowPage("", false));
        assertFalse(TabNavigationPolicy.shouldShowPage("https://example.com", true));
    }
}
