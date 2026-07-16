from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "DownloadControlsFactory.createToolRow(" in text:
    print("Download controls delegation already installed")
    raise SystemExit(0)

start_marker = "    private View downloadToolRow() {"
end_marker = "    private ArrayList<DownloadItem> getFilteredDownloadItems() {"

if text.count(start_marker) != 1 or text.count(end_marker) != 1:
    raise SystemExit("MainActivity download controls markers changed; refusing unsafe refactor")

start = text.index(start_marker)
end = text.index(end_marker, start)
replacement = """    private View downloadToolRow() {
        return DownloadControlsFactory.createToolRow(
                this,
                activeDownloadSort,
                downloadSelectMode,
                activeDownloadSection,
                countCompletedDownloadHistory(),
                this::showDownloadSortDialog,
                () -> {
                    downloadSelectMode = !downloadSelectMode;
                    selectedDownloadIds.clear();
                    lastDownloadControlsSignature = "";
                    renderDownloadList();
                },
                this::confirmClearCompletedDownloadHistory,
                this::shareSelectedDownloads,
                this::deleteSelectedDownloads);
    }

    private View downloadQueueControlRow() {
        return DownloadControlsFactory.createQueueRow(
                this,
                countActiveDownloads(),
                downloadMaxActive,
                countQueuedDownloads(),
                this::pauseAllDownloads,
                this::resumeAllDownloads,
                this::showDownloadQueueSettingsDialog);
    }

    private TextView downloadToolButton(String text) {
        return DownloadControlsFactory.createButton(this, text);
    }

"""

path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")
print("Download controls UI delegated to DownloadControlsFactory")
