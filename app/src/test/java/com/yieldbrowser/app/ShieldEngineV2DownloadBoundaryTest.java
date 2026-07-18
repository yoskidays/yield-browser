package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldEngineV2DownloadBoundaryTest {
    private static final String DOWNLOAD_PAGE =
            "https://ratudrakor.biz.id/2026/07/16/download-drama-korea-see-you-at-work-tomorrow-episode-8-subtitle-indonesia/";

    @Test
    public void downloadArticleIsNotMisclassifiedAsReaderPage() {
        assertTrue(ShieldEngineV2.isDownloadPage(DOWNLOAD_PAGE));
        assertFalse(ShieldEngineV2.isReaderOrContentPage(DOWNLOAD_PAGE));
        assertTrue(ShieldEngineV2.isPopupIsolationContentPage(DOWNLOAD_PAGE));
    }

    @Test
    public void allowsUserInitiatedTrustedDownloadDestinations() {
        String mediaFire = "https://www.mediafire.com/file/abc123/see-you-at-work-episode-8.mkv/file";
        String googleDrive = "https://drive.google.com/file/d/abc123456789/view";
        String directFile = "https://cdn.example.org/files/see-you-at-work-episode-8-720p.mkv";

        assertTrue(ShieldEngineV2.isSafeDownloadNavigation(mediaFire, DOWNLOAD_PAGE, true));
        assertTrue(ShieldEngineV2.isSafeDownloadNavigation(googleDrive, DOWNLOAD_PAGE, true));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                mediaFire, DOWNLOAD_PAGE, true, true, false, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                googleDrive, DOWNLOAD_PAGE, true, true, false, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                directFile, DOWNLOAD_PAGE, true, true, false, false));
    }

    @Test
    public void genericDownloadLookingLandingPageCannotMasqueradeAsFileHost() {
        String disguisedAdLanding =
                "https://unknown-download.example/download/see-you-at-work-episode-8";

        assertFalse(ShieldEngineV2.isSafeDownloadNavigation(
                disguisedAdLanding, DOWNLOAD_PAGE, true));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                disguisedAdLanding, DOWNLOAD_PAGE, true, true, false, false));
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
        String mediaFire = "https://www.mediafire.com/file/abc123/episode-8.mkv/file";

        assertFalse(ShieldEngineV2.isSafeDownloadNavigation(mediaFire, DOWNLOAD_PAGE, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                mediaFire, DOWNLOAD_PAGE, false, true, false, false));
    }
}
