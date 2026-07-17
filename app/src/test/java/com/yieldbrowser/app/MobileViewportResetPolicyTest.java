package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MobileViewportResetPolicyTest {
    @Test
    public void schedulesOnlyInMobileMode() {
        assertTrue(MobileViewportResetPolicy.shouldSchedule(false));
        assertFalse(MobileViewportResetPolicy.shouldSchedule(true));
    }

    @Test
    public void preservesResetDelays() {
        assertArrayEquals(new long[]{120L, 500L, 1200L},
                MobileViewportResetPolicy.delays());
    }

    @Test
    public void appliesOnlyForMatchingTokenAndCurrentMobileMode() {
        assertTrue(MobileViewportResetPolicy.shouldApply(7, 7, false));
        assertFalse(MobileViewportResetPolicy.shouldApply(7, 8, false));
        assertFalse(MobileViewportResetPolicy.shouldApply(7, 7, true));
    }
}
