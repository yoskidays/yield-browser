package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldDownloadListingPolicyTest {
    private static final String DRAMAENCODE_PAGE =
            "https://dramaencode.net/drama-korea-a-bus-proposal-subtitle-indonesia/";

    @Test
    public void recognizesDramaencodeAsDownloadListingWithoutDownloadPath() {
        assertTrue(ShieldNavigationPolicy.isDownloadPage(DRAMAENCODE_PAGE));
        assertFalse(ShieldNavigationPolicy.isDownloadPage(
                "https://example.com/drama-korea-a-bus-proposal-subtitle-indonesia/"));
    }

    @Test
    public void allowsVisibleTrustedDownloadHostsAfterUserGesture() {
        assertTrue(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://www.mediafire.com/file/example/video.mp4/file",
                DRAMAENCODE_PAGE,
                true));
        assertTrue(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://files.fm/u/example",
                DRAMAENCODE_PAGE,
                true));
        assertTrue(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://www.mp4upload.com/example",
                DRAMAENCODE_PAGE,
                true));
        assertTrue(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://fastdown.io/example",
                DRAMAENCODE_PAGE,
                true));
        assertTrue(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://racaty.io/example",
                DRAMAENCODE_PAGE,
                true));
        assertTrue(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://mirrorace.org/example",
                DRAMAENCODE_PAGE,
                true));
        assertFalse(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://www.mediafire.com/file/example/video.mp4/file",
                DRAMAENCODE_PAGE,
                false));
    }

    @Test
    public void blocksObservedMoonlightAdRedirectEvenWithGesture() {
        String adUrl = "https://moonlightthailand-example.com/opaque";
        assertTrue(ShieldUrlRules.isKnownAdHost(ShieldUrlRules.hostOf(adUrl)));
        assertTrue(ShieldNavigationPolicy.shouldBlockMainFrameNavigation(
                adUrl,
                DRAMAENCODE_PAGE,
                true,
                true,
                false,
                false));
    }

    @Test
    public void neverTreatsCheapOrRedirectAdTargetsAsSafeDownloads() {
        assertFalse(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://fake-download.click/file",
                DRAMAENCODE_PAGE,
                true));
        assertFalse(ShieldNavigationPolicy.isSafeDownloadNavigation(
                "https://example.com/redirect?url=https://files.fm/u/example",
                DRAMAENCODE_PAGE,
                true));
    }
}
