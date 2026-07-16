package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DownloadPanelPresentationTest {
    @Test
    public void buildsTitleAndEmptyMessages() {
        assertEquals("Unduhan", DownloadPanelPresentation.title(false, 0));
        assertEquals("3 dipilih", DownloadPanelPresentation.title(true, 3));
        assertEquals("Belum ada file yang selesai diunduh.",
                DownloadPanelPresentation.emptyMessage("Selesai", ""));
        assertEquals("Belum ada unduhan aktif atau tertunda.",
                DownloadPanelPresentation.emptyMessage("Mengunduh", null));
        assertEquals("Tidak ada unduhan yang cocok dengan pencarian.",
                DownloadPanelPresentation.emptyMessage("Selesai", "video"));
    }

    @Test
    public void buildsStorageSummaryAndStableRenderKey() {
        assertEquals("10 GB terpakai  •  aktif 2  •  antri 4",
                DownloadPanelPresentation.storageSummary("10 GB terpakai", false, 2, 4));
        assertEquals("10 GB terpakai  •  sedang menyimpan file besar  •  aktif 1  •  antri 0",
                DownloadPanelPresentation.storageSummary("10 GB terpakai", true, 1, 0));

        String first = DownloadPanelPresentation.controlsSignature(
                "Selesai", "Tanggal", false, 0, 0, 0, false, 2, 4);
        String same = DownloadPanelPresentation.controlsSignature(
                "Selesai", "Tanggal", false, 0, 0, 0, false, 2, 4);
        String changed = DownloadPanelPresentation.controlsSignature(
                "Selesai", "Tanggal", true, 1, 0, 0, false, 2, 4);

        assertEquals(first, same);
        assertNotEquals(first, changed);
    }
}
