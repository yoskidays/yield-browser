package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsBookmarkUpgradePolicyTest {
    private static boolean isHttp(String url) {
        return url != null && url.startsWith("http://");
    }

    private static boolean isHttps(String url) {
        return url != null && url.startsWith("https://");
    }

    @Test
    public void eligibilityRequiresHttpOriginalAndHttpsFinal() {
        assertTrue(HttpsBookmarkUpgradePolicy.isEligible(
                "http://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                HttpsBookmarkUpgradePolicyTest::isHttps));
        assertFalse(HttpsBookmarkUpgradePolicy.isEligible(
                "https://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                HttpsBookmarkUpgradePolicyTest::isHttps));
        assertFalse(HttpsBookmarkUpgradePolicy.isEligible(
                "http://example.com", "http://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                HttpsBookmarkUpgradePolicyTest::isHttps));
    }

    @Test
    public void nonHttpBookmarkShortCircuitsUpgradeAndRelation() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "https://example.com", "http://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                url -> { calls.incrementAndGet(); return url; },
                (first, second) -> { calls.incrementAndGet(); return true; }));
        assertTrue(calls.get() == 0);
    }

    @Test
    public void originalEquivalentBookmarkUpgradesWithoutSecondRelationCheck() {
        AtomicInteger relationCalls = new AtomicInteger();
        assertTrue(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "http://example.com/path", "http://example.com/path", "https://example.com/path",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                url -> "https://example.com/path",
                (first, second) -> {
                    relationCalls.incrementAndGet();
                    return first.equals(second);
                }));
        assertTrue(relationCalls.get() == 1);
    }

    @Test
    public void upgradedCandidateMayMatchFinalUrl() {
        assertTrue(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "http://example.com/path", "http://other.com", "https://example.com/path",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                url -> "https://example.com/path",
                String::equals));
    }

    @Test
    public void unrelatedBookmarkAndMissingDependenciesAreRejected() {
        assertFalse(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "http://other.com", "http://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                url -> "https://other.com", String::equals));
        assertFalse(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "http://example.com", "http://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp, null, String::equals));
        assertFalse(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "http://example.com", "http://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                url -> "https://example.com", null));
    }

    @Test
    public void dependencyFailuresReturnFalse() {
        assertFalse(HttpsBookmarkUpgradePolicy.isEligible(
                "http://example.com", "https://example.com",
                url -> { throw new IllegalStateException("http"); },
                HttpsBookmarkUpgradePolicyTest::isHttps));
        assertFalse(HttpsBookmarkUpgradePolicy.shouldUpgrade(
                "http://example.com", "http://example.com", "https://example.com",
                HttpsBookmarkUpgradePolicyTest::isHttp,
                url -> { throw new IllegalStateException("upgrade"); }, String::equals));
    }
}
