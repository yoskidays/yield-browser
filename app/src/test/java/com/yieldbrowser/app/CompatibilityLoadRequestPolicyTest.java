package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CompatibilityLoadRequestPolicyTest {
    @Test
    public void mobileModeUsesNoCustomHeaders() {
        assertTrue(CompatibilityLoadRequestPolicy.requestHeaders(
                false, "desktop-agent").isEmpty());
    }

    @Test
    public void desktopModeUsesMinimalHeadersInStableOrder() {
        Map<String, String> headers = CompatibilityLoadRequestPolicy.requestHeaders(
                true, "desktop-agent");
        assertEquals(Arrays.asList("User-Agent", "Accept-Language"),
                new ArrayList<>(headers.keySet()));
        assertEquals("desktop-agent", headers.get("User-Agent"));
        assertEquals("id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7",
                headers.get("Accept-Language"));
    }

    @Test
    public void nullDesktopUserAgentIsPreserved() {
        Map<String, String> headers = CompatibilityLoadRequestPolicy.requestHeaders(
                true, null);
        assertTrue(headers.containsKey("User-Agent"));
        assertNull(headers.get("User-Agent"));
    }
}
