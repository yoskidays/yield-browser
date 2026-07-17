package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReloadLoopGuardActivePolicyTest {
    @Test
    public void missingOrExpiredGuardShortCircuitsHostLookup() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(ReloadLoopGuardActivePolicy.isActive(
                "", 100L, 0L, () -> { calls.incrementAndGet(); return "example.com"; }));
        assertFalse(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 99L, 100L, () -> { calls.incrementAndGet(); return "example.com"; }));
        assertTrue(calls.get() == 0);
    }

    @Test
    public void deadlineBoundaryRemainsActive() {
        assertTrue(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 100L, () -> "example.com"));
    }

    @Test
    public void exactAndSubdomainHostsMatch() {
        assertTrue(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 0L, () -> "example.com"));
        assertTrue(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 0L, () -> "sub.example.com"));
    }

    @Test
    public void unrelatedAndMissingHostsAreRejected() {
        assertFalse(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 0L, () -> "notexample.com"));
        assertFalse(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 0L, () -> ""));
        assertFalse(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 0L, null));
    }

    @Test
    public void hostLookupFailureReturnsFalse() {
        assertFalse(ReloadLoopGuardActivePolicy.isActive(
                "example.com", 100L, 0L,
                () -> { throw new IllegalStateException("host"); }));
    }
}
