package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class LongPressLinkPolicyTest {
    @Test
    public void preservesAbsoluteHttpsLink() {
        assertEquals("https://example.com/chapter/2",
                LongPressLinkPolicy.resolveHttpUrl(
                        "https://example.com/chapter/2", "https://example.com/chapter/1"));
    }

    @Test
    public void resolvesRelativeLinkAgainstCurrentPage() {
        assertEquals("https://example.com/manga/chapter-2",
                LongPressLinkPolicy.resolveHttpUrl(
                        "chapter-2", "https://example.com/manga/chapter-1"));
    }

    @Test
    public void resolvesRootRelativeAndProtocolRelativeLinks() {
        assertEquals("https://example.com/next",
                LongPressLinkPolicy.resolveHttpUrl(
                        "/next", "https://example.com/current"));
        assertEquals("http://cdn.example.com/page",
                LongPressLinkPolicy.resolveHttpUrl(
                        "//cdn.example.com/page", "http://example.com/current"));
    }

    @Test
    public void rejectsNonWebSchemesAndInvalidValues() {
        assertNull(LongPressLinkPolicy.resolveHttpUrl(
                "javascript:alert(1)", "https://example.com"));
        assertNull(LongPressLinkPolicy.resolveHttpUrl(
                "mailto:user@example.com", "https://example.com"));
        assertNull(LongPressLinkPolicy.resolveHttpUrl("", "https://example.com"));
        assertNull(LongPressLinkPolicy.resolveHttpUrl("next", "about:blank"));
    }
}
