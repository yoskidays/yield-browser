# Stage 90 guarded Download Manager shell extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
field_marker = "    private Dialog activeDownloadDialog;\n"
field_replacement = field_marker + (
        "    private DownloadManagerShell downloadManagerShell;\n"
        "    private DownloadManagerShell.Bindings activeDownloadBindings;\n")
if field_marker not in text:
    raise SystemExit("Missing Download Manager field marker")
text = text.replace(field_marker, field_replacement, 1)

replacement = """    private void showDownloadManager() {
        if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
            renderDownloadList();
            return;
        }
        downloadManagerShell = new DownloadManagerShell(this);
        activeDownloadBindings = downloadManagerShell.show(
                activeDownloadSection,
                activeDownloadCategory,
                new DownloadListAdapter.Callback() {
                    @Override
                    public void onRowClick(int downloadId, View anchor) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item == null) return;
                        if (downloadSelectMode) {
                            toggleDownloadSelection(item);
                        } else if (canPlayDownloadInsideYield(item)
                                && ("running".equals(item.status)
                                || "paused".equals(item.status)
                                || "failed".equals(item.status)
                                || "completed".equals(item.status)
                                || "verifying".equals(item.status)
                                || "saving".equals(item.status))) {
                            playDownloadInsideYield(item);
                        } else if ("completed".equals(item.status)) {
                            openDownloadedFile(item);
                        } else if ("failed".equals(item.status)
                                || "paused".equals(item.status)
                                || "queued".equals(item.status)
                                || "running".equals(item.status)) {
                            showDownloadItemMenu(anchor, item);
                        }
                    }

                    @Override
                    public boolean onRowLongClick(int downloadId) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item == null) return false;
                        downloadSelectMode = true;
                        toggleDownloadSelection(item);
                        return true;
                    }

                    @Override
                    public void onPrimaryAction(int downloadId) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item != null) handleDownloadPrimaryAction(item);
                    }

                    @Override
                    public void onMore(int downloadId, View anchor) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item != null) showDownloadItemMenu(anchor, item);
                    }
                },
                new DownloadManagerShell.Callback() {
                    @Override
                    public void onSearchRequested() {
                        showDownloadSearchDialog();
                    }

                    @Override
                    public void onSettingsRequested() {
                        showDownloadSettingsPanel();
                    }

                    @Override
                    public void onSectionSelected(String section) {
                        if (section.equals(activeDownloadSection)) return;
                        activeDownloadSection = section;
                        downloadSelectMode = false;
                        selectedDownloadIds.clear();
                        lastDownloadControlsSignature = "";
                        renderDownloadSectionTabs();
                        renderDownloadList();
                    }

                    @Override
                    public void onCategorySelected(String category) {
                        activeDownloadCategory = category;
                        selectedDownloadIds.clear();
                        downloadSelectMode = false;
                        lastDownloadControlsSignature = "";
                        renderDownloadCategoryChips();
                        renderDownloadList();
                    }

                    @Override
                    public void onDismissed() {
                        clearDownloadManagerBindings();
                    }
                });
        activeDownloadDialog = activeDownloadBindings.dialog;
        activeDownloadRecyclerView = activeDownloadBindings.recyclerView;
        activeDownloadAdapter = activeDownloadBindings.adapter;
        activeDownloadCategoryPanel = activeDownloadBindings.categoryPanel;
        activeDownloadControlsPanel = activeDownloadBindings.controlsPanel;
        activeDownloadTitleView = activeDownloadBindings.titleView;
        activeDownloadStorageView = activeDownloadBindings.storageView;
        activeDownloadRunningTab = activeDownloadBindings.runningTab;
        activeDownloadCompletedTab = activeDownloadBindings.completedTab;
        activeDownloadEmptyView = activeDownloadBindings.emptyView;

        renderDownloadList();
        downloadUiTickerRunning = true;
        mainHandler.removeCallbacks(downloadUiTicker);
        mainHandler.postDelayed(downloadUiTicker, getDownloadUiTickerDelayMs());
    }

    private void clearDownloadManagerBindings() {
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacks(downloadUiTicker);
        activeDownloadDialog = null;
        activeDownloadRecyclerView = null;
        activeDownloadAdapter = null;
        activeDownloadCategoryPanel = null;
        activeDownloadControlsPanel = null;
        activeDownloadTitleView = null;
        activeDownloadStorageView = null;
        activeDownloadRunningTab = null;
        activeDownloadCompletedTab = null;
        activeDownloadEmptyView = null;
        activeDownloadBindings = null;
        downloadSelectMode = false;
        selectedDownloadIds.clear();
        lastDownloadControlsSignature = "";
    }

    private void renderDownloadSectionTabs() {
        if (downloadManagerShell != null) {
            downloadManagerShell.styleSectionTabs(activeDownloadBindings, activeDownloadSection);
        }
    }

    private void renderDownloadCategoryChips() {
        if (downloadManagerShell == null) return;
        downloadManagerShell.renderCategories(
                activeDownloadBindings,
                activeDownloadCategory,
                new DownloadManagerShell.Callback() {
                    @Override
                    public void onSearchRequested() {
                    }

                    @Override
                    public void onSettingsRequested() {
                    }

                    @Override
                    public void onSectionSelected(String section) {
                    }

                    @Override
                    public void onCategorySelected(String category) {
                        activeDownloadCategory = category;
                        selectedDownloadIds.clear();
                        downloadSelectMode = false;
                        lastDownloadControlsSignature = "";
                        renderDownloadCategoryChips();
                        renderDownloadList();
                    }

                    @Override
                    public void onDismissed() {
                    }
                });
    }

    private void showDownloadSearchDialog() {
        if (downloadManagerShell == null) downloadManagerShell = new DownloadManagerShell(this);
        downloadManagerShell.showSearchDialog(activeDownloadSearchQuery, query -> {
            activeDownloadSearchQuery = query;
            renderDownloadList();
        });
    }

    private void showDownloadSortDialog() {
        if (downloadManagerShell == null) downloadManagerShell = new DownloadManagerShell(this);
        downloadManagerShell.showSortDialog(activeDownloadSort, sort -> {
            activeDownloadSort = sort;
            lastDownloadControlsSignature = "";
            renderDownloadList();
        });
    }"""
text = replace_between(
        text,
        "    private void showDownloadManager() {",
        "    private void renderDownloadList() {",
        replacement)
PATH.write_text(text)
