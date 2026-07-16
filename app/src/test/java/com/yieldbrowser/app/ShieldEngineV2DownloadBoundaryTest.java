package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldEngineV2DownloadBoundaryTest {
    private static final String DOWNLOAD_PAGE =
            "https://ratudrakor.biz.id/2026/07/08/download-drama-korea-see-you-at-work-tomorrow-episode-4-subtitle-indonesia/";

    @Test
    public void downloadArticleIsNotMisclassifiedAsReaderPage() {
        assertTrue(ShieldEngineV2.isDownloadPage(DOWNLOAD_PAGE));
        assertFalse(ShieldEngineV2.isReaderOrContentPage(DOWNLOAD_PAGE));
        assertTrue(ShieldEngineV2.isPopupIsolationContentPage(DOWNLOAD_PAGE));
    }

    @Test
    public void allowsUserInitiatedCleanDownloadDestinations() {
        String mirror = "https://mirror.example.org/download/see-you-at-work-episode-4";
        String directFile = "https://cdn.example.org/files/see-you-at-work-episode-4-720p.mkv";

        assertTrue(ShieldEngineV2.isSafeDownloadNavigation(mirror, DOWNLOAD_PAGE, true));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                mirror, DOWNLOAD_PAGE, true, true, false, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                directFile, DOWNLOAD_PAGE, true, true, false, false));
    }

    @Test
    public void downloadBoundaryStillBlocksTakeoversAndAffiliateRelays() {
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://unknown-clean-domain.example/landing",
                DOWNLOAD_PAGE, true, true, false, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://onclickads.net/download?click_id=123",
                DOWNLOAD_PAGE, true, true, false, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://mirror.example.org/go/download?url=https%3A%2F%2Ffiles.example.org%2Fa.mkv",
                DOWNLOAD_PAGE, true, true, false, false));
    }

    @Test
    public void crossSiteDownloadRequiresARealUserGesture() {
        String mirror = "https://mirror.example.org/download/see-you-at-work-episode-4";

        assertFalse(ShieldEngineV2.isSafeDownloadNavigation(mirror, DOWNLOAD_PAGE, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                mirror, DOWNLOAD_PAGE, false, true, false, false));
    }
}
