package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadHardAdClickTokenPolicyTest {
    @Test
    public void everyLegacyHardAdTokenIsRecognized() {
        String[] tokens = new String[]{
                "adclick", "ad_click", "adurl=", "click_id", "af_click",
                "clickunder", "popunder", "popupads", "onclickads", "interstitial",
                "utm_medium=affiliates", "deep_and_deferred", "navigate_url=", "reactpath"
        };
        for (String token : tokens) {
            assertTrue("Expected token: " + token,
                    DownloadUrlPolicy.hasHardAdClickToken("https://example.com/?x=" + token));
        }
    }

    @Test
    public void nullAndOrdinaryDownloadUrlsAreNotHardAdClicks() {
        assertFalse(DownloadUrlPolicy.hasHardAdClickToken(null));
        assertFalse(DownloadUrlPolicy.hasHardAdClickToken(
                "https://example.com/download/archive.zip?confirm=1"));
    }

    @Test
    public void classifierPreservesLegacyCaseSensitivity() {
        assertFalse(DownloadUrlPolicy.hasHardAdClickToken("https://example.com/?ADCLICK=1"));
        assertTrue(DownloadUrlPolicy.hasHardAdClickToken("https://example.com/?adclick=1"));
    }
}
