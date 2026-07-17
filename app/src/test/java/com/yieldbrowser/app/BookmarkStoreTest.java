package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BookmarkStoreTest {
    @Test
    public void urlsAndLookupPreserveBookmarkOrder() {
        BookmarkItemData first = new BookmarkItemData("One", "https://one.test", "Work", 1L);
        BookmarkItemData duplicate = new BookmarkItemData("Again", "https://one.test", "Later", 2L);
        BookmarkItemData second = new BookmarkItemData("Two", "https://two.test", "Work", 3L);
        List<BookmarkItemData> items = Arrays.asList(first, duplicate, second, null);

        assertEquals(Arrays.asList("https://one.test", "https://two.test"),
                new ArrayList<>(BookmarkStore.urlsFromItems(items)));
        assertSame(first, BookmarkStore.findByUrl(items, "https://one.test"));
        assertNull(BookmarkStore.findByUrl(items, "https://missing.test"));
    }

    @Test
    public void foldersMergeDefaultsSavedAndCurrentItems() {
        LinkedHashSet<String> saved = new LinkedHashSet<>(Arrays.asList("Work", "Daftar bacaan"));
        List<BookmarkItemData> items = Arrays.asList(
                new BookmarkItemData("One", "https://one.test", " Personal ", 1L),
                new BookmarkItemData("Two", "https://two.test", "Work", 2L));

        assertEquals(Arrays.asList("Bookmark seluler", "Daftar bacaan", "Work", "Personal"),
                BookmarkStore.mergeFolders(items, saved));
        assertEquals(1, BookmarkStore.countInFolder(items, "Work"));
        assertEquals(0, BookmarkStore.countInFolder(items, "Missing"));
    }

    @Test
    public void serializeAndParseRoundTripValidRows() {
        List<BookmarkItemData> items = Arrays.asList(
                new BookmarkItemData("A | B", "https://example.test/a?q=1", "Work", 42L),
                new BookmarkItemData("Ignored", "", "Work", 43L));

        String raw = BookmarkStore.serialize(items);
        List<BookmarkItemData> parsed = BookmarkStore.parse(raw);

        assertEquals(1, parsed.size());
        assertEquals("A | B", parsed.get(0).title);
        assertEquals("https://example.test/a?q=1", parsed.get(0).url);
        assertEquals("Work", parsed.get(0).folder);
        assertEquals(42L, parsed.get(0).time);
    }

    @Test
    public void malformedRowsAreIgnored() {
        List<BookmarkItemData> parsed = BookmarkStore.parse(
                "broken\nA|B|C|not-a-number\nTitle|Url|Folder|12");
        assertEquals(1, parsed.size());
        assertEquals(12L, parsed.get(0).time);
        assertTrue(BookmarkStore.urlsFromItems(null).isEmpty());
    }
}
