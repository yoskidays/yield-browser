package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompatibilityRecoveryThrottlePolicyTest {
    @Test
    public void duplicateWithinWindowIsBlocked() {
        CompatibilityRecoveryThrottlePolicy.Plan plan =
                CompatibilityRecoveryThrottlePolicy.plan(
                        "example.com", "page-key",
                        "example.com", "page-key",
                        5000L, 4999L);
        assertFalse(plan.retry);
        assertEquals(5000L, plan.untilMs);
    }

    @Test
    public void boundaryAtDeadlineAllowsRetry() {
        CompatibilityRecoveryThrottlePolicy.Plan plan =
                CompatibilityRecoveryThrottlePolicy.plan(
                        "example.com", "page-key",
                        "example.com", "page-key",
                        5000L, 5000L);
        assertTrue(plan.retry);
        assertEquals(305000L, plan.untilMs);
    }

    @Test
    public void differentHostOrKeyAllowsRetry() {
        assertTrue(CompatibilityRecoveryThrottlePolicy.plan(
                "other.com", "page-key", "example.com", "page-key",
                5000L, 1000L).retry);
        assertTrue(CompatibilityRecoveryThrottlePolicy.plan(
                "example.com", "other-key", "example.com", "page-key",
                5000L, 1000L).retry);
    }

    @Test
    public void retryPlanCarriesNewStateAndFiveMinuteWindow() {
        CompatibilityRecoveryThrottlePolicy.Plan plan =
                CompatibilityRecoveryThrottlePolicy.plan(
                        "example.com", "page-key", "", "", 0L, 2000L);
        assertTrue(plan.retry);
        assertEquals("example.com", plan.host);
        assertEquals("page-key", plan.key);
        assertEquals(302000L, plan.untilMs);
    }

    @Test
    public void blankHostIsRejectedAndNullKeyBecomesEmpty() {
        CompatibilityRecoveryThrottlePolicy.Plan plan =
                CompatibilityRecoveryThrottlePolicy.plan(
                        "", null, "", "", 0L, 100L);
        assertFalse(plan.retry);
        assertEquals("", plan.host);
        assertEquals("", plan.key);
        assertEquals(0L, plan.untilMs);
    }
}
