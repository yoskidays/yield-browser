package com.yieldbrowser.app;

import android.app.Activity;
import android.view.View;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.List;

/** Builds status-aware download item menus and dispatches semantic actions. */
final class DownloadItemMenuController {
    enum Action {
        PAUSE,
        RESUME,
        RELOAD,
        PRIORITIZE,
        MOVE_UP,
        MOVE_DOWN,
        PLAY,
        OPEN_EXTERNAL,
        SHARE,
        RENAME,
        REMOVE_HISTORY,
        DELETE_FILE
    }

    interface ActionHandler {
        void handle(Action action);
    }

    private final Activity activity;

    DownloadItemMenuController(Activity activity) {
        this.activity = activity;
    }

    void show(View anchor,
              DownloadItem item,
              boolean playable,
              ActionHandler handler) {
        if (item == null || anchor == null) return;
        PopupMenu menu = new PopupMenu(activity, anchor);
        List<Entry> entries = entriesFor(item.status, playable);
        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            menu.getMenu().add(0, index + 1, index, entry.label);
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            int index = menuItem.getItemId() - 1;
            if (index < 0 || index >= entries.size()) return false;
            if (handler != null) handler.handle(entries.get(index).action);
            return true;
        });
        menu.show();
    }

    static List<Entry> entriesFor(String status, boolean playable) {
        ArrayList<Entry> entries = new ArrayList<>();
        String value = status == null ? "" : status;
        if ("running".equals(value)) {
            entries.add(new Entry(Action.PAUSE, "Jeda / Pause"));
        } else if ("queued".equals(value)) {
            entries.add(new Entry(Action.PRIORITIZE, "Prioritaskan / mulai berikutnya"));
            entries.add(new Entry(Action.MOVE_UP, "Naik antrian"));
            entries.add(new Entry(Action.MOVE_DOWN, "Turun antrian"));
            entries.add(new Entry(Action.PAUSE, "Jeda"));
        } else if ("paused".equals(value)) {
            entries.add(new Entry(Action.RESUME, "Lanjutkan"));
            entries.add(new Entry(Action.RELOAD, "Premium Fast • reload"));
        } else if ("failed".equals(value)) {
            entries.add(new Entry(Action.RELOAD, "Premium Fast • reload"));
        }

        boolean progressive = "running".equals(value)
                || "paused".equals(value)
                || "failed".equals(value)
                || "verifying".equals(value)
                || "saving".equals(value);
        if (playable && progressive) {
            entries.add(new Entry(Action.PLAY, "Putar sambil mengunduh"));
        }
        if ("completed".equals(value)) {
            if (playable) entries.add(new Entry(Action.PLAY, "Tonton di Yield"));
            entries.add(new Entry(Action.OPEN_EXTERNAL, "Buka dengan aplikasi lain"));
            entries.add(new Entry(Action.SHARE, "Bagikan"));
            entries.add(new Entry(Action.RENAME, "Ganti nama"));
        }
        entries.add(new Entry(Action.REMOVE_HISTORY, "Hapus riwayat"));
        entries.add(new Entry(Action.DELETE_FILE, "Hapus file + riwayat"));
        return entries;
    }

    static final class Entry {
        final Action action;
        final String label;

        Entry(Action action, String label) {
            this.action = action;
            this.label = label;
        }
    }
}
