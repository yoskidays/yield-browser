package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsNavigationFailurePolicyTest {
    private static boolean isHttps(String url) {
        return url != null && url.startsWith("https://");
    }

    private static boolean sameOrSubdomain(String candidate, String base) {
        return candidate.equals(base) || candidate.endsWith("." + base);
    }

    @Test
    public void disabledMissingViewAndNonHttpsShortCircuitErrorCheck() {
        AtomicInteger errorCalls = new AtomicInteger();
        HttpsNavigationFailurePolicy.ErrorPredicate error = code -> {
            errorCalls.incrementAndGet();
            return true;
        };
        assertFalse(HttpsNavigationFailurePolicy.passesPreflight(
                false, true, "https://example.com", -1,
                HttpsNavigationFailurePolicyTest::isHttps, error));
        assertFalse(HttpsNavigationFailurePolicy.passesPreflight(
                true, false, "https://example.com", -1,
                HttpsNavigationFailurePolicyTest::isHttps, error));
        assertFalse(HttpsNavigationFailurePolicy.passesPreflight(
                true, true, "http://example.com", -1,
                HttpsNavigationFailurePolicyTest::isHttps, error));
        assertTrue(errorCalls.get() == 0);
    }

    @Test
    public void eligibleErrorDeterminesPreflightAfterHttpsValidation() {
        assertTrue(HttpsNavigationFailurePolicy.passesPreflight(
                true, true, "https://example.com", -2,
                HttpsNavigationFailurePolicyTest::isHttps, code -> code == -2));
        assertFalse(HttpsNavigationFailurePolicy.passesPreflight(
                true, true, "https://example.com", -10,
                HttpsNavigationFailurePolicyTest::isHttps, code -> code == -2));
    }

    @Test
    public void emptyPendingOriginalShortCircuitsHostResolution() {
        AtomicInteger hostCalls = new AtomicInteger();
        HttpsNavigationFailurePolicy.HostResolver resolver = url -> {
            hostCalls.incrementAndGet();
            return "example.com";
        };
        assertFalse(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "", "https://example.com", "https://example.com",
                resolver, HttpsNavigationFailurePolicyTest::sameOrSubdomain));
        assertFalse(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                null, "https://example.com", "https://example.com",
                resolver, HttpsNavigationFailurePolicyTest::sameOrSubdomain));
        assertTrue(hostCalls.get() == 0);
    }

    @Test
    public void pendingAndFailedHostsMayRelateInEitherDirection() {
        assertTrue(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "http://example.com", "https://example.com", "https://cdn.example.com",
                url -> url.contains("cdn") ? "cdn.example.com" : "example.com",
                HttpsNavigationFailurePolicyTest::sameOrSubdomain));
        assertTrue(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "http://example.com", "https://cdn.example.com", "https://example.com",
                url -> url.contains("cdn") ? "cdn.example.com" : "example.com",
                HttpsNavigationFailurePolicyTest::sameOrSubdomain));
    }

    @Test
    public void unrelatedEmptyAndMissingHostsAreRejected() {
        assertFalse(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "http://example.com", "https://example.com", "https://other.com",
                url -> url.contains("other") ? "other.com" : "example.com",
                HttpsNavigationFailurePolicyTest::sameOrSubdomain));
        assertFalse(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "http://example.com", "", "https://example.com",
                url -> url.length() == 0 ? "" : "example.com",
                HttpsNavigationFailurePolicyTest::sameOrSubdomain));
        assertFalse(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "http://example.com", "https://example.com", "https://example.com",
                null, HttpsNavigationFailurePolicyTest::sameOrSubdomain));
    }

    @Test
    public void dependencyFailuresReturnFalse() {
        assertFalse(HttpsNavigationFailurePolicy.passesPreflight(
                true, true, "https://example.com", -2,
                url -> { throw new IllegalStateException("https"); }, code -> true));
        assertFalse(HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                "http://example.com", "https://example.com", "https://example.com",
                url -> { throw new IllegalStateException("host"); },
                HttpsNavigationFailurePolicyTest::sameOrSubdomain));
    }
}
