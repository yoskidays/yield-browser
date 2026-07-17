package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class HttpsNavigationSuccessPolicyTest {
    private static boolean isHttps(String url) {
        return url != null && url.startsWith("https://");
    }

    private static boolean sameOrSubdomain(String candidate, String base) {
        return candidate.equals(base) || candidate.endsWith("." + base);
    }

    @Test
    public void nonHttpsCompletionIsIgnoredBeforeHostResolution() {
        AtomicInteger calls = new AtomicInteger();
        assertEquals(HttpsNavigationSuccessPolicy.Action.IGNORE,
                HttpsNavigationSuccessPolicy.evaluate(
                        "http://example.com", "http://example.com", "https://example.com",
                        "example.com", HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> { calls.incrementAndGet(); return "example.com"; },
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
        assertEquals(0, calls.get());
    }

    @Test
    public void noPendingOriginalClearsOnlyMatchingFallbackHost() {
        assertEquals(HttpsNavigationSuccessPolicy.Action.CLEAR_FALLBACK,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://example.com", "", "", "example.com",
                        HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> "example.com", HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
        assertEquals(HttpsNavigationSuccessPolicy.Action.KEEP_FALLBACK,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://other.com", null, "", "example.com",
                        HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> "other.com", HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
    }

    @Test
    public void relatedPendingHostsCompleteAsRelatedInEitherDirection() {
        assertEquals(HttpsNavigationSuccessPolicy.Action.COMPLETE_RELATED,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://cdn.example.com", "http://example.com", "https://example.com",
                        "", HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> url.contains("cdn") ? "cdn.example.com" : "example.com",
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
        assertEquals(HttpsNavigationSuccessPolicy.Action.COMPLETE_RELATED,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://example.com", "http://example.com", "https://cdn.example.com",
                        "", HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> url.contains("cdn") ? "cdn.example.com" : "example.com",
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
    }

    @Test
    public void unrelatedOrMissingPendingHostsCompleteAsUnrelated() {
        assertEquals(HttpsNavigationSuccessPolicy.Action.COMPLETE_UNRELATED,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://other.com", "http://example.com", "https://example.com",
                        "", HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> url.contains("other") ? "other.com" : "example.com",
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
        assertEquals(HttpsNavigationSuccessPolicy.Action.COMPLETE_UNRELATED,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://example.com", "http://example.com", "",
                        "", HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> url.length() == 0 ? "" : "example.com",
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
    }

    @Test
    public void missingOrFailingDependenciesAreIgnored() {
        assertEquals(HttpsNavigationSuccessPolicy.Action.IGNORE,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://example.com", "http://example.com", "https://example.com",
                        "", null, url -> "example.com",
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
        assertEquals(HttpsNavigationSuccessPolicy.Action.IGNORE,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://example.com", "http://example.com", "https://example.com",
                        "", HttpsNavigationSuccessPolicyTest::isHttps, null,
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
        assertEquals(HttpsNavigationSuccessPolicy.Action.IGNORE,
                HttpsNavigationSuccessPolicy.evaluate(
                        "https://example.com", "http://example.com", "https://example.com",
                        "", HttpsNavigationSuccessPolicyTest::isHttps,
                        url -> { throw new IllegalStateException("host"); },
                        HttpsNavigationSuccessPolicyTest::sameOrSubdomain));
    }
}
