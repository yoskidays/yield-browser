package com.yieldbrowser.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure filtering and sorting rules for the Download Manager list. */
final class DownloadListPolicy {
    private DownloadListPolicy() {
    }

    static ArrayList<DownloadItem> filterAndSort(List<DownloadItem> source,
                                                 String section,
                                                 String category,
                                                 String query,
                                                 String sort) {
        ArrayList<DownloadItem> result = new ArrayList<>();
        if (source == null || source.isEmpty()) return result;

        for (DownloadItem item : new ArrayList<>(source)) {
            if (!matchesSection(item, section)) continue;
            if (!DownloadItemUtils.matchesDownloadCategory(item, category)) continue;
            if (!matchesSearch(item, query)) continue;
            result.add(item);
        }

        if ("Antrian".equals(sort)) return result;
        Collections.sort(result, (a, b) -> {
            if ("Nama".equals(sort)) {
                return safeText(a.fileName).compareToIgnoreCase(safeText(b.fileName));
            }
            if ("Ukuran".equals(sort)) {
                return Long.compare(
                        DownloadItemUtils.getDownloadSize(b),
                        DownloadItemUtils.getDownloadSize(a));
            }
            return Integer.compare(b.id, a.id);
        });
        return result;
    }

    static boolean matchesSection(DownloadItem item, String section) {
        if (item == null) return false;
        boolean completed = "completed".equals(item.status);
        return "Selesai".equals(section) ? completed : !completed;
    }

    static boolean matchesSearch(DownloadItem item, String query) {
        if (item == null) return false;
        String normalized = safeText(query).trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return true;
        return safeText(item.fileName).toLowerCase(Locale.US).contains(normalized)
                || safeText(item.url).toLowerCase(Locale.US).contains(normalized)
                || DownloadItemUtils.getDownloadHost(item)
                .toLowerCase(Locale.US).contains(normalized);
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
