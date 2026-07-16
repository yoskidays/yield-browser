package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistoryFaviconPolicyTest {
    @Test
    public void normalizesHostIntoStableCacheKey() {
        assertEquals("example.com",
                HistoryFaviconPolicy.requestKey("https://www.Example.com/path?q=1"));
        assertEquals("sub.example.com",
                HistoryFaviconPolicy.requestKey("http://Sub.Example.com/a"));
        assertEquals("", HistoryFaviconPolicy.requestKey(null));
    }

    @Test
    public void buildsEncodedGoogleFaviconRequest() {
        String request = HistoryFaviconPolicy.requestUrl(
                "https://example.com/a path?q=hello world");

        assertTrue(request.startsWith(
                "https://www.google.com/s2/favicons?sz=96&domain_url="));
        assertTrue(request.endsWith(
                "https%3A%2F%2Fexample.com%2Fa+path%3Fq%3Dhello+world"));
        assertEquals("", HistoryFaviconPolicy.requestUrl("  "));
    }

    @Test
    public void rejectsBitmapForRecycledTarget() {
        assertTrue(HistoryFaviconPolicy.matchesTarget("example.com", "example.com"));
        assertFalse(HistoryFaviconPolicy.matchesTarget("example.com", "other.example"));
        assertFalse(HistoryFaviconPolicy.matchesTarget("example.com", null));
        assertFalse(HistoryFaviconPolicy.matchesTarget(null, "example.com"));
    }
}
