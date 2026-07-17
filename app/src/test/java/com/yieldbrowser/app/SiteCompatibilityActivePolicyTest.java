package com.yieldbrowser.app;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SiteCompatibilityActivePolicyTest {
    @Test
    public void emptyHostIsInactive() {
        SiteCompatibilityActivePolicy.Result result =
                SiteCompatibilityActivePolicy.evaluate(
                        "", "example.com", 1000L, new LinkedHashMap<>(), 100L);
        assertFalse(result.active);
        assertTrue(result.expiredHosts.isEmpty());
    }

    @Test
    public void activeLegacyHostMatchesExactAndSubdomain() {
        assertTrue(SiteCompatibilityActivePolicy.evaluate(
                "WWW.Example.com", "example.com", 1000L,
                new LinkedHashMap<>(), 1000L).active);
        assertTrue(SiteCompatibilityActivePolicy.evaluate(
                "sub.example.com", "WWW.Example.com", 1000L,
                new LinkedHashMap<>(), 999L).active);
    }

    @Test
    public void expiredLegacyFallsThroughToMultiHostMap() {
        Map<String, Long> hosts = new LinkedHashMap<>();
        hosts.put("example.com", 2000L);
        SiteCompatibilityActivePolicy.Result result =
                SiteCompatibilityActivePolicy.evaluate(
                        "sub.example.com", "example.com", 999L, hosts, 1000L);
        assertTrue(result.active);
    }

    @Test
    public void activeMapMatchDiscardsPreviouslyCollectedExpiredHosts() {
        Map<String, Long> hosts = new LinkedHashMap<>();
        hosts.put("expired.com", 10L);
        hosts.put("example.com", 1000L);
        SiteCompatibilityActivePolicy.Result result =
                SiteCompatibilityActivePolicy.evaluate(
                        "sub.example.com", "", 0L, hosts, 100L);
        assertTrue(result.active);
        assertTrue(result.expiredHosts.isEmpty());
    }

    @Test
    public void inactiveResultReturnsExpiredHostsForRemoval() {
        Map<String, Long> hosts = new LinkedHashMap<>();
        hosts.put("expired.com", 99L);
        hosts.put("still-active.com", 100L);
        hosts.put("null-expiry.com", null);
        SiteCompatibilityActivePolicy.Result result =
                SiteCompatibilityActivePolicy.evaluate(
                        "other.com", "", 0L, hosts, 100L);
        assertFalse(result.active);
        assertEquals(2, result.expiredHosts.size());
        assertEquals("expired.com", result.expiredHosts.get(0));
        assertEquals("null-expiry.com", result.expiredHosts.get(1));
    }

    @Test
    public void expiryBoundaryRemainsActiveUntilNowPassesDeadline() {
        Map<String, Long> hosts = new LinkedHashMap<>();
        hosts.put("example.com", 100L);
        assertTrue(SiteCompatibilityActivePolicy.evaluate(
                "example.com", "", 0L, hosts, 100L).active);
        SiteCompatibilityActivePolicy.Result expired =
                SiteCompatibilityActivePolicy.evaluate(
                        "example.com", "", 0L, hosts, 101L);
        assertFalse(expired.active);
        assertEquals("example.com", expired.expiredHosts.get(0));
    }

    @Test
    public void domainMatchingDoesNotAcceptUnrelatedSuffix() {
        assertFalse(SiteCompatibilityActivePolicy.matches("notexample.com", "example.com"));
        assertTrue(SiteCompatibilityActivePolicy.matches("a.example.com", "example.com"));
    }
}
