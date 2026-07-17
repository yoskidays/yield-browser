# Stage 84 guarded quick-menu and About Yield extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
quick_menu = """    private void showQuickMenu() {
        QuickMenuController.State state = new QuickMenuController.State(
                shortcutDownload, shortcutBookmark, shortcutPrivate, shortcutAdBlock,
                shortcutReader, shortcutNightMode, shortcutQrScan, shortcutHistory,
                shortcutFindPage, shortcutShare, shortcutFullscreen, shortcutVideoControls,
                shortcutReloadWebsite, shortcutBlockElement, shortcutSiteFilter,
                dedicatedPrivateProfile, adBlock, readerMode, videoControlsEnabled,
                nightModeLabel());
        new QuickMenuController(this, mainHandler, state, action -> {
            switch (action) {
                case DOWNLOADS: showDownloadManager(); break;
                case BOOKMARKS: showBookmarkList(); break;
                case PROFILE:
                    if (dedicatedPrivateProfile) openNormalBrowserSpace();
                    else openPrivateBrowserSpace();
                    break;
                case AD_BLOCK:
                    adBlock = !adBlock;
                    if (!adBlock) stopYouTubeAutoAssistantNow();
                    onShieldSettingsChanged();
                    break;
                case READER:
                    readerMode = !readerMode;
                    saveSettings();
                    QuietToast.makeText(this,
                            readerMode ? "Reader mode aktif" : "Reader mode nonaktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case NIGHT_MODE: showNightModeSettingsDialog(); break;
                case QR_SCAN: openQrScanner(); break;
                case HISTORY: showHistoryPanel(); break;
                case FIND_PAGE: showFindInPageDialog(); break;
                case SHARE: shareCurrentPage(); break;
                case FULLSCREEN: toggleFullscreenMode(); break;
                case VIDEO_CONTROLS:
                    videoControlsEnabled = !videoControlsEnabled;
                    saveSettings();
                    updateVideoControlsVisibility();
                    QuietToast.makeText(this,
                            videoControlsEnabled ? "Kontrol video aktif" : "Kontrol video nonaktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case RELOAD: reloadCurrentWebsite(); break;
                case BLOCK_ELEMENT: startElementPicker(); break;
                case SITE_FILTER: showUserFiltersManager(); break;
                case SETTINGS: showSettingsPanel(); break;
                case CUSTOMIZE: showCustomizeMenuPanel(); break;
                case EXIT:
                    try {
                        if (!dedicatedPrivateProfile) {
                            recordCurrentPageToHistory();
                            recordWebViewBackForwardHistory();
                            saveBrowserHistory();
                        }
                    } catch (Exception ignored) {
                    }
                    discardPrivateTabsForExplicitExit();
                    if (dedicatedPrivateProfile) launchNormalProfile(false, false);
                    finish();
                    break;
            }
        }).show();
    }"""

text = replace_between(
        text,
        "    private void showQuickMenu() {",
        "    private String getAppVersionName() {",
        quick_menu)
about_wrapper = """    private void showAboutYieldDialog() {
        QuickMenuController.showAbout(this);
    }"""
text = replace_between(
        text,
        "    private String getAppVersionName() {",
        "    private void showSettingsPanel() {",
        about_wrapper)
PATH.write_text(text)
