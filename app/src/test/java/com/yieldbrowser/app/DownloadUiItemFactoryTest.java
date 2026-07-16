package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadUiItemFactoryTest {
    @Test
    public void runningRowShowsSpeedEtaAndPauseAction() {
        DownloadItem item = item("running", 50);
        item.totalBytes = 1000;
        item.downloadedBytes = 500;
        item.smoothedSpeedBytesPerSecond = 2 * 1024 * 1024;
        item.etaSeconds = 90;

        DownloadUiItem row = DownloadUiItemFactory.build(
                item, "Video", "cdn.example", 1000, true, 0, false, false);

        assertEquals("Ⅱ", row.primaryAction);
        assertTrue(row.activityText.contains("MB/s"));
        assertTrue(row.activityText.contains("tersisa 1 menit"));
        assertEquals("Ketuk untuk menonton sambil download", row.detailText);
        assertTrue(row.showProgress);
        assertTrue(row.showPrimaryAction);
    }

    @Test
    public void queuedAndPausedRowsKeepQueueAndSelectionState() {
        DownloadItem queued = item("queued", 0);
        DownloadUiItem queueRow = DownloadUiItemFactory.build(
                queued, "APK", "files.example", 0, false, 3, true, true);
        assertEquals("Menunggu antrean • posisi 3", queueRow.activityText);
        assertEquals("▶", queueRow.primaryAction);
        assertTrue(queueRow.selectionMode);
        assertTrue(queueRow.selected);

        DownloadItem paused = item("paused", 42);
        DownloadUiItem pausedRow = DownloadUiItemFactory.build(
                paused, "Dokumen", "files.example", 0, false, 0, false, false);
        assertEquals("Dijeda • 42%", pausedRow.activityText);
        assertEquals("Tekan lanjutkan untuk meneruskan", pausedRow.detailText);
    }

    @Test
    public void completedRowHidesProgressAndUsesFinishedSize() {
        DownloadItem item = item("completed", 100);
        DownloadUiItem row = DownloadUiItemFactory.build(
                item, "Video", "cdn.example", 5L * 1024L * 1024L,
                true, 0, false, false);

        assertEquals("5 MB", row.sizeText);
        assertEquals("Selesai", row.activityText);
        assertEquals("Ketuk untuk menonton di Yield", row.detailText);
        assertEquals(10_000, row.progressBasisPoints);
        assertEquals(100, row.progressPercent);
        assertFalse(row.showProgress);
        assertFalse(row.showPrimaryAction);
    }

    @Test
    public void verifyingAndSavingUseFinalizeProgress() {
        DownloadItem saving = item("saving", 0);
        saving.finalizeProgress = 67;
        DownloadUiItem savingRow = DownloadUiItemFactory.build(
                saving, "Video", "", 0, false, 0, false, false);
        assertEquals(6700, savingRow.progressBasisPoints);
        assertEquals("Menyimpan ke Downloads…", savingRow.activityText);
        assertTrue(savingRow.showProgress);
        assertFalse(savingRow.showPrimaryAction);

        DownloadItem verifying = item("verifying", 0);
        verifying.finalizeProgress = 12;
        DownloadUiItem verifyingRow = DownloadUiItemFactory.build(
                verifying, "Video", "", 0, false, 0, false, false);
        assertEquals(1200, verifyingRow.progressBasisPoints);
        assertFalse(verifyingRow.showProgress);
        assertFalse(verifyingRow.showPrimaryAction);
    }

    private static DownloadItem item(String status, int progress) {
        return new DownloadItem(7, "https://cdn.example/file", "file.bin",
                "/tmp/file.bin", status, progress);
    }
}
