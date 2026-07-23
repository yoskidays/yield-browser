package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldEngineV2Test {
    private static final String READER = "https://komiku.org/manga/full-time-awakening/";

    @Test
    public void blocksSameOriginRelayBeforeReaderLeavesMainTab() {
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://komiku.org/r/abcdefghijklmnop",
                READER, true, true, false, false));
    }

    @Test
    public void allowsCleanChapterNavigation() {
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://komiku.org/full-time-awakening-chapter-69/",
                READER, true, true, false, false));
    }

    @Test
    public void readerBoundaryBlocksKnownAndUnknownCrossSiteTakeovers() {
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://onclickads.net/click_id=123",
                READER, true, true, false, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://unknown-clean-domain.example/landing",
                READER, true, true, false, false));
    }

    @Test
    public void ordinaryPagesStillAllowCleanExternalLinks() {
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://example.org/article",
                "https://portal.example.com/home", true, false, false, false));
    }

    @Test
    public void explicitTrustedFlowCanLeaveReaderAndReaderCdnAssetsStayAllowed() {
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://accounts.example.org/login",
                READER, true, true, true, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://img.komiku.org/uploads/page-01.webp",
                READER, true, true, false, false));
    }

    @Test
    public void keepsReaderCdnAssetsAndBlocksKnownAdScripts() {
        assertFalse(ShieldEngineV2.shouldBlockSubresource(
                "https://cdn.example.org/images/page-01.webp", READER, false));
        assertTrue(ShieldEngineV2.shouldBlockSubresource(
                "https://doubleclick.net/tag.js", READER, true));
    }

    @Test
    public void allowsNextAndPreviousWhenSourceIsAlreadyAChapterPage() {
        String sourceChapter = "https://komiku.org/teisou-gyakuten-sekai-de-yuiitsu-no-otoko-kishi-no-ore-onna-kishi-gakuen-ni-nyuugaku-shitara-nazeka-chapter-6-1/";
        String nextChapter = "https://komiku.org/teisou-gyakuten-sekai-de-yuiitsu-no-otoko-kishi-no-ore-onna-kishi-gakuen-ni-nyuugaku-shitara-nazeka-chapter-7/";
        String previousChapter = "https://komiku.org/teisou-gyakuten-sekai-de-yuiitsu-no-otoko-kishi-no-ore-onna-kishi-gakuen-ni-nyuugaku-shitara-nazeka-chapter-6/";

        assertTrue(ShieldEngineV2.isSafeSameSiteReaderNavigation(nextChapter, sourceChapter));
        assertTrue(ShieldEngineV2.isSafeSameSiteReaderNavigation(previousChapter, sourceChapter));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                nextChapter, sourceChapter, true, true, false, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                previousChapter, sourceChapter, true, true, false, false));
    }

    @Test
    public void chapterAllowLaneDoesNotAllowSameOriginAdRelay() {
        String sourceChapter = "https://komiku.org/full-time-awakening-chapter-69/";
        String relay = "https://komiku.org/r/abcdefghijklmnop";

        assertFalse(ShieldEngineV2.isSafeSameSiteReaderNavigation(relay, sourceChapter));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                relay, sourceChapter, true, true, false, false));
    }

    @Test
    public void readerPagesDisableOnlyTheLegacyDuplicateClickGuard() {
        assertFalse(ShieldEngineV2.shouldUseLegacyClickGuard(
                "https://komiku.org/omniscient-readers-viewpoint-chapter-304/", true));
        assertFalse(ShieldEngineV2.shouldUseLegacyClickGuard(
                "https://example.org/article/how-to-read-comics/", true));
        assertTrue(ShieldEngineV2.shouldUseLegacyClickGuard(
                "https://example.org/account/settings", true));
        assertFalse(ShieldEngineV2.shouldUseLegacyClickGuard(
                "https://example.org/account/settings", false));
    }

    @Test
    public void allowsCurrentKomikuNextChapterShape() {
        String chapter304 = "https://komiku.org/omniscient-readers-viewpoint-chapter-304/";
        String chapter305 = "https://komiku.org/omniscient-readers-viewpoint-chapter-305/";

        assertTrue(ShieldEngineV2.isSafeSameSiteReaderNavigation(chapter305, chapter304));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                chapter305, chapter304, true, true, false, false));
    }

    @Test
    public void searchResultsAreNeverClassifiedAsReaderPages() {
        String[] searchPages = {
                "https://www.google.co.id/search?q=komiknesia",
                "https://www.bing.com/search?q=komiknesia",
                "https://duckduckgo.com/?q=komiknesia",
                "https://search.yahoo.com/search?p=komiknesia",
                "https://yandex.ru/search/?text=komiknesia",
                "https://www.baidu.com/s?wd=komiknesia",
                "https://search.brave.com/search?q=komiknesia",
                "https://www.startpage.com/sp/search?query=komiknesia",
                "https://www.ecosia.org/search?q=komiknesia",
                "https://www.qwant.com/?q=komiknesia",
                "https://searx.example.org/search?q=komiknesia"
        };

        for (String searchPage : searchPages) {
            assertTrue(ShieldEngineV2.isSearchResultsPage(searchPage));
            assertFalse(ShieldEngineV2.isReaderOrContentPage(searchPage));
            assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                    "https://komiknesia.net/", searchPage, true, false, true, false));
        }
    }

    @Test
    public void readerBoundaryRemainsActiveAfterUniversalSearchFix() {
        assertFalse(ShieldEngineV2.isSearchResultsPage(READER));
        assertTrue(ShieldEngineV2.isReaderOrContentPage(READER));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://unknown-clean-domain.example/landing",
                READER, true, true, false, false));
    }

    @Test
    public void videoPagesUsePopupIsolationBoundary() {
        String video = "https://javtiful.com/id/video/61787/mdsr-0006-2";

        assertTrue(ShieldEngineV2.isPopupIsolationContentPage(video));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://unknown-clean-ad-domain.example/landing",
                video, true, true, false, false));
    }

    @Test
    public void trustedVideoHostsDoNotUsePopupIsolationBoundary() {
        String youtube = "https://www.youtube.com/watch?v=abc123";

        assertFalse(ShieldEngineV2.isPopupIsolationContentPage(youtube));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://accounts.google.com/ServiceLogin",
                youtube, true, false, false, false));
    }

    @Test
    public void videoPagesDisableLegacyDuplicateClickGuard() {
        assertFalse(ShieldEngineV2.shouldUseLegacyClickGuard(
                "https://javtiful.com/id/video/61787/mdsr-0006-2", true));
    }

    @Test
    public void oploverzHomepageUsesPopupIsolationBoundary() {
        String home = "https://oploverz.am/";

        assertTrue(ShieldEngineV2.isPopupIsolationContentPage(home));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://unknown-ad-landing.example/offer",
                home, false, true, false, false));
        assertFalse(ShieldEngineV2.shouldUseLegacyClickGuard(home, true));
    }

    @Test
    public void oploverzBlocksUnknownThirdPartyScriptsButKeepsSafeSupportHosts() {
        String home = "https://oploverz.am/";

        assertTrue(ShieldEngineV2.shouldBlockSubresource(
                "https://rotating-ad-network.example/assets/popup.js", home, false));
        assertTrue(ShieldEngineV2.shouldBlockSubresource(
                "https://rotating-ad-network.example/popunder/loader", home, false));
        assertFalse(ShieldEngineV2.shouldBlockSubresource(
                "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js", home, false));
        assertFalse(ShieldEngineV2.shouldBlockSubresource(
                "https://oploverz.am/wp-content/themes/site/app.js", home, false));
        assertFalse(ShieldEngineV2.shouldBlockSubresource(
                "https://cdn.example.org/posters/anime.webp", home, false));
    }

    @Test
    public void oploverzDownloadButtonsKeepCleanGestureLane() {
        String episode = "https://oploverz.am/clevatess-episode-03-subtitle-indonesia/";
        String fileDon = "https://filedon.co/f/episode-03-720p";
        String rotatingCleanHost = "https://files.vikingcdn.example/download/episode-03";

        assertTrue(ShieldEngineV2.isDownloadPage(episode));
        assertTrue(ShieldEngineV2.isSafeDownloadNavigation(fileDon, episode, true));
        assertTrue(ShieldEngineV2.isSafeDownloadNavigation(rotatingCleanHost, episode, true));
        assertFalse(ShieldEngineV2.isSafeDownloadNavigation(fileDon, episode, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                fileDon, episode, true, true, false, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://oploverz.am/go/episode-03-file", episode,
                true, true, false, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://onclickads.net/click_id=episode-03", episode,
                true, true, false, false));
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                fileDon, episode, false, true, false, false));
    }
}
