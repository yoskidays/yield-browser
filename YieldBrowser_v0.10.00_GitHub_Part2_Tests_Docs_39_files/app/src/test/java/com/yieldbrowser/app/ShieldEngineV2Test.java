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
}
