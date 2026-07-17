package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompatibilityNavigationPolicyTest {
    @Test
    public void inactiveCompatibilityIsNotAFlow() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(CompatibilityNavigationPolicy.isFlow(
                false, "target.com", "source.com", (a, b) -> {
                    calls.incrementAndGet();
                    return true;
                }));
        assertTrue(calls.get() == 0);
    }

    @Test
    public void missingHostKeepsActiveCompatibilityFlow() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(CompatibilityNavigationPolicy.isFlow(
                true, "", "source.com", (a, b) -> {
                    calls.incrementAndGet();
                    return false;
                }));
        assertTrue(calls.get() == 0);
    }

    @Test
    public void relatedHostsMatchInEitherDirection() {
        assertTrue(CompatibilityNavigationPolicy.isFlow(
                true, "sub.example.com", "example.com",
                (a, b) -> a.endsWith("." + b)));
        assertTrue(CompatibilityNavigationPolicy.isFlow(
                true, "example.com", "sub.example.com",
                (a, b) -> a.endsWith("." + b)));
    }

    @Test
    public void unrelatedHostsAreRejected() {
        assertFalse(CompatibilityNavigationPolicy.isFlow(
                true, "one.com", "two.com", (a, b) -> false));
    }

    @Test
    public void nullHostOrMissingRelationIsRejected() {
        assertFalse(CompatibilityNavigationPolicy.isFlow(
                true, null, "source.com", (a, b) -> true));
        assertFalse(CompatibilityNavigationPolicy.isFlow(
                true, "target.com", "source.com", null));
    }

    @Test
    public void secondDirectionIsSkippedWhenFirstMatches() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(CompatibilityNavigationPolicy.isFlow(
                true, "target.com", "source.com", (a, b) -> {
                    calls.incrementAndGet();
                    return true;
                }));
        assertTrue(calls.get() == 1);
    }
}
