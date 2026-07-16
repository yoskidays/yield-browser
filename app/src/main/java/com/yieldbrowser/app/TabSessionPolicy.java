package com.yieldbrowser.app;

import java.util.List;

/** Pure policy helpers for deciding which tabs may survive process recreation. */
final class TabSessionPolicy {
    private TabSessionPolicy() {
        // Utility class.
    }

    static boolean shouldPersist(boolean closed, boolean privateTab, boolean adTab) {
        return !closed && !privateTab && !adTab;
    }

    static boolean shouldRestore(boolean privateTab, boolean adTab) {
        return !privateTab && !adTab;
    }

    /** Returns the nearest persistable index, preferring the current item and then the left side. */
    static int nearestPersistableIndex(boolean[] persistable, int currentIndex) {
        if (persistable == null || persistable.length == 0) return -1;
        int start = Math.max(0, Math.min(currentIndex, persistable.length - 1));
        if (persistable[start]) return start;
        for (int distance = 1; distance < persistable.length; distance++) {
            int left = start - distance;
            if (left >= 0 && persistable[left]) return left;
            int right = start + distance;
            if (right < persistable.length && persistable[right]) return right;
        }
        return -1;
    }

    /**
     * Maps a serialized selection to the filtered restore list. The closest surviving row at or
     * before the requested row wins; if none exists, the first surviving row after it is selected.
     */
    static int restoredSelectionIndex(List<Integer> restoredSourceIndexes, int requestedIndex) {
        if (restoredSourceIndexes == null || restoredSourceIndexes.isEmpty()) return -1;
        int atOrBefore = -1;
        int firstAfter = -1;
        for (int restoredIndex = 0; restoredIndex < restoredSourceIndexes.size(); restoredIndex++) {
            Integer source = restoredSourceIndexes.get(restoredIndex);
            if (source == null) continue;
            if (source <= requestedIndex) atOrBefore = restoredIndex;
            else if (firstAfter < 0) firstAfter = restoredIndex;
        }
        return atOrBefore >= 0 ? atOrBefore : firstAfter;
    }

    static int persistedSelectionIndex(int requestedIndex, int savedCount) {
        if (savedCount <= 0) return 0;
        return Math.max(0, Math.min(requestedIndex, savedCount - 1));
    }
}
