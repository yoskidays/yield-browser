package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BrowserLoadExecutionCoordinatorTest {
    @Test
    public void normalLoadPreservesPreparationSettingsHeadersAndLoadOrder() {
        List<String> events = new ArrayList<>();

        BrowserLoadExecutionCoordinator.execute(
                "https://example.com", false, false,
                () -> events.add("state"),
                () -> events.add("trusted"),
                () -> events.add("tab"),
                () -> events.add("enable-compat"),
                () -> events.add("plain"),
                url -> events.add("compat-load"),
                delay -> events.add("viewport-" + delay),
                url -> events.add("fallback-schedule"),
                () -> events.add("settings"),
                () -> {
                    events.add("headers");
                    return Collections.singletonMap("User-Agent", "ua");
                },
                (url, headers) -> events.add("normal-load"),
                url -> events.add("fallback-load"));

        assertEquals(java.util.Arrays.asList(
                "state", "trusted", "tab", "settings", "headers", "normal-load"),
                events);
    }

    @Test
    public void strictCompatibilityLoadPreservesSettingsLoadViewportAndFallbackOrder() {
        List<String> events = new ArrayList<>();

        BrowserLoadExecutionCoordinator.execute(
                "https://legacy.example", true, true,
                () -> events.add("state"),
                () -> events.add("trusted"),
                () -> events.add("tab"),
                () -> events.add("enable-compat"),
                () -> events.add("plain"),
                url -> events.add("compat-load"),
                delay -> events.add("viewport-" + delay),
                url -> events.add("fallback-schedule"),
                () -> events.add("settings"),
                Collections::emptyMap,
                (url, headers) -> events.add("normal-load"),
                url -> events.add("fallback-load"));

        assertEquals(java.util.Arrays.asList(
                "state", "trusted", "tab", "enable-compat", "plain", "compat-load",
                "viewport-350", "viewport-1300", "viewport-2800", "fallback-schedule"),
                events);
    }

    @Test
    public void failedPrimaryLoadUsesPlainFallbackOnce() {
        List<String> events = new ArrayList<>();

        BrowserLoadExecutionCoordinator.execute(
                "https://example.com", false, false,
                null, null, null, null, null, null, null, null,
                () -> events.add("settings"),
                Collections::emptyMap,
                (url, headers) -> { throw new IllegalStateException("boom"); },
                url -> events.add("fallback-load"));

        assertEquals(java.util.Arrays.asList("settings", "fallback-load"), events);
    }
}
