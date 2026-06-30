package com.yieldbrowser.app;

import android.webkit.WebViewClient;

import java.net.HttpURLConnection;
import java.util.Locale;

/**
 * Kumpulan fungsi murni (pure functions) untuk Yield Browser.
 *
 * Semua method di sini stateless: hasilnya hanya bergantung pada argumen +
 * pustaka standar Java/Android + konstanta yang dapat diakses. Tidak ada satu pun
 * yang menyentuh state instance MainActivity, sehingga aman dipanggil dari mana saja.
 *
 * MainActivity tetap menyimpan wrapper tipis yang mendelegasikan ke sini, jadi
 * seluruh call site lama tidak berubah.
 */
final class BrowserUtils {

    private BrowserUtils() {
        // kelas utilitas — jangan diinstansiasi
    }

    // ---- AdBlock ----------------------------------------------------------

    static String buildAdBlockCosmeticCss() {
        return ".adsbygoogle,ins.adsbygoogle,"
                + "[class~='advertisement'],[class~='advert'],[class~='sponsored'],"
                + "[id^='div-gpt-ad'],[id^='google_ads'],"
                + "iframe[id^='google_ads_'],iframe[id^='aswift_'],"
                + "iframe[src*='doubleclick.net'],iframe[src*='googlesyndication'],iframe[src*='googleadservices'],iframe[src*='adservice.'],iframe[src*='/ads/'],iframe[src*='/adserver'],iframe[src*='onclickads'],iframe[src*='clickadu'],iframe[src*='popads'],iframe[src*='popcash'],iframe[src*='propellerads'],iframe[src*='adsterra'],iframe[src*='hilltopads'],iframe[src*='exoclick'],iframe[src*='juicyads'],iframe[src*='trafficjunky'],"
                + ".ad-banner,.ad-container,.ad-wrapper,.ad-slot,.ad-box,.ad-unit,.ad-area,.ad-frame,.ad-label,.ad-placeholder,.ad-leaderboard,.ad-sidebar,.ad-rail,.ad-billboard,.ads-container,.ads-wrapper,"
                + ".banner-ad,.sidebar-ad,.footer-ad,.header-ad,.in-article-ad,.inline-ad,.native-ad,.sponsored-post,.sponsored-content,.promoted-content,"
                + ".GoogleActiveViewElement"
                + "{display:none!important;visibility:hidden!important;opacity:0!important;max-height:0!important;min-height:0!important;height:0!important;overflow:hidden!important;pointer-events:none!important;}";
    }

    // ---- Download ---------------------------------------------------------

    static String readableSpeed(double bytesPerSecond) {
        if (bytesPerSecond <= 0) return "0 KB/s";
        if (bytesPerSecond >= 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0)).replace(".00", "");
        }
        if (bytesPerSecond >= 1024) {
            return String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSecond / 1024.0).replace(".0", "");
        }
        return String.format(java.util.Locale.US, "%.0f B/s", bytesPerSecond);
    }

    static int chooseDownloadBufferSize(long totalBytes) {
        if (totalBytes <= 0L) return 256 * 1024;                            // ukuran belum diketahui
        if (totalBytes < 4L * 1024L * 1024L) return 64 * 1024;             // file kecil
        if (totalBytes < 64L * 1024L * 1024L) return BrowserConstants.DOWNLOAD_BUFFER_SIZE;  // 128KB (default lama)
        if (totalBytes < 512L * 1024L * 1024L) return 256 * 1024;          // file besar
        return 512 * 1024;                                                  // file sangat besar
    }

    static long getDynamicPartStart(DownloadItem item, int part) {
        if (part == 1) return item.part1Start;
        if (part == 2) return item.part2Start;
        if (part == 3) return item.part3Start;
        return item.part4Start;
    }

    static long getDynamicPartEnd(DownloadItem item, int part) {
        if (part == 1) return item.part1End;
        if (part == 2) return item.part2End;
        if (part == 3) return item.part3End;
        return item.part4End;
    }

    static long getDynamicPartDone(DownloadItem item, int part) {
        if (part == 1) return item.part1Done;
        if (part == 2) return item.part2Done;
        if (part == 3) return item.part3Done;
        return item.part4Done;
    }

    static boolean isActiveDownloadStatus(String status) {
        return "running".equals(status) || "verifying".equals(status) || "saving".equals(status);
    }

    // ---- Jaringan / HTTP --------------------------------------------------

    static boolean isRedirectCode(int code) {
        return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == 307 || code == 308;
    }

    static boolean isHttpsFallbackEligibleError(int errorCode) {
        return errorCode == WebViewClient.ERROR_HOST_LOOKUP
                || errorCode == WebViewClient.ERROR_CONNECT
                || errorCode == WebViewClient.ERROR_TIMEOUT
                || errorCode == WebViewClient.ERROR_IO;
    }

    static String getMobileUserAgent() {
        return "Mozilla/5.0 (Linux; Android 11; RMX1971) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";
    }

    static String getDesktopUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    }

    // ---- Tanggal ----------------------------------------------------------

    static boolean sameDay(java.util.Calendar a, java.util.Calendar b) {
        return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
                && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR);
    }

    // ---- Video ------------------------------------------------------------

    static boolean looksLikeVideoDownload(String url, String fileName, String contentType) {
        String link = (url == null ? "" : url).toLowerCase(Locale.US);
        String name = (fileName == null ? "" : fileName).toLowerCase(Locale.US);
        String type = (contentType == null ? "" : contentType).toLowerCase(Locale.US);
        return type.startsWith("video/") || type.contains("mpegurl") || link.contains(".m3u8")
                || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm")
                || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".ts")
                || link.contains(".mp4") || link.contains(".mkv") || link.contains(".webm");
    }
}
