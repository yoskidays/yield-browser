package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReloadLoopRegistrationPolicyTest {
    @Test
    public void repeatedKeyWithinWindowIncrementsWithoutResettingWindow() {
        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                "page", "page", 1000L, 1, 0L, 13000L);
        assertEquals("page", plan.lastKey);
        assertEquals(1000L, plan.windowStartMs);
        assertEquals(2, plan.count);
        assertFalse(plan.guardTriggered);
    }

    @Test
    public void eventAfterWindowResetsKeyWindowAndCount() {
        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                "page", "page", 1000L, 3, 0L, 13001L);
        assertEquals("page", plan.lastKey);
        assertEquals(13001L, plan.windowStartMs);
        assertEquals(1, plan.count);
        assertFalse(plan.guardTriggered);
    }

    @Test
    public void differentKeyStartsNewWindow() {
        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                "other", "page", 1000L, 3, 0L, 2000L);
        assertEquals("other", plan.lastKey);
        assertEquals(2000L, plan.windowStartMs);
        assertEquals(1, plan.count);
    }

    @Test
    public void fourthEventTriggersTwoMinuteGuardAndResetsCount() {
        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                "page", "page", 1000L, 3, 0L, 2000L);
        assertTrue(plan.guardTriggered);
        assertEquals(0, plan.count);
        assertEquals(122000L, plan.guardUntilMs);
    }

    @Test
    public void toastRequiresStrictlyMoreThanSixSeconds() {
        assertFalse(ReloadLoopRegistrationPolicy.plan(
                "page", "page", 1000L, 3, 1000L, 7000L).showToast);
        assertTrue(ReloadLoopRegistrationPolicy.plan(
                "page", "page", 1000L, 3, 1000L, 7001L).showToast);
    }

    @Test
    public void nullKeyIsNormalizedToEmpty() {
        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                null, null, 0L, 0, 0L, 0L);
        assertEquals("", plan.lastKey);
        assertEquals(1, plan.count);
    }
}
