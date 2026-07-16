package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadListPolicyTest {
    @Test
    public void separatesCompletedHistoryFromActiveDownloads() {
        DownloadItem completed = item(30, "completed", "Movie.mp4", "https://cdn.example/movie.mp4", "Video", 300);
        DownloadItem running = item(20, "running", "App.apk", "https://cdn.example/app.apk", "APK", 200);

        assertTrue(DownloadListPolicy.matchesSection(completed, "Selesai"));
        assertFalse(DownloadListPolicy.matchesSection(running, "Selesai"));
        assertTrue(DownloadListPolicy.matchesSection(running, "Mengunduh"));
    }

    @Test
    public void filtersByCategoryAndSearchWithoutChangingSource() {
        DownloadItem movie = item(30, "completed", "Holiday Movie.mp4", "https://media.example/movie.mp4", "Video", 300);
        DownloadItem document = item(20, "completed", "Invoice.pdf", "https://files.example/invoice.pdf", "Dokumen", 200);
        ArrayList<DownloadItem> source = new ArrayList<>(Arrays.asList(movie, document));

        ArrayList<DownloadItem> filtered = DownloadListPolicy.filterAndSort(
                source, "Selesai", "Video", "holiday", "Tanggal");

        assertEquals(1, filtered.size());
        assertEquals(movie.id, filtered.get(0).id);
        assertEquals(2, source.size());
    }

    @Test
    public void supportsNameSizeDateAndQueueOrdering() {
        DownloadItem alpha = item(10, "completed", "Alpha.mp4", "https://a.example/a.mp4", "Video", 100);
        DownloadItem beta = item(30, "completed", "Beta.mp4", "https://b.example/b.mp4", "Video", 500);
        DownloadItem gamma = item(20, "completed", "Gamma.mp4", "https://c.example/c.mp4", "Video", 300);
        ArrayList<DownloadItem> source = new ArrayList<>(Arrays.asList(gamma, beta, alpha));

        assertEquals(alpha.id, DownloadListPolicy.filterAndSort(source, "Selesai", "Semua", "", "Nama").get(0).id);
        assertEquals(beta.id, DownloadListPolicy.filterAndSort(source, "Selesai", "Semua", "", "Ukuran").get(0).id);
        assertEquals(beta.id, DownloadListPolicy.filterAndSort(source, "Selesai", "Semua", "", "Tanggal").get(0).id);
        assertEquals(gamma.id, DownloadListPolicy.filterAndSort(source, "Selesai", "Semua", "", "Antrian").get(0).id);
    }

    private static DownloadItem item(int id, String status, String name, String url,
                                     String category, long size) {
        DownloadItem item = new DownloadItem(id, url, name, "/tmp/" + name, status, 0);
        item.categoryHint = category;
        item.totalBytes = size;
        item.downloadedBytes = size;
        return item;
    }
}
