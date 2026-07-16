package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TabSessionCodecTest {
    @Test
    public void roundTripsCurrentSixColumnFormat() {
        List<TabSessionCodec.Record> source = Arrays.asList(
                TabSessionCodec.Record.persisted(
                        "Tab satu\tdengan pemisah",
                        "https://example.com/a path?q=1",
                        "example.com",
                        "tab-1"),
                TabSessionCodec.Record.persisted(
                        "Tab dua\nbaris",
                        "",
                        "",
                        "tab-2"));

        String raw = TabSessionCodec.encode(source);
        List<TabSessionCodec.Record> restored = TabSessionCodec.decode(raw);

        assertEquals(2, restored.size());
        assertEquals("Tab satu\tdengan pemisah", restored.get(0).title);
        assertEquals("https://example.com/a path?q=1", restored.get(0).url);
        assertEquals("example.com", restored.get(0).isolationHost);
        assertEquals("tab-1", restored.get(0).tabId);
        assertEquals("Tab dua\nbaris", restored.get(1).title);
        assertFalse(restored.get(0).privateTab);
        assertFalse(restored.get(0).adTab);
    }

    @Test
    public void readsLegacyRowsAndTracksOriginalSelectionPosition() {
        String normal = StorageCodec.encode("Normal") + "\t"
                + StorageCodec.encode("https://example.com") + "\t0\t0";
        String malformed = "broken\trow";
        String privateRow = StorageCodec.encode("Private") + "\t"
                + StorageCodec.encode("https://private.example") + "\t1\t0";

        List<TabSessionCodec.Record> restored = TabSessionCodec.decode(
                normal + "\n" + malformed + "\n" + privateRow);

        assertEquals(2, restored.size());
        assertEquals(0, restored.get(0).sourceIndex);
        assertEquals(2, restored.get(1).sourceIndex);
        assertEquals("", restored.get(0).isolationHost);
        assertEquals("", restored.get(0).tabId);
        assertTrue(restored.get(1).privateTab);
    }

    @Test
    public void suppliesStableDefaultTitleAndHandlesEmptyInput() {
        assertEquals("Tab baru", TabSessionCodec.normalizedTitle(null));
        assertEquals("Tab baru", TabSessionCodec.normalizedTitle("   "));
        assertEquals("Berita", TabSessionCodec.normalizedTitle("Berita"));
        assertTrue(TabSessionCodec.decode("").isEmpty());
        assertEquals("", TabSessionCodec.encode(null));
    }
}
