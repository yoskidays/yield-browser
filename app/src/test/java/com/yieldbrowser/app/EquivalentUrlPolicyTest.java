package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EquivalentUrlPolicyTest {
    @Test
    public void schemeAndFragmentDifferencesAreIgnored() {
        assertTrue(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/path?q=1#old",
                "https://EXAMPLE.com/path?q=1#new"));
    }

    @Test
    public void defaultHttpAndHttpsPortsAreEquivalentToNoPort() {
        assertTrue(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com:80/path", "https://example.com/path"));
        assertTrue(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/path", "https://example.com:443/path"));
    }

    @Test
    public void emptyPathIsNormalizedToSlash() {
        assertTrue(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com", "https://example.com/"));
    }

    @Test
    public void hostPortPathAndQueryDifferencesAreRejected() {
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/path", "https://other.com/path"));
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com:8080/path", "https://example.com/path"));
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/a", "https://example.com/b"));
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/path?q=1", "https://example.com/path?q=2"));
    }

    @Test
    public void queryOrderAndEncodingRemainSignificant() {
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/?a=1&b=2", "https://example.com/?b=2&a=1"));
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://example.com/?q=a%20b", "https://example.com/?q=a+b"));
    }

    @Test
    public void malformedAndMissingUrlsReturnFalse() {
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                null, "https://example.com"));
        assertFalse(EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(
                "http://exa mple.com", "https://example.com"));
    }
}
