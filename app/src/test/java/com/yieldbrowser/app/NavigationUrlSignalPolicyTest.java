package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationUrlSignalPolicyTest {
    @Test
    public void externalSchemesRejectWebAndInternalWebViewSchemes() {
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme(null));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("  "));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("HTTP://example.com"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("https://example.com"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("about:blank"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("javascript:alert(1)"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("data:text/plain,x"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("blob:https://example.com/id"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("file:///tmp/a"));
    }

    @Test
    public void validCustomSchemesAreExternalAndCaseInsensitive() {
        assertTrue(NavigationUrlSignalPolicy.isExternalScheme(" Intent://open "));
        assertTrue(NavigationUrlSignalPolicy.isExternalScheme("mailto:user@example.com"));
        assertTrue(NavigationUrlSignalPolicy.isExternalScheme("my-app+test.1:value"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("1invalid:value"));
        assertFalse(NavigationUrlSignalPolicy.isExternalScheme("noscheme"));
    }

    @Test
    public void mediaYoutubeAndTrustedDownloadShortCircuitAdClassification() {
        AtomicInteger downstreamCalls = new AtomicInteger();
        assertFalse(NavigationUrlSignalPolicy.isLikelyAdClick(
                "https://ads.example/adclick/video.mp4",
                url -> true,
                url -> { downstreamCalls.incrementAndGet(); return false; },
                url -> { downstreamCalls.incrementAndGet(); return false; }));
        assertTrue(downstreamCalls.get() == 0);

        assertFalse(NavigationUrlSignalPolicy.isLikelyAdClick(
                "https://youtube.com/adclick",
                url -> false, url -> true,
                url -> { downstreamCalls.incrementAndGet(); return false; }));
        assertTrue(downstreamCalls.get() == 0);

        assertFalse(NavigationUrlSignalPolicy.isLikelyAdClick(
                "https://files.example/archive.zip?adclick=1",
                url -> false, url -> false, url -> true));
    }

    @Test
    public void everyLegacyAdClickMarkerIsRecognized() {
        String[] markers = new String[]{
                "utm_medium=affiliates", "utm_source=an_", "affiliate", "aff_sub",
                "deep_and_deferred", "navigate_url=", "reactpath", "click_id", "adclick",
                "ad_click", "adurl=", "af_click", "tracking_id", "campaign_id"
        };
        for (String marker : markers) {
            assertTrue(marker, NavigationUrlSignalPolicy.isLikelyAdClick(
                    "HTTPS://EXAMPLE.COM/?X=" + marker.toUpperCase(),
                    url -> false, url -> false, url -> false));
        }
    }

    @Test
    public void externalCommerceAndIntentSchemesAreLikelyAdClicks() {
        String[] urls = new String[]{
                "shopeeid://open", "lazada://open", "tokopedia://open",
                "intent://open", "market://details?id=x", "custom://open"
        };
        for (String url : urls) {
            assertTrue(url, NavigationUrlSignalPolicy.isLikelyAdClick(
                    url, value -> false, value -> false, value -> false));
        }
    }

    @Test
    public void ordinaryWebUrlsAndPredicateFailuresReturnFalse() {
        assertFalse(NavigationUrlSignalPolicy.isLikelyAdClick(
                "https://example.com/article?id=1",
                url -> false, url -> false, url -> false));
        assertFalse(NavigationUrlSignalPolicy.isLikelyAdClick(
                "https://example.com/adclick",
                url -> { throw new IllegalStateException("media"); },
                url -> false, url -> false));
    }
}
