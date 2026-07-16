from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "DownloadPanelPresentation.emptyMessage(" in text:
    print("Download panel presentation delegation already installed")
    raise SystemExit(0)

old_empty = '''            activeDownloadEmptyView.setText(activeDownloadSearchQuery == null || activeDownloadSearchQuery.isEmpty()
                    ? ("Selesai".equals(activeDownloadSection)
                        ? "Belum ada file yang selesai diunduh."
                        : "Belum ada unduhan aktif atau tertunda.")
                    : "Tidak ada unduhan yang cocok dengan pencarian.");'''
new_empty = '''            activeDownloadEmptyView.setText(DownloadPanelPresentation.emptyMessage(
                    activeDownloadSection, activeDownloadSearchQuery));'''

old_title = '''            activeDownloadTitleView.setText(downloadSelectMode
                    ? selectedDownloadIds.size() + " dipilih" : "Unduhan");'''
new_title = '''            activeDownloadTitleView.setText(DownloadPanelPresentation.title(
                    downloadSelectMode, selectedDownloadIds.size()));'''

old_storage = '''            String storageText = cachedDownloadStorageText;
            if (finalizing) storageText += "  •  sedang menyimpan file besar";
            activeDownloadStorageView.setText(storageText
                    + "  •  aktif " + active + "  •  antri " + queued);'''
new_storage = '''            activeDownloadStorageView.setText(DownloadPanelPresentation.storageSummary(
                    cachedDownloadStorageText, finalizing, active, queued));'''

old_signature = '''        String signature = activeDownloadSection + "|" + activeDownloadSort + "|"
                + downloadSelectMode + "|" + selectedDownloadIds.size() + "|"
                + countActiveDownloads() + "|" + countQueuedDownloads() + "|"
                + downloadQueuePaused + "|" + downloadMaxActive + "|"
                + countCompletedDownloadHistory();'''
new_signature = '''        String signature = DownloadPanelPresentation.controlsSignature(
                activeDownloadSection,
                activeDownloadSort,
                downloadSelectMode,
                selectedDownloadIds.size(),
                countActiveDownloads(),
                countQueuedDownloads(),
                downloadQueuePaused,
                downloadMaxActive,
                countCompletedDownloadHistory());'''

for old in (old_empty, old_title, old_storage, old_signature):
    if text.count(old) != 1:
        raise SystemExit("MainActivity panel block changed; refusing unsafe refactor")

text = text.replace(old_empty, new_empty)
text = text.replace(old_title, new_title)
text = text.replace(old_storage, new_storage)
text = text.replace(old_signature, new_signature)
path.write_text(text, encoding="utf-8")
print("Download panel text and render-key logic delegated")
