package com.yieldbrowser.app;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

/**
 * Predikat murni (pure) seputar URL/host untuk subsistem download Yield Browser.
 *
 * Semua method stateless: hasilnya hanya bergantung pada argumen + pustaka standar
 * (Uri, Locale, regex). Tidak ada yang menyentuh state instance MainActivity.
 *
 * MainActivity tetap menyimpan wrapper tipis yang mendelegasikan ke sini, jadi
 * seluruh call site lama tidak berubah.
 */
final class DownloadUrlPolicy {

    private DownloadUrlPolicy() {
        // kelas utilitas — jangan diinstansiasi
    }

    static boolean hasDirectFileDownloadExtension(String u) {
        if (u == null) return false;
        return u.matches(".*\\.(zip|rar|7z|apk|apks|xapk|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|txt|epub|mp3|m4a|wav|ogg|mp4|mkv|webm|avi|mov|ts|m3u8|iso|img|bin|exe|msi)(\\?|#|$).*?");
    }

    static boolean hasTrustedDownloadMarker(String u) {
        if (u == null || u.length() == 0) return false;
        return u.contains("/download")
                || u.contains("download?")
                || u.contains("download=")
                || u.contains("export=download")
                || u.contains("dl=1")
                || u.contains("response-content-disposition=attachment")
                || u.contains("content-disposition=attachment")
                || u.contains("filename=")
                || u.contains("file_name=")
                || u.contains("confirm=")
                || u.contains("uuid=")
                || u.contains("/releases/download/")
                || u.contains("/uc?")
                || u.contains("/file/d/")
                || u.contains("/file/");
    }

    static String normalizeGoogleDriveDownloadUrl(String value) {
        try {
            if (value == null || value.isEmpty()) return value;
            Uri uri = Uri.parse(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.US);
            if (host.contains("drive.usercontent.google.com")) return value;
            if (!host.contains("drive.google.com") && !host.contains("docs.google.com")) return value;

            String id = uri.getQueryParameter("id");
            if ((id == null || id.isEmpty()) && host.contains("drive.google.com")) {
                List<String> parts = uri.getPathSegments();
                for (int i = 0; i + 1 < parts.size(); i++) {
                    if ("d".equals(parts.get(i)) || "folders".equals(parts.get(i))) {
                        id = parts.get(i + 1);
                        break;
                    }
                }
            }
            if (id == null || id.length() < 8 || value.contains("/folders/")) return value;
            return "https://drive.usercontent.google.com/download?id=" + Uri.encode(id)
                    + "&export=download&confirm=t";
        } catch (Exception ignored) {
            return value;
        }
    }

    static boolean looksLikeHlsDownload(String url, String fileName) {
        String link = (url == null ? "" : url).toLowerCase(Locale.US);
        String name = (fileName == null ? "" : fileName).toLowerCase(Locale.US);
        return link.contains(".m3u8") || name.endsWith(".m3u8") || link.contains("mpegurl");
    }

    static boolean isPermanentDownloadError(String reason) {
        if (reason == null) return false;
        return reason.contains("Metode enkripsi HLS")
                || reason.contains("AES-128 HLS dengan byte-range")
                || reason.contains("halaman HTML")
                || reason.contains("Playlist HLS kosong")
                || reason.contains("File hasil download kosong");
    }

    static boolean isTrustedDownloadHostForAllow(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.US);
        return h.equals("drive.usercontent.google.com")
                || h.equals("drive.google.com")
                || h.equals("docs.google.com")
                || h.endsWith(".googleusercontent.com")
                || h.equals("github.com")
                || h.endsWith(".github.com")
                || h.equals("objects.githubusercontent.com")
                || h.equals("raw.githubusercontent.com")
                || h.endsWith(".githubusercontent.com")
                || h.equals("sourceforge.net")
                || h.endsWith(".sourceforge.net")
                || h.equals("mediafire.com")
                || h.endsWith(".mediafire.com")
                || h.equals("dropbox.com")
                || h.endsWith(".dropbox.com")
                || h.equals("dropboxusercontent.com")
                || h.endsWith(".dropboxusercontent.com")
                || h.equals("onedrive.live.com")
                || h.equals("1drv.ms")
                || h.equals("mega.nz")
                || h.endsWith(".mega.nz")
                || h.equals("pixeldrain.com")
                || h.endsWith(".pixeldrain.com")
                || h.equals("gofile.io")
                || h.endsWith(".gofile.io")
                || h.equals("archive.org")
                || h.endsWith(".archive.org");
    }

    static boolean isSuspiciousAdHostForDownloadAllow(String host) {
        if (host == null || host.length() == 0) return true;
        String h = host.toLowerCase(Locale.US);
        if (h.endsWith(".cfd") || h.endsWith(".click") || h.endsWith(".cam") || h.endsWith(".monster")
                || h.endsWith(".quest") || h.endsWith(".buzz") || h.endsWith(".icu") || h.endsWith(".cyou")) {
            return true;
        }
        String[] bad = new String[]{
                "hotterydiseur", "sewarsremeets", "onclickads", "clickadu", "popads", "popcash",
                "propellerads", "adsterra", "hilltopads", "exoclick", "trafficjunky", "juicyads",
                "admaven", "realsrv", "doubleclick", "googlesyndication", "googleadservices",
                "taboola", "outbrain", "mgid", "revcontent"
        };
        for (String b : bad) if (h.contains(b)) return true;
        return false;
    }

    static boolean isStableDownloadHost(String url) {
        String host = BrowserUtils.getHostLower(url);
        if (host.isEmpty()) return false;
        return host.contains("1drv.ms")
                || host.contains("onedrive.live.com")
                || host.contains("sharepoint.com")
                || host.contains("mega.nz")
                || host.contains("mega.co.nz");
    }
}
