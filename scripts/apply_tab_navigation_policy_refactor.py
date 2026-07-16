from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "TabNavigationPolicy.backAction(" in text:
    print("Tab navigation policy delegation already installed")
    raise SystemExit(0)

replacements = [
    (
        """    private void ensureDefaultTab() {
        if (tabs.isEmpty()) {
            tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
        }
        currentTabIndex = Math.max(0, Math.min(currentTabIndex, tabs.size() - 1));
        tabCount = tabs.size();
    }
""",
        """    private void ensureDefaultTab() {
        if (tabs.isEmpty()) {
            tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
        }
        currentTabIndex = TabNavigationPolicy.clampIndex(currentTabIndex, tabs.size());
        tabCount = TabNavigationPolicy.countForUi(tabs.size());
    }
"""
    ),
    (
        """    private boolean isCurrentTabInfo(TabInfo tab) {
        try {
            return tab != null && !tab.closed && !tabs.isEmpty() && currentTabIndex >= 0
                    && currentTabIndex < tabs.size() && tabs.get(currentTabIndex) == tab;
        } catch (Exception e) {
            return false;
        }
    }
""",
        """    private boolean isCurrentTabInfo(TabInfo tab) {
        try {
            return TabNavigationPolicy.isCurrentTab(
                    tabs, currentTabIndex, tab, tab != null && tab.closed);
        } catch (Exception e) {
            return false;
        }
    }
"""
    ),
    (
        """    private void updateTabsCountUi() {
        tabCount = Math.max(1, tabs.size());
        if (tabsCountText != null) {
            tabsCountText.setText(String.valueOf(tabCount));
        }
    }
""",
        """    private void updateTabsCountUi() {
        tabCount = TabNavigationPolicy.countForUi(tabs.size());
        if (tabsCountText != null) {
            tabsCountText.setText(String.valueOf(tabCount));
        }
    }
"""
    ),
    (
        """    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        boolean changingTab = index != currentTabIndex;
        if (changingTab && elementPickerActive) finishElementPicker(false);
        boolean saveBeforeSwitch = !skipNextSwitchTabStateSave;
""",
        """    private void switchToTab(int index) {
        if (!TabNavigationPolicy.isValidIndex(index, tabs.size())) return;
        boolean changingTab = TabNavigationPolicy.changesTab(index, currentTabIndex);
        if (changingTab && elementPickerActive) finishElementPicker(false);
        boolean saveBeforeSwitch = TabNavigationPolicy.shouldSaveBeforeSwitch(
                skipNextSwitchTabStateSave);
"""
    ),
    (
        """            int replacementIndex = tabs.indexOf(replacement);
            currentTabIndex = replacementIndex >= 0
                    ? replacementIndex : Math.max(0, Math.min(index, tabs.size() - 1));
""",
        """            int replacementIndex = tabs.indexOf(replacement);
            currentTabIndex = TabNavigationPolicy.indexAfterClosingCurrent(
                    index, replacementIndex, tabs.size());
"""
    ),
    (
        """                boolean showPage = active.url != null && !active.url.isEmpty()
                        && homeScroll != null && homeScroll.getVisibility() != View.VISIBLE;
""",
        """                boolean showPage = TabNavigationPolicy.shouldShowPage(
                        active.url,
                        homeScroll != null && homeScroll.getVisibility() == View.VISIBLE);
"""
    ),
    (
        """                if (currentTabIndex > index) currentTabIndex--;
                if (currentTabIndex >= tabs.size()) currentTabIndex = tabs.size() - 1;
                if (closingCurrent) {
                    currentTabIndex = Math.max(0, Math.min(fallbackIndex, tabs.size() - 1));
                    skipNextSwitchTabStateSave = true;
                    switchToTab(currentTabIndex);
                }
""",
        """                currentTabIndex = TabNavigationPolicy.indexAfterClosingAdTab(
                        currentTabIndex, index, fallbackIndex, tabs.size(), closingCurrent);
                if (closingCurrent) {
                    skipNextSwitchTabStateSave = true;
                    switchToTab(currentTabIndex);
                }
"""
    ),
    (
        """                if (closingCurrent) {
                    currentTabIndex = Math.max(0, Math.min(currentTabIndex - 1, tabs.size() - 1));
                } else if (currentTabIndex > i) {
                    currentTabIndex--;
                }
""",
        """                currentTabIndex = TabNavigationPolicy.indexAfterDetectedAdRemoval(
                        currentTabIndex, i, tabs.size(), closingCurrent);
"""
    ),
    (
        """            } else if (currentTabIndex < 0 || currentTabIndex >= tabs.size()) {
                currentTabIndex = Math.max(0, Math.min(currentTabIndex, tabs.size() - 1));
            }
""",
        """            } else if (!TabNavigationPolicy.isValidIndex(currentTabIndex, tabs.size())) {
                currentTabIndex = TabNavigationPolicy.clampIndex(currentTabIndex, tabs.size());
            }
"""
    ),
    (
        """    private void navigateSwipeBack() {
        try {
            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) {
                restoreHiddenWebPage();
                return;
            }

            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
                webView.goBack();
                return;
            }

            showHome();
        } catch (Exception ignored) {
        }
    }
""",
        """    private void navigateSwipeBack() {
        try {
            boolean homeVisible = homeScroll != null
                    && homeScroll.getVisibility() == View.VISIBLE;
            boolean webVisible = webView != null
                    && webView.getVisibility() == View.VISIBLE;
            boolean canGoBack = webView != null && webView.canGoBack();
            TabNavigationPolicy.BackAction action = TabNavigationPolicy.backAction(
                    homeVisible, webVisible, canGoBack);
            if (action == TabNavigationPolicy.BackAction.RESTORE_PAGE) {
                restoreHiddenWebPage();
            } else if (action == TabNavigationPolicy.BackAction.WEB_BACK) {
                webView.goBack();
            } else {
                showHome();
            }
        } catch (Exception ignored) {
        }
    }
"""
    ),
    (
        """    private void navigateSwipeForward() {
        try {
            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoForward()) {
                webView.goForward();
                return;
            }

            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) {
                if (webView != null && webView.canGoForward()) {
                    restoreHiddenWebPage();
                    webView.goForward();
                } else {
                    restoreHiddenWebPage();
                }
            }
        } catch (Exception ignored) {
        }
    }
""",
        """    private void navigateSwipeForward() {
        try {
            boolean homeVisible = homeScroll != null
                    && homeScroll.getVisibility() == View.VISIBLE;
            boolean webVisible = webView != null
                    && webView.getVisibility() == View.VISIBLE;
            boolean canGoForward = webView != null && webView.canGoForward();
            TabNavigationPolicy.ForwardAction action = TabNavigationPolicy.forwardAction(
                    homeVisible, webVisible, canGoForward);
            if (action == TabNavigationPolicy.ForwardAction.WEB_FORWARD) {
                webView.goForward();
            } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_AND_FORWARD) {
                restoreHiddenWebPage();
                webView.goForward();
            } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_PAGE) {
                restoreHiddenWebPage();
            }
        } catch (Exception ignored) {
        }
    }
"""
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(
            "Expected exactly one guarded tab-navigation block, found " + str(count)
        )
    text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
print("MainActivity tab index and swipe decisions delegated to TabNavigationPolicy")
