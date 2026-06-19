package com.yieldbrowser.app;

/** Serializes and restores download state independently from Activity/UI concerns. */
final class DownloadHistoryCodec {
    private static final String DELIMITER = "|";

    private DownloadHistoryCodec() {
    }

    static boolean shouldPersist(DownloadItem item) {
        if (item == null || item.status == null) return false;
        switch (item.status) {
            case "completed":
            case "failed":
            case "paused":
            case "running":
            case "queued":
                return true;
            default:
                return false;
        }
    }

    static String serialize(DownloadItem item) {
        synchronized (item.stateLock) {
            return StorageCodec.encode(item.url) + DELIMITER
                    + StorageCodec.encode(item.resolvedUrl) + DELIMITER
                    + StorageCodec.encode(item.fileName) + DELIMITER
                    + StorageCodec.encode(item.path) + DELIMITER
                    + item.status + DELIMITER
                    + item.progress + DELIMITER
                    + item.totalBytes + DELIMITER
                    + item.downloadedBytes + DELIMITER
                    + item.connectionCount + DELIMITER
                    + StorageCodec.encode(item.engineInfo) + DELIMITER
                    + StorageCodec.encode(item.userAgent) + DELIMITER
                    + StorageCodec.encode(item.referer) + DELIMITER
                    + StorageCodec.encode(item.failReason) + DELIMITER
                    + StorageCodec.encode(item.categoryHint) + DELIMITER
                    + item.part1Start + DELIMITER
                    + item.part1End + DELIMITER
                    + item.part1Done + DELIMITER
                    + item.part2Start + DELIMITER
                    + item.part2End + DELIMITER
                    + item.part2Done + DELIMITER
                    + item.part3Start + DELIMITER
                    + item.part3End + DELIMITER
                    + item.part3Done + DELIMITER
                    + item.part4Start + DELIMITER
                    + item.part4End + DELIMITER
                    + item.part4Done + DELIMITER
                    + StorageCodec.encode(item.publicUri) + DELIMITER
                    + item.retryCount + DELIMITER
                    + item.hlsDownload + DELIMITER
                    + StorageCodec.encode(item.etag) + DELIMITER
                    + StorageCodec.encode(item.lastModified) + DELIMITER
                    + item.hlsCompletedSegments + DELIMITER
                    + item.hlsOutputBytes + DELIMITER
                    + StorageCodec.encode(item.hlsPlaylistFingerprint) + DELIMITER
                    + item.hlsInitMapWritten + DELIMITER
                    + item.turboTargetConnections + DELIMITER
                    + StorageCodec.encode(item.turboProfile) + DELIMITER
                    + item.turboRetryPenalty;
        }
    }

