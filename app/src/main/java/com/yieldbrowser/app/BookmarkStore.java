package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.KEY_BOOKMARKS;
import static com.yieldbrowser.app.BrowserConstants.KEY_BOOKMARK_DATA;
import static com.yieldbrowser.app.BrowserConstants.KEY_BOOKMARK_FOLDERS;
import static com.yieldbrowser.app.StorageCodec.decode;
import static com.yieldbrowser.app.StorageCodec.encode;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Bookmark collection, folder metadata, legacy migration, and persistence. */
final class BookmarkStore {
    interface LegacyTitleResolver {
        String resolve(String url);
    }

    private BookmarkStore() {
    }

    static Set<String> getBookmarks(List<BookmarkItemData> items, SharedPreferences prefs) {
        LinkedHashSet<String> current = urlsFromItems(items);
        if (!current.isEmpty()) return new HashSet<>(current);
        if (prefs == null) return new HashSet<>();
        Set<String> saved = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        return saved == null ? new HashSet<>() : new HashSet<>(saved);
    }

    static BookmarkItemData findByUrl(List<BookmarkItemData> items, String url) {
        if (items == null || url == null) return null;
        for (BookmarkItemData item : items) {
            if (item != null && url.equals(item.url)) return item;
        }
        return null;
    }

    static List<String> getFolders(List<BookmarkItemData> items, SharedPreferences prefs) {
        Set<String> saved = null;
        try {
            if (prefs != null) saved = prefs.getStringSet(KEY_BOOKMARK_FOLDERS, new LinkedHashSet<>());
        } catch (Exception ignored) {
        }
        return mergeFolders(items, saved);
    }

    static int countInFolder(List<BookmarkItemData> items, String folder) {
        if (items == null || folder == null) return 0;
        int count = 0;
        for (BookmarkItemData item : items) {
            if (item != null && folder.equals(item.folder)) count++;
        }
        return count;
    }

    static void load(List<BookmarkItemData> items,
                     SharedPreferences prefs,
                     LegacyTitleResolver titleResolver) {
        if (items == null) return;
        items.clear();
        if (prefs == null) return;

        String raw = prefs.getString(KEY_BOOKMARK_DATA, "");
        items.addAll(parse(raw));
        if (!items.isEmpty()) return;

        Set<String> legacy = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        if (legacy == null || legacy.isEmpty()) return;
        for (String url : legacy) {
            String title = titleResolver == null ? "" : titleResolver.resolve(url);
            items.add(new BookmarkItemData(
                    title,
                    url,
                    "Bookmark seluler",
                    System.currentTimeMillis()));
        }
        save(items, prefs);
    }

    static void save(List<BookmarkItemData> items, SharedPreferences prefs) {
        if (prefs == null) return;
        LinkedHashSet<String> urls = urlsFromItems(items);
        LinkedHashSet<String> folders = new LinkedHashSet<>(getFolders(items, prefs));
        prefs.edit()
                .putString(KEY_BOOKMARK_DATA, serialize(items))
                .putStringSet(KEY_BOOKMARKS, new HashSet<>(urls))
                .putStringSet(KEY_BOOKMARK_FOLDERS, folders)
                .apply();
    }

    static LinkedHashSet<String> urlsFromItems(List<BookmarkItemData> items) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (items == null) return urls;
        for (BookmarkItemData item : items) {
            if (item != null && item.url != null && item.url.length() > 0) urls.add(item.url);
        }
        return urls;
    }

    static List<String> mergeFolders(List<BookmarkItemData> items, Set<String> savedFolders) {
        LinkedHashSet<String> folders = new LinkedHashSet<>();
        folders.add("Bookmark seluler");
        folders.add("Daftar bacaan");
        if (savedFolders != null) folders.addAll(savedFolders);
        if (items != null) {
            for (BookmarkItemData item : items) {
                if (item != null && item.folder != null && item.folder.trim().length() > 0) {
                    folders.add(item.folder.trim());
                }
            }
        }
        return new ArrayList<>(folders);
    }

    static List<BookmarkItemData> parse(String raw) {
        ArrayList<BookmarkItemData> parsed = new ArrayList<>();
        if (raw == null || raw.trim().length() == 0) return parsed;
        for (String line : raw.split("\n")) {
            String[] parts = line.split("\\|");
            if (parts.length < 4) continue;
            try {
                parsed.add(new BookmarkItemData(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        Long.parseLong(parts[3])));
            } catch (Exception ignored) {
            }
        }
        return parsed;
    }

    static String serialize(List<BookmarkItemData> items) {
        ArrayList<String> lines = new ArrayList<>();
        if (items != null) {
            for (BookmarkItemData item : items) {
                if (item == null || item.url == null || item.url.trim().length() == 0) continue;
                lines.add(encode(item.title)
                        + "|" + encode(item.url)
                        + "|" + encode(item.folder)
                        + "|" + item.time);
            }
        }
        return String.join("\n", lines);
    }
}
