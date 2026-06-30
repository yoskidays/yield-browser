package com.yieldbrowser.app;

import android.net.Uri;

import java.io.File;

/**
 * Operasi murni atas sebuah {@link DownloadItem} untuk Yield Browser.
 *
 * Semua method hanya membaca/menulis field pada objek {@code item} yang diberikan
 * (plus pustaka standar + helper statis seperti {@link DownloadUiMetrics}). Tidak ada
 * yang menyentuh state instance MainActivity, sehingga aman dipanggil dari mana saja.
 *
 * MainActivity tetap menyimpan wrapper tipis yang mendelegasikan ke sini, jadi
 * seluruh call site lama tidak berubah.
 */
final class DownloadItemUtils {

    private DownloadItemUtils() {
        // kelas utilitas — jangan diinstansiasi
    }

    static long getDownloadSize(DownloadItem item) {
        if (item == null) return 0;
        if (item.hlsDownload && item.hlsOutputBytes > 0) return item.hlsOutputBytes;
        if (item.totalBytes > 0) return item.totalBytes;
        if (item.downloadedBytes > 0) return item.downloadedBytes;
        try {
            if (item.path != null) {
                File file = new File(item.path);
                if (file.exists()) return file.length();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    static String getDownloadHost(DownloadItem item) {
        try {
            if (item.url != null) {
                Uri uri = Uri.parse(item.url);
                String host = uri.getHost();
                return host == null ? "" : host;
            }
        } catch (Exception ignored) {}
        return "";
    }

    static int getVisibleDownloadProgressPercent(DownloadItem item) {
        if (item == null) return 0;
        if ("saving".equals(item.status) || "verifying".equals(item.status)) {
            return Math.max(0, Math.min(100, item.finalizeProgress));
        }
        return Math.max(0, Math.min(100, item.progress));
    }

    static void resetDownloadSpeedState(DownloadItem item) {
        if (item == null) return;
        item.speedBytesPerSecond = 0;
        item.smoothedSpeedBytesPerSecond = 0;
        item.etaSeconds = -1L;
        item.lastSpeedTimeMs = 0L;
        item.lastSpeedBytes = item.downloadedBytes;
    }

    static boolean shouldFallbackTurboToStable(DownloadItem item) {
        return item != null && item.connectionCount >= 3 && item.progress < 98
                && (item.turboSlowSamples >= 4 || item.turboStabilityScore < 35
                || item.turboRetryPenalty >= 2);
    }

    static void updateDownloadSpeed(DownloadItem item, long currentBytes) {
        if (item == null) return;
        long now = System.currentTimeMillis();
        synchronized (item.stateLock) {
            if (item.lastSpeedTimeMs <= 0) {
                item.speedBytesPerSecond = 0;
                item.smoothedSpeedBytesPerSecond = 0;
                item.etaSeconds = -1L;
                item.lastSpeedTimeMs = now;
                item.lastSpeedBytes = currentBytes;
                return;
            }
            long elapsed = now - item.lastSpeedTimeMs;
            if (elapsed < 500L) return;
            long delta = Math.max(0L, currentBytes - item.lastSpeedBytes);
            double sample = (delta * 1000.0) / Math.max(1L, elapsed);
            item.speedBytesPerSecond = sample;
            if (sample > 0) {
                item.smoothedSpeedBytesPerSecond = DownloadUiMetrics.smoothSpeed(
                        item.smoothedSpeedBytesPerSecond, sample);
            } else {
                item.smoothedSpeedBytesPerSecond = DownloadUiMetrics.smoothSpeed(
                        item.smoothedSpeedBytesPerSecond, 0);
            }
            item.etaSeconds = DownloadUiMetrics.estimateRemainingSeconds(
                    item.totalBytes, currentBytes, item.smoothedSpeedBytesPerSecond);
            item.lastSpeedTimeMs = now;
            item.lastSpeedBytes = currentBytes;
        }
    }
}
