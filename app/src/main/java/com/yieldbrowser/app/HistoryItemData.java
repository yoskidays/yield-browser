package com.yieldbrowser.app;

/** One persistent browser-history entry. */
final class HistoryItemData {
    long id;
    String title;
    String url;
    String host;
    long time;
    int visitCount;

    HistoryItemData(String title, String url, long time) {
        this(0L, title, url, "", time, 1);
    }

    HistoryItemData(long id, String title, String url, String host, long time, int visitCount) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.host = host;
        this.time = time;
        this.visitCount = Math.max(1, visitCount);
    }
}
