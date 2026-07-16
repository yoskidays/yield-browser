package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YouTubeAdGuardScriptTest {
    @Test
    public void installsOnlyOnYouTubeWebPages() {
        assertTrue(YouTubeAdGuardScript.shouldInstall("https://www.youtube.com/watch?v=abc"));
        assertTrue(YouTubeAdGuardScript.shouldInstall("https://m.youtube.com/watch?v=abc"));
        assertTrue(YouTubeAdGuardScript.shouldInstall("https://www.youtube-nocookie.com/embed/abc"));

        assertFalse(YouTubeAdGuardScript.shouldInstall("https://youtu.be/abc"));
        assertFalse(YouTubeAdGuardScript.shouldInstall("https://example.com/youtube.com/watch"));
        assertFalse(YouTubeAdGuardScript.shouldInstall("intent://www.youtube.com/watch?v=abc"));
    }

    @Test
    public void scriptSkipsAndHidesAdsWithoutBlockingVideoDeliveryHosts() {
        String script = YouTubeAdGuardScript.script();

        assertTrue(script.contains("ytp-ad-skip-button"));
        assertTrue(script.contains("ytp-ad-overlay-close-button"));
        assertTrue(script.contains("ad-showing"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("ytd-in-feed-ad-layout-renderer"));
        assertTrue(script.contains("playbackRate=16"));
        assertTrue(script.contains("S.oldMuted"));
        assertTrue(script.contains("__yieldShieldV2State"));
        assertTrue(script.contains("restoreHidden"));
        assertTrue(script.contains("data-yield-yt-ad-hidden"));

        assertFalse(script.contains("googlevideo"));
        assertFalse(script.contains("XMLHttpRequest"));
        assertFalse(script.contains("window.fetch="));
    }
}
