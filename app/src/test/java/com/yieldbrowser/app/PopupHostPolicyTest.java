package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PopupHostPolicyTest {
    @Test
    public void emptyHostRejectsBeforeTrustedDownloadCheck() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(PopupHostPolicy.isKnown(
                "https://example.com", "", url -> { calls.incrementAndGet(); return false; }));
        assertTrue(calls.get() == 0);
    }

    @Test
    public void trustedDownloadOverridesPopupSignals() {
        assertFalse(PopupHostPolicy.isKnown(
                "https://onclickads.example/popunder", "onclickads.example", url -> true));
    }

    @Test
    public void legacyHostPatternsAreRecognized() {
        String[] patterns = new String[]{
                "hotterydiseur", "sewarsremeets", "sewarsremeet", "onclickads", "clickadu", "popads", "popcash",
                "popunder", "adsterra", "propellerads", "hilltopads", "exoclick", "trafficjunky", "juicyads",
                "admaven", "pushpush", "pushengage", "pushwoosh", "realsrv", "invest-tracing", "highperformanceformat",
                "highperformancedisplayformat", "xmladfeed", "rotator", "smartlink", "adnxs", "rubiconproject",
                "taboola", "outbrain", "mgid", "revcontent", "doubleclick", "googlesyndication", "googleadservices"
        };
        for (String pattern : patterns) {
            assertTrue("Expected host pattern: " + pattern,
                    PopupHostPolicy.isKnown("https://" + pattern + ".example/path",
                            pattern + ".example", url -> false));
        }
    }

    @Test
    public void riskyTopLevelDomainsAreRecognized() {
        String[] suffixes = new String[]{
                ".cfd", ".click", ".cam", ".monster", ".quest", ".buzz", ".icu", ".cyou"
        };
        for (String suffix : suffixes) {
            assertTrue("Expected suffix: " + suffix,
                    PopupHostPolicy.isKnown("https://example" + suffix,
                            "example" + suffix, url -> false));
        }
    }

    @Test
    public void legacyPopupUrlShapesAreRecognizedCaseInsensitively() {
        String[] markers = new String[]{
                "/popunder", "/popup", "/redirect", "/push/", "?utm_source=ad",
                "&ad_id=", "?ad_id=", "/prebid", "/vast", "/vpaid"
        };
        for (String marker : markers) {
            assertTrue("Expected URL marker: " + marker,
                    PopupHostPolicy.isKnown("HTTPS://EXAMPLE.COM" + marker.toUpperCase(),
                            "example.com", url -> false));
        }
    }

    @Test
    public void ordinaryHostAndUrlAreAllowed() {
        assertFalse(PopupHostPolicy.isKnown(
                "https://news.example/article?id=10", "news.example", url -> false));
    }

    @Test
    public void dependencyFailureReturnsFalse() {
        assertFalse(PopupHostPolicy.isKnown(
                "https://onclickads.example", "onclickads.example",
                url -> { throw new IllegalStateException("trusted"); }));
    }
}
