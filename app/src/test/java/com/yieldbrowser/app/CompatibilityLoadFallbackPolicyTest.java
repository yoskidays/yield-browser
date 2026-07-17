package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CompatibilityLoadFallbackPolicyTest {
    @Test
    public void usesMappedCurrentUrlWhenHostMatches() {
        String resolved = CompatibilityLoadFallbackPolicy.resolve(
                "wrapped",
                "https://fallback.com",
                "example.com",
                value -> "https://sub.example.com/page",
                value -> "sub.example.com",
                (active, expected) -> active.endsWith("." + expected));
        assertEquals("https://sub.example.com/page", resolved);
    }

    @Test
    public void fallsBackWhenMappedUrlIsEmpty() {
        String resolved = CompatibilityLoadFallbackPolicy.resolve(
                "wrapped",
                "https://example.com/page",
                "example.com",
                value -> "",
                value -> "example.com",
                String::equals);
        assertEquals("https://example.com/page", resolved);
    }

    @Test
    public void missingExpectedOrActiveHostIsRejected() {
        assertNull(CompatibilityLoadFallbackPolicy.resolve(
                "https://example.com", null, "", value -> value,
                value -> "example.com", String::equals));
        assertNull(CompatibilityLoadFallbackPolicy.resolve(
                "about:blank", null, "example.com", value -> value,
                value -> "", String::equals));
    }

    @Test
    public void unrelatedHostIsRejected() {
        assertNull(CompatibilityLoadFallbackPolicy.resolve(
                "https://other.com", null, "example.com", value -> value,
                value -> "other.com", (active, expected) -> false));
    }

    @Test
    public void missingRelationIsRejectedAfterHostResolution() {
        AtomicInteger hostCalls = new AtomicInteger();
        assertNull(CompatibilityLoadFallbackPolicy.resolve(
                "https://example.com", null, "example.com", value -> value,
                value -> { hostCalls.incrementAndGet(); return "example.com"; }, null));
        assertEquals(1, hostCalls.get());
    }

    @Test
    public void dependencyFailureReturnsNull() {
        assertNull(CompatibilityLoadFallbackPolicy.resolve(
                "wrapped", "https://fallback.com", "example.com",
                value -> { throw new IllegalStateException("mapper"); },
                value -> "example.com", String::equals));
        assertNull(CompatibilityLoadFallbackPolicy.resolve(
                "https://example.com", null, "example.com", value -> value,
                value -> { throw new IllegalStateException("host"); }, String::equals));
    }
}
