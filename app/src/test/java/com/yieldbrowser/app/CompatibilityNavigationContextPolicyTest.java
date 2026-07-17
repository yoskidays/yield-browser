package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompatibilityNavigationContextPolicyTest {
    @Test
    public void evaluatesSignalsInOrderAndStopsAtFirstTrue() {
        List<Integer> calls = new ArrayList<>();
        assertTrue(CompatibilityNavigationContextPolicy.any(
                () -> { calls.add(1); return false; },
                () -> { calls.add(2); return true; },
                () -> { calls.add(3); return true; }));
        assertEquals(Arrays.asList(1, 2), calls);
    }

    @Test
    public void allFalseSignalsReturnFalse() {
        assertFalse(CompatibilityNavigationContextPolicy.any(
                () -> false,
                () -> false,
                () -> false));
    }

    @Test
    public void nullArrayAndNullEntriesAreIgnored() {
        assertFalse(CompatibilityNavigationContextPolicy.any((CompatibilityNavigationContextPolicy.Evaluator[]) null));
        assertTrue(CompatibilityNavigationContextPolicy.any(
                null,
                () -> true));
    }

    @Test
    public void evaluatorFailureStopsEvaluationAndReturnsFalse() {
        List<Integer> calls = new ArrayList<>();
        assertFalse(CompatibilityNavigationContextPolicy.any(
                () -> { calls.add(1); throw new IllegalStateException("broken"); },
                () -> { calls.add(2); return true; }));
        assertEquals(Arrays.asList(1), calls);
    }
}
