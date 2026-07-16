package com.yieldbrowser.app;

import java.util.List;

/** Rules for clearing finished download records without deleting downloaded files. */
final class DownloadHistoryClearPolicy {
    private DownloadHistoryClearPolicy() {
    }

    static boolean isClearable(DownloadItem item) {
        return item != null && "completed".equals(item.status);
    }

    static int countClearable(List<DownloadItem> items) {
        if (items == null || items.isEmpty()) return 0;
        int count = 0;
        for (DownloadItem item : items) {
            if (isClearable(item)) count++;
        }
        return count;
    }
}
