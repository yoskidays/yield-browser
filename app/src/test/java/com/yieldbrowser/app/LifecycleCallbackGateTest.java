package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LifecycleCallbackGateTest {
    @Test
    public void gateIsInactiveUntilActivityStarts() {
        LifecycleCallbackGate gate = new LifecycleCallbackGate();

        assertFalse(gate.isActive());
        gate.markActive();
        assertTrue(gate.isActive());
    }

    @Test
    public void destroyedActivityRejectsLateCallbacks() {
        LifecycleCallbackGate gate = new LifecycleCallbackGate();
        gate.markActive();

        gate.markDestroyed();

        assertFalse(gate.isActive());
    }

    @Test
    public void recreatedActivityCanActivateGateAgain() {
        LifecycleCallbackGate gate = new LifecycleCallbackGate();
        gate.markActive();
        gate.markDestroyed();

        gate.markActive();

        assertTrue(gate.isActive());
    }
}
