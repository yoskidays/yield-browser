package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BlankRecoveryTargetPolicyTest {
    @Test
    public void mappedCurrentUrlIsReturnedWhenHostAndKeyMatch() {
        String resolved = BlankRecoveryTargetPolicy.resolve(
                "wrapped",
                "https://fallback.com",
                "example.com",
                "page-key",
                value -> "https://sub.example.com/page",
                value -> "sub.example.com",
                value -> "page-key",
                (active, expected) -> active.endsWith("." + expected));
        assertEquals("https://sub.example.com/page", resolved);
    }

    @Test
    public void emptyMappedUrlUsesFallback() {
        String resolved = BlankRecoveryTargetPolicy.resolve(
                "wrapped",
                "https://example.com/page",
                "example.com",
                "page-key",
                value -> "",
                value -> "example.com",
                value -> "page-key",
                String::equals);
        assertEquals("https://example.com/page", resolved);
    }

    @Test
    public void hostMismatchIsRejectedBeforeKeyComparison() {
        AtomicInteger keyCalls = new AtomicInteger();
        assertNull(BlankRecoveryTargetPolicy.resolve(
                "https://other.com/page",
                null,
                "example.com",
                "page-key",
                value -> value,
                value -> "other.com",
                value -> { keyCalls.incrementAndGet(); return "page-key"; },
                (active, expected) -> false));
        assertEquals(1, keyCalls.get());
    }

    @Test
    public void navigationKeyMismatchIsRejected() {
        assertNull(BlankRecoveryTargetPolicy.resolve(
                "https://example.com/other",
                null,
                "example.com",
                "expected-key",
                value -> value,
                value -> "example.com",
                value -> "other-key",
                String::equals));
    }

    @Test
    public void missingDependenciesOrExpectedKeyAreRejected() {
        assertNull(BlankRecoveryTargetPolicy.resolve(
                "https://example.com", null, "example.com", null,
                value -> value, value -> "example.com", value -> "key", String::equals));
        assertNull(BlankRecoveryTargetPolicy.resolve(
                "https://example.com", null, "example.com", "key",
                value -> value, value -> "example.com", value -> "key", null));
    }

    @Test
    public void dependencyFailureReturnsNull() {
        assertNull(BlankRecoveryTargetPolicy.resolve(
                "wrapped", "https://fallback.com", "example.com", "key",
                value -> { throw new IllegalStateException("mapper"); },
                value -> "example.com", value -> "key", String::equals));
    }
}
