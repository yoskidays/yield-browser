package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DownloadManagerShellTest {
    @Test
    public void sortIndexPreservesKnownOptionsAndFallsBackToDate() {
        assertEquals(0, DownloadManagerShell.selectedSortIndex(null));
        assertEquals(0, DownloadManagerShell.selectedSortIndex("Unknown"));
        assertEquals(1, DownloadManagerShell.selectedSortIndex("Antrian"));
        assertEquals(3, DownloadManagerShell.selectedSortIndex("Ukuran"));
    }

    @Test
    public void categoriesAndSortOptionsKeepExpectedOrder() {
        assertEquals("Semua", DownloadManagerShell.CATEGORIES[0]);
        assertEquals("Lainnya", DownloadManagerShell.CATEGORIES[5]);
        assertEquals("Tanggal", DownloadManagerShell.SORT_OPTIONS[0]);
        assertEquals("Ukuran", DownloadManagerShell.SORT_OPTIONS[3]);
    }
}
