package com.yieldbrowser.app;

/** Pure policy helpers for deciding which tabs may survive process recreation. */
final class TabSessionPolicy {
    private TabSessionPolicy() {
        // Utility class.
    }

    static boolean shouldPersist(boolean closed, boolean privateTab, boolean adTab) {
        return !closed && !privateTab && !adTab;
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
}
