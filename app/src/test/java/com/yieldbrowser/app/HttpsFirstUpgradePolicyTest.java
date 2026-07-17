package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpsFirstUpgradePolicyTest {
    private static boolean isHttp(String url) {
        return url != null && url.trim().toLowerCase().startsWith("http://");
    }

    @Test
    public void nonHttpNullAndExemptUrlsRemainUnchanged() {
        assertEquals(null, HttpsFirstUpgradePolicy.upgrade(
                null, HttpsFirstUpgradePolicyTest::isHttp, url -> false));
        assertEquals("https://example.com", HttpsFirstUpgradePolicy.upgrade(
                "https://example.com", HttpsFirstUpgradePolicyTest::isHttp, url -> false));
        assertEquals("http://localhost", HttpsFirstUpgradePolicy.upgrade(
                "http://localhost", HttpsFirstUpgradePolicyTest::isHttp, url -> true));
    }

    @Test
    public void plainHttpUrlUpgradesToHttps() {
        assertEquals("https://example.com/path", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com/path", HttpsFirstUpgradePolicyTest::isHttp, url -> false));
    }

    @Test
    public void portEightyIsRemovedAndPort443IsPreserved() {
        assertEquals("https://example.com/path", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com:80/path", HttpsFirstUpgradePolicyTest::isHttp, url -> false));
        assertEquals("https://example.com:443/path", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com:443/path", HttpsFirstUpgradePolicyTest::isHttp, url -> false));
    }

    @Test
    public void userInfoPathQueryAndFragmentArePreserved() {
        assertEquals("https://user:pass@example.com/a/b?q=1#part", HttpsFirstUpgradePolicy.upgrade(
                "http://user:pass@example.com/a/b?q=1#part",
                HttpsFirstUpgradePolicyTest::isHttp, url -> false));
    }

    @Test
    public void unicodeComponentsAreReturnedAsAsciiUri() {
        assertEquals("https://example.com/caf%C3%A9?q=%C3%A9", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com/café?q=é", HttpsFirstUpgradePolicyTest::isHttp, url -> false));
    }

    @Test
    public void malformedUrlsAndDependencyFailuresReturnOriginal() {
        String malformed = "http://exa mple.com/path";
        assertEquals(malformed, HttpsFirstUpgradePolicy.upgrade(
                malformed, HttpsFirstUpgradePolicyTest::isHttp, url -> false));
        assertEquals("http://example.com", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com", null, url -> false));
        assertEquals("http://example.com", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com", HttpsFirstUpgradePolicyTest::isHttp, null));
        assertEquals("http://example.com", HttpsFirstUpgradePolicy.upgrade(
                "http://example.com", HttpsFirstUpgradePolicyTest::isHttp,
                url -> { throw new IllegalStateException("exempt"); }));
    }
}
