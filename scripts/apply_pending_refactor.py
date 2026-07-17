# Stage 85 guarded settings, AdBlock, and customize-menu extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
main_wrapper = """    private void showSettingsPanel() {
        SettingsPanelController.MainState state = new SettingsPanelController.MainState(
                dedicatedPrivateProfile, searchEngine, videoControlsEnabled, speedMode,
                safeMode, httpsFirstEnabled, nightModeLabel(), readerMode, adBlock,
                adBlock ? "Perlindungan otomatis tanpa notifikasi yang mengganggu."
                        : "Perlindungan situs sedang nonaktif.",
                dataSaver, desktopMode, textZoom);
        new SettingsPanelController(this, mainHandler).showMain(state, (action, owner) -> {
            switch (action) {
                case TRANSLATE: showTranslateOptionsDialog(); break;
                case DOWNLOAD_SETTINGS: showDownloadSettingsPanel(); break;
                case BOOKMARKS: showBookmarkList(); break;
                case PROFILE:
                    if (dedicatedPrivateProfile) openNormalBrowserSpace();
                    else openPrivateBrowserSpace();
                    break;
                case CUSTOMIZE: showCustomizeMenuPanel(); break;
                case QR_SCAN: openQrScanner(); break;
                case SEARCH_ENGINE: showSearchEngineDialog(); break;
                case BLOCK_ELEMENT: startElementPicker(); break;
                case SITE_FILTER: showUserFiltersManager(); break;
                case HISTORY: showHistoryPanel(); break;
                case FIND_PAGE: showFindInPageDialog(); break;
                case SHARE: shareCurrentPage(); break;
                case COPY_LINK: copyCurrentLink(); break;
                case PAGE_INFO: showPageInfoDialog(); break;
                case FULLSCREEN: toggleFullscreenMode(); break;
                case VIDEO_CONTROLS:
                    videoControlsEnabled = !videoControlsEnabled;
                    updateVideoControlsVisibility();
                    saveSettings();
                    showSettingsPanel();
                    break;
                case VIDEO_OPTIMIZATION: showVideoOptimizationDialog(); break;
                case SAVE_OFFLINE: saveCurrentPageOffline(); break;
                case SPEED_MODE:
                    speedMode = !speedMode;
                    applyBrowserSettings();
                    saveSettings();
                    break;
                case SAFE_MODE: safeMode = !safeMode; saveSettings(); break;
                case HTTPS_FIRST:
                    httpsFirstEnabled = !httpsFirstEnabled;
                    if (!httpsFirstEnabled) clearAllHttpsFirstRuntimeState();
                    saveSettings();
                    break;
                case NIGHT_MODE: showNightModeSettingsDialog(); break;
                case READER_MODE: readerMode = !readerMode; saveSettings(); break;
                case AD_BLOCK_SETTINGS: showAdBlockSettingsDialog(); break;
                case DATA_SAVER:
                    dataSaver = !dataSaver;
                    applyBrowserSettings();
                    saveSettings();
                    break;
                case DESKTOP_MODE: toggleDesktopModeSafely(); break;
                case TEXT_ZOOM: showTextZoomDialog(owner); break;
                case CLEAR_CACHE:
                    if (webView != null) webView.clearCache(true);
                    QuietToast.makeText(this, "Cache dibersihkan",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case ABOUT: showAboutYieldDialog(); break;
            }
        });
    }"""
text = replace_between(
        text,
        "    private void showSettingsPanel() {",
        "    private String getAdBlockSummary() {",
        main_wrapper)

ad_block_wrapper = """    private void showAdBlockSettingsDialog() {
        SettingsPanelController.AdBlockState state = new SettingsPanelController.AdBlockState(
                adBlock, adBlockPopupBlocker, adBlockRedirectBlocker,
                adBlockScriptIframeBlocker, adBlockClickHijackBlocker,
                adBlockRedirectToTempTab, adBlockAutoCloseAdTabs);
        new SettingsPanelController(this, mainHandler).showAdBlock(state, (action, owner) -> {
            switch (action) {
                case MASTER:
                    adBlock = !adBlock;
                    if (!adBlock) stopYouTubeAutoAssistantNow();
                    break;
                case POPUP: adBlockPopupBlocker = !adBlockPopupBlocker; break;
                case REDIRECT: adBlockRedirectBlocker = !adBlockRedirectBlocker; break;
                case SCRIPT_IFRAME:
                    adBlockScriptIframeBlocker = !adBlockScriptIframeBlocker;
                    break;
                case CLICK_HIJACK:
                    adBlockClickHijackBlocker = !adBlockClickHijackBlocker;
                    break;
                case TEMP_TAB: adBlockRedirectToTempTab = !adBlockRedirectToTempTab; break;
                case AUTO_CLOSE: adBlockAutoCloseAdTabs = !adBlockAutoCloseAdTabs; break;
            }
            onShieldSettingsChanged();
        });
    }"""
text = replace_between(
        text,
        "    private String getAdBlockSummary() {",
        "    private void openQrScanner() {",
        ad_block_wrapper)

customize_wrapper = """    private void showCustomizeMenuPanel() {
        SettingsPanelController.CustomizeState state =
                new SettingsPanelController.CustomizeState(
                        topIconReload, topIconBookmark, topIconTranslate,
                        shortcutReloadWebsite, shortcutDownload, shortcutBookmark,
                        shortcutPrivate, shortcutAdBlock, shortcutReader, shortcutNightMode,
                        shortcutQrScan, shortcutHistory, shortcutFindPage, shortcutShare,
                        shortcutFullscreen, shortcutBlockElement, shortcutSiteFilter,
                        shortcutVideoControls);
        new SettingsPanelController(this, mainHandler).showCustomize(state, (action, owner) -> {
            switch (action) {
                case TOP_RELOAD: topIconReload = !topIconReload; updateTopActionStates(); break;
                case TOP_BOOKMARK: topIconBookmark = !topIconBookmark; updateTopActionStates(); break;
                case TOP_TRANSLATE: topIconTranslate = !topIconTranslate; updateTopActionStates(); break;
                case RELOAD: shortcutReloadWebsite = !shortcutReloadWebsite; break;
                case DOWNLOAD: shortcutDownload = !shortcutDownload; break;
                case BOOKMARK: shortcutBookmark = !shortcutBookmark; break;
                case PRIVATE: shortcutPrivate = !shortcutPrivate; break;
                case AD_BLOCK: shortcutAdBlock = !shortcutAdBlock; break;
                case READER: shortcutReader = !shortcutReader; break;
                case NIGHT_MODE: shortcutNightMode = !shortcutNightMode; break;
                case QR_SCAN: shortcutQrScan = !shortcutQrScan; break;
                case HISTORY: shortcutHistory = !shortcutHistory; break;
                case FIND_PAGE: shortcutFindPage = !shortcutFindPage; break;
                case SHARE: shortcutShare = !shortcutShare; break;
                case FULLSCREEN: shortcutFullscreen = !shortcutFullscreen; break;
                case BLOCK_ELEMENT: shortcutBlockElement = !shortcutBlockElement; break;
                case SITE_FILTER: shortcutSiteFilter = !shortcutSiteFilter; break;
                case VIDEO_CONTROLS: shortcutVideoControls = !shortcutVideoControls; break;
            }
            saveSettings();
        });
    }"""
text = replace_between(
        text,
        "    private void showCustomizeMenuPanel() {",
        "private void showDownloadSettingsPanel() {",
        customize_wrapper)
PATH.write_text(text)
