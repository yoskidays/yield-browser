package com.yieldbrowser.app;

/** Pure text and render-key rules for the Download Manager panel. */
final class DownloadPanelPresentation {
    private DownloadPanelPresentation() {
    }

    static String title(boolean selectionMode, int selectedCount) {
        return selectionMode ? Math.max(0, selectedCount) + " dipilih" : "Unduhan";
    }

    static String emptyMessage(String section, String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "Tidak ada unduhan yang cocok dengan pencarian.";
        }
        return "Selesai".equals(section)
                ? "Belum ada file yang selesai diunduh."
                : "Belum ada unduhan aktif atau tertunda.";
    }

    static String storageSummary(String storageText, boolean finalizing,
                                 int activeCount, int queuedCount) {
        String base = storageText == null || storageText.isEmpty()
                ? "Penyimpanan tersedia" : storageText;
        if (finalizing) base += "  •  sedang menyimpan file besar";
        return base + "  •  aktif " + Math.max(0, activeCount)
                + "  •  antri " + Math.max(0, queuedCount);
    }

    static String controlsSignature(String section, String sort,
                                    boolean selectionMode, int selectedCount,
                                    int activeCount, int queuedCount,
                                    boolean queuePaused, int maxActive,
                                    int completedCount) {
        return safe(section) + "|" + safe(sort) + "|"
                + selectionMode + "|" + Math.max(0, selectedCount) + "|"
                + Math.max(0, activeCount) + "|" + Math.max(0, queuedCount) + "|"
                + queuePaused + "|" + Math.max(1, maxActive) + "|"
                + Math.max(0, completedCount);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
