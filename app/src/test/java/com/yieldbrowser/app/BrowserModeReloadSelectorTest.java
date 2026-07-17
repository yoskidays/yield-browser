package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BrowserModeReloadSelectorTest {
    @Test
    public void preservesWebViewAddressAndFallbackPriority() {
        List<String> checked = new ArrayList<>();
        String selected = BrowserModeReloadSelector.select(
                new String[]{"web", "address", "fallback"},
                value -> "original-" + value,
                (url, explicit) -> {
                    checked.add(url + ":" + explicit);
                    return url.equals("original-address");
                });

        assertEquals("original-address", selected);
        assertEquals(java.util.Arrays.asList(
                "original-web:true", "original-address:true"), checked);
    }

    @Test
    public void fallsBackToRawCandidateWhenMappedUrlIsBlank() {
        String selected = BrowserModeReloadSelector.select(
                new String[]{"raw-current"},
                value -> " ",
                (url, explicit) -> url.equals("raw-current") && explicit);
        assertEquals("raw-current", selected);
    }

    @Test
    public void marksOnlyFirstTwoCandidatesAsExplicitCurrentPage() {
        List<Boolean> explicitFlags = new ArrayList<>();
        String selected = BrowserModeReloadSelector.select(
                new String[]{"web", "address", "fallback"},
                value -> value,
                (url, explicit) -> {
                    explicitFlags.add(explicit);
                    return url.equals("fallback");
                });
        assertEquals("fallback", selected);
        assertEquals(java.util.Arrays.asList(true, true, false), explicitFlags);
    }

    @Test
    public void returnsNullWhenNoCandidateIsSafe() {
        assertNull(BrowserModeReloadSelector.select(
                new String[]{"one", "two"},
                value -> value,
                (url, explicit) -> false));
    }
}
