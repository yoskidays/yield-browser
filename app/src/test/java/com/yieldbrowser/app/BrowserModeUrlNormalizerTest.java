package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BrowserModeUrlNormalizerTest {
    @Test
    public void returnsNullForNullInput() {
        assertNull(BrowserModeUrlNormalizer.normalize(
                null, false, value -> value, value -> true, value -> true));
    }

    @Test
    public void usesMappedOriginalUrlWhenAvailable() {
        String normalized = BrowserModeUrlNormalizer.normalize(
                "wrapped-url",
                false,
                value -> "https://www.youtube.com/watch?v=1",
                value -> true,
                value -> true);
        assertEquals("https://m.youtube.com/watch?v=1", normalized);
    }

    @Test
    public void fallsBackToRawUrlWhenMappedValueIsBlank() {
        String normalized = BrowserModeUrlNormalizer.normalize(
                "https://example.com/page",
                false,
                value -> " ",
                value -> true,
                value -> false);
        assertEquals("https://example.com/page", normalized);
    }

    @Test
    public void leavesNonWebUrlUnchangedWithoutCheckingYouTube() {
        AtomicBoolean youtubeChecked = new AtomicBoolean(false);
        String normalized = BrowserModeUrlNormalizer.normalize(
                "about:blank",
                true,
                value -> value,
                value -> false,
                value -> {
                    youtubeChecked.set(true);
                    return true;
                });
        assertEquals("about:blank", normalized);
        assertEquals(false, youtubeChecked.get());
    }

    @Test
    public void desktopModeConvertsMobileYouTubeHostsToSecureWww() {
        assertEquals(
                "https://www.youtube.com/watch?v=1",
                BrowserModeUrlNormalizer.normalizeYouTubeHost(
                        "https://m.youtube.com/watch?v=1", true));
        assertEquals(
                "https://www.youtube.com/watch?v=2",
                BrowserModeUrlNormalizer.normalizeYouTubeHost(
                        "http://m.youtube.com/watch?v=2", true));
    }

    @Test
    public void mobileModeConvertsWwwAndBareYouTubeHostsToSecureMobile() {
        assertEquals(
                "https://m.youtube.com/watch?v=1",
                BrowserModeUrlNormalizer.normalizeYouTubeHost(
                        "https://www.youtube.com/watch?v=1", false));
        assertEquals(
                "https://m.youtube.com/watch?v=2",
                BrowserModeUrlNormalizer.normalizeYouTubeHost(
                        "http://www.youtube.com/watch?v=2", false));
        assertEquals(
                "https://m.youtube.com/watch?v=3",
                BrowserModeUrlNormalizer.normalizeYouTubeHost(
                        "https://youtube.com/watch?v=3", false));
        assertEquals(
                "https://m.youtube.com/watch?v=4",
                BrowserModeUrlNormalizer.normalizeYouTubeHost(
                        "http://youtube.com/watch?v=4", false));
    }

    @Test
    public void leavesNonYouTubeWebUrlUnchanged() {
        String normalized = BrowserModeUrlNormalizer.normalize(
                "https://example.com/page",
                true,
                value -> value,
                value -> true,
                value -> false);
        assertEquals("https://example.com/page", normalized);
    }

    @Test
    public void returnsOriginalInputWhenDependencyThrows() {
        String normalized = BrowserModeUrlNormalizer.normalize(
                "wrapped-url",
                true,
                value -> {
                    throw new IllegalStateException("broken mapper");
                },
                value -> true,
                value -> true);
        assertEquals("wrapped-url", normalized);
    }
}
