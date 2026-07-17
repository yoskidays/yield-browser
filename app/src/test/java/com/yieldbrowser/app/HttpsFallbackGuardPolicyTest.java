package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsFallbackGuardPolicyTest {
    private static boolean isHttp(String url) {
        return url != null && url.startsWith("http://");
    }

    @Test
    public void nonHttpAndExpiredGuardsRejectBeforeHostResolution() {
        AtomicInteger hostCalls = new AtomicInteger();
        HttpsFallbackGuardPolicy.HostResolver resolver = url -> {
            hostCalls.incrementAndGet();
            return "example.com";
        };
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "https://example.com", 100L, 0L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp, resolver));
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 99L, 100L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp, resolver));
        assertTrue(hostCalls.get() == 0);
    }

    @Test
    public void deadlineBoundaryRemainsActiveForMatchingHost() {
        assertTrue(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 100L, 100L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp, url -> "example.com"));
    }

    @Test
    public void emptyMismatchedAndMissingFallbackHostsReject() {
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 100L, 0L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp, url -> ""));
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://other.com", 100L, 0L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp, url -> "other.com"));
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 100L, 0L, null,
                HttpsFallbackGuardPolicyTest::isHttp, url -> "example.com"));
    }

    @Test
    public void missingOrFailingDependenciesReturnFalse() {
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 100L, 0L, "example.com", null,
                url -> "example.com"));
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 100L, 0L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp, null));
        assertFalse(HttpsFallbackGuardPolicy.isActive(
                "http://example.com", 100L, 0L, "example.com",
                HttpsFallbackGuardPolicyTest::isHttp,
                url -> { throw new IllegalStateException("host"); }));
    }
}
