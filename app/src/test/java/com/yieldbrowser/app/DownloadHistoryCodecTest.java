package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DownloadHistoryCodecTest {
    @Test
    public void roundTripPreservesMultipartState() {
        DownloadItem source = new DownloadItem(
                12,
                "https://example.com/video?id=1&quality=hd",
                "video test.mp4",
                "/tmp/video test.mp4",
                "paused",
                48
        );
        source.resolvedUrl = "https://cdn.example.com/token/file";
        source.totalBytes = 1_000;
        source.downloadedBytes = 480;
        source.connectionCount = 4;
        source.engineInfo = "Turbo 4 koneksi";
        source.userAgent = "Yield Test/1.0";
        source.referer = "https://example.com/watch";
        source.categoryHint = "Video";
        source.part1Start = 0;
        source.part1End = 249;
        source.part1Done = 250;
        source.part4Start = 750;
        source.part4End = 999;
        source.part4Done = 10;
        source.publicUri = "content://downloads/video";
        source.retryCount = 1;
        source.hlsDownload = false;
        source.etag = "etag-1";
        source.lastModified = "Thu, 18 Jun 2026 12:00:00 GMT";
        source.hlsCompletedSegments = 5;
        source.hlsOutputBytes = 12345;
        source.hlsPlaylistFingerprint = "fingerprint";
        source.turboTargetConnections = 3;
        source.turboProfile = "Google Drive adaptive 3";
        source.turboRetryPenalty = 1;

        DownloadItem restored = DownloadHistoryCodec.deserialize(
                DownloadHistoryCodec.serialize(source),
                99
        );

        assertNotNull(restored);
        assertEquals(99, restored.id);
        assertEquals(source.url, restored.url);
        assertEquals(source.resolvedUrl, restored.resolvedUrl);
        assertEquals(source.fileName, restored.fileName);
        assertEquals(source.path, restored.path);
        assertEquals(source.status, restored.status);
        assertEquals(source.progress, restored.progress);
        assertEquals(source.totalBytes, restored.totalBytes);
        assertEquals(source.downloadedBytes, restored.downloadedBytes);
        assertEquals(source.connectionCount, restored.connectionCount);
        assertEquals(source.part1Done, restored.part1Done);
        assertEquals(source.part4Done, restored.part4Done);
        assertEquals(source.publicUri, restored.publicUri);
        assertEquals(source.retryCount, restored.retryCount);
        assertFalse(restored.hlsDownload);
        assertEquals(source.etag, restored.etag);
        assertEquals(source.lastModified, restored.lastModified);
        assertEquals(source.hlsCompletedSegments, restored.hlsCompletedSegments);
        assertEquals(source.hlsOutputBytes, restored.hlsOutputBytes);
        assertEquals(source.hlsPlaylistFingerprint, restored.hlsPlaylistFingerprint);
        assertEquals(source.turboTargetConnections, restored.turboTargetConnections);
        assertEquals(source.turboProfile, restored.turboProfile);
        assertEquals(source.turboRetryPenalty, restored.turboRetryPenalty);
    }

    @Test
    public void runningDownloadRestoresAsPaused() {
        DownloadItem source = new DownloadItem(1, "https://example.com/a.zip", "a.zip", "/tmp/a.zip", "running", 25);

        DownloadItem restored = DownloadHistoryCodec.deserialize(
                DownloadHistoryCodec.serialize(source),
                2
        );

        assertNotNull(restored);
        assertEquals("paused", restored.status);
        assertEquals("Dijeda setelah aplikasi ditutup", restored.engineInfo);
        assertEquals(0.0, restored.speedBytesPerSecond, 0.0);
    }

    @Test
    public void persistableStatusesAreExplicit() {
        DownloadItem item = new DownloadItem(1, "", "", "", "queued", 0);
        assertTrue(DownloadHistoryCodec.shouldPersist(item));

        item.status = "cancelled";
        assertFalse(DownloadHistoryCodec.shouldPersist(item));
    }

    @Test
    public void legacyV084RowsStillRestore() {
        String row = StorageCodec.encode("https://example.com/old.zip") + "|"
                + StorageCodec.encode("old.zip") + "|"
                + StorageCodec.encode("/tmp/old.zip") + "|paused|25|1000|250|2|"
                + StorageCodec.encode("Stable 2 koneksi") + "|||||0|499|100|500|999|150|"
                + StorageCodec.encode("content://downloads/old") + "|1|false";
        DownloadItem restored = DownloadHistoryCodec.deserialize(row, 7);
        assertNotNull(restored);
        assertEquals("https://example.com/old.zip", restored.url);
        assertEquals("old.zip", restored.fileName);
        assertEquals(250, restored.downloadedBytes);
        assertEquals(100, restored.part1Done);
        assertEquals(150, restored.part2Done);
    }

    @Test
    public void savingDownloadRestoresAsRecoverablePause() {
        DownloadItem source = new DownloadItem(1, "https://example.com/large.bin",
                "large.bin", "/tmp/large.bin", "saving", 100);
        source.finalizeProgress = 73;
        source.finalizeBytes = 730;
        source.finalizeTotalBytes = 1_000;

        DownloadItem restored = DownloadHistoryCodec.deserialize(
                DownloadHistoryCodec.serialize(source), 2);

        assertNotNull(restored);
        assertEquals("paused", restored.status);
        assertEquals(0, restored.finalizeProgress);
        assertEquals(730, restored.finalizeBytes);
        assertEquals(1_000, restored.finalizeTotalBytes);
    }
}