    static DownloadItem deserialize(String row, int id) {
        if (row == null || row.isEmpty()) return null;
        try {
            String[] parts = row.split("\\|", -1);
            if (parts.length >= 38) return deserializeV2(parts, id);
            return deserializeLegacy(parts, id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static DownloadItem deserializeV2(String[] parts, int id) {
        DownloadItem item = new DownloadItem(id, decodeAt(parts, 0), decodeAt(parts, 2),
                decodeAt(parts, 3), valueAt(parts, 4), parseInt(parts, 5, 0));
        item.resolvedUrl = decodeAt(parts, 1);
        item.totalBytes = parseLong(parts, 6, 0L);
        item.downloadedBytes = parseLong(parts, 7, 0L);
        item.connectionCount = parseInt(parts, 8, 0);
        item.engineInfo = decodeAt(parts, 9);
        item.userAgent = decodeAt(parts, 10);
        item.referer = decodeAt(parts, 11);
        item.failReason = decodeAt(parts, 12);
        item.categoryHint = decodeAt(parts, 13);
        item.part1Start = parseLong(parts, 14, 0L);
        item.part1End = parseLong(parts, 15, 0L);
        item.part1Done = parseLong(parts, 16, 0L);
        item.part2Start = parseLong(parts, 17, 0L);
        item.part2End = parseLong(parts, 18, 0L);
        item.part2Done = parseLong(parts, 19, 0L);
        item.part3Start = parseLong(parts, 20, 0L);
        item.part3End = parseLong(parts, 21, 0L);
        item.part3Done = parseLong(parts, 22, 0L);
        item.part4Start = parseLong(parts, 23, 0L);
        item.part4End = parseLong(parts, 24, 0L);
        item.part4Done = parseLong(parts, 25, 0L);
        item.publicUri = decodeAt(parts, 26);
        item.retryCount = parseInt(parts, 27, 0);
        item.hlsDownload = parseBoolean(parts, 28, false);
        item.etag = decodeAt(parts, 29);
        item.lastModified = decodeAt(parts, 30);
        item.hlsCompletedSegments = parseInt(parts, 31, 0);
        item.hlsOutputBytes = parseLong(parts, 32, 0L);
        item.hlsPlaylistFingerprint = decodeAt(parts, 33);
        item.hlsInitMapWritten = parseBoolean(parts, 34, false);
        item.turboTargetConnections = parseInt(parts, 35, 0);
        item.turboProfile = decodeAt(parts, 36);
        item.turboRetryPenalty = parseInt(parts, 37, 0);
        normalizeRestoredState(item);
        return item;
    }

    private static DownloadItem deserializeLegacy(String[] parts, int id) {
        if (parts.length < 5) return null;
        DownloadItem item = new DownloadItem(id, decodeAt(parts, 0), decodeAt(parts, 1),
                decodeAt(parts, 2), valueAt(parts, 3), parseInt(parts, 4, 0));
        item.totalBytes = parseLong(parts, 5, 0L);
        item.downloadedBytes = parseLong(parts, 6, 0L);
        item.connectionCount = parseInt(parts, 7, 0);
        item.engineInfo = parts.length >= 9 ? decodeAt(parts, 8)
                : item.connectionCount > 1 ? "2 koneksi sukses" : "1 koneksi";
        item.userAgent = decodeAt(parts, 9);
        item.referer = decodeAt(parts, 10);
        item.failReason = decodeAt(parts, 11);
        item.categoryHint = decodeAt(parts, 12);
        item.part1Start = parseLong(parts, 13, 0L);
        item.part1End = parseLong(parts, 14, 0L);
        item.part1Done = parseLong(parts, 15, 0L);
        item.part2Start = parseLong(parts, 16, 0L);
        item.part2End = parseLong(parts, 17, 0L);
        item.part2Done = parseLong(parts, 18, 0L);
        if (parts.length >= 28) {
            item.part3Start = parseLong(parts, 19, 0L);
            item.part3End = parseLong(parts, 20, 0L);
            item.part3Done = parseLong(parts, 21, 0L);
            item.part4Start = parseLong(parts, 22, 0L);
            item.part4End = parseLong(parts, 23, 0L);
            item.part4Done = parseLong(parts, 24, 0L);
            item.publicUri = decodeAt(parts, 25);
            item.retryCount = parseInt(parts, 26, 0);
            item.hlsDownload = parseBoolean(parts, 27, false);
        } else {
            item.publicUri = decodeAt(parts, 19);
            item.retryCount = parseInt(parts, 20, 0);
            item.hlsDownload = parseBoolean(parts, 21, false);
        }
        normalizeRestoredState(item);
        return item;
    }

    private static void normalizeRestoredState(DownloadItem item) {
        if ("running".equals(item.status)) {
            item.status = "paused";
            item.speedBytesPerSecond = 0;
            item.engineInfo = "Dijeda setelah aplikasi ditutup";
        }
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    private static String decodeAt(String[] values, int index) {
        return index < values.length ? StorageCodec.decode(values[index]) : "";
    }

    private static int parseInt(String[] values, int index, int fallback) {
        if (index >= values.length) return fallback;
        try { return Integer.parseInt(values[index]); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static long parseLong(String[] values, int index, long fallback) {
        if (index >= values.length) return fallback;
        try { return Long.parseLong(values[index]); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static boolean parseBoolean(String[] values, int index, boolean fallback) {
        return index < values.length ? Boolean.parseBoolean(values[index]) : fallback;
    }
}
