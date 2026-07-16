from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "return DownloadUiItemFactory.build(" in text:
    print("DownloadUiItemFactory delegation already installed")
    raise SystemExit(0)

build_start = "    private DownloadUiItem buildDownloadUiItem(DownloadItem item) {"
progress_start = "    private int calculateUiProgressBasisPoints(DownloadItem item, long downloaded, long total) {"
queue_start = "    private int getDownloadQueuePosition(DownloadItem target) {"

for marker in (build_start, progress_start, queue_start):
    if text.count(marker) != 1:
        raise SystemExit(f"Unexpected MainActivity marker count for: {marker}")

start = text.index(build_start)
progress = text.index(progress_start, start)
queue = text.index(queue_start, progress)

replacement = """    private DownloadUiItem buildDownloadUiItem(DownloadItem item) {
        return DownloadUiItemFactory.build(
                item,
                getDownloadCategory(item),
                getDownloadHost(item),
                getDownloadSize(item),
                canPlayDownloadInsideYield(item),
                getDownloadQueuePosition(item),
                downloadSelectMode,
                selectedDownloadIds.contains(item.id));
    }

"""

updated = text[:start] + replacement + text[queue:]
path.write_text(updated, encoding="utf-8")
print("Download row presentation delegated to DownloadUiItemFactory")
