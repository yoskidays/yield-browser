package com.yieldbrowser.app;

import java.util.Locale;

/** Builds immutable presentation snapshots for Download Manager rows. */
final class DownloadUiItemFactory {
    private DownloadUiItemFactory() {
    }

    static DownloadUiItem build(DownloadItem item,
                                String category,
                                String host,
                                long downloadSize,
                                boolean playable,
                                int queuePosition,
                                boolean selectionMode,
                                boolean selected) {
        long total = item.hlsDownload ? 0L : Math.max(0L, item.totalBytes);
        long downloaded = item.hlsDownload
                ? Math.max(0L, item.hlsOutputBytes)
                : Math.max(0L, item.downloadedBytes);
        int progressBasisPoints = calculateProgressBasisPoints(item, downloaded, total);
        int progressPercent = Math.min(100, Math.max(0, Math.round(progressBasisPoints / 100f)));
        String status = safeText(item.status);
        String sizeText = buildSizeText(item, status, downloaded, total, downloadSize);
        RowState row = buildRowState(item, status, host, playable, queuePosition, progressPercent);

        if (row.completed) {
            progressBasisPoints = 10_000;
            progressPercent = 100;
        }

        return new DownloadUiItem(item.id, item.fileName, category, status,
                sizeText, row.activityText, row.detailText, progressBasisPoints, progressPercent,
                row.showProgress, row.showPrimaryAction, row.primaryAction,
                selectionMode, selected);
    }

    private static String buildSizeText(DownloadItem item, String status,
                                        long downloaded, long total, long downloadSize) {
        if ("completed".equals(status)) return readableFileSize(downloadSize);
        if (item.hlsDownload && item.totalBytes > 0) {
            return readableFileSize(item.hlsOutputBytes) + " • "
                    + item.hlsCompletedSegments + "/" + item.totalBytes + " segmen";
        }
        if (total > 0) return readableFileSize(downloaded) + " / " + readableFileSize(total);
        if (downloaded > 0) return readableFileSize(downloaded);
        return "Menyiapkan ukuran file";
    }

    private static RowState buildRowState(DownloadItem item, String status, String host,
                                          boolean playable, int queuePosition,
                                          int progressPercent) {
        if ("saving".equals(status)) {
            return new RowState("Menyimpan ke Downloads…",
                    "File selesai diunduh • jangan tutup proses", true, false, "", false);
        }
        if ("verifying".equals(status)) {
            return new RowState("Memverifikasi file…",
                    "Memastikan file lengkap sebelum disimpan", false, false, "", false);
        }
        if ("running".equals(status)) {
            double speed = item.smoothedSpeedBytesPerSecond > 0
                    ? item.smoothedSpeedBytesPerSecond : item.speedBytesPerSecond;
            String activity = BrowserUtils.readableSpeed(speed);
            String eta = formatEta(item.etaSeconds);
            if (!eta.isEmpty()) activity += " • " + eta;
            String detail = playable ? "Ketuk untuk menonton sambil download" : safeText(host);
            return new RowState(activity, detail, true, true, "Ⅱ", false);
        }
        if ("queued".equals(status)) {
            String activity = queuePosition > 0
                    ? "Menunggu antrean • posisi " + queuePosition
                    : "Menunggu slot download";
            return new RowState(activity, "Siap dimulai otomatis", true, true, "▶", false);
        }
        if ("paused".equals(status)) {
            String detail = playable
                    ? "Ketuk untuk menonton bagian yang sudah tersedia"
                    : "Tekan lanjutkan untuk meneruskan";
            return new RowState("Dijeda • " + progressPercent + "%", detail,
                    true, true, "▶", false);
        }
        if ("failed".equals(status)) {
            String detail = safeText(item.failReason).isEmpty()
                    ? "Tekan ulangi untuk mencoba lagi" : item.failReason;
            return new RowState("Download gagal", detail, true, true, "↻", false);
        }
        String detail = playable ? "Ketuk untuk menonton di Yield"
                : (safeText(host).isEmpty() ? "Ketuk untuk membuka file" : host);
        return new RowState("Selesai", detail, false, false, "", true);
    }

    static int calculateProgressBasisPoints(DownloadItem item, long downloaded, long total) {
        if (item == null) return 0;
        if ("saving".equals(item.status) || "verifying".equals(item.status)) {
            return Math.max(0, Math.min(10_000, item.finalizeProgress * 100));
        }
        if (item.hlsDownload && item.totalBytes > 0) {
            return DownloadUiMetrics.progressBasisPoints(
                    item.hlsCompletedSegments, item.totalBytes, item.progress);
        }
        return DownloadUiMetrics.progressBasisPoints(downloaded, total, item.progress);
    }

    static String formatEta(long seconds) {
        if (seconds <= 0 || seconds == Long.MAX_VALUE) return "";
        if (seconds < 60) return "tersisa " + Math.max(1, seconds) + " detik";
        long minutes = seconds / 60;
        if (minutes < 60) return "tersisa " + minutes + " menit";
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;
        return "tersisa " + hours + " jam"
                + (remainMinutes > 0 ? " " + remainMinutes + " menit" : "");
    }

    static String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = size;
        int index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024.0;
            index++;
        }
        String formatted = String.format(Locale.US,
                index == 0 ? "%.0f %s" : "%.2f %s", value, units[index]);
        return formatted.replace(".00", "");
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static final class RowState {
        final String activityText;
        final String detailText;
        final boolean showProgress;
        final boolean showPrimaryAction;
        final String primaryAction;
        final boolean completed;

        RowState(String activityText, String detailText,
                 boolean showProgress, boolean showPrimaryAction,
                 String primaryAction, boolean completed) {
            this.activityText = activityText;
            this.detailText = detailText;
            this.showProgress = showProgress;
            this.showPrimaryAction = showPrimaryAction;
            this.primaryAction = primaryAction;
            this.completed = completed;
        }
    }
}
