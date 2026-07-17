package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdBlockHostPolicyTest {
    @Test
    public void normalizeRejectsMissingInputResolverOrHost() {
        assertEquals("", AdBlockHostPolicy.normalize(null, url -> "example.com"));
        assertEquals("", AdBlockHostPolicy.normalize("", url -> "example.com"));
        assertEquals("", AdBlockHostPolicy.normalize("https://example.com", null));
        assertEquals("", AdBlockHostPolicy.normalize("https://example.com", url -> null));
        assertEquals("", AdBlockHostPolicy.normalize("https://example.com", url -> ""));
    }

    @Test
    public void normalizeLowercasesAndRemovesSingleWwwPrefix() {
        assertEquals("example.com", AdBlockHostPolicy.normalize(
                "https://WWW.Example.COM/path", url -> "WWW.Example.COM"));
        assertEquals("www.example.com", AdBlockHostPolicy.normalize(
                "https://www.www.example.com", url -> "www.www.example.com"));
    }

    @Test
    public void normalizationFailureReturnsEmptyHost() {
        assertEquals("", AdBlockHostPolicy.normalize(
                "https://example.com", url -> { throw new IllegalStateException("resolver"); }));
    }

    @Test
    public void sameHostAndSubdomainAreRelated() {
        assertTrue(AdBlockHostPolicy.sameOrSubDomain("example.com", "example.com"));
        assertTrue(AdBlockHostPolicy.sameOrSubDomain("cdn.example.com", "example.com"));
        assertTrue(AdBlockHostPolicy.sameOrSubDomain("a.b.example.com", "example.com"));
    }

    @Test
    public void suffixWithoutDotAndInvalidHostsAreRejected() {
        assertFalse(AdBlockHostPolicy.sameOrSubDomain("notexample.com", "example.com"));
        assertFalse(AdBlockHostPolicy.sameOrSubDomain("example.com.evil", "example.com"));
        assertFalse(AdBlockHostPolicy.sameOrSubDomain(null, "example.com"));
        assertFalse(AdBlockHostPolicy.sameOrSubDomain("example.com", null));
        assertFalse(AdBlockHostPolicy.sameOrSubDomain("", "example.com"));
        assertFalse(AdBlockHostPolicy.sameOrSubDomain("example.com", ""));
    }
}
