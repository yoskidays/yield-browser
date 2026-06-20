package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReaderCompatibilityPolicyTest {
    @Test
    public void acceptsReaderPagesAcrossDifferentDomains() {
        assertTrue(ReaderCompatibilityPolicy.isEligiblePageUrl("https://komiku.org/series-chapter-12/"));
        assertTrue(ReaderCompatibilityPolicy.isEligiblePageUrl("https://example-reader.net/manga/title/chapter/4"));
        assertTrue(ReaderCompatibilityPolicy.isEligiblePageUrl("http://comic.example/baca/episode-7?mode=longstrip"));
    }

    @Test
    public void rejectsDirectAssetsAndNonWebSchemes() {
        assertFalse(ReaderCompatibilityPolicy.isEligiblePageUrl("https://cdn.example/chapter/page-01.webp"));
        assertFalse(ReaderCompatibilityPolicy.isEligiblePageUrl("https://example.org/book.pdf"));
        assertFalse(ReaderCompatibilityPolicy.isEligiblePageUrl("file:///sdcard/page.html"));
        assertFalse(ReaderCompatibilityPolicy.isEligiblePageUrl("javascript:alert(1)"));
    }

    @Test
    public void detectsGenericReaderPathHints() {
        assertTrue(ReaderCompatibilityPolicy.hasReaderPathHint("https://site.test/chapter/12"));
        assertTrue(ReaderCompatibilityPolicy.hasReaderPathHint("https://site.test/series-abc-chapter-12/"));
        assertTrue(ReaderCompatibilityPolicy.hasReaderPathHint("https://site.test/baca/episode-9"));
        assertFalse(ReaderCompatibilityPolicy.hasReaderPathHint("https://site.test/daftar-komik-terbaru"));
    }

    @Test
    public void usesMoreRetriesForCompatibilityPages() {
        assertArrayEquals(new long[]{0L, 300L, 1100L, 2800L, 6000L},
                ReaderCompatibilityPolicy.retrySchedule(true, false));
        assertArrayEquals(new long[]{200L, 900L, 2400L, 5200L},
                ReaderCompatibilityPolicy.retrySchedule(false, true));
        assertArrayEquals(new long[]{450L, 1800L},
                ReaderCompatibilityPolicy.retrySchedule(false, false));
    }
}
