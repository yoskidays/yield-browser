package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsFirstExemptionPolicyTest {
    private static boolean isHttp(String url) {
        return url != null && url.trim().toLowerCase().startsWith("http://");
    }

    @Test
    public void nonHttpUrlsAreExemptBeforeHostClassification() {
        AtomicInteger hostCalls = new AtomicInteger();
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "https://example.com", HttpsFirstExemptionPolicyTest::isHttp,
                host -> { hostCalls.incrementAndGet(); return false; }));
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                null, HttpsFirstExemptionPolicyTest::isHttp,
                host -> { hostCalls.incrementAndGet(); return false; }));
        assertTrue(hostCalls.get() == 0);
    }

    @Test
    public void publicHttpDefaultPortsAreEligibleForUpgrade() {
        assertFalse(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com/path", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
        assertFalse(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com:80/path", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
        assertFalse(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com:443/path", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
    }

    @Test
    public void localPrivateAndMissingHostsAreExempt() {
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://localhost/path", HttpsFirstExemptionPolicyTest::isHttp,
                host -> host.equals("localhost")));
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http:///missing-host", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
    }

    @Test
    public void nonstandardPortsAreExempt() {
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com:8080/path", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com:8443/path", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
    }

    @Test
    public void malformedUrlsAndMissingDependenciesFailExempt() {
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://exa mple.com", HttpsFirstExemptionPolicyTest::isHttp,
                host -> false));
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com", null, host -> false));
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com", HttpsFirstExemptionPolicyTest::isHttp, null));
        assertTrue(HttpsFirstExemptionPolicy.isExempt(
                "http://example.com", HttpsFirstExemptionPolicyTest::isHttp,
                host -> { throw new IllegalStateException("host"); }));
    }
}
