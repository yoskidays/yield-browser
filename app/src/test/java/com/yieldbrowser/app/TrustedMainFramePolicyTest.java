package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrustedMainFramePolicyTest {
    @Test
    public void activationRejectsNonWebAndMissingHosts() {
        assertFalse(TrustedMainFramePolicy.activation(
                false, "example.com", 100L).activate);
        assertFalse(TrustedMainFramePolicy.activation(
                true, "", 100L).activate);
    }

    @Test
    public void activationUsesTenSecondWindow() {
        TrustedMainFramePolicy.Activation activation =
                TrustedMainFramePolicy.activation(true, "example.com", 2000L);
        assertTrue(activation.activate);
        assertEquals("example.com", activation.host);
        assertEquals(12000L, activation.untilMs);
    }

    @Test
    public void trustDeadlineBoundaryRemainsActive() {
        assertTrue(TrustedMainFramePolicy.isTrusted(
                "example.com", 100L, 100L, "sub.example.com",
                (candidate, trusted) -> candidate.endsWith("." + trusted)));
    }

    @Test
    public void expiredOrMissingStateShortCircuitsRelationCheck() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(TrustedMainFramePolicy.isTrusted(
                "example.com", 99L, 100L, "example.com",
                (a, b) -> { calls.incrementAndGet(); return true; }));
        assertFalse(TrustedMainFramePolicy.isTrusted(
                "", 100L, 0L, "example.com",
                (a, b) -> { calls.incrementAndGet(); return true; }));
        assertFalse(TrustedMainFramePolicy.isTrusted(
                "example.com", 100L, 0L, "",
                (a, b) -> { calls.incrementAndGet(); return true; }));
        assertEquals(0, calls.get());
    }

    @Test
    public void unrelatedOrMissingRelationIsRejected() {
        assertFalse(TrustedMainFramePolicy.isTrusted(
                "example.com", 100L, 0L, "other.com", (a, b) -> false));
        assertFalse(TrustedMainFramePolicy.isTrusted(
                "example.com", 100L, 0L, "example.com", null));
    }

    @Test
    public void relationFailureReturnsFalse() {
        assertFalse(TrustedMainFramePolicy.isTrusted(
                "example.com", 100L, 0L, "example.com",
                (a, b) -> { throw new IllegalStateException("relation"); }));
    }
}
