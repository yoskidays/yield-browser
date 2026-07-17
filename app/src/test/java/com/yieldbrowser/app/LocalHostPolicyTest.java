package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalHostPolicyTest {
    @Test
    public void privateIpv4RangesAreRecognized() {
        String[] hosts = new String[]{
                "10.0.0.1", "127.0.0.1", "169.254.10.20", "172.16.0.1",
                "172.31.255.255", "192.168.1.10", "0.12.34.56"
        };
        for (String host : hosts) assertTrue(host, LocalHostPolicy.isPrivateIpv4(host));
    }

    @Test
    public void publicAndMalformedIpv4HostsAreRejected() {
        String[] hosts = new String[]{
                "8.8.8.8", "172.15.0.1", "172.32.0.1", "192.169.1.1",
                "1.2.3", "1.2.3.4.5", "1..3.4", "256.1.1.1", "1.2.3.-1", "abcd"
        };
        for (String host : hosts) assertFalse(host, LocalHostPolicy.isPrivateIpv4(host));
    }

    @Test
    public void nullEmptyLocalhostAndInternalSuffixesAreLocal() {
        assertTrue(LocalHostPolicy.isLocalOrPrivate(null));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("  "));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("LOCALHOST"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("service.internal"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("router.lan"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("hidden.onion"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("singlelabel"));
    }

    @Test
    public void loopbackAndPrivateIpv6FormsAreLocal() {
        assertTrue(LocalHostPolicy.isLocalOrPrivate("[::1]"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("0:0:0:0:0:0:0:1"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("fc00::1"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("fd12::1"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("fe80::1"));
        assertTrue(LocalHostPolicy.isLocalOrPrivate("febf::1"));
    }

    @Test
    public void publicDnsAndGlobalIpv6AreNotLocal() {
        assertFalse(LocalHostPolicy.isLocalOrPrivate("example.com"));
        assertFalse(LocalHostPolicy.isLocalOrPrivate("8.8.8.8"));
        assertFalse(LocalHostPolicy.isLocalOrPrivate("2001:4860:4860::8888"));
    }
}
