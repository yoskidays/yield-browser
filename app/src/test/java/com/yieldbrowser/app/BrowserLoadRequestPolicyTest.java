package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserLoadRequestPolicyTest {
    @Test
    public void trimsInputAndRecognizesDirectWebViewSchemesCaseInsensitively() {
        assertEquals("https://example.com", BrowserLoadRequestPolicy.trimInput(
                "  https://example.com  "));
        assertEquals("", BrowserLoadRequestPolicy.trimInput(null));

        assertTrue(BrowserLoadRequestPolicy.isDirectWebViewUrl("javascript:alert(1)"));
        assertTrue(BrowserLoadRequestPolicy.isDirectWebViewUrl("ABOUT:blank"));
        assertTrue(BrowserLoadRequestPolicy.isDirectWebViewUrl("Data:text/plain,hello"));
        assertFalse(BrowserLoadRequestPolicy.isDirectWebViewUrl("https://example.com"));
    }

    @Test
    public void buildsDesktopHeadersInStableOrder() {
        Map<String, String> headers = BrowserLoadRequestPolicy.requestHeaders(
                true, "mobile-agent", "desktop-agent");

        assertEquals(Arrays.asList(
                        "User-Agent",
                        "Sec-CH-UA-Mobile",
                        "Sec-CH-UA-Platform",
                        "Upgrade-Insecure-Requests",
                        "Accept-Language"),
                new ArrayList<>(headers.keySet()));
        assertEquals("desktop-agent", headers.get("User-Agent"));
        assertEquals("?0", headers.get("Sec-CH-UA-Mobile"));
        assertEquals("\"Windows\"", headers.get("Sec-CH-UA-Platform"));
        assertEquals("1", headers.get("Upgrade-Insecure-Requests"));
        assertEquals("id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7",
                headers.get("Accept-Language"));
    }

    @Test
    public void buildsMobileHeadersAndNormalizesNullUserAgent() {
        Map<String, String> headers = BrowserLoadRequestPolicy.requestHeaders(
                false, null, "desktop-agent");

        assertEquals("", headers.get("User-Agent"));
        assertEquals("?1", headers.get("Sec-CH-UA-Mobile"));
        assertEquals("\"Android\"", headers.get("Sec-CH-UA-Platform"));
    }
}
