# Stage 88 guarded tabs-panel extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
wrapper = """    private void showTabsPanel() {
        TabInfo current = getCurrentTab();
        boolean defaultPrivate = BrowserSpacePolicy.isPrivateSpace(
                dedicatedPrivateProfile,
                current != null && current.privateTab,
                Build.VERSION.SDK_INT);
        showTabsPanelForSpace(defaultPrivate);
    }

    private void showTabsPanelForSpace(boolean privateSpace) {
        if (BrowserSpacePolicy.mustOpenOtherProcess(
                privateSpace, dedicatedPrivateProfile, Build.VERSION.SDK_INT)) {
            if (privateSpace) launchDedicatedPrivateProfile(true);
            else launchNormalProfile(true, false);
            return;
        }
        saveCurrentTabState();
        new TabsPanelController(this, tabs, getCurrentTab(), new TabsPanelController.Host() {
            @Override
            public void selectSpace(boolean selectedPrivateSpace) {
                showTabsPanelForSpace(selectedPrivateSpace);
            }

            @Override
            public void createTab(boolean selectedPrivateSpace) {
                if (selectedPrivateSpace) {
                    if (dedicatedPrivateProfile) newTabInCurrentProfile();
                    else newPrivateTab();
                } else if (dedicatedPrivateProfile) {
                    launchNormalProfile(false, true);
                } else {
                    newTabInCurrentProfile();
                }
            }

            @Override
            public void selectTab(TabInfo tab) {
                switchToTab(tab);
            }

            @Override
            public void closeTab(TabInfo tab, boolean selectedPrivateSpace) {
                closeTab(tab);
            }

            @Override
            public boolean isActivityFinishing() {
                return isFinishing();
            }
        }).show(privateSpace);
    }"""
text = replace_between(
        text,
        "    private void showTabsPanel() {",
        "    private void showQuickMenu() {",
        wrapper)
PATH.write_text(text)
