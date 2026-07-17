package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlSchemePolicyTest {
    @Test
    public void nullEmptyAndNonWebSchemesAreRejected() {
        assertFalse(UrlSchemePolicy.isHttpOrHttps(null));
        assertFalse(UrlSchemePolicy.isHttpOrHttps(""));
        assertFalse(UrlSchemePolicy.isHttpOrHttps("about:blank"));
        assertFalse(UrlSchemePolicy.isHttp("file:///tmp/a"));
        assertFalse(UrlSchemePolicy.isHttps("javascript:alert(1)"));
    }

    @Test
    public void predicatesTrimAndIgnoreSchemeCase() {
        assertTrue(UrlSchemePolicy.isHttpOrHttps("  HTTP://Example.com  "));
        assertTrue(UrlSchemePolicy.isHttpOrHttps("\nHTTPS://Example.com/path\t"));
        assertTrue(UrlSchemePolicy.isHttp("  HTTP://Example.com"));
        assertTrue(UrlSchemePolicy.isHttps(" HTTPS://Example.com"));
    }

    @Test
    public void specificPredicatesDoNotAcceptTheOtherWebScheme() {
        assertFalse(UrlSchemePolicy.isHttp("https://example.com"));
        assertFalse(UrlSchemePolicy.isHttps("http://example.com"));
    }

    @Test
    public void lookalikePrefixesAreRejected() {
        assertFalse(UrlSchemePolicy.isHttpOrHttps("httpx://example.com"));
        assertFalse(UrlSchemePolicy.isHttpOrHttps("xhttps://example.com"));
        assertFalse(UrlSchemePolicy.isHttp("http:example.com"));
        assertFalse(UrlSchemePolicy.isHttps("https:example.com"));
    }
}
