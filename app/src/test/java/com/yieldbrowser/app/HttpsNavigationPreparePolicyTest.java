package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsNavigationPreparePolicyTest {
    private static boolean isHttp(String url) {
        return url != null && url.startsWith("http://");
    }

    @Test
    public void nullAndDisabledNavigationReturnWithoutStateChanges() {
        HttpsNavigationPreparePolicy.Result nullResult = HttpsNavigationPreparePolicy.prepare(
                null, true, true, true, "http://example.com",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> "https://example.com");
        assertEquals(null, nullResult.targetUrl);
        assertFalse(nullResult.consumeFallbackInProgress);

        HttpsNavigationPreparePolicy.Result disabled = HttpsNavigationPreparePolicy.prepare(
                "  http://example.com  ", false, true, true, "http://example.com",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> "https://example.com");
        assertEquals("http://example.com", disabled.targetUrl);
        assertFalse(disabled.consumeFallbackInProgress);
        assertFalse(disabled.clearPendingState);
        assertFalse(disabled.startPendingState);
    }

    @Test
    public void nonHttpAndExemptUrlsShortCircuitBeforeFallbackHandling() {
        AtomicInteger downstreamCalls = new AtomicInteger();
        HttpsNavigationPreparePolicy.Result nonHttp = HttpsNavigationPreparePolicy.prepare(
                "https://example.com", true, true, true, "https://example.com",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> { downstreamCalls.incrementAndGet(); return false; },
                url -> { downstreamCalls.incrementAndGet(); return url; });
        assertEquals("https://example.com", nonHttp.targetUrl);
        assertEquals(0, downstreamCalls.get());

        HttpsNavigationPreparePolicy.Result exempt = HttpsNavigationPreparePolicy.prepare(
                "http://localhost", true, true, true, "http://localhost",
                HttpsNavigationPreparePolicyTest::isHttp, url -> true,
                url -> { downstreamCalls.incrementAndGet(); return false; },
                url -> { downstreamCalls.incrementAndGet(); return url; });
        assertEquals("http://localhost", exempt.targetUrl);
        assertEquals(0, downstreamCalls.get());
    }

    @Test
    public void matchingFallbackConsumesAndClearsPendingState() {
        HttpsNavigationPreparePolicy.Result result = HttpsNavigationPreparePolicy.prepare(
                "http://example.com", true, true, true, "http://example.com",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> "https://example.com");
        assertEquals("http://example.com", result.targetUrl);
        assertTrue(result.consumeFallbackInProgress);
        assertTrue(result.clearPendingState);
        assertFalse(result.startPendingState);
    }

    @Test
    public void nonmatchingFallbackIsConsumedBeforeGuardOrUpgrade() {
        HttpsNavigationPreparePolicy.Result guarded = HttpsNavigationPreparePolicy.prepare(
                "http://example.com", true, true, true, "http://other.com",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> true, url -> "https://example.com");
        assertEquals("http://example.com", guarded.targetUrl);
        assertTrue(guarded.consumeFallbackInProgress);
        assertFalse(guarded.clearPendingState);
        assertFalse(guarded.startPendingState);
    }

    @Test
    public void successfulUpgradeStartsPendingStateOnlyWhenTabExists() {
        HttpsNavigationPreparePolicy.Result withTab = HttpsNavigationPreparePolicy.prepare(
                "http://example.com", true, true, false, "",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> "https://example.com");
        assertEquals("https://example.com", withTab.targetUrl);
        assertTrue(withTab.startPendingState);

        HttpsNavigationPreparePolicy.Result withoutTab = HttpsNavigationPreparePolicy.prepare(
                "http://example.com", true, false, false, "",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> "https://example.com");
        assertEquals("https://example.com", withoutTab.targetUrl);
        assertFalse(withoutTab.startPendingState);
    }

    @Test
    public void nullOrUnchangedUpgradeReturnsCleanUrl() {
        HttpsNavigationPreparePolicy.Result unchanged = HttpsNavigationPreparePolicy.prepare(
                " http://example.com ", true, true, false, "",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> url);
        assertEquals("http://example.com", unchanged.targetUrl);
        assertFalse(unchanged.startPendingState);

        HttpsNavigationPreparePolicy.Result missing = HttpsNavigationPreparePolicy.prepare(
                "http://example.com", true, true, false, "",
                HttpsNavigationPreparePolicyTest::isHttp, url -> false,
                url -> false, url -> null);
        assertEquals("http://example.com", missing.targetUrl);
        assertFalse(missing.startPendingState);
    }
}
