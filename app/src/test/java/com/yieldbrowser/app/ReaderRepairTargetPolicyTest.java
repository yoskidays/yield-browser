package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReaderRepairTargetPolicyTest {
    @Test
    public void usesMappedCurrentUrlWhenEligible() {
        String resolved = ReaderRepairTargetPolicy.resolve(
                "wrapped",
                "https://fallback.com",
                "example.com",
                value -> "https://example.com/chapter/1",
                value -> true,
                value -> "example.com",
                (a, b) -> a.equals(b));
        assertEquals("https://example.com/chapter/1", resolved);
    }

    @Test
    public void fallsBackWhenMappedCurrentUrlIsEmpty() {
        String resolved = ReaderRepairTargetPolicy.resolve(
                "wrapped",
                "https://example.com/chapter/2",
                "example.com",
                value -> "",
                value -> true,
                value -> "example.com",
                (a, b) -> a.equals(b));
        assertEquals("https://example.com/chapter/2", resolved);
    }

    @Test
    public void ineligibleTargetIsRejectedBeforeHostChecks() {
        AtomicInteger hostCalls = new AtomicInteger();
        assertNull(ReaderRepairTargetPolicy.resolve(
                "https://example.com/image.jpg",
                null,
                "example.com",
                value -> value,
                value -> false,
                value -> { hostCalls.incrementAndGet(); return "example.com"; },
                (a, b) -> true));
        assertEquals(0, hostCalls.get());
    }

    @Test
    public void relatedHostsMatchInEitherDirection() {
        assertEquals(
                "https://sub.example.com/chapter",
                ReaderRepairTargetPolicy.resolve(
                        "https://sub.example.com/chapter",
                        null,
                        "example.com",
                        value -> value,
                        value -> true,
                        value -> "sub.example.com",
                        (a, b) -> a.endsWith("." + b)));
        assertEquals(
                "https://example.com/chapter",
                ReaderRepairTargetPolicy.resolve(
                        "https://example.com/chapter",
                        null,
                        "sub.example.com",
                        value -> value,
                        value -> true,
                        value -> "example.com",
                        (a, b) -> a.endsWith("." + b)));
    }

    @Test
    public void unrelatedHostsAreRejected() {
        assertNull(ReaderRepairTargetPolicy.resolve(
                "https://other.com/chapter",
                null,
                "example.com",
                value -> value,
                value -> true,
                value -> "other.com",
                (a, b) -> false));
    }

    @Test
    public void missingHostAllowsEligibleTarget() {
        assertEquals(
                "https://example.com/chapter",
                ReaderRepairTargetPolicy.resolve(
                        "https://example.com/chapter",
                        null,
                        "",
                        value -> value,
                        value -> true,
                        value -> "example.com",
                        null));
    }

    @Test
    public void dependencyFailureReturnsNull() {
        assertNull(ReaderRepairTargetPolicy.resolve(
                "wrapped",
                "https://fallback.com",
                "example.com",
                value -> { throw new IllegalStateException("mapper"); },
                value -> true,
                value -> "example.com",
                (a, b) -> true));
    }
}
