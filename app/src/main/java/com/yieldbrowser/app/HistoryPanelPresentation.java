package com.yieldbrowser.app;

/** Pure state and text rules for the browser-history panel. */
final class HistoryPanelPresentation {
    static final int LOAD_MORE_THRESHOLD = 8;
    static final long SEARCH_DEBOUNCE_MS = 220L;

    private HistoryPanelPresentation() {
    }

    static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    static String emptyMessage(String query) {
        return normalizeQuery(query).isEmpty()
                ? "Riwayat masih kosong."
                : "Tidak ada riwayat yang cocok.";
    }

    static boolean shouldShowEmpty(boolean loading, boolean adapterEmpty) {
        return !loading && adapterEmpty;
    }

    static boolean shouldShowInitialLoading(boolean loading, boolean adapterEmpty) {
        return loading && adapterEmpty;
    }

    static boolean shouldLoadNextPage(int scrollDy,
                                      boolean loading,
                                      boolean endReached,
                                      int lastVisiblePosition,
                                      int itemCount) {
        if (scrollDy <= 0 || loading || endReached || itemCount <= 0) return false;
        return lastVisiblePosition >= Math.max(0, itemCount - LOAD_MORE_THRESHOLD);
    }

    static boolean isEndReached(int returnedCount, int pageSize) {
        return returnedCount < Math.max(1, pageSize);
    }
}
