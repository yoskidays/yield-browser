package com.yieldbrowser.app;

import java.util.List;

/** Pure index and back/forward decisions for browser tabs. */
final class TabNavigationPolicy {
    enum BackAction {
        RESTORE_PAGE,
        WEB_BACK,
        SHOW_HOME
    }

    enum ForwardAction {
        WEB_FORWARD,
        RESTORE_AND_FORWARD,
        RESTORE_PAGE,
        NONE
    }

    private TabNavigationPolicy() {
    }

    static int clampIndex(int requestedIndex, int size) {
        if (size <= 0) return 0;
        return Math.max(0, Math.min(requestedIndex, size - 1));
    }

    static int countForUi(int size) {
        return Math.max(1, size);
    }

    static boolean isValidIndex(int index, int size) {
        return index >= 0 && index < size;
    }

    static boolean changesTab(int requestedIndex, int currentIndex) {
        return requestedIndex != currentIndex;
    }

    static boolean shouldSaveBeforeSwitch(boolean skipNextSave) {
        return !skipNextSave;
    }

    static boolean isCurrentTab(List<?> tabs,
                                int currentIndex,
                                Object candidate,
                                boolean candidateClosed) {
        return candidate != null
                && !candidateClosed
                && tabs != null
                && isValidIndex(currentIndex, tabs.size())
                && tabs.get(currentIndex) == candidate;
    }

    static int indexAfterClosingCurrent(int removedIndex,
                                        int replacementIndex,
                                        int sizeAfterRemoval) {
        if (sizeAfterRemoval <= 0) return 0;
        if (isValidIndex(replacementIndex, sizeAfterRemoval)) return replacementIndex;
        return clampIndex(removedIndex, sizeAfterRemoval);
    }

    static int indexAfterRemovingTab(int currentIndex,
                                     int removedIndex,
                                     int sizeAfterRemoval) {
        if (sizeAfterRemoval <= 0) return 0;
        int adjusted = currentIndex > removedIndex ? currentIndex - 1 : currentIndex;
        return clampIndex(adjusted, sizeAfterRemoval);
    }

    static int indexAfterClosingAdTab(int currentIndex,
                                      int removedIndex,
                                      int fallbackIndex,
                                      int sizeAfterRemoval,
                                      boolean closingCurrent) {
        if (sizeAfterRemoval <= 0) return 0;
        if (closingCurrent) return clampIndex(fallbackIndex, sizeAfterRemoval);
        return indexAfterRemovingTab(currentIndex, removedIndex, sizeAfterRemoval);
    }

    static int indexAfterDetectedAdRemoval(int currentIndex,
                                           int removedIndex,
                                           int sizeAfterRemoval,
                                           boolean closingCurrent) {
        if (sizeAfterRemoval <= 0) return 0;
        if (closingCurrent || currentIndex > removedIndex) {
            return clampIndex(currentIndex - 1, sizeAfterRemoval);
        }
        return clampIndex(currentIndex, sizeAfterRemoval);
    }

    static boolean shouldShowPage(String url, boolean homeVisible) {
        return url != null && !url.isEmpty() && !homeVisible;
    }

    static BackAction backAction(boolean homeVisible,
                                 boolean webVisible,
                                 boolean canGoBack) {
        if (homeVisible) return BackAction.RESTORE_PAGE;
        if (webVisible && canGoBack) return BackAction.WEB_BACK;
        return BackAction.SHOW_HOME;
    }

    static ForwardAction forwardAction(boolean homeVisible,
                                       boolean webVisible,
                                       boolean canGoForward) {
        if (webVisible && canGoForward) return ForwardAction.WEB_FORWARD;
        if (homeVisible && canGoForward) return ForwardAction.RESTORE_AND_FORWARD;
        if (homeVisible) return ForwardAction.RESTORE_PAGE;
        return ForwardAction.NONE;
    }
}
