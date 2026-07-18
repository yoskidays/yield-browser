# Stage 93 guarded Download Manager presenter and item-menu extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    depth = 0
    end = -1
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise SystemExit(f"Unbalanced method: {signature}")
    return source[:start] + replacement + source[end:]


text = PATH.read_text()
old_fields = """    private final Object downloadHistoryLock = new Object();
    private String lastDownloadControlsSignature = "";
    private long lastDownloadStorageUiMs;
    private String cachedDownloadStorageText = "Penyimpanan tersedia";"""
new_fields = """    private final Object downloadHistoryLock = new Object();
    private DownloadPanelPresenter downloadPanelPresenter;
    private DownloadItemMenuController downloadItemMenuController;"""
if old_fields not in text:
    raise SystemExit("Missing Download Manager presenter fields")
text = text.replace(old_fields, new_fields, 1)
text = text.replace("lastDownloadControlsSignature = \"\";", "invalidateDownloadControls();")

render = """    private void ensureDownloadPanelPresenter() {
        if (downloadPanelPresenter != null) return;
        downloadPanelPresenter = new DownloadPanelPresenter(
                this,
                downloadItems,
                new DownloadPanelPresenter.Host() {
                    @Override
                    public boolean canPlay(DownloadItem item) {
                        return canPlayDownloadInsideYield(item);
                    }

                    @Override
                    public boolean hasFinalizingDownload() {
                        return MainActivity.this.hasFinalizingDownload();
                    }

                    @Override
                    public String storageUsageText() {
                        return getStorageUsageText();
                    }

                    @Override
                    public int activeCount() {
                        return countActiveDownloads();
                    }

                    @Override
                    public int queuedCount() {
                        return countQueuedDownloads();
                    }

                    @Override
                    public int completedHistoryCount() {
                        return countCompletedDownloadHistory();
                    }

                    @Override
                    public void showSort() {
                        showDownloadSortDialog();
                    }

                    @Override
                    public void toggleSelectMode() {
                        downloadSelectMode = !downloadSelectMode;
                        selectedDownloadIds.clear();
                        invalidateDownloadControls();
                        renderDownloadList();
                    }

                    @Override
                    public void clearCompletedHistory() {
                        confirmClearCompletedDownloadHistory();
                    }

                    @Override
                    public void shareSelected() {
                        shareSelectedDownloads();
                    }

                    @Override
                    public void deleteSelected() {
                        deleteSelectedDownloads();
                    }

                    @Override
                    public void pauseAll() {
                        pauseAllDownloads();
                    }

                    @Override
                    public void resumeAll() {
                        resumeAllDownloads();
                    }

                    @Override
                    public void showQueueSettings() {
                        showDownloadQueueSettingsDialog();
                    }
                });
    }

    private void invalidateDownloadControls() {
        if (downloadPanelPresenter != null) downloadPanelPresenter.invalidateControls();
    }

    private void renderDownloadList() {
        if (activeDownloadBindings == null) return;
        ensureDownloadPanelPresenter();
        downloadPanelPresenter.render(
                activeDownloadBindings,
                new DownloadPanelPresenter.State(
                        activeDownloadSection,
                        activeDownloadCategory,
                        activeDownloadSearchQuery,
                        activeDownloadSort,
                        downloadSelectMode,
                        selectedDownloadIds,
                        downloadQueuePaused,
                        downloadMaxActive));
    }"""
text = replace_method(text, "    private void renderDownloadList() {", render)
for signature in [
    "    private void renderDownloadControlsIfNeeded() {",
    "    private View downloadToolRow() {",
    "    private View downloadQueueControlRow() {",
    "    private TextView downloadToolButton(String text) {",
    "    private ArrayList<DownloadItem> getFilteredDownloadItems() {",
    "    private DownloadUiItem buildDownloadUiItem(DownloadItem item) {",
    "    private int getDownloadQueuePosition(DownloadItem target) {",
]:
    text = replace_method(text, signature, "")

menu = """    private void showDownloadItemMenu(View anchor, DownloadItem item) {
        if (downloadItemMenuController == null) {
            downloadItemMenuController = new DownloadItemMenuController(this);
        }
        downloadItemMenuController.show(
                anchor,
                item,
                canPlayDownloadInsideYield(item),
                action -> {
                    switch (action) {
                        case PAUSE: pauseDownloadItem(item); break;
                        case RESUME: resumeDownloadItem(item); break;
                        case RELOAD: reloadDownloadItem(item); break;
                        case PRIORITIZE: prioritizeQueuedDownload(item, true); break;
                        case MOVE_UP: moveQueuedDownload(item, -1); break;
                        case MOVE_DOWN: moveQueuedDownload(item, 1); break;
                        case PLAY: playDownloadInsideYield(item); break;
                        case OPEN_EXTERNAL: openDownloadedFile(item); break;
                        case SHARE: shareDownloadedFile(item); break;
                        case RENAME: renameDownloadedFile(item); break;
                        case REMOVE_HISTORY: removeDownloadItem(item, false); break;
                        case DELETE_FILE: removeDownloadItem(item, true); break;
                    }
                });
    }"""
text = replace_method(text, "    private void showDownloadItemMenu(View anchor, DownloadItem item) {", menu)
PATH.write_text(text)
