package com.yieldbrowser.app;

/** User-defined shortcut displayed on the browser home screen. */
final class ShortcutItemData {
    String label;
    String url;

    ShortcutItemData(String label, String url) {
        this.label = label;
        this.url = url;
    }
}
