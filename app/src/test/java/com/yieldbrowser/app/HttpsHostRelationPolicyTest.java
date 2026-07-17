package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsHostRelationPolicyTest {
    private static boolean sameOrSubdomain(String candidate, String base) {
        return candidate.equals(base) || candidate.endsWith("." + base);
    }

    @Test
    public void sameHostsAndEitherSubdomainDirectionAreRelated() {
        assertTrue(HttpsHostRelationPolicy.areRelated(
                "example.com", "example.com", HttpsHostRelationPolicyTest::sameOrSubdomain));
        assertTrue(HttpsHostRelationPolicy.areRelated(
                "cdn.example.com", "example.com", HttpsHostRelationPolicyTest::sameOrSubdomain));
        assertTrue(HttpsHostRelationPolicy.areRelated(
                "example.com", "cdn.example.com", HttpsHostRelationPolicyTest::sameOrSubdomain));
    }

    @Test
    public void unrelatedAndMissingHostsAreRejected() {
        assertFalse(HttpsHostRelationPolicy.areRelated(
                "example.com", "other.com", HttpsHostRelationPolicyTest::sameOrSubdomain));
        assertFalse(HttpsHostRelationPolicy.areRelated(
                null, "example.com", HttpsHostRelationPolicyTest::sameOrSubdomain));
        assertFalse(HttpsHostRelationPolicy.areRelated(
                "", "example.com", HttpsHostRelationPolicyTest::sameOrSubdomain));
        assertFalse(HttpsHostRelationPolicy.areRelated(
                "example.com", "", HttpsHostRelationPolicyTest::sameOrSubdomain));
        assertFalse(HttpsHostRelationPolicy.areRelated(
                "example.com", "example.com", null));
    }

    @Test
    public void reverseCheckIsLazyWhenForwardDirectionMatches() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(HttpsHostRelationPolicy.areRelated(
                "cdn.example.com", "example.com", (candidate, base) -> {
                    calls.incrementAndGet();
                    return candidate.endsWith("." + base);
                }));
        assertTrue(calls.get() == 1);
    }

    @Test
    public void relationFailureReturnsFalse() {
        assertFalse(HttpsHostRelationPolicy.areRelated(
                "example.com", "example.com",
                (candidate, base) -> { throw new IllegalStateException("relation"); }));
    }
}
