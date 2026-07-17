package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrictCompatibilityUrlPolicyTest {
    @Test
    public void emptyHostIsRejectedWithoutEvaluatingDependencies() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(StrictCompatibilityUrlPolicy.isStrict(
                "",
                host -> { calls.incrementAndGet(); return true; },
                () -> { calls.incrementAndGet(); return true; }));
        assertTrue(calls.get() == 0);
    }

    @Test
    public void knownHostShortCircuitsActiveEvaluation() {
        AtomicInteger activeCalls = new AtomicInteger();
        assertTrue(StrictCompatibilityUrlPolicy.isStrict(
                "example.com",
                host -> true,
                () -> { activeCalls.incrementAndGet(); return false; }));
        assertTrue(activeCalls.get() == 0);
    }

    @Test
    public void unknownHostUsesActiveCompatibilityState() {
        assertTrue(StrictCompatibilityUrlPolicy.isStrict(
                "example.com", host -> false, () -> true));
        assertFalse(StrictCompatibilityUrlPolicy.isStrict(
                "example.com", host -> false, () -> false));
    }

    @Test
    public void missingDependenciesDefaultToFalse() {
        assertFalse(StrictCompatibilityUrlPolicy.isStrict(
                "example.com", null, null));
    }

    @Test
    public void dependencyFailureReturnsFalse() {
        assertFalse(StrictCompatibilityUrlPolicy.isStrict(
                "example.com",
                host -> { throw new IllegalStateException("known"); },
                () -> true));
        assertFalse(StrictCompatibilityUrlPolicy.isStrict(
                "example.com",
                host -> false,
                () -> { throw new IllegalStateException("active"); }));
    }
}
