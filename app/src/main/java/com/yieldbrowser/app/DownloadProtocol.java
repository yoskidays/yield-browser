package com.yieldbrowser.app;

import java.net.HttpURLConnection;
import java.util.Locale;

/** Strict HTTP range validation and file-part integrity helpers. */
final class DownloadProtocol {
    private DownloadProtocol() {
    }

    static RangeInfo parseContentRange(String value) {
        if (value == null) return null;
        String clean = value.trim().toLowerCase(Locale.US);
        if (!clean.startsWith("bytes ")) return null;
        int dash = clean.indexOf('-', 6);
        int slash = clean.indexOf('/', dash + 1);
        if (dash < 0 || slash < 0) return null;
        try {
            long start = Long.parseLong(clean.substring(6, dash).trim());
            long end = Long.parseLong(clean.substring(dash + 1, slash).trim());
            String totalText = clean.substring(slash + 1).trim();
            long total = "*".equals(totalText) ? -1L : Long.parseLong(totalText);
            if (start < 0 || end < start || (total > 0 && end >= total)) return null;
            return new RangeInfo(start, end, total);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static RangeInfo requireRange(HttpURLConnection connection,
                                  long expectedStart,
                                  long expectedEnd,
                                  long expectedTotal) throws Exception {
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_PARTIAL) {
            throw new Exception("Server mengabaikan Range (HTTP " + code + ")");
        }
        RangeInfo range = parseContentRange(connection.getHeaderField("Content-Range"));
        if (range == null) throw new Exception("Content-Range server tidak valid");
        if (range.start != expectedStart || range.end != expectedEnd) {
            throw new Exception("Range server tidak sesuai: " + range.start + "-" + range.end);
        }
        validateTotalAndLength(connection, range, expectedTotal,
                expectedEnd - expectedStart + 1L);
        return range;
    }

    static RangeInfo requireRangeFrom(HttpURLConnection connection,
                                      long expectedStart,
                                      long expectedTotal) throws Exception {
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_PARTIAL) {
            throw new Exception("Server mengabaikan resume Range (HTTP " + code + ")");
        }
        RangeInfo range = parseContentRange(connection.getHeaderField("Content-Range"));
        if (range == null) throw new Exception("Content-Range resume tidak valid");
        if (range.start != expectedStart) {
            throw new Exception("Posisi resume berubah: " + range.start);
        }
        long expectedLength = range.end - range.start + 1L;
        validateTotalAndLength(connection, range, expectedTotal, expectedLength);
        return range;
    }

    private static void validateTotalAndLength(HttpURLConnection connection,
                                               RangeInfo range,
                                               long expectedTotal,
                                               long expectedLength) throws Exception {
        if (expectedTotal > 0 && range.total > 0 && range.total != expectedTotal) {
            throw new Exception("Ukuran file berubah di server");
        }
        long contentLength = connection.getContentLengthLong();
        if (contentLength > 0 && contentLength != expectedLength) {
            throw new Exception("Panjang respons Range tidak sesuai");
        }
    }

    static long expectedLength(long start, long end) {
        return end >= start ? end - start + 1L : 0L;
    }

    static final class RangeInfo {
        final long start;
        final long end;
        final long total;

        RangeInfo(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.total = total;
        }
    }
}
