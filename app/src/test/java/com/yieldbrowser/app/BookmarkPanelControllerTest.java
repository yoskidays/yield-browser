package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BookmarkPanelControllerTest {
    @Test
    public void filterFolderMatchesTitleAndUrlCaseInsensitively() {
        BookmarkItemData one = new BookmarkItemData(
                "OpenAI News", "https://openai.com/news", "Work", 1L);
        BookmarkItemData two = new BookmarkItemData(
                "Example", "https://docs.example.test", "Work", 2L);
        BookmarkItemData other = new BookmarkItemData(
                "OpenAI", "https://openai.com", "Personal", 3L);

        List<BookmarkItemData> result = BookmarkPanelController.filterFolder(
                Arrays.asList(one, two, other), "Work", "OPENAI");
        assertEquals(Arrays.asList(one), result);

        result = BookmarkPanelController.filterFolder(
                Arrays.asList(one, two, other), "Work", "docs.example");
        assertEquals(Arrays.asList(two), result);
    }

    @Test
    public void titleInitialAndHostFallbackAreStable() {
        BookmarkItemData titled = new BookmarkItemData(
                "Yield", "https://www.example.test/page", "Work", 1L);
        BookmarkItemData untitled = new BookmarkItemData(
                "", "https://openai.com", "Work", 2L);

        assertEquals("Yield", BookmarkPanelController.safeTitle(titled));
        assertEquals("https://openai.com", BookmarkPanelController.safeTitle(untitled));
        assertEquals("Y", BookmarkPanelController.bookmarkInitial(titled));
        assertEquals("example.test", BookmarkPanelController.shortHost(titled.url));
        assertEquals("not a url", BookmarkPanelController.shortHost("not a url"));
    }
}
