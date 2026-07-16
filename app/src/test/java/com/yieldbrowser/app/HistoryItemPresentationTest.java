package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistoryItemPresentationTest {
    private final HistoryItemPresentation presentation = new HistoryItemPresentation(
            Locale.US, TimeZone.getTimeZone("UTC"));

    @Test
    public void buildsSafeTitleHostSubtitleAndInitial() {
        HistoryItemData item = new HistoryItemData(
                7L,
                "  Example Page  ",
                "https://www.example.com/path",
                "",
                1_700_000_000_000L,
                3);

        assertEquals("Example Page", presentation.title(item));
        assertEquals("example.com", presentation.host(item));
        assertTrue(presentation.subtitle(item).startsWith("example.com • "));
        assertTrue(presentation.subtitle(item).endsWith(" • 3 kunjungan"));
        assertEquals("E", presentation.fallbackInitial(item));
    }

    @Test
    public void fallsBackToUrlAndQuestionMarkSafely() {
        HistoryItemData urlOnly = new HistoryItemData(1L, "", "https://site.test/a", "", 0L, 1);
        HistoryItemData empty = new HistoryItemData(2L, "", "", "", 0L, 1);

        assertEquals("https://site.test/a", presentation.title(urlOnly));
        assertEquals("site.test", presentation.host(urlOnly));
        assertEquals("H", presentation.fallbackInitial(urlOnly));
        assertEquals("?", presentation.fallbackInitial(empty));
    }

    @Test
    public void detectsDayHeadersAndRelativeLabels() {
        long now = 1_700_000_000_000L;
        HistoryItemData first = new HistoryItemData(1L, "A", "https://a.test", "a.test", now, 1);
        HistoryItemData sameDay = new HistoryItemData(2L, "B", "https://b.test", "b.test", now - 60_000L, 1);
        HistoryItemData previousDay = new HistoryItemData(3L, "C", "https://c.test", "c.test", now - 86_400_000L, 1);

        assertTrue(presentation.shouldShowDayHeader(first, null));
        assertFalse(presentation.shouldShowDayHeader(sameDay, first));
        assertTrue(presentation.shouldShowDayHeader(previousDay, first));
        assertEquals("Hari ini", presentation.dayLabel(now, now));
        assertEquals("Kemarin", presentation.dayLabel(now - 86_400_000L, now));
    }
}
