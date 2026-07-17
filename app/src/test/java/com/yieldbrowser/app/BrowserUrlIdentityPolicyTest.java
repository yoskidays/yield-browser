package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowserUrlIdentityPolicyTest {
    @Test
    public void normalizesMappedHostAndRemovesWwwPrefix() {
        assertEquals(
                "example.com",
                BrowserUrlIdentityPolicy.normalizedHost(
                        "wrapped",
                        value -> "https://WWW.Example.com/path",
                        value -> "WWW.Example.com"));
    }

    @Test
    public void fallsBackToRawUrlWhenMappedValueIsBlank() {
        assertEquals(
                "example.com",
                BrowserUrlIdentityPolicy.normalizedHost(
                        "https://example.com/path",
                        value -> " ",
                        value -> "example.com"));
    }

    @Test
    public void missingOrNullHostReturnsEmpty() {
        assertEquals("", BrowserUrlIdentityPolicy.normalizedHost(
                "https://example.com", value -> value, null));
        assertEquals("", BrowserUrlIdentityPolicy.normalizedHost(
                "about:blank", value -> value, value -> null));
    }

    @Test
    public void hostDependencyFailureReturnsEmpty() {
        assertEquals("", BrowserUrlIdentityPolicy.normalizedHost(
                "https://example.com",
                value -> { throw new IllegalStateException("mapper"); },
                value -> "example.com"));
        assertEquals("", BrowserUrlIdentityPolicy.normalizedHost(
                "https://example.com",
                value -> value,
                value -> { throw new IllegalStateException("parser"); }));
    }

    @Test
    public void navigationLoopKeyUsesOriginalUrlAndRemovesFragment() {
        assertEquals(
                "https://example.com/path?q=1",
                BrowserUrlIdentityPolicy.navigationLoopKey(
                        "wrapped",
                        value -> "  HTTPS://Example.COM/path?q=1#section  "));
    }

    @Test
    public void navigationLoopKeyFallsBackToRawWhenMappedBlank() {
        assertEquals(
                "https://example.com/path",
                BrowserUrlIdentityPolicy.navigationLoopKey(
                        " HTTPS://Example.com/path#top ",
                        value -> ""));
    }

    @Test
    public void navigationLoopKeyReturnsRawNormalizedWhenMapperThrows() {
        assertEquals(
                "https://example.com/path#top",
                BrowserUrlIdentityPolicy.navigationLoopKey(
                        " HTTPS://Example.com/path#top ",
                        value -> { throw new IllegalStateException("mapper"); }));
    }

    @Test
    public void navigationLoopKeyHandlesNull() {
        assertEquals("", BrowserUrlIdentityPolicy.navigationLoopKey(null, value -> value));
    }
}
