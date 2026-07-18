package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwipeNavigationControllerTest {
    @Test
    public void eligibleGestureRequiresFastHorizontalMovement() {
        assertTrue(SwipeNavigationController.isEligibleGesture(120f, 20f, 500L, 90, 120));
        assertTrue(SwipeNavigationController.isEligibleGesture(-120f, -20f, 900L, 90, 120));
        assertFalse(SwipeNavigationController.isEligibleGesture(89f, 0f, 500L, 90, 120));
        assertFalse(SwipeNavigationController.isEligibleGesture(120f, 121f, 500L, 90, 120));
        assertFalse(SwipeNavigationController.isEligibleGesture(120f, 0f, 901L, 90, 120));
    }
}
