# Stage 86 guarded download settings extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
wrapper = """    private void showDownloadSettingsPanel() {
        new DownloadSettingsController(this, new DownloadSettingsController.Host() {
            @Override
            public DownloadSettingsController.State state() {
                return new DownloadSettingsController.State(
                        getDownloadLocationText(),
                        downloadQueueEnabled,
                        downloadMaxActive,
                        downloadDynamic4Connections,
                        downloadAutoRetry,
                        downloadHlsEnabled,
                        downloadPlayWhileDownloadingEnabled,
                        downloadSpeedLimitKBps);
            }

            @Override
            public String queueSummary() {
                return getDownloadQueueSummary();
            }

            @Override
            public void handle(DownloadSettingsController.Action action,
                               int value,
                               Dialog ownerDialog) {
                switch (action) {
                    case OPEN_MANAGER:
                        showDownloadManager();
                        break;
                    case CHOOSE_FOLDER:
                        showDownloadFolderDialog(ownerDialog);
                        break;
                    case CLEAR_COMPLETED:
                        synchronized (downloadItems) {
                            for (int i = downloadItems.size() - 1; i >= 0; i--) {
                                if (!"running".equals(downloadItems.get(i).status)) {
                                    downloadItems.remove(i);
                                }
                            }
                        }
                        saveDownloadHistory();
                        QuietToast.makeText(MainActivity.this,
                                "Riwayat unduhan selesai dibersihkan",
                                QuietToast.LENGTH_SHORT).show();
                        break;
                    case TOGGLE_QUEUE:
                        downloadQueueEnabled = !downloadQueueEnabled;
                        downloadQueuePaused = false;
                        saveSettings();
                        pumpDownloadQueue();
                        refreshDownloadPanel();
                        break;
                    case SET_MAX_ACTIVE:
                        downloadMaxActive = value;
                        downloadQueueEnabled = true;
                        downloadQueuePaused = false;
                        saveSettings();
                        pumpDownloadQueue();
                        refreshDownloadPanel();
                        QuietToast.makeText(MainActivity.this,
                                "Maksimal download aktif: " + value,
                                QuietToast.LENGTH_SHORT).show();
                        break;
                    case PAUSE_ALL:
                        pauseAllDownloads();
                        break;
                    case RESUME_ALL:
                        resumeAllDownloads();
                        break;
                    case SORT_QUEUE:
                        activeDownloadSort = "Antrian";
                        renderDownloadList();
                        QuietToast.makeText(MainActivity.this,
                                "Tampilan diurutkan berdasarkan antrian",
                                QuietToast.LENGTH_SHORT).show();
                        break;
                    case TOGGLE_DYNAMIC_CONNECTIONS:
                        downloadDynamic4Connections = !downloadDynamic4Connections;
                        saveSettings();
                        break;
                    case TOGGLE_AUTO_RETRY:
                        downloadAutoRetry = !downloadAutoRetry;
                        saveSettings();
                        break;
                    case TOGGLE_HLS:
                        downloadHlsEnabled = !downloadHlsEnabled;
                        saveSettings();
                        break;
                    case TOGGLE_PLAY_WHILE_DOWNLOADING:
                        downloadPlayWhileDownloadingEnabled =
                                !downloadPlayWhileDownloadingEnabled;
                        saveSettings();
                        break;
                    case SET_SPEED_LIMIT:
                        downloadSpeedLimitKBps = value;
                        saveSettings();
                        QuietToast.makeText(MainActivity.this,
                                "Speed limiter: "
                                        + DownloadSettingsController.speedLabel(value),
                                QuietToast.LENGTH_SHORT).show();
                        break;
                }
            }
        }).showMain();
    }

    private String getDownloadQueueSummary() {
        return "Maks aktif: " + downloadMaxActive
                + " • aktif: " + countActiveDownloads()
                + " • antri: " + countQueuedDownloads();
    }"""
text = replace_between(
        text,
        "private void showDownloadSettingsPanel() {",
        "    private void showDownloadQueueSettingsDialog() {",
        wrapper)
text = replace_between(
        text,
        "    private void showAdvancedDownloadFeaturesDialog(Dialog parentDialog) {",
        "    private void showVideoControlsIfAllowed() {",
        "")
PATH.write_text(text)
