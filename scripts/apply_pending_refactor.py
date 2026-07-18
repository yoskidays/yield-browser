# Stage 87 guarded QR, search-engine, and video-optimization extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
utility_wrappers = """    private void showQrScannerDialog() {
        BrowserUtilityDialogs.showQrScanner(this, result -> {
            String value = BrowserUtilityDialogs.normalizeQrValue(result);
            if (value.length() == 0) {
                QuietToast.makeText(this, "QR kosong", QuietToast.LENGTH_SHORT).show();
                return;
            }
            QuietToast.makeText(this, "QR terbaca", QuietToast.LENGTH_SHORT).show();
            addressBar.setText(value);
            openAddressBarUrl();
        });
    }

    private void showSearchEngineDialog() {
        BrowserUtilityDialogs.showSearchEngine(this, searchEngine, selected -> {
            searchEngine = selected;
            saveSettings();
            QuietToast.makeText(this,
                    "Search engine: " + searchEngine,
                    QuietToast.LENGTH_SHORT).show();
        });
    }"""
text = replace_between(
        text,
        "    private void showQrScannerDialog() {",
        "    private void showCustomizeMenuPanel() {",
        utility_wrappers)

video_wrapper = """    private void showVideoOptimizationDialog() {
        VideoOptimizationDialogController.State state =
                new VideoOptimizationDialogController.State(
                        videoBufferBooster,
                        hlsSegmentPrefetch,
                        videoFloatingPlayer,
                        videoBackgroundPlay);
        new VideoOptimizationDialogController(this).show(state, action -> {
            switch (action) {
                case BUFFER_BOOSTER:
                    videoBufferBooster = !videoBufferBooster;
                    applyBrowserSettings();
                    injectVideoOptimizationIfNeeded();
                    saveSettings();
                    break;
                case HLS_PREFETCH:
                    hlsSegmentPrefetch = !hlsSegmentPrefetch;
                    injectVideoOptimizationIfNeeded();
                    saveSettings();
                    break;
                case MINIMIZE_NORMAL:
                    videoFloatingPlayer = false;
                    saveSettings();
                    QuietToast.makeText(this,
                            "Minimize normal aktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case BACKGROUND_PLAY:
                    videoBackgroundPlay = !videoBackgroundPlay;
                    applyBrowserSettings();
                    injectVideoOptimizationIfNeeded();
                    saveSettings();
                    break;
            }
        });
    }

    private View videoOptSwitchRow(String title,
                                   String desc,
                                   boolean enabled,
                                   View.OnClickListener listener) {
        return SettingsUi.videoOptSwitchRow(this, title, desc, enabled, listener);
    }"""
text = replace_between(
        text,
        "    private void showVideoOptimizationDialog() {",
        "    private void detectVideoQualities() {",
        video_wrapper)
PATH.write_text(text)
