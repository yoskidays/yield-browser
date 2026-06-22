package com.yieldbrowser.app;

/** One bookmark and its lightweight folder metadata. */
final class BookmarkItemData {
    String title;
    String url;
    String folder;
    long time;

    BookmarkItemData(String title, String url, String folder, long time) {
        this.title = title;
        this.url = url;
        this.folder = folder;
        this.time = time;
    }
}
