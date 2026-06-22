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
    public void blocksKnownCrossSiteAdButAllowsCleanExternalLink() {
        assertTrue(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://onclickads.net/click_id=123",
                READER, true, true, false, false));
        assertFalse(ShieldEngineV2.shouldBlockMainFrameNavigation(
                "https://example.org/article",
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

}
