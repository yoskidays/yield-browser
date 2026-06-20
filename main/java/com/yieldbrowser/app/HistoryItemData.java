package com.yieldbrowser.app;

/** One browser-history entry. */
final class HistoryItemData {
    String title;
    String url;
    long time;

    HistoryItemData(String title, String url, long time) {
        this.title = title;
        this.url = url;
        this.time = time;
    }
}
